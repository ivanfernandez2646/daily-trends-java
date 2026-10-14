package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.MotherCreator;

public final class FeedDescriptionMother {

    private FeedDescriptionMother() {}

    public static FeedDescription random() {
        return new FeedDescription(MotherCreator.words(8));
    }
}
