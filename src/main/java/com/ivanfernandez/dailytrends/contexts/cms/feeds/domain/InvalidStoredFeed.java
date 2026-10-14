package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import org.jspecify.annotations.Nullable;

/** A stored document that cannot become a {@link Feed}. A server error, not an invalid argument. */
public final class InvalidStoredFeed extends RuntimeException {

    public InvalidStoredFeed(@Nullable String id, String reason) {
        super("Invalid stored feed <" + id + ">: " + reason);
    }
}
