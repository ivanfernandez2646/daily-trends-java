package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

/** Shortcut for tests that work with primitives; variations go through {@link FeedMother#builder()}. */
public final class FeedPrimitivesMother {

    private FeedPrimitivesMother() {}

    public static FeedPrimitives random() {
        return FeedMother.builder().primitives();
    }
}
