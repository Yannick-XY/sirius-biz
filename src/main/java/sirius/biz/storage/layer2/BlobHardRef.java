/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.storage.layer2;

import sirius.db.mixing.BaseEntity;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Part;

/**
 * Represents a reference to a {@link Blob} which can be placed as field within an {@link BaseEntity}.
 * <p>
 * Being a hard reference the lifetime of the blob is bound to the containing entity. If the entity is deleted,
 * the blob is deleted as well.
 */
public class BlobHardRef {

    protected final String space;
    protected Blob blob;
    protected String key;
    protected boolean changed;

    @Part
    protected static BlobStorage storage;

    /**
     * Creates a new reference for the given space.
     *
     * @param space the space to place referenced objects in
     */
    public BlobHardRef(String space) {
        this.space = space;
    }

    /**
     * Retrieves the actual referenced blob from the storage layer.
     *
     * @return the referenced blob or <tt>null</tt> if there is no referenced blob
     */
    public Blob getBlob() {
        if (blob == null && Strings.isFilled(key)) {
            blob = storage.getSpace(space).findByBlobKey(key).orElse(null);
            if (blob == null) {
                key = null;
            }
        }
        return blob;
    }

    /**
     * Assigns a blob to be referenced.
     *
     * @param blob the blob to be referenced
     */
    public void setBlob(Blob blob) {
        this.blob = blob;
        if (blob == null) {
            if (Strings.isFilled(this.key)) {
                this.changed = true;
            }
            this.key = null;
        } else {
            if (!Strings.areEqual(this.key, blob.getBlobKey())) {
                this.changed = true;
            }
            this.key = blob.getBlobKey();
        }
    }

    /**
     * Specifies an object key to reference.
     *
     * @param key the key of the object to reference
     */
    public void setKey(String key) {
        if (!Strings.areEqual(this.key, key)) {
            this.changed = true;
        }

        if (Strings.isEmpty(key)) {
            this.key = null;
            this.blob = null;
        } else {
            this.key = key;
            if (this.blob != null && !Strings.areEqual(this.blob.getBlobKey(), key)) {
                this.blob = null;
            }
        }
    }

    /**
     * Determines the filename of the referenced blob.
     *
     * @return the filename, or <tt>null</tt> if either no blob or one without a filename is referenced
     */
    public String getFilename() {
        if (isEmpty() || getBlob() == null) {
            return null;
        }

        return getBlob().getFilename();
    }

    /**
     * Returns the key of the referenced blob.
     *
     * @return the key of the blob being referenced
     */
    public String getKey() {
        return key;
    }

    /**
     * Determines if a blob is being referenced.
     *
     * @return <tt>true</tt> if a reference is present, <tt>false</tt> otherwise
     */
    public boolean isFilled() {
        return Strings.isFilled(key);
    }

    /**
     * Determines if no blob is being referenced.
     *
     * @return <tt>true</tt> if no reference is present, <tt>false</tt> otherwise
     */
    public boolean isEmpty() {
        return Strings.isEmpty(key);
    }

    /**
     * Determines if the referenced blob was fetched already.
     *
     * @return <tt>true</tt> if the blob is fetched, <tt>false</tt> otherwise
     */
    public boolean isFetched() {
        return key == null || blob != null;
    }

    @Override
    public String toString() {
        return "BlobHardRef: " + (isFilled() ? getKey() : "(empty)");
    }

    /**
     * Returns the space in which referenced blobs are stored.
     *
     * @return the name of the space in which referenced blobs are stored
     */
    public String getSpace() {
        return space;
    }

    /**
     * Provides a builder which can be used to create a delivery or download link.
     *
     * @return a builder to create a download or delivery URL
     */
    public URLBuilder url() {
        if (blob != null) {
            return new URLBuilder(storage.getSpace(space), blob);
        }

        return new URLBuilder(storage.getSpace(space), key);
    }
}
