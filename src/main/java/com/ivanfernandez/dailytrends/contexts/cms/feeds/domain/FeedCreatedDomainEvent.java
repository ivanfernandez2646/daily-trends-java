package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Map;

public final class FeedCreatedDomainEvent extends DomainEvent {

    public static final String EVENT_NAME = "feed.created";

    private final String title;

    public FeedCreatedDomainEvent(String aggregateId, String title, Instant occurredOn) {
        super(EVENT_NAME, aggregateId, occurredOn);
        this.title = title;
    }

    public String title() {
        return title;
    }

    @Override
    public Map<String, Object> attributes() {
        return Map.of("title", title);
    }
}
