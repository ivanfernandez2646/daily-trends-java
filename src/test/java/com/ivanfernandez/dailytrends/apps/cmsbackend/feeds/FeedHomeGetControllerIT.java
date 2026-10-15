package com.ivanfernandez.dailytrends.apps.cmsbackend.feeds;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.apps.cmsbackend.IntegrationTest;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.application.scrap.ScrapeRunCoordinator;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedMother;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.fakes.FakeFrontPageScraper;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Adapted from Node's {@code home-feed.feature}, with fake scrapers and MongoDB in Testcontainers. The
 * cooldown is off so each test can start its own background run.
 */
@IntegrationTest
@Import(FakeScrapersConfiguration.class)
@TestPropertySource(properties = "daily-trends.scraping.cooldown=0s")
class FeedHomeGetControllerIT {

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

    @Autowired
    @Qualifier("elMundoFake")
    private FakeFrontPageScraper elMundoScraper;

    private int callsBefore;

    @BeforeEach
    void emptyTheCollection() {
        mongoTemplate.getCollection("feeds").deleteMany(new Document());
        callsBefore = elMundoScraper.calls();
    }

    @Test
    void answers_an_empty_array_and_starts_no_run_when_the_database_is_empty() {
        MvcTestResult response = mvc.get().uri("/feed/home").exchange();

        assertThat(response).hasStatus(HttpStatus.OK).hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
        assertThat(response).bodyJson().isEqualTo("[]");
        assertThat(backgroundRunsStarted()).isZero();
    }

    @Test
    void answers_the_el_mundo_feed_first_and_the_el_espanol_one_second_without_a_run_when_the_page_is_fresh() {
        Instant now = clock.now();
        seed(FeedMother.builder()
                .id("de32eacb-1ce8-408f-b07b-8c3b0e0437c0")
                .source(FeedSource.CMS)
                .createdAt("2023-06-16T14:58:39.733Z"));
        seed(FeedMother.builder()
                .id("aacff872-8f95-43ad-9434-076befee22ee")
                .title("Test Feed Find 1")
                .description("Description - Test Feed Find 1")
                .source(FeedSource.EL_ESPANOL)
                .createdAt("2023-06-17T14:58:39.733Z"));
        seed(FeedMother.builder()
                .id("2305ecd9-068d-45bc-ada8-c6778ecf863e")
                .source(FeedSource.CMS)
                .createdAt("2023-06-18T14:58:39.733Z"));
        seed(FeedMother.builder()
                .id("798516e0-ee38-4a64-a0be-f3af830d8815")
                .title("Test Feed Find 4")
                .description("Description - Test Feed Find 4")
                .source(FeedSource.EL_MUNDO)
                .createdAt(now));

        MvcTestResult response = mvc.get().uri("/feed/home").exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response)
                .bodyJson()
                .extractingPath("$[*].id")
                .asArray()
                .containsExactly("798516e0-ee38-4a64-a0be-f3af830d8815", "aacff872-8f95-43ad-9434-076befee22ee");
        assertThat(response)
                .bodyJson()
                .extractingPath("$[*].source")
                .asArray()
                .containsExactly("EL_MUNDO", "EL_ESPANOL");
        assertThat(response)
                .bodyJson()
                .extractingPath("$[0]")
                .asMap()
                .containsOnlyKeys("id", "title", "description", "author", "source", "created_at", "updated_at")
                .containsEntry("title", "Test Feed Find 4");
        assertThat(backgroundRunsStarted()).isZero();
    }

    @Test
    void shows_the_scraped_feeds_once_a_scrap_run_has_saved_them_when_only_cms_feeds_exist() {
        seed(FeedMother.builder().source(FeedSource.CMS));
        seed(FeedMother.builder().source(FeedSource.CMS));

        assertThat(mvc.get().uri("/feed/home").exchange()).bodyJson().isEqualTo("[]");
        assertThat(mvc.get().uri("/feed/scrap").exchange())
                .bodyJson()
                .extractingPath("$")
                .asArray()
                .hasSize(10);

        MvcTestResult home = mvc.get().uri("/feed/home").exchange();
        assertThat(home)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$")
                .asArray()
                .hasSize(10);
        assertThat(home).bodyJson().extractingPath("$[*].source").asArray().doesNotContain("CMS");
    }

    @Test
    void answers_the_stale_feed_at_once_and_a_background_run_replaces_it_with_the_scraped_feeds() {
        seed(FeedMother.builder()
                .title("Yesterday's headline")
                .source(FeedSource.EL_MUNDO)
                .createdAt(clock.now().minus(Duration.ofDays(3))));

        MvcTestResult stale = mvc.get().uri("/feed/home").exchange();

        assertThat(stale).hasStatus(HttpStatus.OK);
        assertThat(stale).bodyJson().extractingPath("$[*].title").asArray().containsExactly("Yesterday's headline");
        assertThat(backgroundRunsStarted()).isEqualTo(1);

        List<String> scrapedTitles = new ArrayList<>(FakeScrapersConfiguration.EL_MUNDO_TITLES);
        scrapedTitles.addAll(FakeScrapersConfiguration.EL_ESPANOL_TITLES);
        MvcTestResult fresh = mvc.get().uri("/feed/home").exchange();
        assertThat(fresh)
                .bodyJson()
                .extractingPath("$[*].title")
                .asArray()
                .containsExactlyInAnyOrderElementsOf(scrapedTitles);
    }

    private void seed(FeedMother.Builder feed) {
        repository.save(feed.build());
    }

    /**
     * How many background runs the requests of this test started. A refresh holds the guard from the moment
     * it is accepted, so {@code runNow()} returns only after it, and adds exactly one run of its own.
     */
    private int backgroundRunsStarted() {
        coordinator.runNow();
        return elMundoScraper.calls() - callsBefore - 1;
    }
}
