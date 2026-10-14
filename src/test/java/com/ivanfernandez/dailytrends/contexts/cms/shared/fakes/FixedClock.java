package com.ivanfernandez.dailytrends.contexts.cms.shared.fakes;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** A clock that only moves when the test advances it. */
public final class FixedClock implements Clock {

    private Instant now;
    private final ZoneId zone;

    public FixedClock(Instant now, ZoneId zone) {
        this.now = now;
        this.zone = zone;
    }

    public static FixedClock at(String isoInstant) {
        return new FixedClock(Instant.parse(isoInstant), ZoneId.of("Europe/Madrid"));
    }

    @Override
    public synchronized Instant now() {
        return now;
    }

    @Override
    public ZoneId zone() {
        return zone;
    }

    public synchronized void advance(Duration duration) {
        now = now.plus(duration);
    }
}
