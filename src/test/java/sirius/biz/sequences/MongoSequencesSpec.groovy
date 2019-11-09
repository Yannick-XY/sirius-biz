/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.sequences

import sirius.db.mongo.Mango
import sirius.kernel.InScenario
import sirius.kernel.di.Injector
import sirius.kernel.di.std.Part

@InScenario("test-mongo.conf")
class MongoSequencesSpec extends SequencesSpec {

    @Part
    private static Mango mango

    def setupSpec() {
        sequences.sequenceStrategy = Injector.context().getPart("mongo", SequenceStrategy.class)
    }

    def "saving entities with sequential ids works"() {
        setup:
        SequentialMongoBizEntityA entity1 = new SequentialMongoBizEntityA()
        SequentialMongoBizEntityA entity2 = new SequentialMongoBizEntityA()
        //reset sequences that may have been set by other tests
        sequences.setNextValue(entity2.getTypeName(), 1, true)
        when:
        mango.update(entity1)
        mango.update(entity2)
        then:
        entity1.getId() == "1"
        and:
        entity2.getId() == "2"
    }

    def "different type of entities use different sequences"() {
        setup:
        SequentialMongoBizEntityA entity1 = new SequentialMongoBizEntityA()
        SequentialMongoBizEntityB entity2 = new SequentialMongoBizEntityB()
        //reset sequnces that may have been set by other tests
        sequences.setNextValue(entity1.getTypeName(), 1, true)
        sequences.setNextValue(entity2.getTypeName(), 1, true)
        when:
        mango.update(entity1)
        mango.update(entity2)
        then:
        entity1.getId() == "1"
        and:
        entity2.getId() == "1"
    }
    
}
