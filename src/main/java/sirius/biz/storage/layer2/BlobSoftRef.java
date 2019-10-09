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

import java.util.regex.Pattern;

/**
 * Represents a reference to a {@link Blob} which can be placed as field within an {@link BaseEntity}.
 * <p>
 * Being a soft reference the lifetime of the blob is <b>not</b> bound to the containing entity. Rather the blob
 * is selected from the browsable blobs of a space and continues to live independently if the referencing entity is
 * deleted.
 */
public class BlobSoftRef extends BlobHardRef {

    private final boolean supportsURL;

    private static final Pattern URL_PATTERN = Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);

    /**
     * Creates a new reference for the given space.
     *
     * @param space       the space to place referenced objects in
     * @param supportsURL if <tt>true</tt> a URL can also be used instead of an object key
     */
    public BlobSoftRef(String space, boolean supportsURL) {
        super(space);
        this.supportsURL = supportsURL;
    }

    /**
     * Creates a new reference for the given space.
     *
     * @param space the space to place referenced objects in
     */
    public BlobSoftRef(String space) {
        this(space, false);
    }

    @Override
    public String getFilename() {
        if (isURL()) {
            return Strings.splitAtLast(getKey(), "/").getSecond();
        }

        return super.getFilename();
    }

    /**
     * Determines if an URL was stored instead of an object key.
     *
     * @return <tt>true</tt> if an URL was stored, <tt>false</tt> if an object key or nothing yet is stored
     */
    public boolean isURL() {
        return supportsURL && Strings.isFilled(key) && URL_PATTERN.matcher(key).find();
    }

    @Override
    public String toString() {
        return "BlobSoftRef: " + (isFilled() ? getKey() : "(empty)");
    }

    /**
     * Determines if this reference also supports to store URLs instead of object keys.
     *
     * @return <tt>true</tt> if URLs can be stored in this reference, <tt>false</tt> otherwise
     */
    public boolean isSupportsURL() {
        return supportsURL;
    }

    @Override
    public URLBuilder url() {
        if (isURL()) {
            return new ExternalURLBuilder(key);
        }
        return super.url();
    }
}
