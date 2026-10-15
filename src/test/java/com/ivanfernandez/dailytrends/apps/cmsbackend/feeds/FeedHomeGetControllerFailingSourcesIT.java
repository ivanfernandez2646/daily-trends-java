package com.ivanfernandez.dailytrends.apps.cmsbackend.feeds;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.apps.cmsbackend.IntegrationTest;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.application.scrap.ScrapeRunCoordinator;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedMother;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import java.time.Duration;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
@Import(FailingScrapersConfiguration.class)
@TestPropertySource(properties = "daily-trends.scraping.cooldown=0s")
class FeedHomeGetControllerFailingSourcesIT {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private FeedRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private Clock clock;

    @Autowired
    private ScrapeRunCoordinator coordinator;

    @BeforeEach
    void emptyTheCollection() {
        mongoTemplate.getCollection("feeds").deleteMany(new Document());
    }

    @Test
    void keeps_answering_the_stale_feed_when_every_source_fails() {
        repository.save(FeedMother.builder()
                .title("Yesterday's headline")
                .source(FeedSource.EL_MUNDO)
                .createdAt(clock.now().minus(Duration.ofDays(3)))
                .build());
        mvc.get().uri("/feed/home").exchange();
        // The refresh holds the guard from the moment it is accepted, so this returns after the failed run.
        coordinator.runNow();

        var response = mvc.get().uri("/feed/home").exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$[*].title").asArray().containsExactly("Yesterday's headline");
    }
}
