package com.ivanfernandez.dailytrends.contexts.cms.shared.infrastructure;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.DomainEvent;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.DomainEventSubscriber;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.EventBus;
import java.util.List;

/** Dispatches synchronously, in the publisher's thread, to the subscribers of each event's class. */
public final class InMemoryEventBus implements EventBus {

    private final List<DomainEventSubscriber<?>> subscribers;

    public InMemoryEventBus(List<DomainEventSubscriber<?>> subscribers) {
        this.subscribers = List.copyOf(subscribers);
    }

    @Override
    public void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            for (DomainEventSubscriber<?> subscriber : subscribers) {
                deliver(subscriber, event);
            }
        }
    }

    private static <E extends DomainEvent> void deliver(DomainEventSubscriber<E> subscriber, DomainEvent event) {
        if (subscriber.subscribedTo().isInstance(event)) {
            subscriber.on(subscriber.subscribedTo().cast(event));
        }
    }
}
