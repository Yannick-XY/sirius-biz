/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.codelists;

import sirius.db.mixing.BaseEntity;
import sirius.db.mixing.Mapping;
import sirius.db.mixing.types.BaseEntityRef;
import sirius.kernel.commons.Explain;

/**
 * Provides the database independent interface for describing a code list entry.
 * <p>
 * Note that all fields are represented via {@link CodeListEntryData}.
 *
 * @param <I> the type of database IDs used by the concrete implementation
 * @param <L> the effective entity type used to represent code lists
 */
@SuppressWarnings("squid:S1214")
@Explain("We rather keep the constants here, as this emulates the behaviour and layout of a real enttiy.")
public interface CodeListEntry<I, L extends BaseEntity<I> & CodeList> {

    /**
     * Represents the reference of the code list to which this entry belongs.
     */
    Mapping CODE_LIST = Mapping.named("codeList");

    /**
     * Contains the composite which holds the actual entry data.
     */
    Mapping CODE_LIST_ENTRY_DATA = Mapping.named("codeListEntryData");

    /**
     * Returns a string representation of the entity ID.
     * <p>
     * If the entity is new, "new" will be returned.
     *
     * @return the entity ID as string or "new" if the entity {@link BaseEntity#isNew()}.
     */
    String getIdAsString();

    BaseEntityRef<I, L> getCodeList();

    CodeListEntryData getCodeListEntryData();
}
