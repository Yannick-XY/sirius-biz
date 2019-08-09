/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.tenants.mongo;

import sirius.biz.jobs.JobFactory;
import sirius.biz.jobs.batch.file.LineBasedImportJob;
import sirius.biz.jobs.batch.file.LineBasedImportJobFactory;
import sirius.biz.process.ProcessContext;
import sirius.biz.tenants.UserAccountController;
import sirius.db.mixing.BaseEntity;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.web.security.Permission;

import javax.annotation.Nonnull;

/**
 * Provides an import job for {@link MongoUserAccount user accounts} stored in MongoDB.
 */
@Register(classes = JobFactory.class, framework = MongoTenants.FRAMEWORK_TENANTS_MONGO)
@Permission(UserAccountController.PERMISSION_MANAGE_USER_ACCOUNTS)
public class MongoUserAccountImportJobFactory extends LineBasedImportJobFactory {

    @Part
    private MongoTenants tenants;

    @Override
    protected LineBasedImportJob<?> createJob(ProcessContext process) {
        MongoTenant currentTenant = tenants.getRequiredTenant();

        return new LineBasedImportJob<MongoUserAccount>(fileParameter,
                                                        ignoreEmptyParameter,
                                                        MongoUserAccount.class,
                                                        getDictionary(),
                                                        process) {
            @Override
            protected MongoUserAccount fillAndVerify(MongoUserAccount entity) {
                setOrVerify(entity, entity.getTenant(), currentTenant);
                return super.fillAndVerify(entity);
            }
        };
    }

    @Override
    protected Class<? extends BaseEntity<?>> getImportType() {
        return MongoUserAccount.class;
    }

    @Nonnull
    @Override
    public String getName() {
        return "import-mongo-user-accounts";
    }
}
