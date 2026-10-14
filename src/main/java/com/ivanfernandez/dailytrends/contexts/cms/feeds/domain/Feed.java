package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.AggregateRoot;
import java.time.Instant;

public final class Feed extends AggregateRoot {

    private final FeedId id;
    private final FeedTitle title;
    private final FeedDescription description;
    private final FeedAuthor author;
    private final FeedSource source;
    private final FeedCreatedAt createdAt;
    private final FeedUpdatedAt updatedAt;

    private Feed(
            FeedId id,
            FeedTitle title,
            FeedDescription description,
            FeedAuthor author,
            FeedSource source,
            FeedCreatedAt createdAt,
            FeedUpdatedAt updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.author = author;
        this.source = source;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Feed create(
            FeedId id,
            FeedTitle title,
            FeedDescription description,
            FeedAuthor author,
            FeedSource source,
            Instant now) {
        var feed = new Feed(id, title, description, author, source, new FeedCreatedAt(now), FeedUpdatedAt.none());
        feed.record(new FeedCreatedDomainEvent(id.value(), title.value(), now));
        return feed;
    }

    public static Feed fromPrimitives(FeedPrimitives primitives) {
        return new Feed(
                new FeedId(primitives.id()),
                new FeedTitle(primitives.title()),
                new FeedDescription(primitives.description()),
                new FeedAuthor(primitives.author()),
                primitives.source(),
                new FeedCreatedAt(primitives.createdAt()),
                new FeedUpdatedAt(primitives.updatedAt()));
    }

    public FeedPrimitives toPrimitives() {
        return new FeedPrimitives(
                id.value(),
                title.value(),
                description.value(),
                author.value(),
                source,
                createdAt.value(),
                updatedAt.value());
    }

    public FeedId id() {
        return id;
    }

    public FeedTitle title() {
        return title;
    }

    public FeedDescription description() {
        return description;
    }

    public FeedAuthor author() {
        return author;
    }

    public FeedSource source() {
        return source;
    }

    public FeedCreatedAt createdAt() {
        return createdAt;
    }

    public FeedUpdatedAt updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Feed feed && id.equals(feed.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
