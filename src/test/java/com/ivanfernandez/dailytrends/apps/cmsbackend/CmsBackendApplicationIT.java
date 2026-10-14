package com.ivanfernandez.dailytrends.apps.cmsbackend;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.EventBus;
import java.time.ZoneId;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

@IntegrationTest
class CmsBackendApplicationIT {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private Clock clock;

    @Autowired
    private EventBus eventBus;

    @Test
    void starts_connected_to_mongodb() {
        Document result = mongoTemplate.executeCommand(new Document("ping", 1));

        assertThat(result.get("ok")).isEqualTo(1.0);
    }

    @Test
    void provides_a_clock_in_the_business_timezone_and_an_event_bus() {
        assertThat(clock.zone()).isEqualTo(ZoneId.of("Europe/Madrid"));
        assertThat(eventBus).isNotNull();
    }
}
