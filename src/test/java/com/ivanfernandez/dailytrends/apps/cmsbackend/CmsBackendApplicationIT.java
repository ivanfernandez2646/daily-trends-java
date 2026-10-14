package com.ivanfernandez.dailytrends.apps.cmsbackend;

import static org.assertj.core.api.Assertions.assertThat;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

@IntegrationTest
class CmsBackendApplicationIT {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void starts_connected_to_mongodb() {
        Document result = mongoTemplate.executeCommand(new Document("ping", 1));

        assertThat(result.get("ok")).isEqualTo(1.0);
    }
}
