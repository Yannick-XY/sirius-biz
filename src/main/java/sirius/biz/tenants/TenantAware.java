/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.tenants;

import sirius.biz.model.BizEntity;
import sirius.db.mixing.Column;
import sirius.db.mixing.EntityRef;
import sirius.kernel.health.Exceptions;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Base class which marks subclasses as aware of their tenant they belong to.
 */
public abstract class TenantAware extends BizEntity {

    /**
     * Contains the tenant the entity belongs to.
     */
    public static final Column TENANT = Column.named("tenant");
    private final EntityRef<Tenant> tenant = EntityRef.on(Tenant.class, EntityRef.OnDelete.CASCADE);

    public EntityRef<Tenant> getTenant() {
        return tenant;
    }

    /**
     * Asserts that the given object has the same tenant as this object.
     *
     * @param fieldLabel the field in which the referenced object would be stored - used to generate an appropriate
     *                   error message
     * @param other      the object to check
     */
    public void assertSameTenant(Supplier<String> fieldLabel, TenantAware other) {
        if (other != null && (!Objects.equals(other.getTenant().getId(), getTenant().getId()))) {
            throw Exceptions.createHandled()
                            .withNLSKey("TenantAware.invalidTenant")
                            .set("field", fieldLabel.get())
                            .handle();
        }
    }
}
