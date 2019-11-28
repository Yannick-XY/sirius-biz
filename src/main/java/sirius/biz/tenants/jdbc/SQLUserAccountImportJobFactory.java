/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.tenants.jdbc;

import sirius.biz.jobs.JobFactory;
import sirius.biz.jobs.batch.file.EntityImportJobFactory;
import sirius.biz.tenants.UserAccountController;
import sirius.db.mixing.BaseEntity;
import sirius.kernel.di.std.Register;
import sirius.web.security.Permission;

import javax.annotation.Nonnull;

/**
 * Provides an import job for {@link SQLUserAccount user accounts} stored in a JDBC database.
 */
@Register(classes = JobFactory.class, framework = SQLTenants.FRAMEWORK_TENANTS_JDBC)
@Permission(UserAccountController.PERMISSION_MANAGE_USER_ACCOUNTS)
public class SQLUserAccountImportJobFactory extends EntityImportJobFactory {

    @Nonnull
    @Override
    public String getName() {
        return "import-sql-user-accounts";
    }

    @Override
    protected Class<? extends BaseEntity<?>> getImportType() {
        return SQLUserAccount.class;
    }

    @Override
    protected boolean hasPresetFor(Object targetObject) {
        return targetObject == SQLUserAccount.class;
    }
}
