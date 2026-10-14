package com.ivanfernandez.dailytrends.apps.cmsbackend.feeds;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedPrimitives;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** The feed JSON of every route: keys in component order, {@code null} written, never omitted. */
public record FeedResponse(
        String id,
        String title,
        @Nullable String description,
        String author,
        String source,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") @Nullable Instant updatedAt) {

    public static FeedResponse from(FeedPrimitives feed) {
        return new FeedResponse(
                feed.id(),
                feed.title(),
                feed.description(),
                feed.author(),
                feed.source().name(),
                feed.createdAt(),
                feed.updatedAt());
    }
}
