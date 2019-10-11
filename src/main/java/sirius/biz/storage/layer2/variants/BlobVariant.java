/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.storage.layer2.variants;

import sirius.biz.storage.layer1.FileHandle;
import sirius.biz.storage.layer2.Blob;
import sirius.web.http.WebContext;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Represents a derived variant of a {@link Blob}.
 * <p>
 * This could be a resized JPG derived from a given EPS file.
 */
public interface BlobVariant {

    /**
     * Returns the unique ID of this revision.
     *
     * @return the id of this revision
     */
    String getIdAsString();

    /**
     * Contains the variant designator which was used to derive this variant.
     *
     * @return the variant designator
     */
    String getVariantName();

    /**
     * Contains the physical key used by the {@link sirius.biz.storage.layer1.ObjectStorageSpace} to store the data.
     *
     * @return the layer 1 object key which contains the data of this revision
     */
    String getPhysicalObjectKey();

    /**
     *
     */
    LocalDateTime getLastConversionAttempt();

    /**
     * Returns the size of the revision in bytes.
     *
     * @return the size in bytes
     */
    long getSize();

    /**
     * Provides a on-disk copy of the data associated with this blob
     *
     * @return a handle to the data of this blob
     */
    Optional<FileHandle> download();

    boolean isQueuedForConversion();

    int getNumAttempts();
}
