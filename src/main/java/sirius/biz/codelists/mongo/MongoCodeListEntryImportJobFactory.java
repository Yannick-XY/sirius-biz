/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.codelists.mongo;

import sirius.biz.codelists.CodeList;
import sirius.biz.codelists.CodeListController;
import sirius.biz.codelists.CodeListEntryData;
import sirius.biz.importer.format.FieldDefinition;
import sirius.biz.importer.format.ImportDictionary;
import sirius.biz.jobs.JobFactory;
import sirius.biz.jobs.batch.file.EntityImportJob;
import sirius.biz.jobs.batch.file.EntityImportJobFactory;
import sirius.biz.jobs.params.CodeListParameter;
import sirius.biz.jobs.params.Parameter;
import sirius.biz.process.ProcessContext;
import sirius.biz.tenants.mongo.MongoTenants;
import sirius.db.mixing.BaseEntity;
import sirius.kernel.commons.Context;
import sirius.kernel.di.std.Register;
import sirius.kernel.nls.NLS;
import sirius.web.security.Permission;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Provides an import job for {@link MongoCodeList code lists} stored in MongoDB.
 */
@Register(classes = JobFactory.class, framework = MongoTenants.FRAMEWORK_TENANTS_MONGO)
@Permission(CodeListController.PERMISSION_MANAGE_CODELISTS)
public class MongoCodeListEntryImportJobFactory extends EntityImportJobFactory {

    /**
     * Contains the mongo code list to import the code list entries into.
     */
    private CodeListParameter codeListParameter = new CodeListParameter("codeList", "$CodeList");

    @Override
    protected void collectParameters(Consumer<Parameter<?, ?>> parameterCollector) {
        super.collectParameters(parameterCollector);

        parameterCollector.accept(codeListParameter);
    }

    @Override
    protected EntityImportJob<MongoCodeListEntry> createJob(ProcessContext process) {
        return new MongoCodeListEntryImportJob(process);
    }

    protected class MongoCodeListEntryImportJob extends EntityImportJob<MongoCodeListEntry> {

        private CodeList codeList;

        /**
         * Creates a new job for the given factory, name and process.
         *
         * @param process the process context itself
         */
        private MongoCodeListEntryImportJob(ProcessContext process) {
            super(fileParameter,
                  ignoreEmptyParameter,
                  importModeParameter,
                  MongoCodeListEntry.class,
                  getDictionary(),
                  process);
            codeList = process.require(codeListParameter);
        }

        @Override
        protected MongoCodeListEntry findAndLoad(Context ctx) {
            ctx.put(MongoCodeListEntry.CODE_LIST.toString(), ((MongoCodeList) codeList).getId());

            MongoCodeListEntry entry = super.findAndLoad(ctx);
            if (entry.isNew()) {
                entry.getCodeListEntryData()
                     .setCode(ctx.get(MongoCodeListEntry.CODE_LIST_ENTRY_DATA.inner(CodeListEntryData.CODE).toString())
                                 .toString());
            }

            return entry;
        }

        @Override
        protected MongoCodeListEntry fillAndVerify(MongoCodeListEntry entity) {
            setOrVerify(entity, entity.getCodeList(), (MongoCodeList) codeList);
            return super.fillAndVerify(entity);
        }
    }

    @Override
    protected Class<? extends BaseEntity<?>> getImportType() {
        return MongoCodeListEntry.class;
    }

    @Nonnull
    @Override
    public String getName() {
        return "import-mongo-code-list-entries";
    }

    @Override
    protected boolean hasPresetFor(Object targetObject) {
        return targetObject instanceof MongoCodeList;
    }

    @Override
    protected void computePresetFor(Object targetObject, Map<String, Object> preset) {
        preset.put(codeListParameter.getName(), ((MongoCodeList) targetObject).getId());
    }

    @Override
    protected void enhanceDictionary(ImportDictionary dictionary) {
        super.enhanceDictionary(dictionary);
        FieldDefinition code =
                new FieldDefinition(MongoCodeListEntry.CODE_LIST_ENTRY_DATA.inner(CodeListEntryData.CODE).toString(),
                                    FieldDefinition.typeString(null));
        code.addAlias("$Model.code");
        code.withLabel(NLS.get("Model.code"));
        dictionary.addField(code);
    }
}
