package com.ivanfernandez.dailytrends.apps.cmsbackend.feeds;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.apps.cmsbackend.IntegrationTest;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedPrimitives;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

/** Uses the application's own {@link JsonMapper}, so it checks the format the routes really write. */
@IntegrationTest
class FeedResponseJsonIT {

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void writes_the_keys_in_order_with_dates_with_and_without_milliseconds() {
        var feed = new FeedPrimitives(
                "9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b",
                "Title",
                "Description",
                "Author",
                FeedSource.EL_MUNDO,
                Instant.parse("2026-10-12T08:00:00Z"),
                Instant.parse("2026-10-12T08:00:00.123Z"));

        String json = jsonMapper.writeValueAsString(FeedResponse.from(feed));

        assertThat(json).isEqualTo("""
                        {"id":"9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b","title":"Title","description":"Description",\
                        "author":"Author","source":"EL_MUNDO","created_at":"2026-10-12T08:00:00Z",\
                        "updated_at":"2026-10-12T08:00:00.123Z"}""");
    }

    @Test
    void writes_a_missing_description_and_update_date_as_null() {
        var feed = new FeedPrimitives(
                "9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b",
                "Title",
                null,
                "Author",
                FeedSource.EL_ESPANOL,
                Instant.parse("2026-10-12T08:00:00Z"),
                null);

        String json = jsonMapper.writeValueAsString(FeedResponse.from(feed));

        assertThat(json).isEqualTo("""
                        {"id":"9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b","title":"Title","description":null,\
                        "author":"Author","source":"EL_ESPANOL","created_at":"2026-10-12T08:00:00Z",\
                        "updated_at":null}""");
    }
}
