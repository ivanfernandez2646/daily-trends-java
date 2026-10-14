package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FeedTest {

    private static final Instant NOW = Instant.parse("2026-10-12T08:00:00.123456Z");

    @Test
    void create_sets_the_creation_date_and_no_update_date() {
        Feed feed = FeedMother.created(NOW);

        assertThat(feed.createdAt()).isEqualTo(new FeedCreatedAt(Instant.parse("2026-10-12T08:00:00.123Z")));
        assertThat(feed.updatedAt()).isEqualTo(FeedUpdatedAt.none());
    }

    @Test
    void create_records_a_feed_created_event() {
        Feed feed = FeedMother.created(NOW);

        assertThat(feed.pullDomainEvents())
                .singleElement()
                .isInstanceOfSatisfying(FeedCreatedDomainEvent.class, event -> {
                    assertThat(event.eventName()).isEqualTo("feed.created");
                    assertThat(event.aggregateId()).isEqualTo(feed.id().value());
                    assertThat(event.title()).isEqualTo(feed.title().value());
                    assertThat(event.attributes())
                            .containsExactly(Map.entry("title", feed.title().value()));
                    assertThat(event.occurredOn()).isEqualTo(NOW);
                    assertThat(event.eventId()).isNotNull();
                });
    }

    @Test
    void pulling_the_events_clears_them() {
        Feed feed = FeedMother.created(NOW);

        feed.pullDomainEvents();

        assertThat(feed.pullDomainEvents()).isEmpty();
    }

    @Test
    void from_primitives_records_no_events() {
        Feed feed = Feed.fromPrimitives(FeedPrimitivesMother.random());

        assertThat(feed.pullDomainEvents()).isEmpty();
    }

    @Test
    void round_trips_through_primitives() {
        var primitives = new FeedPrimitives(
                "9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b",
                "Title",
                null,
                "Author",
                FeedSource.EL_MUNDO,
                Instant.parse("2026-10-12T08:00:00.123Z"),
                Instant.parse("2026-10-13T08:00:00Z"));

        assertThat(Feed.fromPrimitives(primitives).toPrimitives()).isEqualTo(primitives);
    }

    @Test
    void is_equal_by_id_only() {
        Feed feed = FeedMother.random();
        Feed sameIdOtherValues = FeedMother.builder().id(feed.id().value()).build();

        assertThat(feed)
                .isEqualTo(sameIdOtherValues)
                .hasSameHashCodeAs(sameIdOtherValues)
                .isNotEqualTo(FeedMother.random());
    }
}
