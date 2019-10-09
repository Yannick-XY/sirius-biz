/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.storage.layer2;

import io.netty.handler.codec.http.HttpResponseStatus;
import sirius.biz.storage.layer1.ObjectStorage;
import sirius.biz.storage.util.StorageUtils;
import sirius.kernel.commons.Explain;
import sirius.kernel.commons.Files;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Values;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.web.http.Response;
import sirius.web.http.WebContext;
import sirius.web.http.WebDispatcher;

import javax.annotation.Nullable;

/**
 * Responsible for delivering {@link Blob blobs} managed by the {@link BlobStorage Layer 2} of the storage framework.
 * <p>
 * This will mainly deliver blobs via URLs generated by {@link URLBuilder}. As this is a central activity, doesn't need
 * any user authentication (URLs are presigned) and its reponsibility can b checked easily (all requests URIs start
 * with a common {@link BlobDispatcher#URI_PREFIX}), this has a pretty low priority.
 * <p>
 * Note that this dispatcher itself doesn't do more than decoding and veriying the URL. All the heavy lifting is
 * either done by {@link sirius.biz.storage.layer1.ObjectStorageSpace#deliver(Response, String)} for physical URLs
 * or {@link BlobStorageSpace#deliver(String, String, Response)} for virtual URLs.
 */
@Register(framework = StorageUtils.FRAMEWORK_STORAGE)
public class BlobDispatcher implements WebDispatcher {

    /**
     * Contains the base prefix for all URIs handled by this dispatcher.
     * <p>
     * As this dispatcher is most probably in active in every system we do not want to "block" a common name
     * like "storage" or the like. However, we have to pick a name after all. Therefore we went with a rather short
     * acronym from the mainframe area (DASD stood/stands/will always stand for Direct Attached Storage Device) and
     * is nowerdays simply called a harddisk. Yes, it doesn't match the purpose of the URI but its short, not a common
     * term and <b>fun</b>.
     */
    public static final String URI_PREFIX = "/dasd";
    private static final int URI_PREFIX_LENGTH = URI_PREFIX.length();

    /**
     * Marks the request as "physical" access (direct layer 1 access).
     */
    public static final String FLAG_PHYSICAL = "p";

    /**
     * Marks the request as "download".
     * <p>
     * This will invoke {@link Response#download(String)} before starting delivery.
     */
    public static final String FLAG_DOWNLOAD = "d";

    /**
     * Marks the request as "virtual" (layer 2) access.
     */
    public static final String FLAG_VIRTUAL = "v";

    /**
     * Marks the reuqest as cachable.
     * <p>
     * Otherwise all HTTPS cache settings would be turned off.
     */
    public static final String FLAG_CACHABLE = "c";

    /**
     * Detects the flag combination for a direct physical delivery (no download).
     * <p>
     * This will expect an URI like: <tt>/dasd/p/SPACE/ACCESS_TOKEN/PHYSICAL_KEY.FILE_EXTENSION</tt>
     */
    private static final String PHYSICAL_DELIVERY = FLAG_PHYSICAL;

    /**
     * Detects the flag combination for a physical download.
     * <p>
     * This will expect an URI like: <tt>/dasd/pd/SPACE/ACCESS_TOKEN/PHYSICAL_KEY/FILENAME.FILE_EXTENSION</tt>
     */
    private static final String PHYSICAL_DOWNLOAD = FLAG_PHYSICAL + FLAG_DOWNLOAD;

    /**
     * Detects the flag combination for a virtual delivery which has no HTTP cache support.
     * <p>
     * This will expect an URI like: <tt>/dasd/v/SPACE/ACCESS_TOKEN/VARIANT/BLOB_KEY.FILE_EXTENSION</tt>
     */
    private static final String VIRTUAL_DELIVERY = FLAG_VIRTUAL;

