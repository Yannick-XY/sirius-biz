/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.model;

import sirius.biz.web.Autoloaded;
import sirius.db.mixing.Column;
import sirius.db.mixing.Composite;
import sirius.db.mixing.annotations.BeforeSave;
import sirius.db.mixing.annotations.Length;
import sirius.db.mixing.annotations.NullAllowed;
import sirius.db.mixing.annotations.Transient;
import sirius.db.mixing.annotations.Trim;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Part;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;
import sirius.web.mails.Mails;

import java.util.regex.Pattern;

/**
 * Provides various contact information for a person or company which can be embedded into other entities or mixins.
 */
public class ContactData extends Composite {

    /**
     * Validates a phone numer.
     */
    public static final Pattern VALID_PHONE_NUMBER =
            Pattern.compile("\\+?\\d+( \\d+)*( */( *\\d+)+)?( *\\-( *\\d+)+)?");

    @Transient
    private boolean validatePhoneNumbers;

    /**
     * Creates a new instance.
     *
     * @param validatePhoneNumbers determines if phone numbers should be validated or not
     */
    public ContactData(boolean validatePhoneNumbers) {
        this.validatePhoneNumbers = validatePhoneNumbers;
    }

    /**
     * Contains an email address.
     * <p>
     * If the field is filled, it has to be a valid email address, otherwise an exception will be thrown.
     */
    public static final Column EMAIL = Column.named("email");
    @Trim
    @NullAllowed
    @Autoloaded
    @Length(length = 150)
    private String email;

    /**
     * Contains a phone number.
     * <p>
     * If the field is filled and <tt>validatePhoneNumbers</tt> is <tt>true</tt>, an exception is thrown
     * if the phone number has an illegal format.
     */
    public static final Column PHONE = Column.named("phone");
    @Trim
    @NullAllowed
    @Autoloaded
    @Length(length = 150)
    private String phone;

    /**
     * Contains a fax number.
     * <p>
     * If the field is filled and <tt>validatePhoneNumbers</tt> is <tt>true</tt>, an exception is thrown
     * if the fax number has an illegal format.
     */
    public static final Column FAX = Column.named("fax");
    @Trim
    @NullAllowed
    @Autoloaded
    @Length(length = 150)
    private String fax;

    /**
     * Contains a mobile number.
     * <p>
     * If the field is filled and <tt>validatePhoneNumbers</tt> is <tt>true</tt>, an exception is thrown
     * if the mobile phone number has an illegal format.
     */
    public static final Column MOBILE = Column.named("mobile");
    @Trim
    @NullAllowed
    @Autoloaded
    @Length(length = 150)
    private String mobile;

    @Part
    private static Mails mails;

    @BeforeSave
    protected void onSave() throws Exception {
        if (Strings.isFilled(email)) {
            mails.failForInvalidEmail(email, null);
        }
        if (validatePhoneNumbers) {
            if (Strings.isFilled(phone) && !VALID_PHONE_NUMBER.matcher(phone).matches()) {
                throw Exceptions.createHandled()
                                .withNLSKey("ContactData.invalidPhone")
                                .set("field", NLS.get("Model.phone"))
                                .set("value", phone)
                                .handle();
            }
            if (Strings.isFilled(fax) && !VALID_PHONE_NUMBER.matcher(fax).matches()) {
                throw Exceptions.createHandled()
                                .withNLSKey("ContactData.invalidPhone")
                                .set("field", NLS.get("Model.fax"))
                                .set("value", fax)
                                .handle();
            }
            if (Strings.isFilled(mobile) && !VALID_PHONE_NUMBER.matcher(mobile).matches()) {
                throw Exceptions.createHandled()
                                .withNLSKey("ContactData.invalidPhone")
                                .set("field", NLS.get("Model.mobile"))
                                .set("value", mobile)
                                .handle();
            }
        }
    }

    /**
     * Returns the email address.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address.
     *
     * @param email the email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the phone number.
     *
     * @return the phone numner
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the phone number.
     *
     * @param phone the phone number
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Returns the fax number.
     *
     * @return the fax number
     */
    public String getFax() {
        return fax;
    }

    /**
     * Sets the fax number.
     *
     * @param fax the fax number
     */
    public void setFax(String fax) {
        this.fax = fax;
    }

    /**
     * Returns the mobile number.
     *
     * @return the mobile number
     */
    public String getMobile() {
        return mobile;
    }

    /**
     * Sets the mobile number.
     *
     * @param mobile the mobile number
     */
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    @Override
    public String toString() {
        return email + " / " + phone + " / " + fax + " / " + mobile;
    }
}
