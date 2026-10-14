package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.MotherCreator;

public final class FeedPrimitivesMother {

    private FeedPrimitivesMother() {}

    public static FeedPrimitives random() {
        return new FeedPrimitives(
                MotherCreator.uuid(),
                MotherCreator.words(3),
                MotherCreator.words(8),
                MotherCreator.words(2),
                MotherCreator.oneOf(FeedSource.values()),
                MotherCreator.instant(),
                null);
    }
}
