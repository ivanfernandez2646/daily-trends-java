package com.ivanfernandez.dailytrends.contexts.cms.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedCreatedDomainEvent;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedMother;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.DomainEvent;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.DomainEventSubscriber;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryEventBusTest {

    @Test
    void delivers_each_event_to_its_subscribers() {
        var subscriber = new RecordingFeedCreatedSubscriber();
        var bus = new InMemoryEventBus(List.of(subscriber));
        List<DomainEvent> events =
                FeedMother.created(Instant.parse("2026-10-12T08:00:00Z")).pullDomainEvents();

        bus.publish(events);

        assertThat(subscriber.received()).containsExactlyElementsOf(events);
    }

    @Test
    void ignores_events_without_subscribers() {
        var bus = new InMemoryEventBus(List.of());
        List<DomainEvent> events =
                FeedMother.created(Instant.parse("2026-10-12T08:00:00Z")).pullDomainEvents();

        assertThatCode(() -> bus.publish(events)).doesNotThrowAnyException();
    }

    private static final class RecordingFeedCreatedSubscriber implements DomainEventSubscriber<FeedCreatedDomainEvent> {

        private final List<DomainEvent> received = new ArrayList<>();

        @Override
        public Class<FeedCreatedDomainEvent> subscribedTo() {
            return FeedCreatedDomainEvent.class;
        }

        @Override
        public void on(FeedCreatedDomainEvent event) {
            received.add(event);
        }

        List<DomainEvent> received() {
            return received;
        }
    }
}
