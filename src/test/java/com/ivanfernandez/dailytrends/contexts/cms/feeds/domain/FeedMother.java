package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.MotherCreator;
import java.time.Instant;

public final class FeedMother {

    private FeedMother() {}

    /** A stored feed: no recorded events. */
    public static Feed random() {
        return Feed.fromPrimitives(FeedPrimitivesMother.random());
    }

    /** A newly created feed: it records {@code feed.created}. */
    public static Feed created(Instant now) {
        return Feed.create(
                FeedIdMother.random(),
                FeedTitleMother.random(),
                FeedDescriptionMother.random(),
                FeedAuthorMother.random(),
                MotherCreator.oneOf(FeedSource.values()),
                now);
    }
}
