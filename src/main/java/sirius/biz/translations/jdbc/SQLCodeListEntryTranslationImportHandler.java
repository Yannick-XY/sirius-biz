/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.translations.jdbc;

import sirius.biz.codelists.CodeList;
import sirius.biz.codelists.CodeListEntryData;
import sirius.biz.codelists.jdbc.SQLCodeListEntry;
import sirius.biz.codelists.jdbc.SQLCodeLists;
import sirius.biz.importer.ImportHandler;
import sirius.biz.importer.ImportHandlerFactory;
import sirius.biz.importer.ImporterContext;
import sirius.biz.importer.SQLEntityImportHandler;
import sirius.biz.importer.format.FieldDefinition;
import sirius.biz.importer.format.ImportDictionary;
import sirius.biz.translations.Translation;
import sirius.biz.translations.TranslationData;
import sirius.kernel.commons.Context;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;

/**
 * Provides an import handler for {@link SQLTranslation translations} of {@link SQLCodeListEntry code list entries}.
 */
public class SQLCodeListEntryTranslationImportHandler extends SQLEntityImportHandler<SQLTranslation> {
    @Part
    private static SQLCodeLists codeLists;

    /**
     * Provides the factory to instantiate this import handler.
     */
    @Register(framework = SQLCodeLists.FRAMEWORK_CODE_LISTS_JDBC)
    public static class SQLCodeListEntryTranslationImportHandlerFactory implements ImportHandlerFactory {

        @Override
        public boolean accepts(Class<?> type, ImporterContext context) {
            return type == SQLTranslation.class;
        }

        @Override
        public ImportHandler<?> create(Class<?> type, ImporterContext context) {
            return new SQLCodeListEntryTranslationImportHandler(type, context);
        }
    }

    /**
     * Creates a new instance for the given type of entities and import context.
     *
     * @param clazz   the type of entities being handled
     * @param context the import context to use
     */
    protected SQLCodeListEntryTranslationImportHandler(Class<?> clazz, ImporterContext context) {
        super(clazz, context);
    }

    @Override
    public ImportDictionary getImportDictionary() {
        ImportDictionary dictionary = new ImportDictionary();
        // TODO: add checks, descriptions and aliases
        dictionary.addField(FieldDefinition.stringField(SQLCodeListEntry.CODE_LIST_ENTRY_DATA.inner(CodeListEntryData.CODE)
                                                                                             .getName()));
        dictionary.addField(FieldDefinition.stringField(Translation.TRANSLATION_DATA.inner(TranslationData.LANG)
                                                                                    .getName()));
        dictionary.addField(FieldDefinition.stringField(Translation.TRANSLATION_DATA.inner(TranslationData.FIELD)
                                                                                    .getName()));
        dictionary.addField(FieldDefinition.stringField(Translation.TRANSLATION_DATA.inner(TranslationData.TEXT)
                                                                                    .getName()));
        return dictionary;
    }

    @Override
    public SQLTranslation load(Context data, SQLTranslation entity) {
        CodeList codeList = (CodeList) data.get("codeList");
        String cleCode =
                (String) data.get(SQLCodeListEntry.CODE_LIST_ENTRY_DATA.inner(CodeListEntryData.CODE).getName());

        codeLists.getEntry(codeList.getCodeListData().getCode(), cleCode)
                 .ifPresent(cle -> data.put("translationData_owner", cle.getUniqueName()));
        return super.load(data, entity);
    }
}
