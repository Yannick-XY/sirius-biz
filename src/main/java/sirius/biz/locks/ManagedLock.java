/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.locks;

import sirius.db.mixing.Column;
import sirius.db.mixing.Entity;
import sirius.db.mixing.annotations.Index;
import sirius.db.mixing.annotations.Length;
import sirius.db.mixing.annotations.Unique;
import sirius.kernel.di.std.Framework;

import java.time.LocalDateTime;

/**
 * Represents a lock in the database which is used to implement object based locking via a optimistic strategy.
 *
 * @see Locks
 */
@Framework("locks")
@Index(name = "unique_name", columns = "name", unique = true)
public class ManagedLock extends Entity {

    /**
     * Contains the name of the lock.
     */
    public static final Column NAME = Column.named("name");
    @Unique
    @Length(100)
    private String name;

    /**
     * Contains the owner holding the lock.
     */
    public static final Column OWNER = Column.named("owner");
    @Length(150)
    private String owner;

    /**
     * Contains the thread holding the lock.
     */
    public static final Column THREAD = Column.named("thread");
    @Length(150)
    private String thread;

    /**
     * Determines if the lock is currently acquired.
     */
    public static final Column ACQUIRED = Column.named("acquired");
    private LocalDateTime acquired;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public LocalDateTime getAcquired() {
        return acquired;
    }

    public void setAcquired(LocalDateTime acquired) {
        this.acquired = acquired;
    }
}
