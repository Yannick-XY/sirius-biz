/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.tenants;

import sirius.biz.web.BizController;
import sirius.db.mixing.BaseEntity;
import sirius.kernel.commons.Lambdas;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Part;
import sirius.kernel.health.Exceptions;
import sirius.web.controller.Routed;
import sirius.web.http.WebContext;
import sirius.web.security.SAMLHelper;
import sirius.web.security.SAMLResponse;
import sirius.web.security.UserContext;
import sirius.web.security.UserInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Permis a login via SAML.
 */
public abstract class SAMLController<I, T extends BaseEntity<I> & Tenant<I>, U extends BaseEntity<I> & UserAccount<I, T>>
        extends BizController {

    @Part
    private SAMLHelper saml;

    @ConfigValue("product.wondergemRoot")
    private String wondergemRoot;

    @ConfigValue("security.roles")
    private List<String> roles;

    /**
     * Lists all possible SAML tenants or permits to create a login form for a custom SAML provider.
     *
     * @param ctx the current request
     */
    @Routed("/saml")
    public void saml(WebContext ctx) {
        List<T> tenants = obtainTenantsForSaml(ctx);

        ctx.respondWith().template("/templates/tenants/saml.html.pasta", tenants);
    }

    @SuppressWarnings("unchecked")
    private List<T> obtainTenantsForSaml(WebContext ctx) {
        // If GET parameters are present, we create a "fake" tenant to provide a custom SAML target.
        // This can be used if several identity providers are available for a single tenant.
        // We can verify several tenants but we can only redirect to a single identity provider.
        // Therefore these parameters can be used to create a SAML request to a custom one.
        if (ctx.hasParameter("issuerName")) {
            return createFakeTenantForUserSubmittedData(ctx);
        }

        return querySAMLTenants();
    }

    protected List<T> createFakeTenantForUserSubmittedData(WebContext ctx) {
        T fakeTenant = null;
        try {
            fakeTenant = getTenantClass().getDeclaredConstructor().newInstance();
            fakeTenant.getTenantData().setSamlRequestIssuerName(ctx.require("issuerName").asString());
            fakeTenant.getTenantData().setSamlIssuerUrl(ctx.require("issuerUrl").asString().replace("javascript:", ""));
            fakeTenant.getTenantData().setSamlIssuerIndex(ctx.get("issuerIndex").asString("0"));
        } catch (Exception e) {
            Exceptions.handle(BizController.LOG, e);
        }

        return Collections.singletonList(fakeTenant);
    }

    protected abstract Class<T> getTenantClass();

    /**
     * Processes a SAML response and tries to create or update a user which is then logged in.
     *
     * @param ctx the SAML response as request
     */
    @Routed("/saml/login")
    public void samlLogin(WebContext ctx) {
        if (!ctx.isUnsafePOST()) {
            ctx.respondWith().redirectToGet("/saml");
            return;
        }

        SAMLResponse response = saml.parseSAMLResponse(ctx);

        if (Strings.isEmpty(response.getNameId())) {
            throw Exceptions.createHandled()
                            .withSystemErrorMessage("SAML Error: No or empty name ID was given.")
                            .handle();
        }

        TenantUserManager<?, ?, ?> manager =
                (TenantUserManager<?, ?, ?>) UserContext.getCurrentScope().getUserManager();
        UserInfo user = manager.findUserByName(ctx, response.getNameId());

        if (user == null) {
            user = tryCreateUser(ctx, response);
        } else {
            verifyUser(response, user);
        }

        UserContext userContext = UserContext.get();
        userContext.setCurrentUser(user);
        manager.onExternalLogin(ctx, user);

        ctx.respondWith().redirectToGet(ctx.get("goto").asString(wondergemRoot));
    }

    private UserInfo tryCreateUser(WebContext ctx, SAMLResponse response) {
        if (Strings.isEmpty(response.getIssuer())) {
            throw Exceptions.createHandled().withSystemErrorMessage("SAML Error: No issuer in request!").handle();
        }

        T tenant = findTenant(response);
        checkFingerprint(tenant, response);

        try {
            U account = getUserClass().getDeclaredConstructor().newInstance();
            account.getTenant().setValue(tenant);
            account.getUserAccountData().getLogin().setUsername(response.getNameId());
            account.getUserAccountData().getLogin().setCleartextPassword(UUID.randomUUID().toString());
            account.getUserAccountData().setExternalLoginRequired(true);
            updateAccount(response, account);
            account.getMapper().update(account);

            TenantUserManager<?, ?, ?> manager =
                    (TenantUserManager<?, ?, ?>) UserContext.getCurrentScope().getUserManager();
            UserInfo user = manager.findUserByName(ctx, response.getNameId());
            if (user == null) {
                throw Exceptions.createHandled().withSystemErrorMessage("SAML Error: Failed to create a user").handle();
            }

            return user;
        } catch (Exception e) {
            throw Exceptions.handle(BizController.LOG, e);
        }
    }

    protected abstract Class<U> getUserClass();

    private T findTenant(SAMLResponse response) {
        for (T tenant : querySAMLTenants()) {
            if (checkIssuer(tenant, response)) {
                return tenant;
            }
        }

        throw Exceptions.createHandled()
                        .withSystemErrorMessage("SAML Error: No matching tenant found for issuer: %s",
                                                response.getIssuer())
                        .handle();
    }

    protected abstract List<T> querySAMLTenants();

    private void verifyUser(SAMLResponse response, UserInfo user) {
        U account = user.getUserObject(getUserClass());
        T tenant = account.getTenant().getValue();

        if (!checkIssuer(tenant, response)) {
            throw Exceptions.createHandled().withSystemErrorMessage("SAML Error: Issuer mismatch!").handle();
        }
        if (!checkFingerprint(tenant, response)) {
            throw Exceptions.createHandled().withSystemErrorMessage("SAML Error: Fingerprint mismatch!").handle();
        }

        account = account.getMapper().refreshOrFail(account);
        updateAccount(response, account);
        account.getMapper().update(account);
    }

    private boolean checkFingerprint(T tenant, SAMLResponse response) {
        return isInList(tenant.getTenantData().getSamlFingerprint(), response.getFingerprint());
    }

    private boolean checkIssuer(T tenant, SAMLResponse response) {
        return isInList(tenant.getTenantData().getSamlIssuerName(), response.getIssuer());
    }

    private boolean isInList(String values, String valueToCheck) {
        if (Strings.isEmpty(values)) {
            return false;
        }

        for (String value : values.split(",")) {
            if (Strings.isFilled(value) && value.equalsIgnoreCase(valueToCheck)) {
                return true;
            }
        }

        return false;
    }

    private void updateAccount(SAMLResponse response, U account) {
        account.getUserAccountData().getPermissions().getPermissions().clear();
        response.getAttribute(SAMLResponse.ATTRIBUTE_GROUP)
                .stream()
                .filter(Strings::isFilled)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(Strings::isFilled)
                .filter(role -> roles.contains(role))
                .collect(Lambdas.into(account.getUserAccountData().getPermissions().getPermissions()));

        if (Strings.isFilled(response.getAttributeValue(SAMLResponse.ATTRIBUTE_GIVEN_NAME))) {
            account.getUserAccountData()
                   .getPerson()
                   .setFirstname(response.getAttributeValue(SAMLResponse.ATTRIBUTE_GIVEN_NAME));
        }
        if (Strings.isFilled(response.getAttributeValue(SAMLResponse.ATTRIBUTE_SURNAME))) {
            account.getUserAccountData()
                   .getPerson()
                   .setLastname(response.getAttributeValue(SAMLResponse.ATTRIBUTE_SURNAME));
        }
        if (Strings.isFilled(response.getAttributeValue(SAMLResponse.ATTRIBUTE_EMAIL_ADDRESS))) {
            account.getUserAccountData().setEmail(response.getAttributeValue(SAMLResponse.ATTRIBUTE_EMAIL_ADDRESS));
        }
    }
}
