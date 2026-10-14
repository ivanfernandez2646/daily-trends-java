package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.MotherCreator;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * The entry point for feed fixtures. {@link #random()} and {@link #builder()} give stored feeds (no
 * recorded events); {@link #created(Instant)} gives a new one that records {@code feed.created}.
 */
public final class FeedMother {

    private FeedMother() {}

    public static Feed random() {
        return builder().build();
    }

    public static Feed created(Instant now) {
        return Feed.create(
                FeedIdMother.random(),
                FeedTitleMother.random(),
                FeedDescriptionMother.random(),
                FeedAuthorMother.random(),
                MotherCreator.oneOf(FeedSource.values()),
                now);
    }

    /** Random valid values; set only what the test is about. */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String id = MotherCreator.uuid();
        private String title = MotherCreator.words(3);
        private @Nullable String description = MotherCreator.words(8);
        private String author = MotherCreator.words(2);
        private FeedSource source = MotherCreator.oneOf(FeedSource.values());
        private Instant createdAt = MotherCreator.instant();
        private @Nullable Instant updatedAt;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        public Builder source(FeedSource source) {
            this.source = source;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder createdAt(String isoInstant) {
            return createdAt(Instant.parse(isoInstant));
        }

        public Builder updatedAt(@Nullable Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Feed build() {
            return Feed.fromPrimitives(primitives());
        }

        public FeedPrimitives primitives() {
            return new FeedPrimitives(id, title, description, author, source, createdAt, updatedAt);
        }
    }
}
