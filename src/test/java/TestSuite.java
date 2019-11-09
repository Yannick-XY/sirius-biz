/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

import com.googlecode.junittoolbox.SuiteClasses;
import org.junit.runner.RunWith;
import sirius.kernel.Scenario;
import sirius.kernel.ScenarioSuite;
import sirius.kernel.Scenarios;

@RunWith(ScenarioSuite.class)
@SuiteClasses({"**/*Test.class", "**/*Spec.class"})
@Scenarios({@Scenario(file = "test-jdbc.conf"), @Scenario(file = "test-mongo.conf")})
public class TestSuite {

}
