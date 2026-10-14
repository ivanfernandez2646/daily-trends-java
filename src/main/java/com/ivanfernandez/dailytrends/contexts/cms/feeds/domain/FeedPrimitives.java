package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record FeedPrimitives(
        String id,
        String title,
        @Nullable String description,
        String author,
        FeedSource source,
        Instant createdAt,
        @Nullable Instant updatedAt) {}
