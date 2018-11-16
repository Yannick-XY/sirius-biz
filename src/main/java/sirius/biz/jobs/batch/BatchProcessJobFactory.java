/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.jobs.batch;

import com.alibaba.fastjson.JSONObject;
import sirius.biz.cluster.work.DistributedTaskExecutor;
import sirius.biz.cluster.work.DistributedTasks;
import sirius.biz.jobs.BasicJobFactory;
import sirius.biz.process.ProcessContext;
import sirius.biz.process.ProcessLink;
import sirius.biz.process.Processes;
import sirius.biz.process.logs.ProcessLog;
import sirius.kernel.di.std.Part;
import sirius.web.http.WebContext;
import sirius.web.security.UserContext;
import sirius.web.services.JSONStructuredOutput;

import java.util.Map;

/**
 * Provides a base implementation for batch jobs which are executed as
 * {@link sirius.biz.cluster.work.DistributedTasks.DistributedTask} within a process {@link Process}.
 */
public abstract class BatchProcessJobFactory extends BasicJobFactory {

    public static final String CONTEXT_PROCESS = "process";
    public static final String CONTEXT_JOB_FACTORY = "jobFactory";

    @Part
    protected Processes processes;

    @Part
    protected DistributedTasks tasks;

    @Override
    public String getIcon() {
        return "fa-cogs";
    }

    @Override
    public boolean canStartInUI() {
        return true;
    }

    @Override
    public void executeInUI(WebContext request, Map<String, String> context) {
        String processId = startWithContext(context);
        request.respondWith().redirectToGet("/ps/" + processId);
    }

    @Override
    public boolean canStartInCall() {
        return true;
    }

    @Override
    protected void executeInCall(JSONStructuredOutput out, Map<String, String> context) {
        String processId = startWithContext(context);
        processes.outputAsJSON(processId, out);
    }

    @Override
    public boolean canStartInBackground() {
        return true;
    }

    @Override
    protected void executeInBackground(Map<String, String> context) {
        startWithContext(context);
    }

    /**
     * Creates a {@link Process} and schedules a {@link sirius.biz.cluster.work.DistributedTasks.DistributedTask}
     * to execute the job (on this node or another one).
     *
     * @param context the parameters supplied by the user
     * @return the id of the newly created process
     */
    protected String startWithContext(Map<String, String> context) {
        String processId = processes.createProcessForCurrentUser(createProcessTitle(context), getIcon(), context);
        logScheduledMessage(processId);
        addLinkToJob(processId);
        createAndScheduleDistributedTask(processId);

        return processId;
    }

    /**
     * Logs an initial message to record when the job was scheduled.
     *
     * @param processId the process representing the execution of this job
     */
    protected void logScheduledMessage(String processId) {
        processes.log(processId, ProcessLog.info().withNLSKey("BatchProcessJobFactory.scheduled"));
    }

    /**
     * Adds a link for this job to the {@link Process}.
     * <p>
     * This can be overwritten to suppress this behaviour.
     *
     * @param processId the id of the process which has been created
     */
    protected void addLinkToJob(String processId) {
        processes.addLink(processId,
                          new ProcessLink().withLabel("$BatchProcessJobFactory.jobLink").withUri("/job/" + getName()));
    }

    private void createAndScheduleDistributedTask(String processId) {
        JSONObject executorContext =
                new JSONObject().fluentPut(CONTEXT_PROCESS, processId).fluentPut(CONTEXT_JOB_FACTORY, getName());
        if (tasks.getQueueInfo(tasks.getQueueName(getExecutor())).isPrioritized()) {
            tasks.submitPrioritizedTask(getExecutor(), getPenaltyToken(), executorContext);
        } else {
            tasks.submitFIFOTask(getExecutor(), executorContext);
        }
    }

    /**
     * Creates the title for the {@link Process} based on the given context.
     *
     * @param context the parameters supplied by the user
     * @return the title to use for the process
     */
    protected abstract String createProcessTitle(Map<String, String> context);

    /**
     * Returns the executor which is responsible for resolving the created {@link sirius.biz.process.Process} and
     * then invoking {@link #executeTask(ProcessContext)}.
     * <p>
     * Different executors can be used to run jobs in differen queues or with different priorization settings.
     *
     * @return the executor used to handle the queueing and eventually the execution of this job
     */
    protected abstract Class<? extends DistributedTaskExecutor> getExecutor();

    /**
     * If the queue in which the {@link #getExecutor() executor} places its tasks is <b>prioritized</b>,
     * we use this method to determine the penalty token.
     * <p>
     * By default this is the tenant id. Therefore all tasks of the same tenant increase the penalty
     * (and thus lowers the priority of newly scheduled tasks).
     *
     * @return the penalty token to use
     */
    protected String getPenaltyToken() {
        return UserContext.getCurrentUser().getTenantId();
    }

    /**
     * Executes the task on the target node and covered within the execution context of a {@link Process}.
     *
     * @param process the context of the previously generated process to communicate with the outside world
     * @throws Exception in case of any error which should abort this job
     */
    protected abstract void executeTask(ProcessContext process) throws Exception;
}
