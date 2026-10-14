package com.ivanfernandez.dailytrends.apps.cmsbackend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class DailyTrendsPropertiesTest {

    private static final Map<String, String> VALID = Map.of(
            "daily-trends.timezone", "Europe/Madrid",
            "daily-trends.scraping.timeout", "10s",
            "daily-trends.scraping.cooldown", "300s");

    @Test
    void binds_the_values() {
        DailyTrendsProperties properties = bind(Map.of());

        assertThat(properties.timezone()).isEqualTo(ZoneId.of("Europe/Madrid"));
        assertThat(properties.scraping().timeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.scraping().cooldown()).isEqualTo(Duration.ofSeconds(300));
    }

    @ParameterizedTest
    @CsvSource({"500ms, 500", "PT10S, 10000"})
    void accepts_spring_duration_formats(String timeout, long expectedMillis) {
        DailyTrendsProperties properties = bind(Map.of("daily-trends.scraping.timeout", timeout));

        assertThat(properties.scraping().timeout()).isEqualTo(Duration.ofMillis(expectedMillis));
    }

    @Test
    void accepts_a_zero_cooldown() {
        DailyTrendsProperties properties = bind(Map.of("daily-trends.scraping.cooldown", "0s"));

        assertThat(properties.scraping().cooldown()).isZero();
    }

    @ParameterizedTest
    @CsvSource({
        "daily-trends.scraping.timeout, 0s",
        "daily-trends.scraping.timeout, -1s",
        "daily-trends.scraping.cooldown, -1s",
        "daily-trends.timezone, Mars/Olympus"
    })
    void rejects_an_invalid_value_naming_the_property(String property, String value) {
        assertThatThrownBy(() -> bind(Map.of(property, value)))
                .isInstanceOf(BindException.class)
                .hasStackTraceContaining(property);
    }

    private static DailyTrendsProperties bind(Map<String, String> overrides) {
        Map<String, String> values = new HashMap<>(VALID);
        values.putAll(overrides);
        return new Binder(new MapConfigurationPropertySource(values))
                .bind("daily-trends", DailyTrendsProperties.class)
                .get();
    }
}
