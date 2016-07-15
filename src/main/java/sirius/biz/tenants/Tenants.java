/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.tenants;

import sirius.biz.web.BizController;
import sirius.db.mixing.OMA;
import sirius.kernel.di.std.Framework;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.web.security.UserContext;
import sirius.web.security.UserInfo;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Helps for extract the current {@link UserAccount} and {@link Tenant}.
 * <p>
 * Also some boiler plate methods are provided to perform some assertions.
 */
@Framework("tenants")
@Register(classes = Tenants.class)
public class Tenants {

    @Part
    private OMA oma;

    /**
     * Returns the current user as {@link UserAccount} which is logged in.
     *
     * @return the current user wrapped as {@link Optional} or an empty optional, if no user is logged in.
     */
    @Nonnull
    public Optional<UserAccount> getCurrentUser() {
        UserInfo user = UserContext.getCurrentUser();
        if (user.isLoggedIn()) {
            return Optional.ofNullable(user.getUserObject(UserAccount.class));
        }

        return Optional.empty();
    }

    /**
     * Returns the current user or throws an exception if no user is currently available.
     *
     * @return the currently logged in user
     */
    @Nonnull
    public UserAccount getRequiredUser() {
        Optional<UserAccount> ua = getCurrentUser();
        if (ua.isPresent()) {
            return ua.get();
        }
        throw Exceptions.handle()
                        .to(BizController.LOG)
                        .withSystemErrorMessage("A user of type UserAccount was expected but not present!")
                        .handle();
    }

    /**
     * Determines if there is currently a user logged in.
     *
     * @return <tt>true</tt> if a user is present, <tt>false</tt> otherwise
     */
    public boolean hasUser() {
        return getCurrentUser().isPresent();
    }

    /**
     * Returns the {@link Tenant} of the current user.
     *
     * @return the tenant of the current user wrapped as {@link Optional} or an empty optional, if no user is logged in.
     */
    @Nonnull
    public Optional<Tenant> getCurrentTenant() {
        return getCurrentUser().flatMap(u -> Optional.ofNullable(u.getTenant().getValue()));
    }

    /**
     * Returns the tenant of the currently logged in user or throws an exception if no user is present.
     *
     * @return the tenant of the currently logged in user
     */
    @Nonnull
    public Tenant getRequiredTenant() {
        Optional<Tenant> t = getCurrentTenant();
        if (t.isPresent()) {
            return t.get();
        }
        throw Exceptions.handle()
                        .to(BizController.LOG)
                        .withSystemErrorMessage("A tenant of type Tenant was expected but not present!")
                        .handle();
    }

    /**
     * Determines if there is a user logged in which has a tenant.
     *
     * @return <tt>true</tt> if there is a user with a known tenant currently logged in
     */
    public boolean hasTenant() {
        return getCurrentTenant().isPresent();
    }
}
