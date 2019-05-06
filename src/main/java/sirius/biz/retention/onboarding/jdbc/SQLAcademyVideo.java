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
import sirius.db.jdbc.SQLEntity;
import sirius.db.mixing.Mapping;

public class SQLAcademyVideo extends SQLEntity implements AcademyVideo {

    private final AcademyVideoData video = new AcademyVideoData();

    public AcademyVideoData getVideo() {
        return video;
    }
}
