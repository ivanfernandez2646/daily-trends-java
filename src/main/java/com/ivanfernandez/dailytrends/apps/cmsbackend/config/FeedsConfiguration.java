package com.ivanfernandez.dailytrends.apps.cmsbackend.config;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.persistence.mongo.MongoFeedRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration(proxyBeanMethods = false)
public class FeedsConfiguration {

    @Bean
    FeedRepository feedRepository(MongoTemplate mongoTemplate) {
        return new MongoFeedRepository(mongoTemplate);
    }
}
