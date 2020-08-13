/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.translations.jdbc;

import sirius.biz.codelists.jdbc.SQLCodeLists;
import sirius.biz.importer.ImportHandler;
import sirius.biz.importer.ImportHandlerFactory;
import sirius.biz.importer.ImporterContext;
import sirius.biz.importer.SQLEntityImportHandler;
import sirius.biz.translations.Translation;
import sirius.biz.translations.TranslationData;
import sirius.db.mixing.Mapping;
import sirius.kernel.di.std.Register;

import java.util.function.BiConsumer;

/**
 *
 */
public class SQLTranslationImportHandler extends SQLEntityImportHandler<SQLTranslation> {
    /**
     * Provides the factory to instantiate this import handler.
     */
    @Register(framework = SQLCodeLists.FRAMEWORK_CODE_LISTS_JDBC)
    public static class SQLTranslationImportHandlerFactory implements ImportHandlerFactory {

        @Override
        public boolean accepts(Class<?> type, ImporterContext context) {
            return type == SQLTranslation.class;
        }

        @Override
        public ImportHandler<?> create(Class<?> type, ImporterContext context) {
            return new SQLTranslationImportHandler(type, context);
        }
    }

    /**
     * Creates a new instance for the given type of entities and import context.
     *
     * @param clazz   the type of entities being handled
     * @param context the import context to use
     */
    protected SQLTranslationImportHandler(Class<?> clazz, ImporterContext context) {
        super(clazz, context);
    }

    @Override
    protected void collectDefaultExportableMappings(BiConsumer<Integer, Mapping> collector) {
        collector.accept(100, Translation.TRANSLATION_DATA.inner(TranslationData.OWNER));
        collector.accept(120, Translation.TRANSLATION_DATA.inner(TranslationData.LANG));
        collector.accept(130, Translation.TRANSLATION_DATA.inner(TranslationData.FIELD));
        collector.accept(140, Translation.TRANSLATION_DATA.inner(TranslationData.TEXT));
    }
}
