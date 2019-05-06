/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.retention.onboarding;

import sirius.biz.analytics.checks.DailyCheck;
import sirius.biz.protocol.Traced;
import sirius.db.mixing.BaseEntity;
import sirius.kernel.di.std.Part;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

public abstract class RecomputeOnboardingVideosCheck<E extends BaseEntity<?> & OnboardingParticipant>
        extends DailyCheck<E> {

    @Part
    private OnboardingEngine onboardingEngine;

    @Override
    protected void execute(E entity) {
        updateOnboardingVideos(entity);
        updateOnboardingData(entity);
    }

    protected void updateOnboardingVideos(E entity) {
        determineAcademies(entity, (provider, academy) -> updateOnboardingVideosForAcademy(entity, provider, academy));
    }

    protected void updateOnboardingVideosForAcademy(E entity, String provider, String academy) {
        Set<String> currentVideoIds =
                onboardingEngine.fetchCurrentOnboardingVideoIds(provider, academy, entity.getUniqueName());
        List<? extends AcademyVideo> videos = onboardingEngine.getVideos(provider, academy);
        for (AcademyVideo video : videos) {
            OnboardingVideo<?> onboardingVideo =
                    onboardingEngine.createOrUpdateOnboardingVideo(entity.getUniqueName(), video, permission -> checkPermission(entity, permission));
            currentVideoIds.remove(onboardingVideo.getIdAsString());
        }

        onboardingEngine.markOutdatedOnboardingVideosAsDeleted(provider,
                                                               academy,
                                                               entity.getUniqueName(),
                                                               currentVideoIds);
    }

    protected abstract Boolean checkPermission(E entity, String permission);

    protected void updateOnboardingData(E entity) {
        if (entity instanceof Traced) {
            ((Traced) entity).getTrace().setSilent(true);
        }
        entity.getDescriptor().getMapper().update(entity);
    }

    protected abstract void determineAcademies(E entity, BiConsumer<String, String> academyConsumer);
}


