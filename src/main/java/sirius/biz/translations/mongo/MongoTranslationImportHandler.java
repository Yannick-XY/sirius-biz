/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.translations.mongo;

import sirius.biz.codelists.mongo.MongoCodeLists;
import sirius.biz.importer.ImportHandler;
import sirius.biz.importer.ImportHandlerFactory;
import sirius.biz.importer.ImporterContext;
import sirius.biz.importer.MongoEntityImportHandler;
import sirius.biz.translations.Translation;
import sirius.biz.translations.TranslationData;
import sirius.db.mixing.Mapping;
import sirius.kernel.commons.Context;
import sirius.kernel.di.std.Register;

import java.util.Optional;
import java.util.function.BiConsumer;

public class MongoTranslationImportHandler extends MongoEntityImportHandler<MongoTranslation> {

    /**
     * Provides the factory to instantiate this import handler.
     */
    @Register(framework = MongoCodeLists.FRAMEWORK_CODE_LISTS_MONGO)
    public static class MongoTranslationImportHandlerFactory implements ImportHandlerFactory {

        @Override
        public boolean accepts(Class<?> type, ImporterContext context) {
            return type == MongoTranslation.class;
        }

        @Override
        public ImportHandler<?> create(Class<?> type, ImporterContext context) {
            return new MongoTranslationImportHandler(type, context);
        }
    }

    /**
     * Creates a new instance for the given type of entities and import context.
     *
     * @param clazz   the type of entities being handled
     * @param context the import context to use
     */
    protected MongoTranslationImportHandler(Class<?> clazz, ImporterContext context) {
        super(clazz, context);
    }

    @Override
    public Optional<MongoTranslation> tryFind(Context data) {
        return Optional.empty();
    }

    @Override
    protected void collectDefaultExportableMappings(BiConsumer<Integer, Mapping> collector) {
        collector.accept(100, Translation.TRANSLATION_DATA.inner(TranslationData.OWNER));
        collector.accept(120, Translation.TRANSLATION_DATA.inner(TranslationData.LANG));
        collector.accept(130, Translation.TRANSLATION_DATA.inner(TranslationData.FIELD));
        collector.accept(140, Translation.TRANSLATION_DATA.inner(TranslationData.TEXT));
    }
}
