package com.ivanfernandez.dailytrends.contexts.cms.feeds.application.scrap;

import static com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource.CMS;
import static com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource.EL_ESPANOL;
import static com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource.EL_MUNDO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedCreatedDomainEvent;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedField;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedMother;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedTitle;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FrontPageScraper;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.fakes.FakeFrontPageScraper;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.fakes.InMemoryFeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Criteria;
import com.ivanfernandez.dailytrends.contexts.cms.shared.fakes.FixedClock;
import com.ivanfernandez.dailytrends.contexts.cms.shared.fakes.RecordingEventBus;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class FeedScraperTest {

    private static final Executor SAME_THREAD = task -> task.run();

    private final FixedClock clock = FixedClock.at("2026-10-14T08:00:00Z");
    private final InMemoryFeedRepository repository = new InMemoryFeedRepository();
    private final RecordingEventBus eventBus = new RecordingEventBus();
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logs.start();
        feedScraperLogger().addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        feedScraperLogger().detachAppender(logs);
    }

    @Test
    void saves_publishes_and_returns_the_feeds_of_every_scraper_in_scraper_order() {
        var feedScraper = feedScraper(
                FakeFrontPageScraper.returning(EL_MUNDO, clock, "Mundo 1", "Mundo 2"),
                FakeFrontPageScraper.returning(EL_ESPANOL, clock, "Español 1"));

        List<Feed> saved = feedScraper.scrape();

        assertThat(titlesOf(saved)).containsExactly("Mundo 1", "Mundo 2", "Español 1");
        assertThat(saved).extracting(feed -> feed.source()).containsExactly(EL_MUNDO, EL_MUNDO, EL_ESPANOL);
        assertThat(repository.all()).containsExactlyInAnyOrderElementsOf(saved);
        assertThatPublishedFeedCreatedFor(saved);
    }

    @Test
    void saves_the_feeds_of_the_other_scrapers_when_one_fails() {
        var feedScraper = feedScraper(
                FakeFrontPageScraper.failing(EL_MUNDO, new IllegalStateException("connection reset")),
                FakeFrontPageScraper.returning(EL_ESPANOL, clock, "Español 1"));

        List<Feed> saved = feedScraper.scrape();

        assertThat(titlesOf(saved)).containsExactly("Español 1");
        assertThat(repository.all()).containsExactlyElementsOf(saved);
        assertThatPublishedFeedCreatedFor(saved);
        assertThat(logs.list).singleElement().satisfies(log -> {
            assertThat(log.getLevel()).isEqualTo(Level.WARN);
            assertThat(log.getFormattedMessage()).contains("EL_MUNDO");
            assertThat(log.getThrowableProxy().getMessage()).isEqualTo("connection reset");
        });
    }

    @Test
    void returns_nothing_when_every_scraper_fails_or_finds_nothing() {
        var feedScraper = feedScraper(
                FakeFrontPageScraper.failing(EL_MUNDO, new IllegalStateException("timeout")),
                FakeFrontPageScraper.returning(EL_ESPANOL, clock));

        List<Feed> saved = feedScraper.scrape();

        assertThat(saved).isEmpty();
        assertThat(repository.all()).isEmpty();
        assertThat(eventBus.published()).isEmpty();
    }

    @Test
    void skips_a_headline_already_stored_for_the_source_on_the_same_business_day() {
        Feed stored = storedFeed(EL_MUNDO, "Mundo 1", "2026-10-13T22:00:00Z");
        var feedScraper = feedScraper(FakeFrontPageScraper.returning(EL_MUNDO, clock, "Mundo 1", "Mundo 2"));

        List<Feed> saved = feedScraper.scrape();

        assertThat(titlesOf(saved)).containsExactly("Mundo 2");
        assertThat(repository.all()).hasSize(2).contains(stored);
        assertThatPublishedFeedCreatedFor(saved);
    }

    @Test
    void saves_only_the_first_of_a_headline_listed_twice_in_one_run() {
        var feedScraper = feedScraper(FakeFrontPageScraper.returning(EL_MUNDO, clock, "Mundo 1", "Mundo 1"));

        List<Feed> saved = feedScraper.scrape();

        assertThat(saved).hasSize(1);
        assertThat(repository.all()).containsExactlyElementsOf(saved);
        assertThatPublishedFeedCreatedFor(saved);
    }

    @Test
    void saves_a_headline_stored_on_the_previous_business_day() {
        storedFeed(EL_MUNDO, "Mundo 1", "2026-10-13T21:59:59.999Z");
        var feedScraper = feedScraper(FakeFrontPageScraper.returning(EL_MUNDO, clock, "Mundo 1"));

        List<Feed> saved = feedScraper.scrape();

        assertThat(titlesOf(saved)).containsExactly("Mundo 1");
        assertThat(repository.all()).hasSize(2);
    }

    @Test
    void saves_a_headline_stored_today_by_another_source() {
        storedFeed(CMS, "Mundo 1", "2026-10-14T07:00:00Z");
        var feedScraper = feedScraper(FakeFrontPageScraper.returning(EL_MUNDO, clock, "Mundo 1"));

        List<Feed> saved = feedScraper.scrape();

        assertThat(titlesOf(saved)).containsExactly("Mundo 1");
        assertThat(repository.all()).hasSize(2);
    }

    @Test
    void uses_the_business_timezone_to_decide_the_same_day() {
        var afterMadridMidnight = FixedClock.at("2026-10-08T22:30:00Z");
        storedFeed(EL_MUNDO, "Mundo 1", "2026-10-08T21:30:00Z");
        var feedScraper = new FeedScraper(
                List.of(FakeFrontPageScraper.returning(EL_MUNDO, afterMadridMidnight, "Mundo 1")),
                repository,
                eventBus,
                afterMadridMidnight,
                SAME_THREAD);

        List<Feed> saved = feedScraper.scrape();

        assertThat(titlesOf(saved)).containsExactly("Mundo 1");
        assertThat(repository.all()).hasSize(2);
    }

    @Test
    void propagates_a_save_error_and_keeps_the_feeds_saved_before_it() {
        var failingRepository = new FailingOnSaveFeedRepository(repository, 2);
        var feedScraper = new FeedScraper(
                List.of(FakeFrontPageScraper.returning(EL_MUNDO, clock, "Mundo 1", "Mundo 2", "Mundo 3")),
                failingRepository,
                eventBus,
                clock,
                SAME_THREAD);

        assertThatThrownBy(feedScraper::scrape)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("save number 2 failed");
        assertThat(titlesOf(repository.all())).containsExactly("Mundo 1");
        assertThat(eventBus.published()).hasSize(1);
    }

    @Test
    void runs_the_scrapers_in_parallel() {
        var bothStarted = new CountDownLatch(2);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var feedScraper = new FeedScraper(
                    List.of(waitingForEachOther(EL_MUNDO, bothStarted), waitingForEachOther(EL_ESPANOL, bothStarted)),
                    repository,
                    eventBus,
                    clock,
                    executor);

            List<Feed> saved = feedScraper.scrape();

            assertThat(saved).extracting(feed -> feed.source()).containsExactly(EL_MUNDO, EL_ESPANOL);
        }
    }

    private FeedScraper feedScraper(FrontPageScraper... scrapers) {
        return new FeedScraper(List.of(scrapers), repository, eventBus, clock, SAME_THREAD);
    }

    private Feed storedFeed(FeedSource source, String title, String createdAt) {
        Feed feed = FeedMother.builder()
                .source(source)
                .title(title)
                .createdAt(createdAt)
                .build();
        repository.save(feed);
        return feed;
    }

    /** Fails unless the other scraper is running at the same time. */
    private FrontPageScraper waitingForEachOther(FeedSource source, CountDownLatch bothStarted) {
        var feeds = FakeFrontPageScraper.returning(source, clock, source + " headline");
        return FakeFrontPageScraper.answering(source, () -> {
            bothStarted.countDown();
            try {
                if (!bothStarted.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("the scrapers did not run in parallel");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            return feeds.scrape();
        });
    }

    private void assertThatPublishedFeedCreatedFor(List<Feed> feeds) {
        assertThat(eventBus.published())
                .allSatisfy(event -> assertThat(event).isInstanceOf(FeedCreatedDomainEvent.class))
                .extracting(event -> event.aggregateId())
                .containsExactlyElementsOf(
                        feeds.stream().map(feed -> feed.id().value()).toList());
    }

    private static Logger feedScraperLogger() {
        return (Logger) LoggerFactory.getLogger(FeedScraper.class);
    }

    private static List<String> titlesOf(List<Feed> feeds) {
        return feeds.stream().map(feed -> feed.title().value()).toList();
    }

    private static final class FailingOnSaveFeedRepository implements FeedRepository {

        private final FeedRepository delegate;
        private final int failingSave;
        private int saves;

        FailingOnSaveFeedRepository(FeedRepository delegate, int failingSave) {
            this.delegate = delegate;
            this.failingSave = failingSave;
        }

        @Override
        public void save(Feed feed) {
            saves++;
            if (saves == failingSave) {
                throw new IllegalStateException("save number " + saves + " failed");
            }
            delegate.save(feed);
        }

        @Override
        public List<Feed> search(Criteria<FeedField> criteria) {
            return delegate.search(criteria);
        }

        @Override
        public boolean existsHeadline(FeedSource source, FeedTitle title, Instant from, Instant to) {
            return delegate.existsHeadline(source, title, from, to);
        }
    }
}
