package com.ivanfernandez.dailytrends.contexts.cms.shared.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public abstract class DomainEvent {

    private final UUID eventId;
    private final String eventName;
    private final String aggregateId;
    private final Instant occurredOn;

    protected DomainEvent(String eventName, String aggregateId, Instant occurredOn) {
        this.eventId = UUID.randomUUID();
        this.eventName = eventName;
        this.aggregateId = aggregateId;
        this.occurredOn = occurredOn;
    }

    public UUID eventId() {
        return eventId;
    }

    public String eventName() {
        return eventName;
    }

    public String aggregateId() {
        return aggregateId;
    }

    public Instant occurredOn() {
        return occurredOn;
    }

    public abstract Map<String, Object> attributes();
}
