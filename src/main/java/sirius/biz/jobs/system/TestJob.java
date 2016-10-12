/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.jobs.system;

import sirius.kernel.commons.Context;
import sirius.kernel.di.std.Register;
import sirius.web.tasks.ManagedTaskContext;

import javax.annotation.Nonnull;

/**
 * Created by aha on 22.07.16.
 */
@Register(classes = SystemJobDescription.class)
public class TestJob extends SystemJobDescription {

    @Override
    public void execute(Context parameters, ManagedTaskContext task) {
        task.log("Hello");
    }

    @Nonnull
    @Override
    public String getName() {
        return "test";
    }
}
