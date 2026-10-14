package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.InvalidArgumentError;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FeedCreatedAtTest {

    @Test
    void truncates_to_milliseconds() {
        var createdAt = new FeedCreatedAt(Instant.parse("2026-10-12T08:00:00.123456789Z"));

        assertThat(createdAt.value()).isEqualTo(Instant.parse("2026-10-12T08:00:00.123Z"));
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejects_null() {
        assertThatThrownBy(() -> new FeedCreatedAt(null))
                .isInstanceOf(InvalidArgumentError.class)
                .hasMessage("<FeedCreatedAt> does not allow the value <null>");
    }

    @Test
    void is_equal_by_value_after_truncation() {
        assertThat(new FeedCreatedAt(Instant.parse("2026-10-12T08:00:00.123999Z")))
                .isEqualTo(new FeedCreatedAt(Instant.parse("2026-10-12T08:00:00.123Z")))
                .isNotEqualTo(new FeedCreatedAt(Instant.parse("2026-10-12T08:00:00.124Z")));
    }
}
