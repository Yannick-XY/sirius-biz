/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.importer.format

import sirius.kernel.commons.Value
import spock.lang.Specification

class DateTimeFormatCheckSpec extends Specification {

    def "valid dates throws no exception"() {
        when:
        new DateTimeFormatCheck("dd.MM.uuuu").perform(Value.of("23.10.2019"))
        new DateTimeFormatCheck("dd.MM.uuuu").perform(Value.of("01.05.1854"))
        new DateTimeFormatCheck("dd.MM.uuuu").perform(Value.of("24.12.9000"))
        then:
        noExceptionThrown()
    }

    def "invalid date throws exception"() {
        when:
        new DateTimeFormatCheck("dd.MM.uuuu").perform(Value.of("31.09.2019"))
        then:
        thrown(IllegalArgumentException)
    }

    def "date with to little numbers is invalid"() {
        when:
        new DateTimeFormatCheck("dd.MM.uuuu").perform(Value.of("4.4.19"))
        then:
        thrown(IllegalArgumentException)
    }

    def "date with to much numbers is invalid"() {
        when:
        new DateTimeFormatCheck("dd.MM.uuuu").perform(Value.of("4.011.19"))
        then:
        thrown(IllegalArgumentException)
    }
}
