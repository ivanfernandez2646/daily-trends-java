package com.ivanfernandez.dailytrends.contexts.cms.feeds.application.scrap;

import static com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource.EL_MUNDO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedField;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedTitle;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.fakes.FakeFrontPageScraper;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.fakes.InMemoryFeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Criteria;
import com.ivanfernandez.dailytrends.contexts.cms.shared.fakes.DirectExecutorService;
import com.ivanfernandez.dailytrends.contexts.cms.shared.fakes.FixedClock;
import com.ivanfernandez.dailytrends.contexts.cms.shared.fakes.RecordingEventBus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;

class ScrapeRunCoordinatorTest {

    private static final Executor SAME_THREAD = task -> task.run();
    private static final Duration COOLDOWN = Duration.ofSeconds(300);
    private static final int TIMEOUT_SECONDS = 5;

    private final FixedClock clock = FixedClock.at("2026-10-14T08:00:00Z");
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final ExecutorService callers = Executors.newVirtualThreadPerTaskExecutor();

    @BeforeEach
    void captureLogs() {
        logs.start();
        coordinatorLogger().addAppender(logs);
    }

    @AfterEach
    void releaseResources() {
        coordinatorLogger().detachAppender(logs);
        callers.shutdownNow();
    }

    @Test
    void a_background_refresh_runs_the_scraper_and_saves_its_feeds() {
        var repository = new InMemoryFeedRepository();
        var scraper = FakeFrontPageScraper.returning(EL_MUNDO, clock, "Headline");
        var coordinator = coordinator(repository, new DirectExecutorService(), COOLDOWN, scraper);

        coordinator.requestBackgroundRefresh();

        assertThat(scraper.calls()).isEqualTo(1);
        assertThat(repository.all()).hasSize(1);
    }

    @Test
    void ignores_a_refresh_requested_while_a_run_is_in_progress() throws Exception {
        var run = new HeldRun();
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var coordinator = coordinator(new InMemoryFeedRepository(), executor, COOLDOWN, run.scraper());

        coordinator.requestBackgroundRefresh();
        run.awaitStarted();
        coordinator.requestBackgroundRefresh();
        run.release();
        awaitFinished(executor);

        assertThat(run.scraper().calls()).isEqualTo(1);
    }

