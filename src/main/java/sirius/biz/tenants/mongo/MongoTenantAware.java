/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.tenants.mongo;

import sirius.biz.mongo.MongoBizEntity;
import sirius.biz.tenants.Tenant;
import sirius.biz.web.TenantAware;
import sirius.db.mixing.annotations.Index;
import sirius.db.mongo.Mango;
import sirius.db.mongo.types.MongoRef;
import sirius.kernel.di.std.Part;

import java.util.Optional;

/**
 * Base class which marks subclasses as aware of their tenant they belong to.
 * <p>
 * Note that an index is automatically created containing the tenant itself and the searchPrefixes,
 * which are added via {@link MongoBizEntity}. You can skip the index creation by defining an {@link Index}
 * without columns.
 */
@Index(name = "index_tenant_prefixes",
        columns = {"tenant", "searchPrefixes"},
        columnSettings = {Mango.INDEX_ASCENDING, Mango.INDEX_ASCENDING})
public abstract class MongoTenantAware extends MongoBizEntity implements TenantAware {

    @Part
    private static MongoTenants tenants;

    /**
     * Contains the tenant the entity belongs to.
     */
    private final MongoRef<MongoTenant> tenant = MongoRef.writeOnceOn(MongoTenant.class, MongoRef.OnDelete.REJECT);

    @Override
    public MongoRef<MongoTenant> getTenant() {
        return tenant;
    }

    @Override
    public String getTenantAsString() {
        return getTenant().isFilled() ? String.valueOf(getTenant().getId()) : null;
    }

    @Override
    public void fillWithCurrentTenant() {
        getTenant().setValue(tenants.getRequiredTenant());
    }

    @Override
    public void setOrVerifyCurrentTenant() {
        if (getTenant().isEmpty()) {
            fillWithCurrentTenant();
        } else {
            tenants.assertTenant(this);
        }
    }

    /**
     * Fills the tenant with the given one.
     *
     * @param tenant the tenant to set for this entity
     */
    public void withTenant(Tenant<?> tenant) {
        getTenant().setValue((MongoTenant) tenant);
    }

    /**
     * Fetches the tenant from cache or throws an exception if no tenant is present.
     *
     * @return the tenant which this object belongs to
     */
    public MongoTenant fetchCachedRequiredTenant() {
        return tenants.fetchCachedRequiredTenant(tenant);
    }

    /**
     * Fetches the tenant from cache wrapped in a Optional.
     *
     * @return the optional tenant which this object belongs to
     */
    public Optional<MongoTenant> fetchCachedTenant() {
        return tenants.fetchCachedTenant(tenant);
    }
}
