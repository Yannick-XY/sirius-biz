/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.tenants.mongo;

import sirius.biz.model.PersonData;
import sirius.biz.tenants.TenantUserManager;
import sirius.biz.tenants.UserAccount;
import sirius.biz.tenants.UserAccountController;
import sirius.biz.tenants.UserAccountData;
import sirius.biz.web.BasePageHelper;
import sirius.biz.web.MongoPageHelper;
import sirius.db.mixing.query.QueryField;
import sirius.db.mongo.MongoQuery;
import sirius.kernel.di.std.Register;
import sirius.web.controller.Controller;

/**
 * Provides a GUI for managing user accounts.
 */
@Register(classes = {Controller.class, UserAccountController.class}, framework = MongoTenants.FRAMEWORK_TENANTS_MONGO)
public class MongoUserAccountController extends UserAccountController<String, MongoTenant, MongoUserAccount> {

    @Override
    protected BasePageHelper<MongoUserAccount, ?, ?, ?> getUsersAsPage() {
        MongoQuery<MongoUserAccount> baseQuery = mango.select(MongoUserAccount.class)
                                                      .orderAsc(UserAccount.USER_ACCOUNT_DATA.inner(UserAccountData.PERSON)
                                                                                             .inner(PersonData.LASTNAME))
                                                      .orderAsc(UserAccount.USER_ACCOUNT_DATA.inner(UserAccountData.PERSON)
                                                                                             .inner(PersonData.FIRSTNAME));

        MongoPageHelper<MongoUserAccount> pageHelper = MongoPageHelper.withQuery(tenants.forCurrentTenant(baseQuery));
        pageHelper.withSearchFields(QueryField.startsWith(MongoUserAccount.SEARCH_PREFIXES));

        return pageHelper;
    }

    @Override
    protected BasePageHelper<MongoUserAccount, ?, ?, ?> getSelectableUsersAsPage() {
        MongoQuery<MongoUserAccount> baseQuery = mango.select(MongoUserAccount.class);

        if (!hasPermission(TenantUserManager.PERMISSION_SYSTEM_TENANT)) {
            baseQuery = baseQuery.eq(UserAccount.TENANT, tenants.getRequiredTenant());
        }

        baseQuery.orderAsc(UserAccount.USER_ACCOUNT_DATA.inner(UserAccountData.PERSON).inner(PersonData.LASTNAME))
                 .orderAsc(UserAccount.USER_ACCOUNT_DATA.inner(UserAccountData.PERSON).inner(PersonData.FIRSTNAME));

        MongoPageHelper<MongoUserAccount> pageHelper = MongoPageHelper.withQuery(baseQuery);
        pageHelper.withSearchFields(QueryField.startsWith(MongoUserAccount.SEARCH_PREFIXES));

        return pageHelper;
    }
}
