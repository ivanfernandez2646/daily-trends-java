package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.InvalidArgumentError;
import org.junit.jupiter.api.Test;

class FeedFieldTest {

    @Test
    void finds_a_field_by_its_domain_name() {
        assertThat(FeedField.fromValue("created_at")).isEqualTo(FeedField.CREATED_AT);
        assertThat(FeedField.CREATED_AT.value()).isEqualTo("created_at");
    }

    @Test
    void rejects_an_unknown_field() {
        assertThatThrownBy(() -> FeedField.fromValue("tittle"))
                .isInstanceOf(InvalidArgumentError.class)
                .hasMessage("<FeedField> does not allow the value <tittle>");
    }
}
