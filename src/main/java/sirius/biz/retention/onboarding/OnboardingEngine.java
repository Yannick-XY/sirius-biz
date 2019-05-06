/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.retention.onboarding;

import sirius.biz.analytics.flags.ExecutionFlags;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.std.Part;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public abstract class OnboardingEngine {

    @Part
    private ExecutionFlags executionFlags;

    @Part
    private GlobalContext context;

    public List<? extends AcademyVideo> getVideos(String academyProvider, String academy) {
//     executionFlags.
        loadVideosFromProvider(academyProvider, academy);

        return queryAcademyVideos(academyProvider, academy);
    }

    protected void loadVideosFromProvider(String academyProvider, String academy) {
        Set<String> currentVideoIds = fetchCurrentAcademyVideoIds(academyProvider, academy);

        try {
            AcademyProvider provider = context.findPart(academyProvider, AcademyProvider.class);
            provider.fetchVideos(academy, video -> {
                video.setAcademyProvider(academyProvider);
                video.setAcademy(academy);
                persistAcademyVideo(video);
                currentVideoIds.remove(video.getVideoId());
            });
        } catch (Exception e) {
            Exceptions.handle(Log.BACKGROUND, e);
        }

        markOutdatedAcademyVideosAsDeleted(academyProvider, academy, currentVideoIds);
    }

    protected abstract Set<String> fetchCurrentAcademyVideoIds(String academyProvider, String academy);

    protected abstract void persistAcademyVideo(AcademyVideoData video);

    protected abstract void markOutdatedAcademyVideosAsDeleted(String academyProvider,
                                                               String academy,
                                                               Set<String> videoIds);

    protected abstract List<? extends AcademyVideo> queryAcademyVideos(String academyProvider, String academy);

    public abstract Set<String> fetchCurrentOnboardingVideoIds(String academyProvider, String academy, String owner);

    public abstract OnboardingVideo<?> createOrUpdateOnboardingVideo(String owner,
                                                                     AcademyVideo academyVideo,
                                                                     Function<String, Boolean> permissionChecker);

    public abstract void markOutdatedOnboardingVideosAsDeleted(String academyProvider,
                                                               String academy,
                                                               String owner,
                                                               Set<String> videoIds);

    public abstract void deleteOnboardingVideosFor(String owner);
}
