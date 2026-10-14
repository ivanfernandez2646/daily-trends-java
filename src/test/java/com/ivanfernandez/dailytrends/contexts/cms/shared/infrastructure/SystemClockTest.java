package com.ivanfernandez.dailytrends.contexts.cms.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SystemClockTest {

    private final SystemClock clock = new SystemClock(ZoneId.of("Europe/Madrid"));

    @Test
    void exposes_the_business_zone() {
        assertThat(clock.zone()).isEqualTo(ZoneId.of("Europe/Madrid"));
    }

    @Test
    void reads_the_system_time() {
        Instant before = Instant.now();

        Instant now = clock.now();

        assertThat(now).isBetween(before, Instant.now());
    }
}
