/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.tenants.deletion;

import sirius.biz.process.ProcessContext;
import sirius.biz.process.logs.ProcessLog;
import sirius.biz.tenants.Tenant;
import sirius.biz.tenants.mongo.MongoTenantAware;
import sirius.biz.web.TenantAware;
import sirius.db.mixing.Mixing;
import sirius.db.mongo.Mango;
import sirius.db.mongo.MongoQuery;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.std.Part;

/**
 * Deletes all entities of a given subclass of {@link MongoTenantAware} which belong to the given tenant.
 */
public abstract class DeleteMongoEntitiesTask implements DeleteTenantTask {

    @Part
    protected Mango mango;

    @Part
    protected Mixing mixing;

    @Override
    public void beforeExecution(ProcessContext processContext, Tenant<?> tenant, boolean simulate) {
        processContext.log(ProcessLog.info()
                                     .withNLSKey("DeleteTenantTask.beforeExecution")
                                     .withContext("count", getQuery(tenant).count())
                                     .withContext("name", getEntityName()));
    }

    @Override
    public void execute(ProcessContext processContext, Tenant<?> tenant) {
        getQuery(tenant).iterateAll(entity -> {
            Watch watch = Watch.start();
            mango.delete(entity);
            processContext.addTiming(DeleteTenantJobFactory.TIMING_DELETED_ITEMS, watch.elapsedMillis());
        });
    }

    protected MongoQuery<? extends MongoTenantAware> getQuery(Tenant<?> tenant) {
        return mango.select(getEntityClass()).eq(TenantAware.TENANT, tenant);
    }

    /**
     * Defines the class of entities to be deleted.
     *
     * @return the class of the entities to be deleted
     */
    protected abstract Class<? extends MongoTenantAware> getEntityClass();

    protected String getEntityName() {
        return mixing.getDescriptor(getEntityClass()).getPluralLabel();
    }

    @Override
    public int getPriority() {
        return 100;
    }
}
