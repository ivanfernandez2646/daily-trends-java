package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.MotherCreator;

public final class FeedTitleMother {

    private FeedTitleMother() {}

    public static FeedTitle random() {
        return new FeedTitle(MotherCreator.words(3));
    }
}
