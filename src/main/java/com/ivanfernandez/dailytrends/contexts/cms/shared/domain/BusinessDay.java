package com.ivanfernandez.dailytrends.contexts.cms.shared.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** A calendar day in the business timezone, from local midnight to the next local midnight. */
public record BusinessDay(LocalDate date, ZoneId zone) {

    public static BusinessDay of(Instant instant, ZoneId zone) {
        return new BusinessDay(LocalDate.ofInstant(instant, zone), zone);
    }

    public Instant start() {
        return date.atStartOfDay(zone).toInstant();
    }

    public Instant end() {
        return date.plusDays(1).atStartOfDay(zone).toInstant();
    }
}
