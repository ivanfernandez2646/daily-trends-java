package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.MotherCreator;

public final class FeedAuthorMother {

    private FeedAuthorMother() {}

    public static FeedAuthor random() {
        return new FeedAuthor(MotherCreator.words(2));
    }
}
