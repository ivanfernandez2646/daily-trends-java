package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.InvalidArgumentError;

public record FeedTitle(String value) {

    public FeedTitle {
        if (value == null || value.isBlank()) {
            throw new InvalidArgumentError("<FeedTitle> is mandatory. Current value: <" + value + ">");
        }
    }
}
