package com.ivanfernandez.dailytrends.apps.cmsbackend.feeds;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedPrimitives;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** The feed JSON of every route: keys in component order, {@code null} written, never omitted. */
public record FeedResponse(
        String id,
        String title,
        // springdoc does not read JSpecify's @Nullable, so the schema says it.
        @Schema(types = {"string", "null"}) @Nullable String description,
        String author,
        String source,
        @JsonProperty("created_at") Instant createdAt,

        @JsonProperty("updated_at")
        @Schema(
                types = {"string", "null"},
                format = "date-time")
        @Nullable
        Instant updatedAt) {

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
