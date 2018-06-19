/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.sequences;

import sirius.db.jdbc.OMA;
import sirius.db.mixing.Mixing;
import sirius.kernel.commons.Wait;
import sirius.kernel.di.std.Framework;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.HandledException;
import sirius.kernel.health.Log;

import java.sql.SQLException;

/**
 * Provides a facility to generate unique consecutive numbers.
 * <p>
 * For each sequence name, a call to {@link #generateId(String)} will return a unique number. The initial or next
 * number being returned can be specified by {@link #setCounterValue(String, long, boolean)}.
 * <p>
 * Note that these sequences are global and not tenant aware. Therefore care must be taken to generate unique names for
 * sequences. A viable option is to use {@link sirius.db.mixing.BaseEntity#getUniqueName()} of the entity which utilizes the generator.
 */
@Framework("biz.sequences")
@Register(classes = Sequences.class)
public class Sequences {

    public static final Log LOG = Log.get("sequences");

    @Part
    private OMA oma;

    /**
     * Returns the next value in the given sequence.
     * <p>
     * Note that this method doesn't use locks or transactions. It rather utilizes optimistic locking, which scales
     * extremely well. However, the algorithm used is not intended for extreme parallel usage. In such scenarios,
     * it will not content but rather give up after some tries and report an appropriate exception.
     *
     * @param sequence the name of the sequence which is counted up.
     * @return the next value (which has not yet been returned). If the sequence is unknown, <tt>1</tt> is returned.
     * @throws HandledException If the system was unable to generate a unique sequence number. This might happen in
     *                          extreme load conditions, as internally an optimistic locking algorithm is employed.
     */
    public long generateId(String sequence) {
        try {
            int retries = 25;
            while (retries-- > 0) {
                Long id = tryGenerateId(sequence);
                if (id != null) {
                    return id;
                }

                // Block a short random amount of time to resolve conflicts with other waiting threads
                Wait.randomMillis(50, 100);
            }

            throw Exceptions.handle()
                            .to(LOG)
                            .withSystemErrorMessage(
                                    "Failed to generate an unique sequence number for %s. Giving up after 25 retries.",
                                    sequence)
                            .handle();
        } catch (SQLException e) {
            throw Exceptions.handle()
                            .to(LOG)
                            .error(e)
                            .withSystemErrorMessage(
                                    "Failed to generate an unique number for %s due to a database error: %s",
                                    sequence)
                            .handle();
        }
    }

    private Long tryGenerateId(String sequence) throws SQLException {
        // Select the current value which will be returned if all goes well....
        SequenceCounter result = oma.select(SequenceCounter.class).eq(SequenceCounter.NAME, sequence).queryFirst();
        if (result == null) {
            return createSequence(sequence);
        }

        int numRowsChanged = oma.getDatabase(Mixing.DEFAULT_REALM)
                                .createQuery("UPDATE sequencecounter"
                                             + "     SET nextValue = nextValue + 1"
                                             + "     WHERE name = ${name} "
                                             + "     AND nextValue = ${value}")
                                .set("name", sequence)
                                .set("value", result.getNextValue())
                                .executeUpdate();
        if (numRowsChanged == 1) {
            // Nobody else changed the counter, so we can savely return the determined value...
            return result.getNextValue();
        }
        return null;
    }

    private Long createSequence(String sequence) {
        SequenceCounter result;
        try {
            // Try to create a new record, as no counter is yet present...
            result = new SequenceCounter();
            result.setName(sequence);
            result.setNextValue(2);
            oma.update(result);
            return 1L;
        } catch (HandledException e) {
            // This only happens if another thread / server inserted the entity already...
            Exceptions.ignore(e);
            return null;
        }
    }

    /**
     * Sets the initial or next value of the given sequence.
     * <p>
     * Unless <tt>force</tt> is set to <tt>true</tt>, the value has to be higher than the current counter value to
     * prevent non unique numbers from being generated.
     *
     * @param sequence  the sequence to update
     * @param nextValue the next  value that will be returned when calling {@link #generateId(String)} for this
     *                  sequence.
     * @param force     if <tt>true</tt>, no sanity checks are performed and the sequence can be reset to <b>ANY</b>
     *                  value. This is rather dangerous, as it might lead to the generation of duplicate ids. If
     *                  set to <tt>false</tt>, the given <tt>nextValue</tt> has to be higher than the current sequence
     *                  value.
     */
    public void setCounterValue(String sequence, long nextValue, boolean force) {
        try {
            // Select the current value which will be returned if all goes well....
            if (oma.select(SequenceCounter.class).eq(SequenceCounter.NAME, sequence).exists()) {
                updateCounterValue(sequence, nextValue, force);
            } else {
                createSequenceWithValue(sequence, nextValue);
            }
        } catch (SQLException e) {
            throw Exceptions.handle()
                            .to(LOG)
                            .error(e)
                            .withSystemErrorMessage(
                                    "Failed to specify the next value for sequence %s due to a database error: %s",
                                    sequence)
                            .handle();
        }
    }

    private void createSequenceWithValue(String sequence, long nextValue) {
        try {
            // Try to create a new record, as no counter is yet present...
            SequenceCounter counter = new SequenceCounter();
            counter.setName(sequence);
            counter.setNextValue(nextValue);
            oma.update(counter);
        } catch (HandledException e) {
            throw Exceptions.handle()
                            .to(LOG)
                            .error(e)
                            .withSystemErrorMessage("Failed to specify the next value for sequence %s - %s (%s)",
                                                    sequence)
                            .handle();
        }
    }

    private void updateCounterValue(String sequence, long nextValue, boolean force) throws SQLException {
        String sql = "UPDATE sequencecounter SET nextValue = ${value} WHERE name = ${name}";
        if (!force) {
            sql += "  AND nextValue <= ${value}";
        }

        int updatedRows = oma.getDatabase(Mixing.DEFAULT_REALM)
                             .createQuery(sql)
                             .set("name", sequence)
                             .set("value", nextValue)
                             .executeUpdate();
        if (updatedRows != 1) {
            throw Exceptions.handle()
                            .to(LOG)
                            .withSystemErrorMessage("Failed to specify the next value for sequence %s", sequence)
                            .handle();
        }
    }
}
