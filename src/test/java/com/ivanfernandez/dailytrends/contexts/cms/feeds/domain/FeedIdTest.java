package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.InvalidArgumentError;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FeedIdTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "c232ab00-9414-11ec-b3c8-9f6bdeced846", // v1
                "9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b", // v4
                "886313e1-3b8a-5372-9b90-0c9aee199e5d", // v5
                "9B2F3A4E-1C5D-4E6F-8A7B-0C1D2E3F4A5B", // upper case
                "00000000-0000-0000-0000-000000000000" // nil
            })
    void accepts_uuid_versions_1_to_5_and_the_nil_uuid(String value) {
        assertThat(new FeedId(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "01890a5d-ac96-774b-bcce-b302099a8057", // v7
                "not-a-uuid",
                "9b2f3a4e1c5d4e6f8a7b0c1d2e3f4a5b",
                ""
            })
    void rejects_any_other_value(String value) {
        assertThatThrownBy(() -> new FeedId(value))
                .isInstanceOf(InvalidArgumentError.class)
                .hasMessage("<FeedId> does not allow the value <" + value + ">");
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejects_null() {
        assertThatThrownBy(() -> new FeedId(null))
                .isInstanceOf(InvalidArgumentError.class)
                .hasMessage("<FeedId> does not allow the value <null>");
    }

    @Test
    void is_equal_by_value() {
        String value = "9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b";

        assertThat(new FeedId(value)).isEqualTo(new FeedId(value)).isNotEqualTo(FeedIdMother.random());
    }

    @Test
    void random_creates_a_new_uuid_v4_each_time() {
        FeedId first = FeedId.random();
        FeedId second = FeedId.random();

        assertThat(UUID.fromString(first.value()).version()).isEqualTo(4);
        assertThat(first).isNotEqualTo(second);
    }
}
