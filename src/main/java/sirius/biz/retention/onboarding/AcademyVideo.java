/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.retention.onboarding;

import sirius.db.mixing.Mapping;

public interface AcademyVideo {

    Mapping VIDEO = Mapping.named("video");
    AcademyVideoData getVideo();

}
