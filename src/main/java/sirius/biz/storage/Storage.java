/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.storage;

import com.amazonaws.internal.ResettableInputStream;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import sirius.biz.tenants.Tenant;
import sirius.db.KeyGenerator;
import sirius.db.mixing.OMA;
import sirius.db.mixing.SmartQuery;
import sirius.db.mixing.constraints.FieldOperator;
import sirius.kernel.Sirius;
import sirius.kernel.async.Tasks;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.web.http.WebContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides the main facility for storing and retrieving objects (files) to or from buckets.
 * <p>
 * The object storage is organized in two layers. The virtual layer is completely stored in the database and contains
 * all metadata of buckets and objects.
 * <p>
 * Everytime an object is created or updated, a <b>physical object</b> if created and stored using the {@link
 * PhysicalStorageEngine} of the bucket. Therefore a physical object key which is stored in the virtual objects
 * always contains the same data. If the data changes, the physical key changes but the virtual key remains the same.
 * <p>
 * Using this approach, the generated URLs can be cached without having the worry about this in the data model.
 * <p>
 * For database entities referencing virtual objects a {@link StoredObjectRef} can be used, which takes care of
 * referential integrity (deletes the object, if the entity is deleted etc.)
 */
@Register(classes = Storage.class)
public class Storage {

    public static final Log LOG = Log.get("storage");
    private static final byte[] EMPTY_BUFFER = new byte[0];

    @Part
    private OMA oma;

    @Part
    private KeyGenerator keyGen;

    @Part
    private Tasks tasks;

    @Part
    private GlobalContext context;

    @ConfigValue("storage.sharedSecret")
    private String sharedSecret;
    private String safeSharedSecret;

    private Map<String, BucketInfo> buckets;

    private Map<String, BucketInfo> getBucketMap() {
        if (buckets == null) {
            buckets = loadBuckets();
        }
        return buckets;
    }

    private LinkedHashMap<String, BucketInfo> loadBuckets() {
        return Sirius.getSettings()
                     .getExtensions("storage.buckets")
                     .stream()
                     .map(BucketInfo::new)
                     .collect(Collectors.toMap(BucketInfo::getName,
                                               Function.identity(),
                                               (a, b) -> a,
                                               LinkedHashMap::new));
    }

    /**
     * Returns a list of all known buckets known to the system.
     *
     * @return a collection of all buckets known to the system (declared in <tt>storage.buckets</tt> in the system
     * config).
     */
    public Collection<BucketInfo> getBuckets() {
        return getBucketMap().values();
    }

    /**
     * Returns the bucket infos for the bucket with the given name.
     *
     * @param bucket the name of the bucket to fetch infos for
     * @return the bucket wrapped as optional or an empty optional if the bucket is unknown
     */
    public Optional<BucketInfo> getBucket(String bucket) {
        return Optional.ofNullable(getBucketMap().get(bucket));
    }

    protected PhysicalStorageEngine getStorageEngine(String bucketName) {
        return getBucket(bucketName).map(BucketInfo::getEngine)
                                    .orElseThrow(() -> Exceptions.handle()
                                                                 .withSystemErrorMessage("Unknown storage bucket: %s",
                                                                                         bucketName)
                                                                 .handle());
    }

    /**
     * Tries to find the object with the given id, for the given tenant and bucket.
     *
     * @param tenant the tenant to filter on
     * @param bucket the bucket to search in
     * @param key    the globally unique id to search by
     * @return the object in the given bucket with the given key wrapped as optional or an empty optional if no such
     * object exists.
     */
    public Optional<StoredObject> findByKey(@Nullable Tenant tenant, String bucket, String key) {
        return Optional.ofNullable(oma.select(VirtualObject.class)
                                      .eqIgnoreNull(VirtualObject.TENANT, tenant)
                                      .eq(VirtualObject.BUCKET, bucket)
                                      .eq(VirtualObject.OBJECT_KEY, key)
                                      .queryFirst());
    }

