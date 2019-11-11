/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.importer;

/**
 * Sublasses can be created and instantiated via {@link ImporterContext#findHelper(Class)}.
 * <p>
 * As these helpers are instantiated per {@link Importer} / {@link ImporterContext} they can carry around state
 * and supply helper methods.
 */
public abstract class ImportHelper {

    /**
     * Contains the context for which this helper was created.
     */
    protected ImportContext context;

    /**
     * Creates a new instance for the given context.
     * <p>
     * Note that a helper must provide a public constructor with this signature.
     *
     * @param context the context for which this helper was created
     */
    protected ImportHelper(ImportContext context) {
        this.context = context;
    }

    /**
     * Commits / flushes all data.
     * <p>
     * This is called if either {@link ImporterContext#commit()} or {@link ImporterContext#close()} is called and
     * can be used to flush / commit batch updates or the like.
     */
    public void commit() {
        // nothing to do by default
    }
}
