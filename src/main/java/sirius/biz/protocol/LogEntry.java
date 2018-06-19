/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.protocol;

import sirius.db.mixing.Mapping;
import sirius.db.jdbc.SQLEntity;
import sirius.db.mixing.annotations.Index;
import sirius.db.mixing.annotations.Length;
import sirius.db.mixing.annotations.Lob;
import sirius.kernel.di.std.Framework;

import java.time.LocalDateTime;

/**
 * Stores a log message in the database.
 */
@Framework(Protocols.FRAMEWORK_PROTOCOLS)
@Index(name = "category_idx", columns = "category")
@Index(name = "level_idx", columns = "level")
@Index(name = "tod_idx", columns = "tod")
public class LogEntry extends SQLEntity {

    /**
     * Contains the logged message.
     */
    public static final Mapping MESSAGE = Mapping.named("message");
    @Lob
    private String message;

    /**
     * Contains the name of the logger which create the message.
     */
    public static final Mapping CATEGORY = Mapping.named("category");
    @Length(50)
    private String category;

    /**
     * Contains the log level of the message.
     */
    public static final Mapping LEVEL = Mapping.named("level");
    @Length(50)
    private String level;

    /**
     * Contains the name of the node on which the message was logged.
     */
    public static final Mapping NODE = Mapping.named("node");
    @Length(50)
    private String node;

    /**
     * Contains the timestamp when the message was logged.
     */
    public static final Mapping TOD = Mapping.named("tod");
    private LocalDateTime tod = LocalDateTime.now();

    /**
     * Contains the name of the user which was active when the message was logged.
     */
    public static final Mapping USER = Mapping.named("user");
    @Length(255)
    private String user;

    /**
     * Returns a CSS class which can be used to style the HTML table displaying the entries.
     *
     * @return a CSS clas matching the log level of the entry
     */
    public String getLabelClass() {
        if ("ERROR".equals(getLevel())) {
            return "label-important";
        }
        if ("WARN".equals(getLevel())) {
            return "label-warning";
        }
        if ("DEBUG".equals(getLevel())) {
            return "";
        }
        return "label-info";
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public LocalDateTime getTod() {
        return tod;
    }

    public void setTod(LocalDateTime tod) {
        this.tod = tod;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
