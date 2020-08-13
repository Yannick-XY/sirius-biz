/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.translations.mongo;

import sirius.biz.codelists.CodeList;
import sirius.biz.codelists.CodeListEntryData;
import sirius.biz.codelists.mongo.MongoCodeListEntry;
import sirius.biz.codelists.mongo.MongoCodeLists;
import sirius.biz.importer.ImportHandler;
import sirius.biz.importer.ImportHandlerFactory;
import sirius.biz.importer.ImporterContext;
import sirius.biz.importer.MongoEntityImportHandler;
import sirius.biz.importer.format.FieldDefinition;
import sirius.biz.importer.format.ImportDictionary;
import sirius.biz.translations.Translation;
import sirius.biz.translations.TranslationData;
import sirius.kernel.commons.Context;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;

import java.util.Optional;

/**
 * Provides an import handler for {@link MongoTranslation translations} of {@link MongoCodeListEntry code list entries}.
 */
public class MongoCodeListEntryTranslationImportHandler extends MongoEntityImportHandler<MongoTranslation> {

    @Part
    private static MongoCodeLists codeLists;

    /**
     * Provides the factory to instantiate this import handler.
     */
    @Register(framework = MongoCodeLists.FRAMEWORK_CODE_LISTS_MONGO)
    public static class MongoCodeListEntryTranslationImportHandlerFactory implements ImportHandlerFactory {

        @Override
        public boolean accepts(Class<?> type, ImporterContext context) {
            return type == MongoTranslation.class;
        }

        @Override
        public ImportHandler<?> create(Class<?> type, ImporterContext context) {
            return new MongoCodeListEntryTranslationImportHandler(type, context);
        }
    }

    /**
     * Creates a new instance for the given type of entities and import context.
     *
     * @param clazz   the type of entities being handled
     * @param context the import context to use
     */
    protected MongoCodeListEntryTranslationImportHandler(Class<?> clazz, ImporterContext context) {
        super(clazz, context);
    }

    @Override
    public Optional<MongoTranslation> tryFind(Context data) {
        if (data.containsKey(Translation.TRANSLATION_DATA.inner(TranslationData.OWNER).getName())) {
            return mango.select(MongoTranslation.class)
                        .eq(Translation.TRANSLATION_DATA.inner(TranslationData.OWNER),
                            data.get(Translation.TRANSLATION_DATA.inner(TranslationData.OWNER).getName()))
                        .eq(Translation.TRANSLATION_DATA.inner(TranslationData.FIELD),
                            data.get(Translation.TRANSLATION_DATA.inner(TranslationData.FIELD).getName()))
                        .eq(Translation.TRANSLATION_DATA.inner(TranslationData.LANG),
                            data.get(Translation.TRANSLATION_DATA.inner(TranslationData.LANG).getName()))
                        .eq(Translation.TRANSLATION_DATA.inner(TranslationData.TEXT),
                            data.get(Translation.TRANSLATION_DATA.inner(TranslationData.TEXT).getName()))
                        .one();
        }

        return Optional.empty();
    }

    @Override
    public ImportDictionary getImportDictionary() {
        ImportDictionary dictionary = new ImportDictionary();
        // TODO: add checks, descriptions, and aliases
        dictionary.addField(FieldDefinition.stringField(MongoCodeListEntry.CODE_LIST_ENTRY_DATA.inner(CodeListEntryData.CODE)
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
    public MongoTranslation load(Context data, MongoTranslation entity) {
        CodeList codeList = (CodeList) data.get("codeList");
        String cleCode =
                (String) data.get(MongoCodeListEntry.CODE_LIST_ENTRY_DATA.inner(CodeListEntryData.CODE).getName());

        codeLists.getEntry(codeList.getCodeListData().getCode(), cleCode)
                 .ifPresent(cle -> data.put("translationData_owner", cle.getUniqueName()));

        return super.load(data, entity);
    }
}
