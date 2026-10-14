package com.ivanfernandez.dailytrends.apps.cmsbackend.config;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("daily-trends")
public record DailyTrendsProperties(ZoneId timezone, Scraping scraping) {

    public DailyTrendsProperties {
        requireNonNull(timezone, "daily-trends.timezone is required");
        requireNonNull(scraping, "daily-trends.scraping is required");
    }

    public record Scraping(Duration timeout, Duration cooldown) {

        public Scraping {
            requireNonNull(timeout, "daily-trends.scraping.timeout is required");
            requireNonNull(cooldown, "daily-trends.scraping.cooldown is required");
            if (timeout.isZero() || timeout.isNegative()) {
                throw new IllegalArgumentException("daily-trends.scraping.timeout must be greater than zero");
            }
            if (cooldown.isNegative()) {
                throw new IllegalArgumentException("daily-trends.scraping.cooldown must not be negative");
            }
        }
    }
}
