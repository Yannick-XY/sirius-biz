/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.sequences

import sirius.kernel.InScenario
import sirius.kernel.di.Injector

@InScenario("test-jdbc.conf")
class SQLSequencesSpec extends SequencesSpec {

    def setupSpec() {
        sequences.sequenceStrategy = Injector.context().getPart("sql", SequenceStrategy.class)
    }

}
