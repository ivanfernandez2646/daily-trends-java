package com.ivanfernandez.dailytrends.apps.cmsbackend.feeds;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.apps.cmsbackend.IntegrationTest;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedMother;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Criteria;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Adapted from Node's {@code scrap-feed.feature}, with fake scrapers and MongoDB in Testcontainers. */
@IntegrationTest
@Import(FakeScrapersConfiguration.class)
class FeedScrapGetControllerIT {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private FeedRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void emptyTheCollection() {
        mongoTemplate.getCollection("feeds").deleteMany(new Document());
    }

    @Test
    void saves_and_returns_the_scraped_feeds_in_scraper_order() {
        seedTwoCmsFeeds();

        MvcTestResult response = mvc.get().uri("/feed/scrap").exchange();

        assertThat(response).hasStatus(HttpStatus.OK).hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
        List<String> expectedTitles = new ArrayList<>(FakeScrapersConfiguration.EL_MUNDO_TITLES);
        expectedTitles.addAll(FakeScrapersConfiguration.EL_ESPANOL_TITLES);
        assertThat(response)
                .bodyJson()
                .extractingPath("$[*].title")
                .asArray()
                .containsExactlyElementsOf(expectedTitles);
        assertThat(response)
                .bodyJson()
                .extractingPath("$[*].source")
                .asArray()
                .containsExactly(
                        "EL_MUNDO",
                        "EL_MUNDO",
                        "EL_MUNDO",
                        "EL_MUNDO",
                        "EL_MUNDO",
                        "EL_ESPANOL",
                        "EL_ESPANOL",
                        "EL_ESPANOL",
                        "EL_ESPANOL",
                        "EL_ESPANOL");
        assertThat(response)
                .bodyJson()
                .extractingPath("$[0]")
                .asMap()
                .containsOnlyKeys("id", "title", "description", "author", "source", "created_at", "updated_at")
                .containsEntry("updated_at", null);
        assertThat(repository.search(Criteria.all())).hasSize(12);
    }

    @Test
    void saves_nothing_when_the_front_pages_have_not_changed_since_the_last_run_today() {
        seedTwoCmsFeeds();
        mvc.get().uri("/feed/scrap").exchange();

        MvcTestResult response = mvc.get().uri("/feed/scrap").exchange();

        assertThat(response).hasStatus(HttpStatus.OK).bodyJson().isEqualTo("[]");
        assertThat(repository.search(Criteria.all())).hasSize(12);
    }

    private void seedTwoCmsFeeds() {
        repository.save(FeedMother.builder().source(FeedSource.CMS).build());
        repository.save(FeedMother.builder().source(FeedSource.CMS).build());
    }
}
