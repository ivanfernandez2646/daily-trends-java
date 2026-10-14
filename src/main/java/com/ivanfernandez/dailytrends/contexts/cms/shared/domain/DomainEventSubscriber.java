package com.ivanfernandez.dailytrends.contexts.cms.shared.domain;

public interface DomainEventSubscriber<E extends DomainEvent> {

    Class<E> subscribedTo();

    void on(E event);
}
