/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.translations.jdbc;

import sirius.biz.translations.Translation;
import sirius.biz.translations.TranslationData;
import sirius.db.jdbc.SQLEntity;

/**
 * Stores translations as a table.
 * <p>
 * Note that translations should only be accessed via siblings of {@link sirius.biz.translations.BasicTranslations}.
 */
public class SQLTranslation extends SQLEntity implements Translation {
    private final TranslationData translationData = new TranslationData();

    @Override
    public TranslationData getTranslationData() {
        return translationData;
    }
}
