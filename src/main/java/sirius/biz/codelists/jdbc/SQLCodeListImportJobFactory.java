/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.codelists.jdbc;

import sirius.biz.codelists.CodeListController;
import sirius.biz.codelists.CodeListEntry;
import sirius.biz.importer.ImportContext;
import sirius.biz.jobs.JobFactory;
import sirius.biz.jobs.batch.file.EntityImportJobFactory;
import sirius.biz.jobs.params.CodeListParameter;
import sirius.biz.jobs.params.Parameter;
import sirius.biz.process.ProcessContext;
import sirius.db.mixing.BaseEntity;
import sirius.kernel.di.std.Register;
import sirius.web.security.Permission;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Provides an import job for {@link SQLCodeList code lists} stored in a JDBC database.
 */
@Register(classes = JobFactory.class, framework = SQLCodeLists.FRAMEWORK_CODE_LISTS_JDBC)
@Permission(CodeListController.PERMISSION_MANAGE_CODELISTS)
public class SQLCodeListImportJobFactory extends EntityImportJobFactory {

    private CodeListParameter codeListParameter = new CodeListParameter("codeList", "$CodeList").markRequired();

    @Nonnull
    @Override
    public String getName() {
        return "import-sql-code-list-entries";
    }

    @Override
    protected Class<? extends BaseEntity<?>> getImportType() {
        return SQLCodeListEntry.class;
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
    protected boolean hasPresetFor(Object targetObject) {
        return targetObject instanceof SQLCodeList;
    }

    @Override
    protected void computePresetFor(Object targetObject, Map<String, Object> preset) {
        preset.put(codeListParameter.getName(), ((SQLCodeList) targetObject).getCodeListData().getCode());
    }
}
