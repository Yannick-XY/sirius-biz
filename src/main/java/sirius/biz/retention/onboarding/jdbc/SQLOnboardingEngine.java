/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.retention.onboarding.jdbc;

import sirius.biz.retention.onboarding.AcademyVideo;
import sirius.biz.retention.onboarding.AcademyVideoData;
import sirius.biz.retention.onboarding.OnboardingEngine;
import sirius.biz.retention.onboarding.OnboardingVideo;
import sirius.biz.retention.onboarding.OnboardingVideoData;
import sirius.db.jdbc.OMA;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Register(classes = OnboardingEngine.class)
public class SQLOnboardingEngine extends OnboardingEngine {

    @Part
    private OMA oma;

    @Override
    protected Set<String> fetchCurrentAcademyVideoIds(String academyProvider, String academy) {
        return oma.select(SQLAcademyVideo.class)
                  .eq(SQLAcademyVideo.VIDEO.inner(AcademyVideoData.ACADEMY_PROVIDER), academyProvider)
                  .eq(SQLAcademyVideo.VIDEO.inner(AcademyVideoData.ACADEMY), academy)
                  .fields(SQLAcademyVideo.VIDEO.inner(AcademyVideoData.VIDEO_ID))
                  .queryList()
                  .stream()
                  .map(v -> v.getVideo().getVideoId())
                  .collect(Collectors.toSet());
    }

    @Override
    protected void persistAcademyVideo(AcademyVideoData videoData) {
        SQLAcademyVideo video = oma.select(SQLAcademyVideo.class)
                                   .eq(SQLAcademyVideo.VIDEO.inner(AcademyVideoData.ACADEMY_PROVIDER),
                                       videoData.getAcademyProvider())
                                   .eq(SQLAcademyVideo.VIDEO.inner(AcademyVideoData.ACADEMY), videoData.getAcademy())
                                   .eq(SQLAcademyVideo.VIDEO.inner(AcademyVideoData.VIDEO_ID), videoData.getVideoId())
                                   .first()
                                   .orElseGet(SQLAcademyVideo::new);
        video.getVideo().loadFrom(videoData);
        oma.update(video);
    }

    @Override
    protected void markOutdatedAcademyVideosAsDeleted(String academyProvider, String academy, Set<String> videoIds) {
        for (String videoId : videoIds) {
            oma.select(SQLAcademyVideo.class)
               .eq(SQLAcademyVideo.VIDEO.inner(AcademyVideoData.ACADEMY_PROVIDER), academyProvider)
               .eq(SQLAcademyVideo.VIDEO.inner(AcademyVideoData.ACADEMY), academy)
               .eq(SQLAcademyVideo.VIDEO.inner(AcademyVideoData.VIDEO_ID), videoId)
               .first()
               .ifPresent(video -> {
                   try {
                       video.getVideo().setDeleted(true);
                       oma.update(video);
                   } catch (Exception e) {
                       Exceptions.handle(Log.BACKGROUND, e);
                   }
               });
        }
    }

    @Override
    protected List<? extends AcademyVideo> queryAcademyVideos(String academyProvider, String academy) {
        return oma.select(SQLAcademyVideo.class)
                  .eq(SQLAcademyVideo.VIDEO.inner(AcademyVideoData.ACADEMY_PROVIDER), academyProvider)
                  .eq(SQLAcademyVideo.VIDEO.inner(AcademyVideoData.ACADEMY), academy)
                  .orderAsc(SQLAcademyVideo.VIDEO.inner(AcademyVideoData.PRIORITY))
                  .queryList();
    }

    @Override
    public Set<String> fetchCurrentOnboardingVideoIds(String academyProvider, String academy, String owner) {
        return oma.select(SQLOnboardingVideo.class)
                  .eq(SQLOnboardingVideo.ONBOARDING_VIDEO.inner(OnboardingVideoData.ACADEMY_PROVIDER), academyProvider)
                  .eq(SQLOnboardingVideo.ONBOARDING_VIDEO.inner(OnboardingVideoData.ACADEMY), academy)
                  .eq(SQLOnboardingVideo.ONBOARDING_VIDEO.inner(OnboardingVideoData.OWNER), owner)
                  .fields(SQLAcademyVideo.ID)
                  .queryList()
                  .stream()
                  .map(SQLOnboardingVideo::getIdAsString)
                  .collect(Collectors.toSet());
    }

    @Override
    public OnboardingVideo<?> createOrUpdateOnboardingVideo(String owner,
                                                            AcademyVideo academyVideo,
                                                            Function<String, Boolean> permissionChecker) {
        SQLOnboardingVideo onboardingVideo = oma.select(SQLOnboardingVideo.class)
                                                .eq(SQLOnboardingVideo.ACADEMY_VIDEO, academyVideo)
                                                .eq(SQLOnboardingVideo.ONBOARDING_VIDEO.inner(OnboardingVideoData.OWNER),
                                                    owner)
                                                .first()
                                                .orElseGet(() -> {
                                                    SQLOnboardingVideo newVideo = new SQLOnboardingVideo();
                                                    newVideo.getAcademyVideo()
                                                            .setId(((SQLAcademyVideo) academyVideo).getId());
                                                    newVideo.getOnboardingVideo().setCreated(LocalDateTime.now());
                                                    return newVideo;
                                                });

        AcademyVideoData academyVideoData = academyVideo.getVideo();
        OnboardingVideoData onboardingVideoData = onboardingVideo.getOnboardingVideo();
        onboardingVideoData.setLastUpdated(LocalDateTime.now());
        onboardingVideoData.setDeleted(false);
        onboardingVideoData.setAcademyProvider(academyVideoData.getAcademyProvider());
        onboardingVideoData.setAcademy(academyVideoData.getAcademy());
        onboardingVideoData.setOwner(owner);
        onboardingVideoData.setPriority(academyVideoData.getPriority());
        onboardingVideoData.setRecommended(academyVideoData.isMandatory()
                                           && permissionChecker.apply(academyVideoData.getRequiredFeature())
                                           && permissionChecker.apply(academyVideoData.getRequiredPermission()));

        oma.update(onboardingVideo);

        return onboardingVideo;
    }

    @Override
    public void markOutdatedOnboardingVideosAsDeleted(String academyProvider,
                                                      String academy,
                                                      String owner,
                                                      Set<String> videoIds) {
        for (String videoId : videoIds) {
            oma.find(SQLOnboardingVideo.class, videoId).ifPresent(video -> {
                try {
                    video.getOnboardingVideo().setDeleted(true);
                    oma.update(video);
                } catch (Exception e) {
                    Exceptions.handle(Log.BACKGROUND, e);
                }
            });
        }
    }

    @Override
    public void deleteOnboardingVideosFor(String owner) {
        oma.select(SQLOnboardingVideo.class)
           .eq(SQLOnboardingVideo.ONBOARDING_VIDEO.inner(OnboardingVideoData.OWNER), owner)
           .delete();
    }
}
