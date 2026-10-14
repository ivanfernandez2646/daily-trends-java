package com.ivanfernandez.dailytrends.contexts.cms.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BusinessDayTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    @ParameterizedTest
    @CsvSource({"2026-10-08T21:30:00Z, 2026-10-08", "2026-10-08T22:30:00Z, 2026-10-09"})
    void is_the_calendar_day_in_the_business_timezone(String instant, String date) {
        BusinessDay day = BusinessDay.of(Instant.parse(instant), MADRID);

        assertThat(day.date()).isEqualTo(LocalDate.parse(date));
    }

    @ParameterizedTest
    @CsvSource({"2026-10-08T21:59:59.999Z", "2026-10-07T22:00:00Z"})
    void starts_and_ends_at_local_midnight(String instant) {
        BusinessDay day = BusinessDay.of(Instant.parse(instant), MADRID);

        assertThat(day.start()).isEqualTo(Instant.parse("2026-10-07T22:00:00Z"));
        assertThat(day.end()).isEqualTo(Instant.parse("2026-10-08T22:00:00Z"));
    }

    @ParameterizedTest
    @CsvSource({"2026-03-29T12:00:00Z, PT23H", "2026-10-25T12:00:00Z, PT25H"})
    void lasts_23_or_25_hours_on_daylight_saving_days(String instant, Duration length) {
        BusinessDay day = BusinessDay.of(Instant.parse(instant), MADRID);

        assertThat(Duration.between(day.start(), day.end())).isEqualTo(length);
    }
}
