/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.translations.mongo;

import sirius.biz.codelists.CodeListController;
import sirius.biz.codelists.CodeListEntry;
import sirius.biz.codelists.jdbc.SQLCodeList;
import sirius.biz.codelists.mongo.MongoCodeLists;
import sirius.biz.importer.ImportContext;
import sirius.biz.jobs.batch.file.EntityImportJobFactory;
import sirius.biz.jobs.params.CodeListParameter;
import sirius.biz.jobs.params.Parameter;
import sirius.biz.process.ProcessContext;
import sirius.db.mixing.BaseEntity;
import sirius.kernel.di.std.Register;
import sirius.web.http.QueryString;
import sirius.web.security.Permission;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Provides an import job for {@link MongoTranslation translations} of {@link sirius.biz.codelists.mongo.MongoCodeListEntry code list entries} stored in a JDBC database.
 */
@Register(framework = MongoCodeLists.FRAMEWORK_CODE_LISTS_MONGO)
@Permission(CodeListController.PERMISSION_MANAGE_CODELISTS)
public class MongoCodeListEntryTranslationImportJobFactory extends EntityImportJobFactory {
    private CodeListParameter codeListParameter = new CodeListParameter("codeList", "$CodeList").markRequired();

    @Override
    protected Class<? extends BaseEntity<?>> getImportType() {
        return MongoTranslation.class;
    }

    @Nonnull
    @Override
    public String getName() {
        return "import-mongo-translations";
    }

    @Override
    protected void collectParameters(Consumer<Parameter<?, ?>> parameterCollector) {
        parameterCollector.accept(codeListParameter);
        super.collectParameters(parameterCollector);
    }

    @Override
    protected void transferParameters(ImportContext context, ProcessContext processContext) {
        context.set(CodeListEntry.CODE_LIST, processContext.require(codeListParameter));
    }

    @Override
    protected boolean hasPresetFor(QueryString queryString, Object targetObject) {
        return targetObject instanceof SQLCodeList;
    }

    @Override
    protected void computePresetFor(QueryString queryString, Object targetObject, Map<String, Object> preset) {
        preset.put(codeListParameter.getName(), ((SQLCodeList) targetObject).getCodeListData().getCode());
    }
}
