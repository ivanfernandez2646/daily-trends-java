package com.ivanfernandez.dailytrends.contexts.cms.shared.domain;

import java.util.List;

public interface EventBus {

    void publish(List<DomainEvent> events);
}
