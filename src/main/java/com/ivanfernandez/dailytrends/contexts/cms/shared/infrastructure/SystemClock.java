package com.ivanfernandez.dailytrends.contexts.cms.shared.infrastructure;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** The only place that reads the system time. */
public final class SystemClock implements Clock {

    private final ZoneId zone;

    public SystemClock(ZoneId zone) {
        this.zone = zone;
    }

    @Override
    public Instant now() {
        return Instant.now();
    }

    @Override
    public ZoneId zone() {
        return zone;
    }
}