    @Test
    void ignores_a_refresh_within_the_cooldown_and_starts_one_at_exactly_the_cooldown() {
        var scraper = FakeFrontPageScraper.returning(EL_MUNDO, clock, "Headline");
        var coordinator = coordinator(new InMemoryFeedRepository(), new DirectExecutorService(), COOLDOWN, scraper);
        coordinator.requestBackgroundRefresh();

        clock.advance(COOLDOWN.minusSeconds(1));
        coordinator.requestBackgroundRefresh();
        assertThat(scraper.calls()).isEqualTo(1);

        clock.advance(Duration.ofSeconds(1));
        coordinator.requestBackgroundRefresh();
        assertThat(scraper.calls()).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(RunOutcome.class)
    void starts_the_cooldown_whatever_the_outcome_of_the_run(RunOutcome outcome) {
        var scraper = outcome.scraper(clock);
        var coordinator = coordinator(outcome.repository(), new DirectExecutorService(), COOLDOWN, scraper);

        coordinator.requestBackgroundRefresh();
        coordinator.requestBackgroundRefresh();

        assertThat(scraper.calls()).isEqualTo(1);
        clock.advance(COOLDOWN);
        coordinator.requestBackgroundRefresh();
        assertThat(scraper.calls()).isEqualTo(2);
    }

    @Test
    void a_zero_cooldown_lets_every_refresh_start() {
        var scraper = FakeFrontPageScraper.returning(EL_MUNDO, clock, "Headline");
        var coordinator =
                coordinator(new InMemoryFeedRepository(), new DirectExecutorService(), Duration.ZERO, scraper);

        coordinator.requestBackgroundRefresh();
        coordinator.requestBackgroundRefresh();

        assertThat(scraper.calls()).isEqualTo(2);
    }

    @Test
    void a_failing_background_run_is_logged_does_not_propagate_and_releases_the_guard() {
        var scraper = FakeFrontPageScraper.returning(EL_MUNDO, clock, "Headline");
        var coordinator = coordinator(new FailingRepository(), new DirectExecutorService(), COOLDOWN, scraper);

        coordinator.requestBackgroundRefresh();

        assertThat(logs.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getThrowableProxy().getMessage()).isEqualTo("storage down");
        });
        assertThatThrownBy(coordinator::runNow).hasMessage("storage down");
        assertThat(scraper.calls()).isEqualTo(2);
    }

    @Test
    void run_now_ignores_the_cooldown() {
        var scraper = FakeFrontPageScraper.returning(EL_MUNDO, clock, "Headline");
        var coordinator = coordinator(new InMemoryFeedRepository(), new DirectExecutorService(), COOLDOWN, scraper);
        coordinator.requestBackgroundRefresh();

        coordinator.runNow();

        assertThat(scraper.calls()).isEqualTo(2);
    }

    @Test
    void run_now_does_not_start_the_cooldown() {
        var scraper = FakeFrontPageScraper.returning(EL_MUNDO, clock, "Headline");
        var coordinator = coordinator(new InMemoryFeedRepository(), new DirectExecutorService(), COOLDOWN, scraper);
        coordinator.runNow();

        coordinator.requestBackgroundRefresh();

        assertThat(scraper.calls()).isEqualTo(2);
    }

    @Test
    void run_now_returns_the_feeds_saved_by_its_run() {
        var coordinator = coordinator(
                new InMemoryFeedRepository(),
                new DirectExecutorService(),
                COOLDOWN,
                FakeFrontPageScraper.returning(EL_MUNDO, clock, "One", "Two"));

        assertThat(coordinator.runNow())
                .extracting(feed -> feed.title().value())
                .containsExactly("One", "Two");
    }

    @Test
    void run_now_waits_for_a_background_run_in_progress_instead_of_overlapping_it() throws Exception {
        var run = new HeldRun();
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var coordinator = coordinator(new InMemoryFeedRepository(), executor, COOLDOWN, run.scraper());
        coordinator.requestBackgroundRefresh();
        run.awaitStarted();

        Future<List<Feed>> now = callers.submit(coordinator::runNow);

        assertThatThrownBy(() -> now.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
        run.release();
        assertThat(now.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEmpty();
        assertThat(run.scraper().calls()).isEqualTo(2);
        assertThat(run.maxConcurrentCalls()).isEqualTo(1);
    }

    @Test
    void ignores_a_refresh_requested_while_run_now_is_in_progress() throws Exception {
        var run = new HeldRun();
        var coordinator =
                coordinator(new InMemoryFeedRepository(), new DirectExecutorService(), COOLDOWN, run.scraper());
        Future<List<Feed>> now = callers.submit(coordinator::runNow);
        run.awaitStarted();

        coordinator.requestBackgroundRefresh();
        run.release();
        now.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(run.scraper().calls()).isEqualTo(1);
    }

    @Test
    void many_concurrent_refresh_requests_start_exactly_one_run() throws Exception {
        var run = new HeldRun();
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var coordinator = coordinator(new InMemoryFeedRepository(), executor, COOLDOWN, run.scraper());
        var go = new CountDownLatch(1);
        List<Future<Void>> requests = IntStream.range(0, 50)
                .mapToObj(i -> callers.submit(() -> {
                    go.await();
                    coordinator.requestBackgroundRefresh();
                    return (Void) null;
                }))
                .toList();

        go.countDown();
        for (Future<Void> request : requests) {
            request.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        run.release();
        awaitFinished(executor);

        assertThat(run.scraper().calls()).isEqualTo(1);
    }

    @Test
    void closing_interrupts_a_run_in_progress_and_waits_for_it() throws Exception {
        var run = new HeldRun();
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var coordinator = coordinator(new InMemoryFeedRepository(), executor, COOLDOWN, run.scraper());
        coordinator.requestBackgroundRefresh();
        run.awaitStarted();

        coordinator.close();

        assertThat(run.wasInterrupted()).isTrue();
        assertThat(executor.isTerminated()).isTrue();
    }

    @Test
    void ignores_refresh_requests_after_closing() {
        var scraper = FakeFrontPageScraper.returning(EL_MUNDO, clock, "Headline");
        var coordinator = coordinator(new InMemoryFeedRepository(), new DirectExecutorService(), COOLDOWN, scraper);
        coordinator.close();

        coordinator.requestBackgroundRefresh();

        assertThat(scraper.calls()).isZero();
    }

    private ScrapeRunCoordinator coordinator(
            FeedRepository repository, ExecutorService executor, Duration cooldown, FakeFrontPageScraper... scrapers) {
        var feedScraper = new FeedScraper(List.of(scrapers), repository, new RecordingEventBus(), clock, SAME_THREAD);
        return new ScrapeRunCoordinator(feedScraper, clock, cooldown, executor);
    }

    private static void awaitFinished(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    private static Logger coordinatorLogger() {
        return (Logger) LoggerFactory.getLogger(ScrapeRunCoordinator.class);
    }

    private enum RunOutcome {
        SAVED_FEEDS,
        NOTHING_SAVED,
        EVERY_SOURCE_FAILED,
        EXCEPTION;

        FakeFrontPageScraper scraper(FixedClock clock) {
            return switch (this) {
                case SAVED_FEEDS, EXCEPTION -> FakeFrontPageScraper.returning(EL_MUNDO, clock, "Headline");
                case NOTHING_SAVED -> FakeFrontPageScraper.returning(EL_MUNDO, clock);
                case EVERY_SOURCE_FAILED ->
                    FakeFrontPageScraper.failing(EL_MUNDO, new IllegalStateException("blocked"));
            };
        }

        FeedRepository repository() {
            return this == EXCEPTION ? new FailingRepository() : new InMemoryFeedRepository();
        }
    }

    /** A scraper that blocks until released, recording how many calls overlap and whether it was interrupted. */
    private static final class HeldRun {

        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch released = new CountDownLatch(1);
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maxActive = new AtomicInteger();
        private final AtomicBoolean interrupted = new AtomicBoolean();
        private final FakeFrontPageScraper scraper = FakeFrontPageScraper.answering(EL_MUNDO, this::hold);

        private List<Feed> hold() {
            maxActive.accumulateAndGet(active.incrementAndGet(), Math::max);
            started.countDown();
            try {
                released.await();
            } catch (InterruptedException e) {
                interrupted.set(true);
                throw new IllegalStateException("interrupted", e);
            } finally {
                active.decrementAndGet();
            }
            return List.of();
        }

        FakeFrontPageScraper scraper() {
            return scraper;
        }

        void awaitStarted() throws InterruptedException {
            assertThat(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }

        void release() {
            released.countDown();
        }

        int maxConcurrentCalls() {
            return maxActive.get();
        }

        boolean wasInterrupted() {
            return interrupted.get();
        }
    }

    private static final class FailingRepository implements FeedRepository {

        @Override
        public void save(Feed feed) {
            throw new IllegalStateException("storage down");
        }

        @Override
        public List<Feed> search(Criteria<FeedField> criteria) {
            return List.of();
        }

        @Override
        public boolean existsHeadline(FeedSource source, FeedTitle title, Instant from, Instant to) {
            return false;
        }
    }
}
