/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.retention.onboarding.jdbc;

import sirius.biz.retention.onboarding.AcademyVideo;
import sirius.biz.retention.onboarding.OnboardingVideo;
import sirius.biz.retention.onboarding.OnboardingVideoData;
import sirius.db.jdbc.SQLEntity;
import sirius.db.jdbc.SQLEntityRef;
import sirius.db.mixing.Mapping;
import sirius.db.mixing.types.BaseEntityRef;

public class SQLOnboardingVideo extends SQLEntity implements OnboardingVideo<Long> {

    private final SQLEntityRef<SQLAcademyVideo> academyVideo =
            SQLEntityRef.on(SQLAcademyVideo.class, BaseEntityRef.OnDelete.REJECT);

    public static final Mapping ONBOARDING_VIDEO = Mapping.named("onboardingVideo");
    private final OnboardingVideoData onboardingVideo = new OnboardingVideoData();

    @Override
    public BaseEntityRef<Long, ? extends AcademyVideo> getAcademyVideo() {
        return academyVideo;
    }

    public OnboardingVideoData getOnboardingVideo() {
        return onboardingVideo;
    }
}
