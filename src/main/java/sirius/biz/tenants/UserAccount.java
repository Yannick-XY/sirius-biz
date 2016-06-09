/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.tenants;

import sirius.biz.protocol.JournalData;
import sirius.biz.model.LoginData;
import sirius.biz.model.PermissionData;
import sirius.biz.model.PersonData;
import sirius.biz.web.Autoloaded;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Framework;
import sirius.kernel.di.std.Part;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;
import sirius.db.mixing.Column;
import sirius.db.mixing.annotations.BeforeDelete;
import sirius.db.mixing.annotations.BeforeSave;
import sirius.db.mixing.annotations.Length;
import sirius.db.mixing.annotations.Trim;
import sirius.web.mails.Mails;

/**
 * Created by aha on 07.05.15.
 */
@Framework("tenants")
public class UserAccount extends TenantAware {

    @Trim
    @Autoloaded
    @Length(length = 150)
    private String email;
    public static final Column EMAIL = Column.named("email");

    private final PersonData person = new PersonData();
    public static final Column PERSON = Column.named("person");

    private final LoginData login = new LoginData();
    public static final Column LOGIN = Column.named("login");

    private final PermissionData permissions = new PermissionData(this);
    public static final Column PERMISSIONS = Column.named("permissions");

    private final JournalData journal = new JournalData(this);
    public static final Column JOURNAL = Column.named("journal");

    @Part
    private static Mails ms;

    @BeforeSave
    protected void verifyData() {
        if (Strings.isFilled(email) && !ms.isValidMailAddress(email.trim(), null)) {
            throw Exceptions.createHandled().withNLSKey("Model.invalidEmail").set("value", email).handle();
        }
        if (Strings.isEmpty(getLogin().getUsername())) {
            getLogin().setUsername(getEmail());
        }
    }

    @BeforeSave
    @BeforeDelete
    protected void onModify() {
        TenantUserManager.flushCacheForUserAccount(this);
    }

    public int getMinPasswordLength() {
        return Sirius.getConfig().getInt("security.passwordMinLength");
    }

    public int getSanePasswordLength() {
        return Sirius.getConfig().getInt("security.passwordSaneLength");
    }

    public PersonData getPerson() {
        return person;
    }

    public LoginData getLogin() {
        return login;
    }

    public PermissionData getPermissions() {
        return permissions;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public JournalData getJournal() {
        return journal;
    }

    @Override
    public String toString() {
        if (Strings.isFilled(getPerson().toString())) {
            return getPerson().toString();
        }
        if (Strings.isFilled(getLogin().getUsername())) {
            return getLogin().getUsername();
        }

        return NLS.get("Model.userAccount");
    }
}
