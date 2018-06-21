/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.protocol;

import sirius.db.mixing.Composite;
import sirius.db.mixing.Mapping;
import sirius.db.mixing.annotations.BeforeSave;
import sirius.db.mixing.annotations.Length;
import sirius.db.mixing.annotations.NullAllowed;
import sirius.db.mixing.annotations.Transient;
import sirius.kernel.async.TaskContext;
import sirius.web.security.UserContext;

import java.time.LocalDateTime;

/**
 * Provides tracing information which can be embedded into other entities or mixins.
 */
public class TraceData extends Composite {

    /**
     * Stores the username of the user which created the assoicated entity.
     */
    public static final Mapping CREATED_BY = Mapping.named("createdBy");
    @NoJournal
    @NullAllowed
    @Length(50)
    private String createdBy;

    /**
     * Stores the timstamp when the associated entity was created.
     */
    public static final Mapping CREATED_AT = Mapping.named("createdAt");
    @NoJournal
    @NullAllowed
    private LocalDateTime createdAt;

    /**
     * Stores the system string ({@link TaskContext#getSystemString()} where the associated entity was created.
     */
    public static final Mapping CREATED_IN = Mapping.named("createdIn");
    @NoJournal
    @NullAllowed
    @Length(150)
    private String createdIn;

    /**
     * Stores the username of the user which last changed the associated entity.
     */
    public static final Mapping CHANGED_BY = Mapping.named("changedBy");
    @NoJournal
    @NullAllowed
    @Length(50)
    private String changedBy;

    /**
     * Stores the timestamp when the associated entity was last changed.
     */
    public static final Mapping CHANGED_AT = Mapping.named("changedAt");
    @NoJournal
    @NullAllowed
    private LocalDateTime changedAt;

    /**
     * Stores the system string ({@link TaskContext#getSystemString()} where the associated entity was last changed.
     */
    public static final Mapping CHANGED_IN = Mapping.named("changedIn");
    @NoJournal
    @NullAllowed
    @Length(150)
    private String changedIn;

    @Transient
    private boolean silent;

    @BeforeSave
    protected void update() {
        if (!silent) {
            if (createdAt == null) {
                createdBy = UserContext.getCurrentUser().getUserName();
                createdAt = LocalDateTime.now();
                createdIn = TaskContext.get().getSystemString();
            }
            changedBy = UserContext.getCurrentUser().getUserName();
            changedAt = LocalDateTime.now();
            changedIn = TaskContext.get().getSystemString();
        }
    }

    /**
     * Determines if change tracking is currently disabled.
     *
     * @return <tt>true</tt>, if change tracking is currently disabled, <tt>false</tt> otherwise
     */
    public boolean isSilent() {
        return silent;
    }

    /**
     * Can be used to disable or re-enable change tracking.
     * <p>
     * Some changes, performed by the system, should not update the tracing information. A login of a user
     * (which might increment its login counter) would be an example, where change tracking should be disabled.
     *
     * @param silent <tt>true</tt> if change tracking for the current entity instance should be disabled or
     *               <tt>false</tt> to re-enable
     */
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedIn() {
        return createdIn;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public String getChangedIn() {
        return changedIn;
    }
}
