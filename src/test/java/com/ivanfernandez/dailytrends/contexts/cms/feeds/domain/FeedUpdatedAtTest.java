package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class FeedUpdatedAtTest {

    @Test
    void accepts_null_as_no_update() {
        assertThat(FeedUpdatedAt.none().value()).isNull();
        assertThat(new FeedUpdatedAt(null)).isEqualTo(FeedUpdatedAt.none());
    }

    @Test
    void truncates_to_milliseconds() {
        var updatedAt = new FeedUpdatedAt(Instant.parse("2026-10-12T08:00:00.123456789Z"));

        assertThat(updatedAt.value()).isEqualTo(Instant.parse("2026-10-12T08:00:00.123Z"));
    }

    @Test
    void is_equal_by_value_after_truncation() {
        assertThat(new FeedUpdatedAt(Instant.parse("2026-10-12T08:00:00.123999Z")))
                .isEqualTo(new FeedUpdatedAt(Instant.parse("2026-10-12T08:00:00.123Z")))
                .isNotEqualTo(FeedUpdatedAt.none());
    }
}
