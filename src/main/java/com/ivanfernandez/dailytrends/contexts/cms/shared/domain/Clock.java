package com.ivanfernandez.dailytrends.contexts.cms.shared.domain;

import java.time.Instant;
import java.time.ZoneId;

public interface Clock {

    Instant now();

    /** The business timezone: it defines "today" and "the same day". */
    ZoneId zone();
}
