/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.translations.mongo;

import sirius.biz.codelists.CodeListController;
import sirius.biz.codelists.mongo.MongoCodeLists;
import sirius.biz.jobs.batch.file.EntityImportJobFactory;
import sirius.db.mixing.BaseEntity;
import sirius.kernel.di.std.Register;
import sirius.web.security.Permission;

import javax.annotation.Nonnull;

@Register(framework = MongoCodeLists.FRAMEWORK_CODE_LISTS_MONGO)
@Permission(CodeListController.PERMISSION_MANAGE_CODELISTS)
public class MongoTranslationImportJobFactory extends EntityImportJobFactory {
    @Override
    protected Class<? extends BaseEntity<?>> getImportType() {
        return MongoTranslation.class;
    }

    @Nonnull
    @Override
    public String getName() {
        return "import-mongo-translations";
    }
}
