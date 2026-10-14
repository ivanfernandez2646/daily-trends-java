package com.ivanfernandez.dailytrends.apps.cmsbackend.config;

import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration(proxyBeanMethods = false)
public class MongoConfiguration {

    private static final long SERVER_SELECTION_TIMEOUT_SECONDS = 5;

    @Bean
    MongoClientSettingsBuilderCustomizer serverSelectionTimeout() {
        return builder -> builder.applyToClusterSettings(
                cluster -> cluster.serverSelectionTimeout(SERVER_SELECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    // The client connects lazily; without this ping an unreachable MongoDB would only fail the first request.
    @Bean
    SmartInitializingSingleton mongoStartupPing(MongoTemplate mongoTemplate) {
        return () -> mongoTemplate.executeCommand(new Document("ping", 1));
    }
}
