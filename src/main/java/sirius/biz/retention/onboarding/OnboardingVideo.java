/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.retention.onboarding;

import sirius.db.mixing.Mapping;
import sirius.db.mixing.types.BaseEntityRef;

public interface OnboardingVideo<I> {

    Mapping ACADEMY_VIDEO = Mapping.named("academyVideo");

    BaseEntityRef<I, ? extends AcademyVideo> getAcademyVideo();

    String getIdAsString();
}
