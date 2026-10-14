package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.MotherCreator;

public final class FeedIdMother {

    private FeedIdMother() {}

    public static FeedId random() {
        return new FeedId(MotherCreator.uuid());
    }
}
