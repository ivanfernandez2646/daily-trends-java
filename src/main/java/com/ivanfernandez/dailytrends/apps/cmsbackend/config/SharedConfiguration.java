package com.ivanfernandez.dailytrends.apps.cmsbackend.config;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.EventBus;
import com.ivanfernandez.dailytrends.contexts.cms.shared.infrastructure.InMemoryEventBus;
import com.ivanfernandez.dailytrends.contexts.cms.shared.infrastructure.SystemClock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SharedConfiguration {

    @Bean
    Clock clock(DailyTrendsProperties properties) {
        return new SystemClock(properties.timezone());
    }

    @Bean
    EventBus eventBus() {
        return new InMemoryEventBus(List.of());
    }
}
