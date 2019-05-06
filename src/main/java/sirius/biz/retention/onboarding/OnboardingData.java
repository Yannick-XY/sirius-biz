/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.retention.onboarding;

import sirius.db.mixing.BaseEntity;
import sirius.db.mixing.annotations.AfterDelete;
import sirius.db.mixing.annotations.Transient;
import sirius.kernel.di.std.Part;

public class OnboardingData {

    private int percentWatched;
    private int percentSkipped;
    private int numWatchableVideos;
    private int numAvailableVideos;

    @Transient
    private BaseEntity<?> owner;

    @Part
    private OnboardingEngine onboardingEngine;

    public OnboardingData(BaseEntity<?> owner) {
        this.owner = owner;
    }

    @AfterDelete
    protected void deleteVideos() {
        onboardingEngine.deleteOnboardingVideosFor(owner.getIdAsString());
    }

    public int getPercentWatched() {
        return percentWatched;
    }

    public void setPercentWatched(int percentWatched) {
        this.percentWatched = percentWatched;
    }

    public int getPercentSkipped() {
        return percentSkipped;
    }

    public void setPercentSkipped(int percentSkipped) {
        this.percentSkipped = percentSkipped;
    }

    public int getNumWatchableVideos() {
        return numWatchableVideos;
    }

    public void setNumWatchableVideos(int numWatchableVideos) {
        this.numWatchableVideos = numWatchableVideos;
    }

    public int getNumAvailableVideos() {
        return numAvailableVideos;
    }

    public void setNumAvailableVideos(int numAvailableVideos) {
        this.numAvailableVideos = numAvailableVideos;
    }
}
