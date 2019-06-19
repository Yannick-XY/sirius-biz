/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.tenants;

import java.util.function.Consumer;

/**
 * Provides additional roles, which are given to the {@link sirius.web.security.UserInfo UserInfo} in the {@link TenantUserManager}.
 */
public interface AdditionalRolesProvider {

    /**
     * Adds additonal roles to the given roleConsumer, based on the given {@link UserAccount}.
     *
     * @param user         the {@link UserAccount} for which the roles should be calculated
     * @param roleConsumer the consumer for the additonal roles
     */
    void addAdditionalRoles(UserAccount<?, ?> user, Consumer<String> roleConsumer);
}