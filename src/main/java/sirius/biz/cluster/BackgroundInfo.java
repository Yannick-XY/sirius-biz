/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.cluster;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains infomation about all background jobs or processes running on a node.
 */
public class BackgroundInfo {
    private String nodeName;
    private boolean bleeding;
    private String uptime;
    protected Map<String, BackgroundJobInfo> jobs = new HashMap<>();

    protected BackgroundInfo(String nodeName, boolean bleeding, String uptime) {
        this.nodeName = nodeName;
        this.bleeding = bleeding;
        this.uptime = uptime;
    }

    /**
     * Returns the name of the node.
     *
     * @return the name of the node
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * Returns the uptime of the node.
     *
     * @return the uptime of the node
     */
    public String getUptime() {
        return uptime;
    }

    public boolean isBleeding() {
        return bleeding;
    }

    /**
     * Provides a map of all background jobs running on a node.
     *
     * @return a map of all background jobs using the name as key
     */
    public Map<String, BackgroundJobInfo> getJobs() {
        return Collections.unmodifiableMap(jobs);
    }
}
