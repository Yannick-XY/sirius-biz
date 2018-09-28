/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.isenguard


import sirius.kernel.BaseSpecification
import sirius.kernel.di.std.Part

import java.util.concurrent.atomic.AtomicInteger

class IsenguardSpec extends BaseSpecification {

    @Part
    private static Isenguard isenguard

    def "rateLimitingWorks"() {
        when:
        def counter = new AtomicInteger()
        isenguard.isRateLimitReached("127.0.0.1",
                                     "test",
                                     Isenguard.USE_LIMIT_FROM_CONFIG,
                                     { -> counter.incrementAndGet() })
        isenguard.isRateLimitReached("127.0.0.1", "test", Isenguard.USE_LIMIT_FROM_CONFIG,
                                     { -> counter.incrementAndGet() })
        isenguard.isRateLimitReached("127.0.0.1", "test", Isenguard.USE_LIMIT_FROM_CONFIG,
                                     { -> counter.incrementAndGet() })
        def fourth = isenguard.isRateLimitReached("127.0.0.1", "test", Isenguard.USE_LIMIT_FROM_CONFIG,
                                                  { -> counter.incrementAndGet() })
        def fifth = isenguard.isRateLimitReached("127.0.0.1", "test", Isenguard.USE_LIMIT_FROM_CONFIG,
                                                 { -> counter.incrementAndGet() })
        def sixth = isenguard.isRateLimitReached("127.0.0.1", "test", Isenguard.USE_LIMIT_FROM_CONFIG,
                                                 { -> counter.incrementAndGet() })
        then:
        fourth == false
        fifth == true
        sixth == true
        counter.get() == 1
    }

}
