package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.InvalidArgumentError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FeedTitleTest {

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    void rejects_an_empty_or_blank_value(String value) {
        assertThatThrownBy(() -> new FeedTitle(value))
                .isInstanceOf(InvalidArgumentError.class)
                .hasMessage("<FeedTitle> is mandatory. Current value: <" + value + ">");
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejects_null() {
        assertThatThrownBy(() -> new FeedTitle(null))
                .isInstanceOf(InvalidArgumentError.class)
                .hasMessage("<FeedTitle> is mandatory. Current value: <null>");
    }

    @Test
    void keeps_the_value_untrimmed() {
        assertThat(new FeedTitle(" a ").value()).isEqualTo(" a ");
    }

    @Test
    void is_equal_by_value() {
        assertThat(new FeedTitle("a")).isEqualTo(new FeedTitle("a")).isNotEqualTo(new FeedTitle("b"));
    }
}
