package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.InvalidArgumentError;
import java.util.Arrays;

/** The fields a feed search can filter or order by, with their domain names. */
public enum FeedField {
    ID("id"),
    TITLE("title"),
    DESCRIPTION("description"),
    AUTHOR("author"),
    SOURCE("source"),
    CREATED_AT("created_at"),
    UPDATED_AT("updated_at");

    private final String value;

    FeedField(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static FeedField fromValue(String value) {
        return Arrays.stream(values())
                .filter(field -> field.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new InvalidArgumentError("<FeedField> does not allow the value <" + value + ">"));
    }
}
