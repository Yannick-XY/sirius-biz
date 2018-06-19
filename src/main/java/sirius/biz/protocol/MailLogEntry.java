/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.protocol;

import sirius.db.jdbc.SQLEntity;
import sirius.db.mixing.Mapping;
import sirius.db.mixing.annotations.Length;
import sirius.db.mixing.annotations.Lob;
import sirius.db.mixing.annotations.NullAllowed;
import sirius.kernel.di.std.Framework;

import java.time.LocalDateTime;

/**
 * Stores a copy of a mail sent by the system.
 */

@Framework(Protocols.FRAMEWORK_PROTOCOLS)
public class MailLogEntry extends SQLEntity {

    /**
     * Contains the timestamp when the mail was sent
     */
    public static final Mapping TOD = Mapping.named("tod");
    private LocalDateTime tod = LocalDateTime.now();

    /**
     * Stores a flag which determines if sending the mail was successful.
     */
    public static final Mapping SUCCESS = Mapping.named("success");
    private boolean success = true;

    /**
     * Contains the message ID generated by JavaMail.
     */
    public static final Mapping MESSAGE_ID = Mapping.named("messageId");
    @Length(128)
    private String messageId;

    /**
     * Contains the eMail address of the sender.
     */
    public static final Mapping SENDER = Mapping.named("sender");
    @Length(255)
    private String sender;

    /**
     * Contains the name of the sender.
     */
    public static final Mapping SENDER_NAME = Mapping.named("senderName");
    @Length(255)
    @NullAllowed
    private String senderName;

    /**
     * Contains the eMail address of the receiver.
     */
    public static final Mapping RECEIVER = Mapping.named("receiver");
    @Length(255)
    private String receiver;

    /**
     * Contains the name of the receiver.
     */
    public static final Mapping RECEIVER_NAME = Mapping.named("receiverName");
    @Length(255)
    @NullAllowed
    private String receiverName;

    /**
     * Contains the subject of the mail.
     */
    public static final Mapping SUBJECT = Mapping.named("subject");
    @Length(1024)
    private String subject;

    /**
     * Contains the text content of the mail.
     */
    public static final Mapping TEXT = Mapping.named("text");
    @Lob
    @NullAllowed
    private String text;

    /**
     * Contains the HTML content of the mail.
     */
    public static final Mapping HTML = Mapping.named("html");
    @Lob
    @NullAllowed
    private String html;

    /**
     * Contains the node which acutally sent the mail.
     */
    public static final Mapping NODE = Mapping.named("node");
    @Length(255)
    private String node;

    /**
     * Contains the type of the email (what template was used to generate it).
     */
    public static final Mapping MAIL_TYPE = Mapping.named("mailType");
    @Length(255)
    @NullAllowed
    private String mailType;

    public LocalDateTime getTod() {
        return tod;
    }

    public void setTod(LocalDateTime tod) {
        this.tod = tod;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getMailType() {
        return mailType;
    }

    public void setMailType(String mailType) {
        this.mailType = mailType;
    }
}