    /**
     * Detects the flag combination for a virtual download which has no HTTP cache support.
     * <p>
     * This will expect an URI like: <tt>/dasd/vd/SPACE/ACCESS_TOKEN/VARIANT/BLOB_KEY/FILENAME.FILE_EXTENSION</tt>
     */
    private static final String VIRTUAL_DOWNLOAD = FLAG_VIRTUAL + FLAG_DOWNLOAD;

    /**
     * Detects the flag combination for a cachable virtual delivery.
     * <p>
     * This will expect an URI like: <tt>/dasd/cv/SPACE/ACCESS_TOKEN/VARIANT/BLOB_KEY.FILE_EXTENSION</tt>
     */
    private static final String VIRTUAL_CACHABLE_DELIVERY = FLAG_CACHABLE + FLAG_VIRTUAL;

    /**
     * Detects the flag combination for a cachable virtual download.
     * <p>
     * This will expect an URI like: <tt>/dasd/cvd/SPACE/ACCESS_TOKEN/VARIANT/BLOB_KEY/FILENAME.FILE_EXTENSION</tt>
     */
    private static final String VIRTUAL_CACHABLE_DOWNLOAD = FLAG_CACHABLE + FLAG_VIRTUAL + FLAG_DOWNLOAD;

    @Part
    private BlobStorage blobStorage;

    @Part
    private ObjectStorage objectStorage;

    @Part
    private StorageUtils utils;

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean dispatch(WebContext request) throws Exception {
        String uri = request.getRequestedURI();
        if (!uri.startsWith(URI_PREFIX)) {
            return false;
        }

        uri = uri.substring(URI_PREFIX_LENGTH);

        Values uriParts = Values.of(uri.split("/"));
        String type = uriParts.at(0).asString();
        if (Strings.areEqual(type, PHYSICAL_DELIVERY)) {
            String filename = stripAdditionalText(uriParts.at(3).asString());
            physicalDelivery(request,
                             uriParts.at(1).asString(),
                             uriParts.at(2).asString(),
                             Files.getFilenameWithoutExtension(filename),
                             filename);
            return true;
        }

        if (Strings.areEqual(type, PHYSICAL_DOWNLOAD)) {
            physicalDownload(request,
                             uriParts.at(1).asString(),
                             uriParts.at(2).asString(),
                             uriParts.at(3).asString(),
                             stripAdditionalText(uriParts.at(4).asString()));
            return true;
        }

        if (Strings.areEqual(type, VIRTUAL_DELIVERY)) {
            String filename = stripAdditionalText(uriParts.at(4).asString());
            virtualDelivery(request,
                            uriParts.at(1).asString(),
                            uriParts.at(2).asString(),
                            uriParts.at(3).asString(),
                            Files.getFilenameWithoutExtension(filename),
                            filename,
                            false,
                            false);
            return true;
        }

        if (Strings.areEqual(type, VIRTUAL_DOWNLOAD)) {
            virtualDelivery(request,
                            uriParts.at(1).asString(),
                            uriParts.at(2).asString(),
                            uriParts.at(3).asString(),
                            uriParts.at(4).asString(),
                            stripAdditionalText(uriParts.at(5).asString()),
                            true,
                            false);
            return true;
        }

        if (Strings.areEqual(type, VIRTUAL_CACHABLE_DELIVERY)) {
            String filename = stripAdditionalText(uriParts.at(4).asString());
            virtualDelivery(request,
                            uriParts.at(1).asString(),
                            uriParts.at(2).asString(),
                            uriParts.at(3).asString(),
                            Files.getFilenameWithoutExtension(filename),
                            filename,
                            false,
                            true);
            return true;
        }

        if (Strings.areEqual(type, VIRTUAL_CACHABLE_DOWNLOAD)) {
            virtualDelivery(request,
                            uriParts.at(1).asString(),
                            uriParts.at(2).asString(),
                            uriParts.at(3).asString(),
                            uriParts.at(4).asString(),
                            stripAdditionalText(uriParts.at(5).asString()),
                            true,
                            true);
            return true;
        }

        return false;
    }

