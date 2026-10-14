package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.InvalidArgumentError;

public record FeedAuthor(String value) {

    public FeedAuthor {
        if (value == null || value.isBlank()) {
            throw new InvalidArgumentError("<FeedAuthor> is mandatory. Current value: <" + value + ">");
        }
    }
}
