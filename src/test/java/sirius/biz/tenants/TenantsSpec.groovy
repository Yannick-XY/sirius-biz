/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.tenants


import sirius.kernel.BaseSpecification
import sirius.kernel.InScenario
import sirius.web.security.UserContext
import sirius.web.security.UserInfo

@InScenario(["test-jdbc.conf", "test-mongo.conf"])
class TenantsSpec extends BaseSpecification {

    def "installTestTenant works"() {
        when:
        TenantsHelper.installTestTenant()
        then:
        UserContext.get().getUser().hasPermission(UserInfo.PERMISSION_LOGGED_IN)
    }
}
