/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.protocol;

import sirius.biz.web.BizController;
import sirius.db.es.Elastic;
import sirius.db.mixing.BaseEntity;
import sirius.db.mixing.Composite;
import sirius.db.mixing.EntityDescriptor;
import sirius.db.mixing.Mixing;
import sirius.db.mixing.Property;
import sirius.db.mixing.annotations.AfterDelete;
import sirius.db.mixing.annotations.AfterSave;
import sirius.db.mixing.annotations.Transient;
import sirius.kernel.Sirius;
import sirius.kernel.async.TaskContext;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;
import sirius.web.security.UserContext;

import java.time.LocalDateTime;
import java.util.stream.Stream;

/**
 * Provides a hook which records all changed fields into the system journal which can be embedded into other entities
 * or
 * mixins.
 * <p>
 * To skip a field, a {@link NoJournal} annotation can be placed. To skip a record entirely, {@link
 * #setSilent(boolean)}
 * can be called before the update or delete.
 */
public class JournalData extends Composite {

    @Transient
    private volatile boolean silent;

    @Transient
    private BaseEntity<?> owner;

    /**
     * Creates a new instance for the given entity.
     *
     * @param owner the entity which fields are to be recorded.
     */
    public JournalData(BaseEntity<?> owner) {
        this.owner = owner;
    }

    @AfterSave
    protected void onSave() {
        if (silent || !Sirius.isFrameworkEnabled(Protocols.FRAMEWORK_JOURNAL)) {
            return;
        }

        try {
            String changes = buildChangeJournal();

            if (changes.length() > 0) {
                addJournalEntry(owner, changes);
            }
        } catch (Exception e) {
            Exceptions.handle(e);
        }
    }

    /**
     * Enumerates all properties being journaled.
     * <p>
     * These are essentially all properties which do not wear a {@link NoJournal}.
     *
     * @return a stream of all journaled properties
     */
    public Stream<Property> fetchJournaledProperties() {
        return owner.getDescriptor()
                    .getProperties()
                    .stream()
                    .filter(p -> !p.getAnnotation(NoJournal.class).isPresent());
    }

    /**
     * Enumerates all journaled properties which are changed.
     *
     * @return a stream of all journaled properties which are changed
     */
    public Stream<Property> fetchJournaledAndChangedProperties() {
        return fetchJournaledProperties().filter(p -> p.getDescriptor().isChanged(owner, p));
    }

    /**
     * Reports all changed properties as a string.
     * <p>
     * This will output one line per changed propery like {@code old_value -&gt; new_value}.
     *
     * @return a string which lists all changed properties
     */
    public String buildChangeJournal() {
        StringBuilder changes = new StringBuilder();
        fetchJournaledAndChangedProperties().forEach(p -> {
            changes.append(p.getName());
            changes.append(": ");
            changes.append(NLS.toUserString(owner.getPersistedValue(p), NLS.getDefaultLanguage()));
            changes.append(" -> ");
            changes.append(NLS.toUserString(p.getValue(owner), NLS.getDefaultLanguage()));
            changes.append("\n");
        });

        return changes.toString();
    }

    /**
     * Adds an entry to the journal of the given entity.
     *
     * @param entity  the entity to write a journal entry for
     * @param changes the entry to add to the journal
     */
    public static void addJournalEntry(BaseEntity<?> entity, String changes) {
        if (!Sirius.isFrameworkEnabled(Protocols.FRAMEWORK_PROTOCOLS) || entity.isNew()) {
            return;
        }

        try {
            JournalEntry entry = new JournalEntry();
            entry.setTod(LocalDateTime.now());
            entry.setChanges(changes);
            entry.setTargetId(String.valueOf(entity.getId()));
            entry.setTargetName(entity.toString());
            entry.setTargetType(Mixing.getNameForType(entity.getClass()));
            entry.setSubsystem(TaskContext.get().getSystemString());
            entry.setUserId(UserContext.getCurrentUser().getUserId());
            entry.setUsername(UserContext.getCurrentUser().getProtocolUsername());
            elastic.update(entry);
        } catch (Exception e) {
            Exceptions.handle(Elastic.LOG, e);
        }
    }

    @AfterDelete
    protected void onDelete() {
        if (!silent) {
            try {
                addJournalEntry(owner, "Entity has been deleted.");
            } catch (Exception e) {
                Exceptions.handle(e);
            }
        }
    }

    /**
     * Determines if the next change should be skipped (not recorded).
     *
     * @return <tt>true</tt> if the next change should be skipped, <tt>false</tt> otherwise
     */
    public boolean isSilent() {
        return silent;
    }

    /**
     * Sets the skip flag.
     * <p>
     * Calling this with <tt>true</tt>, will skip all changes performed on the referenced entity instance.
     *
     * @param silent <tt>true</tt> to skip the recording of all changes on the referenced entity instance,
     *               <tt>false</tt> to re-enable.
     */
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    /**
     * Determines if there are recordable changes in the referenced entity.
     *
     * @return <tt>true</tt> if at least one journaled field changed, <tt>false</tt> otherwise
     */
    public boolean hasJournaledChanges() {
        if (silent) {
            return false;
        }

        EntityDescriptor descriptor = owner.getDescriptor();
        for (Property p : descriptor.getProperties()) {
            if (!p.getAnnotation(NoJournal.class).isPresent() && descriptor.isChanged(owner, p)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the URI shown in <tt>tracing.html.pasta</tt>.
     *
     * @return the URI which permits access to the journal of the attached entity (owner).
     */
    public String getProtocolUri() {
        if (owner.isNew()) {
            return "";
        }

        String type = Mixing.getNameForType(owner.getClass());
        String id = String.valueOf(owner.getId());

        return BizController.signLink("/system/protocol/" + type + "/" + id);
    }
}
