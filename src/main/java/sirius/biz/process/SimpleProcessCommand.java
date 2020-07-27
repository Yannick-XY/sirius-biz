/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.process;

import sirius.biz.process.logs.ProcessLog;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Values;

import java.util.Map;

/**
 * Provides the baseline for a simple {@link ProcessCommand} with no arguments.
 * <p>
 * Includes boilerplate for running the command in a simulation unless told to actually execute the command.
 * Implementing classes need to wrap the actual 'real execution' (like db updates/deletes) in an if checking for {@link #shouldExecute(ProcessContext)}.
 */
public abstract class SimpleProcessCommand extends ProcessCommand {

    private static final String EXECUTE = "execute";

    protected abstract String createBaseTitle();

    @Override
    protected String createTitle(Values args) {
        if (isExecParameterGiven(args)) {
            return createBaseTitle();
        }
        return createBaseTitle() + " (Simulation)";
    }

    protected abstract String createBaseDescription();

    @Override
    public String getDescription() {
        return createBaseDescription() + " Only simulates unless '" + EXECUTE + "' is appended.";
    }

    protected abstract void executeSimpleProcess(ProcessContext context);

    @Override
    protected void executeProcess(ProcessContext context) {
        executeSimpleProcess(context);
        if (isSimulation(context)) {
            context.log(ProcessLog.warn()
                                  .withMessage("Process was started as simulation - to actually execute it append '"
                                               + EXECUTE
                                               + "' to the command"));
        }
    }

    @Override
    protected void fillContext(Values args, Map<String, String> context) {
        if (isExecParameterGiven(args)) {
            context.put(EXECUTE, EXECUTE);
        }
    }

    protected boolean shouldExecute(ProcessContext context) {
        return context.get(EXECUTE).isFilled();
    }

    protected boolean isSimulation(ProcessContext context) {
        return !shouldExecute(context);
    }

    private boolean isExecParameterGiven(Values args) {
        return args.length() == 1 && Strings.areEqual(args.at(0).asString().toLowerCase(), EXECUTE);
    }
}
