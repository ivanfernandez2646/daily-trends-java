package com.ivanfernandez.dailytrends.contexts.cms.shared.fakes;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.DomainEvent;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.EventBus;
import java.util.ArrayList;
import java.util.List;

/** Keeps every published event, in order. Thread-safe. */
public final class RecordingEventBus implements EventBus {

    private final List<DomainEvent> published = new ArrayList<>();

    @Override
    public synchronized void publish(List<DomainEvent> events) {
        published.addAll(events);
    }

    public synchronized List<DomainEvent> published() {
        return List.copyOf(published);
    }
}
