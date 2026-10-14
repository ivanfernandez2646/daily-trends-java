package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.jspecify.annotations.Nullable;

/**
 * Optional: {@code null} means the feed was never updated. Every {@code Instant} is valid, so the
 * spec's "does not allow the value" message cannot happen here.
 */
public record FeedUpdatedAt(@Nullable Instant value) {

    public FeedUpdatedAt {
        if (value != null) {
            value = value.truncatedTo(ChronoUnit.MILLIS);
        }
    }

    public static FeedUpdatedAt none() {
        return new FeedUpdatedAt(null);
    }
}
