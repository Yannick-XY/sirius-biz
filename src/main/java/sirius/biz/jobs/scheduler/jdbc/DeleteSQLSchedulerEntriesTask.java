/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.jobs.scheduler.jdbc;

import sirius.biz.jobs.scheduler.SchedulerEntry;
import sirius.biz.tenants.deletion.DeleteSQLEntitiesTask;
import sirius.biz.tenants.deletion.DeleteTenantTask;
import sirius.biz.tenants.jdbc.SQLTenantAware;
import sirius.kernel.di.std.Register;

/**
 * Deletes all {@link SchedulerEntry scheduler entries} of the given tenant.
 */
@Register(classes = DeleteTenantTask.class, framework = SQLSchedulerController.FRAMEWORK_SCHEDULER_JDBC)
public class DeleteSQLSchedulerEntriesTask extends DeleteSQLEntitiesTask {

    @Override
    protected Class<? extends SQLTenantAware> getEntityClass() {
        return SQLSchedulerEntry.class;
    }

    @Override
    public int getPriority() {
        return 110;
    }
}
