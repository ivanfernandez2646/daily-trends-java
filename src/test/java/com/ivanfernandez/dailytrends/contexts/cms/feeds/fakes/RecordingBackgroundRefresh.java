package com.ivanfernandez.dailytrends.contexts.cms.feeds.fakes;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.application.scrap.BackgroundRefresh;
import java.util.concurrent.atomic.AtomicInteger;

/** Counts the refresh requests it receives. Thread-safe. */
public final class RecordingBackgroundRefresh implements BackgroundRefresh {

    private final AtomicInteger requests = new AtomicInteger();

    @Override
    public void requestBackgroundRefresh() {
        requests.incrementAndGet();
    }

    public int requests() {
        return requests.get();
    }
}
