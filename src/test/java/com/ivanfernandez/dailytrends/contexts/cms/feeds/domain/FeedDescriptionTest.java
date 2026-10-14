package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FeedDescriptionTest {

    @Test
    void accepts_null() {
        assertThat(new FeedDescription(null).value()).isNull();
    }

    @Test
    void accepts_an_empty_string() {
        assertThat(new FeedDescription("").value()).isEmpty();
    }

    @Test
    void is_equal_by_value() {
        assertThat(new FeedDescription(null))
                .isEqualTo(new FeedDescription(null))
                .isNotEqualTo(new FeedDescription(""));
        assertThat(new FeedDescription("a")).isEqualTo(new FeedDescription("a"));
    }
}