    /**
     * Normalizes the given path to be used in {@link VirtualObject#PATH}
     *
     * @param path the path to cleanup
     * @return the normalized path without \ or // or " "
     */
    public static String normalizePath(String path) {
        if (Strings.isEmpty(path)) {
            return null;
        }
        String normalizedPath = path.trim().replace(" ", "").replace("\\", "/").replaceAll("/+", "/").toLowerCase();
        if (normalizedPath.length() == 0) {
            return null;
        }
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return normalizedPath;
    }

    /**
     * Tries to find the object with the given path, for the given tenant and bucket.
     *
     * @param tenant     the tenant to filter on
     * @param bucketName the bucket to search in
     * @param path       the path used to lookup the object
     * @return the object in the given bucket with the given path wrapped as optional or an empty optional if no such
     * object exists.
     */
    public Optional<StoredObject> findByPath(Tenant tenant, String bucketName, String path) {
        String normalizedPath = normalizePath(path);

        if (normalizedPath == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(findWithNormalizedPath(tenant, bucketName, normalizedPath));
    }

    /**
     * Tries to find the object with the given path, for the given tenant and bucket. If it doesn't exist, it is
     * created.
     *
     * @param tenant     the tenant to filter on
     * @param bucketName the bucket to search in
     * @param path       the path used to lookup the object
     * @return the object in the given bucket with the given path which was either found or newly created
     */
    public StoredObject findOrCreateObjectByPath(Tenant tenant, String bucketName, String path) {
        String normalizedPath = normalizePath(path);

        VirtualObject result = findWithNormalizedPath(tenant, bucketName, normalizedPath);

        if (result == null) {
            result = new VirtualObject();
            result.getTenant().setValue(tenant);
            result.setBucket(bucketName);
            result.setPath(path);
            oma.update(result);
        }

        return result;
    }

    private VirtualObject findWithNormalizedPath(Tenant tenant, String bucketName, String normalizedPath) {
        return oma.select(VirtualObject.class)
                  .eq(VirtualObject.TENANT, tenant)
                  .eq(VirtualObject.BUCKET, bucketName)
                  .eq(VirtualObject.PATH, normalizedPath)
                  .queryFirst();
    }

    protected VirtualObject createObjectWithReference(Tenant tenant, String bucketName, String reference, String name) {
        VirtualObject result = new VirtualObject();
        result.getTenant().setValue(tenant);
        result.setBucket(bucketName);
        result.setReference(reference);
        result.setTemporary(true);
        result.setPath(normalizePath(name));

        oma.update(result);
        return result;
    }

    protected void deleteReferencedObjects(String reference, String excludedObjectKey) {
        if (Strings.isEmpty(reference)) {
            return;
        }
        SmartQuery<VirtualObject> qry = oma.select(VirtualObject.class).eq(VirtualObject.REFERENCE, reference);
        if (Strings.isFilled(excludedObjectKey)) {
            qry.where(FieldOperator.on(VirtualObject.OBJECT_KEY).notEqual(excludedObjectKey));
        }

        qry.delete();
    }

    protected void markAsUsed(String objectKey) {
        try {
            oma.getDatabase()
               .createQuery("UPDATE virtualobject SET temporary = 0 WHERE objectKey=${objectKey}")
               .set("objectKey", objectKey)
               .executeUpdate();
        } catch (SQLException e) {
            Exceptions.handle()
                      .to(LOG)
                      .error(e)
                      .withSystemErrorMessage("An error occured, when marking the object '%s' as used: %s (%s)",
                                              objectKey)
                      .handle();
        }
    }

    /**
     * Computes a base64 representation of the md5 sum of the given file.
     *
     * @param file the file to hash
     * @return a base64 encoded string representing the md5 hash of the given file
     * @throws IOException in case of an IO error
     */
    public String calculateMd5(File file) throws IOException {
        return BaseEncoding.base64().encode(Files.hash(file, Hashing.md5()).asBytes());
    }

    /**
     * Updates the given file using the given data.
     *
     * @param file     the S3 file to update
     * @param data     the data to store
     * @param filename the original filename to save
     */
    public void updateFile(@Nonnull StoredObject file, @Nonnull File data, @Nullable String filename) {
        try {
            try (InputStream in = new ResettableInputStream(data)) {
                String md5 = calculateMd5(data);
                updateFile(file, in, filename, md5, data.length());
            }
        } catch (Exception e) {
            throw Exceptions.handle()
                            .to(LOG)
                            .error(e)
                            .withSystemErrorMessage("Cannot upload the file: %s (%s) - %s (%s)", file, filename)
                            .handle();
        }
    }

    /**
     * Updates the given file based on the given stream.
     *
     * @param file     the S3 file to update
     * @param data     the data to store
     * @param filename the original filename to save
     * @param md5      the md5 hash value to use to verify the upload
     * @param size     the size (number of bytes) being uploaded (must be positive)
     */
    public void updateFile(@Nonnull StoredObject file,
                           @Nonnull InputStream data,
                           @Nullable String filename,
                           @Nullable String md5,
                           long size) {
        VirtualObject object = (VirtualObject) file;
        String newPhysicalKey = keyGen.generateId();
        try {
            PhysicalStorageEngine engine = getStorageEngine(object.getBucket());
            // Store new file
            engine.storePhysicalObject(object.getBucket(), newPhysicalKey, data, md5, size);

            object.setFileSize(size);
            object.setMd5(md5);
            if (Strings.isFilled(filename)) {
                object.setPath(filename);
            }
            object.setPhysicalKey(newPhysicalKey);
            oma.override(object);

            // Delete all (now outdated) versions
            oma.select(VirtualObjectVersion.class).eq(VirtualObjectVersion.VIRTUAL_OBJECT, object).delete();
            // Delete old file
            engine.deletePhysicalObject(object.getBucket(), object.getPhysicalKey());
        } catch (IOException e) {
            throw Exceptions.handle().to(LOG).error(e).withNLSKey("Storage.uploadFailed").handle();
        }
    }

    /**
     * Retrieves the actual data stored by the given object.
     *
     * @param file the object to fetch the contents for
     * @return an input stream which provides the contents of the object / file
     */
    @Nonnull
    public InputStream getData(@Nonnull StoredObject file) {
        VirtualObject object = (VirtualObject) file;

        InputStream result = getStorageEngine(object.getBucket()).getData(object.getBucket(), object.getPhysicalKey());
        if (result == null) {
            return new ByteArrayInputStream(EMPTY_BUFFER);
        } else {
            return result;
        }
    }

    /**
     * Deletes the given object and alls of its versions.
     *
     * @param object the object to delete
     */
    public void delete(StoredObject object) {
        if (object instanceof VirtualObject) {
            oma.delete((VirtualObject) object);
        }
    }

    /**
     * Deletes the physical object in the given bucket.
     *
     * @param bucket      the bucket to delete the object from
     * @param physicalKey the physical key of the object to delete
     */
    protected void deletePhysicalObject(String bucket, String physicalKey) {
        getStorageEngine(bucket).deletePhysicalObject(bucket, physicalKey);
    }

    /**
     * Verifies the authentication hash for the given key.
     *
     * @param key  the key to verify
     * @param hash the hash to verify
     * @return <tt>true</tt> if the hash verifies the given object key, <tt>false</tt> otherwise
     */
    protected boolean verifyHash(String key, String hash) {
        // Check for a hash for today...
        if (Strings.areEqual(hash, computeHash(key, 0))) {
            return true;
        }

        // Check for an eternally valid hash...
        if (Strings.areEqual(hash, computeEternallyValidHash(key))) {
            return true;
        }

        // Check for hashes up to two days of age...
        for (int i = 1; i < 3; i++) {
            if (Strings.areEqual(hash, computeHash(key, -i)) || Strings.areEqual(hash, computeHash(key, i))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Delivers a pyhsical file or object.
     *
     * @param ctx           the request to respond to
     * @param bucket        the bucket to deliver from
     * @param physicalKey   the physical file to deliver
     * @param fileExtension the file extension of the file (to determine the <tt>Content-Type</tt>)
     */
    protected void deliverPhysicalFile(WebContext ctx, String bucket, String physicalKey, String fileExtension) {
        getStorageEngine(bucket).deliver(ctx, bucket, physicalKey, fileExtension);
    }

    /**
     * Creates a builder to construct a download URL for an object.
     *
     * @param bucket    the bucket containing the object
     * @param objectKey the object to create an URL for
     * @return a builder to construct a download URL
     */
    public DownloadBuilder prepareDownload(String bucket, String objectKey) {
        return new DownloadBuilder(this, bucket, objectKey);
    }

    /**
     * Creates a builder for download URLs based on a {@link VirtualObject} which might avoid a lookup.
     *
     * @param object the object to deliver
     * @return a builder to construct a download URL
     */
    protected DownloadBuilder prepareDownload(VirtualObject object) {
        return new DownloadBuilder(this, object);
    }

    /**
     * Creates a download URL for a fully populated builder.
     *
     * @param downloadBuilder the builder specifying the details of the url
     * @return a download URL for the object described by the builder
     */
    protected String createURL(DownloadBuilder downloadBuilder) {
        String result = getStorageEngine(downloadBuilder.getBucket()).createURL(downloadBuilder);
        if (result == null) {
            result = buildURL(downloadBuilder);
        }

        return result;
    }

    /**
     * Provides a facility to provide an internal download URL which utilizes {@link
     * PhysicalStorageEngine#deliver(WebContext, String, String, String)}.
     * <p>
     * This is the default way of delivering files. However, a {@link PhysicalStorageEngine} can provide its
     * own URLs which are handled outside of the system.
     *
     * @param downloadBuilder the builder specifying the details of the download
     * @return the download URL
     */
    private String buildURL(DownloadBuilder downloadBuilder) {
        StringBuilder result = new StringBuilder();
        if (Strings.isFilled(downloadBuilder.getBaseURL())) {
            result.append(downloadBuilder.getBaseURL());
        }
        result.append("/storage/physical/");
        result.append(downloadBuilder.getBucket());
        result.append("/");
        if (downloadBuilder.isEternallyValid()) {
            result.append(computeEternallyValidHash(downloadBuilder.getPhysicalKey()));
        } else {
            result.append(computeHash(downloadBuilder.getPhysicalKey(), 0));
        }
        result.append("/");
        result.append(downloadBuilder.getPhysicalKey());
        result.append(".");
        result.append(downloadBuilder.getFileExtension());
        if (Strings.isFilled(downloadBuilder.getFilename())) {
            result.append("?name=");
            result.append(Strings.urlEncode(downloadBuilder.getFilename()));
        }

        return result.toString();
    }

    /**
     * Computes an authentication hash for the given physical storage key and the offset in days (from the current).
     *
     * @param physicalKey the key to authenticate
     * @param offsetDays  the offset from the current day
     * @return a hash valid for the given day and key
     */
    private String computeHash(String physicalKey, int offsetDays) {
        return Hashing.md5()
                      .hashString(physicalKey + getTimestampOfDay(offsetDays) + getSharedSecret(), Charsets.UTF_8)
                      .toString();
    }

    /**
     * Computes an authentication hash which is eternally valid.
     *
     * @param physicalKey the key to authenticate
     * @return a hash valid forever
     */
    private String computeEternallyValidHash(String physicalKey) {
        return Hashing.md5().hashString(physicalKey + getSharedSecret(), Charsets.UTF_8).toString();
    }

    /**
     * Generates a timestamp for the day plus the provided day offset.
     *
     * @param day the offset from the current day
     * @return the effective timestamp (number of days since 01.01.1970) in days
     */
    private String getTimestampOfDay(int day) {
        Instant midnight = LocalDate.now().plusDays(day).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return String.valueOf(midnight.toEpochMilli());
    }

    /**
     * Determines the shared secret to use.
     *
     * @return the shared secret to use. Which is either taken from <tt>storage.sharedSecret</tt> in the system config
     * or a random value if the system is not configured properly
     */
    private String getSharedSecret() {
        if (safeSharedSecret == null) {
            if (Strings.isFilled(sharedSecret)) {
                safeSharedSecret = sharedSecret;
            } else {
                LOG.WARN("Please specify a secure and random value for 'storage.sharedSecret' in the 'instance.conf'!");
                return String.valueOf(System.currentTimeMillis());
            }
        }

        return safeSharedSecret;
    }
}
