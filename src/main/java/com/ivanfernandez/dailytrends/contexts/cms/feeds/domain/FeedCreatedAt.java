package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.InvalidArgumentError;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record FeedCreatedAt(Instant value) {

    public FeedCreatedAt {
        if (value == null) {
            throw new InvalidArgumentError("<FeedCreatedAt> does not allow the value <null>");
        }
        value = value.truncatedTo(ChronoUnit.MILLIS);
    }
}
