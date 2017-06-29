/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.storage;

import sirius.biz.model.TraceData;
import sirius.db.mixing.OMA;
import sirius.db.mixing.constraints.FieldOperator;
import sirius.kernel.async.BackgroundLoop;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Removes old and outdates files from buckets.
 * <p>
 * A bucket can specify a max age for its objects. Older objects are automatically deleted by the system (vis this
 * loop).
 */
@Register
public class StorageCleanupLoop extends BackgroundLoop {

    @Part
    private OMA oma;

    @Part
    private Storage storage;

    @Nonnull
    @Override
    public String getName() {
        return "storage-cleanup";
    }

    @Override
    protected double maxCallFrequency() {
        return 0.01f;
    }

    @Override
    protected void doWork() throws Exception {
        cleanupTemporaryUploads();
        cleanupBuckets();
    }

    /**
     * Deletes objects which were created for a {@link StoredObjectRef} but the entity containing the objects
     * was never saved.
     *
     * @see VirtualObject#TEMPORARY
     */
    private void cleanupTemporaryUploads() {
        List<VirtualObject> objectsToDelete = oma.select(VirtualObject.class)
                                                 .eq(VirtualObject.TEMPORARY, true)
                                                 .where(FieldOperator.on(VirtualObject.TRACE.inner(TraceData.CHANGED_AT))
                                                                     .lessThan(LocalDateTime.now().minusHours(1)))
                                                 .limit(256)
                                                 .queryList();
        if (!objectsToDelete.isEmpty()) {
            for (VirtualObject obj : objectsToDelete) {
                oma.delete(obj);
            }

            Storage.LOG.INFO("Deleted %s temporary uploads...", objectsToDelete.size());
        }
    }

    private void cleanupBuckets() {
        for (BucketInfo bucket : storage.getBuckets()) {
            if (bucket.getDeleteFilesAfterDays() > 0) {
                cleanupBucket(bucket);
            }
        }
    }

    private void cleanupBucket(BucketInfo bucket) {
        LocalDate limit = LocalDate.now().minusDays(bucket.getDeleteFilesAfterDays());
        List<VirtualObject> objectsToDelete = oma.select(VirtualObject.class)
                                                 .eq(VirtualObject.BUCKET, bucket.getName())
                                                 .where(FieldOperator.on(VirtualObject.TRACE.inner(TraceData.CHANGED_AT))
                                                                     .lessThan(limit))
                                                 .limit(256)
                                                 .queryList();
        if (!objectsToDelete.isEmpty()) {
            for (VirtualObject obj : objectsToDelete) {
                oma.delete(obj);
            }

            Storage.LOG.INFO("Deleted %s old files in '%s'...", objectsToDelete.size(), bucket.getName());
        }
    }
}
