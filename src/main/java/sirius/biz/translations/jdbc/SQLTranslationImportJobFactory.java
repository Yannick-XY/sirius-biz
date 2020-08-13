/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.translations.jdbc;

import sirius.biz.codelists.CodeListController;
import sirius.biz.codelists.jdbc.SQLCodeLists;
import sirius.biz.jobs.batch.file.EntityImportJobFactory;
import sirius.db.mixing.BaseEntity;
import sirius.kernel.di.std.Register;
import sirius.web.security.Permission;

import javax.annotation.Nonnull;

@Register(framework = SQLCodeLists.FRAMEWORK_CODE_LISTS_JDBC)
@Permission(CodeListController.PERMISSION_MANAGE_CODELISTS)
public class SQLTranslationImportJobFactory extends EntityImportJobFactory {

    @Override
    protected Class<? extends BaseEntity<?>> getImportType() {
        return SQLTranslation.class;
    }

    @Nonnull
    @Override
    public String getName() {
        return "import-sql-translations";
    }
}
