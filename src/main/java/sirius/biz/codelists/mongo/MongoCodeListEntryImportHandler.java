/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.codelists.mongo;

import sirius.biz.codelists.CodeListEntryData;
import sirius.biz.importer.ImportHandler;
import sirius.biz.importer.ImportHandlerFactory;
import sirius.biz.importer.ImporterContext;
import sirius.biz.importer.MongoEntityImportHandler;
import sirius.biz.tenants.mongo.MongoTenants;
import sirius.biz.tenants.mongo.MongoUserAccount;
import sirius.db.mixing.Property;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Value;
import sirius.kernel.di.std.Register;

import java.util.Optional;

/**
 * Provides an import handler for {@link MongoCodeListEntry code list entries}.
 */
public class MongoCodeListEntryImportHandler extends MongoEntityImportHandler<MongoCodeListEntry> {

    /**
     * Provides the factory to instantiate this import handler.
     */
    @Register(framework = MongoTenants.FRAMEWORK_TENANTS_MONGO)
    public static class MongoCodeListImportHandlerFactory implements ImportHandlerFactory {

        @Override
        public boolean accepts(Class<?> type, ImporterContext context) {
            return type == MongoCodeListEntry.class;
        }

        @Override
        public ImportHandler<?> create(Class<?> type, ImporterContext context) {
            return new MongoCodeListEntryImportHandler(type, context);
        }
    }

    /**
     * Creates a new instance for the given type of entities and import context.
     *
     * @param clazz   the type of entities being handled
     * @param context the import context to use
     */
    protected MongoCodeListEntryImportHandler(Class<?> clazz, ImporterContext context) {
        super(clazz, context);
    }

    /**
     * Tries to find an entity using the supplied <tt>data</tt>.
     *
     * @param data the data used to describe the entity to find
     * @return a matching entity wrapped as optional or an empty optional if there is no matching entity
     */
    @Override
    public Optional<MongoCodeListEntry> tryFind(Context data) {
        if (data.containsKey(MongoCodeListEntry.CODE_LIST_ENTRY_DATA.inner(CodeListEntryData.CODE).getName())
            && data.containsKey(MongoCodeListEntry.CODE_LIST.getName())) {
            return mango.select(MongoCodeListEntry.class)
                        .eq(MongoCodeListEntry.CODE_LIST_ENTRY_DATA.inner(CodeListEntryData.CODE),
                            data.getValue(MongoCodeListEntry.CODE_LIST_ENTRY_DATA.inner(CodeListEntryData.CODE)
                                                                                 .getName()))
                        .eq(MongoCodeListEntry.CODE_LIST, data.getValue(MongoCodeListEntry.CODE_LIST.getName()))
                        .one();
        }

        return Optional.empty();
    }

}
