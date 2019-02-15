/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.codelists.mongo;

import sirius.biz.codelists.CodeList;
import sirius.biz.codelists.CodeListData;
import sirius.biz.tenants.mongo.MongoTenantAware;
import sirius.kernel.di.std.Framework;

/**
 * Provides the MongoDB implementation of {@link CodeList}.
 */
@Framework(MongoCodeLists.FRAMEWORK_CODE_LISTS_MONGO)
public class MongoCodeList extends MongoTenantAware implements CodeList {

    private final CodeListData codeListData = new CodeListData(this);

    @Override
    public CodeListData getCodeListData() {
        return codeListData;
    }
}
