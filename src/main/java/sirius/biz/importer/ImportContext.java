/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.importer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import sirius.db.jdbc.batch.BatchContext;
import sirius.db.mixing.BaseEntity;
import sirius.kernel.di.std.Parts;
import sirius.kernel.health.Exceptions;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ImportContext {

    private Importer importer;
    private BatchContext batchContext;

    @Parts(ImportHandlerFactory.class)
    private static Collection<ImportHandlerFactory> factories;

    private Map<Class<?>, ImportHandler<?>> handlers = new HashMap<>();
    private Cache<String, Object> localCache = CacheBuilder.newBuilder().maximumSize(256).build();

    protected ImportContext(Importer importer) {
        this.importer = importer;
        batchContext = new BatchContext(() -> "Batch Context of ImportContext", Duration.ofMinutes(2));
    }

    @SuppressWarnings("unchecked")
    public <E extends BaseEntity<?>> ImportHandler<E> findHandler(Class<E> type) {
        return (ImportHandler<E>) handlers.computeIfAbsent(type, this::lookupHandler);
    }

    @SuppressWarnings("unchecked")
    private ImportHandler<?> lookupHandler(Class<?> type) {
        for (ImportHandlerFactory factory : factories) {
            if (factory.accepts(type)) {
                return factory.create(type, importer, this);
            }
        }

        if (type.getSuperclass() != null && !type.getSuperclass().equals(type)) {
            return lookupHandler(type.getSuperclass());
        } else {
            throw Exceptions.createHandled()
                            .withSystemErrorMessage("Cannot find an import handler for type: %s", type)
                            .handle();
        }
    }

    public Cache<String, Object> getLocalCache() {
        return localCache;
    }

    protected void close() throws IOException {
        batchContext.close();
    }

    public BatchContext getBatchContext() {
        return batchContext;
    }
}