    /**
     * Strips of a SEO text to retrieve the effective filename.
     * <p>
     * Such an "enhanced" filename is generated when {@link URLBuilder#withAddonText(String)} was used.
     *
     * @param input the full filename with an optional SEO text as prefix
     * @return the filename with the additionak text stripped of
     */
    private String stripAdditionalText(String input) {
        Tuple<String, String> additionalTextAndKey = Strings.splitAtLast(input, "--");
        if (additionalTextAndKey.getSecond() == null) {
            return additionalTextAndKey.getFirst();
        } else {
            return additionalTextAndKey.getSecond();
        }
    }

    /**
     * Prepares a {@link Response} and delegates the call to the layer 1.
     *
     * @param request     the request to handle
     * @param space       the space which is accessed
     * @param accessToken the security token to verify
     * @param physicalKey the physical object key used to determine which object should be delivered
     * @param filename    the filename which is used to setup a proper <tt>Content-Type</tt>
     */
    private void physicalDelivery(WebContext request,
                                  String space,
                                  String accessToken,
                                  String physicalKey,
                                  String filename) {
        if (!utils.verifyHash(physicalKey, accessToken)) {
            request.respondWith().error(HttpResponseStatus.UNAUTHORIZED);
            return;
        }

        Response response = request.respondWith().infinitelyCached().named(filename);
        objectStorage.getSpace(space).deliver(response, physicalKey);
    }

    /**
     * Prepares a {@link Response} as download and delegates the call to the layer 1.
     *
     * @param request     the request to handle
     * @param space       the space which is accessed
     * @param accessToken the security token to verify
     * @param physicalKey the physical object key used to determine which object should be delivered
     * @param filename    the filename which is used to setup a proper <tt>Content-Type</tt>
     */
    private void physicalDownload(WebContext request,
                                  String space,
                                  String accessToken,
                                  String physicalKey,
                                  String filename) {
        if (!utils.verifyHash(physicalKey, accessToken)) {
            request.respondWith().error(HttpResponseStatus.UNAUTHORIZED);
            return;
        }

        Response response = request.respondWith().infinitelyCached().download(filename);
        objectStorage.getSpace(space).deliver(response, physicalKey);
    }

    /**
     * Prepares a {@link Response} and delegates the call to the layer 2.
     *
     * @param request     the request to handle
     * @param space       the space which is accessed
     * @param accessToken the security token to verify
     * @param variant     the variant to deliver - {@link URLBuilder#VARIANT_RAW} will be used to deliver the blob
     *                    itself
     * @param blobKey     the blob object key used to determine which {@link Blob} should be delivered
     * @param filename    the filename which is used to setup a proper <tt>Content-Type</tt>
     * @param download    determines if a download should be generated
     * @param cachable    determines if HTTP caching should be supported (<tt>true</tt>) or suppressed (<tt>false</tt>)
     */
    @SuppressWarnings("squid:S00107")
    @Explain("As this is a super hot code path we use 8 parameters instead of a parameter object"
             + " as this makes the URL parsing quite obvious")
    private void virtualDelivery(WebContext request,
                                 String space,
                                 String accessToken,
                                 @Nullable String variant,
                                 String blobKey,
                                 String filename,
                                 boolean download,
                                 boolean cachable) {
        if (!utils.verifyHash(Strings.isFilled(variant) ? blobKey + "-" + variant : blobKey, accessToken)) {
            request.respondWith().error(HttpResponseStatus.UNAUTHORIZED);
            return;
        }

        BlobStorageSpace storageSpace = blobStorage.getSpace(space);
        Response response = request.respondWith();
        if (cachable) {
            response.cached();
        } else {
            response.notCached();
        }
        if (download) {
            if (Strings.areEqual(filename, blobKey)) {
                filename = storageSpace.resolveFilename(blobKey).orElse(filename);
            }
            response.download(filename);
        } else {
            response.named(filename);
        }

        storageSpace.deliver(blobKey, variant, response);
    }
}
