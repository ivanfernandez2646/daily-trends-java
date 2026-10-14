package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import org.jspecify.annotations.Nullable;

/** Optional: {@code null} means the feed has no description. */
public record FeedDescription(@Nullable String value) {}
