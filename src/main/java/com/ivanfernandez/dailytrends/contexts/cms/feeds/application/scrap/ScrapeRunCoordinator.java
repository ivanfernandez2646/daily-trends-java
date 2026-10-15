package com.ivanfernandez.dailytrends.contexts.cms.feeds.application.scrap;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the single-flight guard and the cooldown shared by every scrape run in the process. A background
 * refresh is ignored while a run is in progress or within the cooldown; {@link #runNow()} waits for the
 * guard and ignores the cooldown.
 */
public final class ScrapeRunCoordinator implements BackgroundRefresh, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScrapeRunCoordinator.class);
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);

    private final FeedScraper feedScraper;
    private final Clock clock;
    private final Duration cooldown;
    private final ExecutorService executor;
    // A semaphore, not a lock: a background run is released by the executor thread that ran it.
    private final Semaphore guard = new Semaphore(1);
    private volatile @Nullable Instant lastBackgroundRunFinishedAt;

    public ScrapeRunCoordinator(FeedScraper feedScraper, Clock clock, Duration cooldown, ExecutorService executor) {
        this.feedScraper = feedScraper;
        this.clock = clock;
        this.cooldown = cooldown;
        this.executor = executor;
    }

    @Override
    public void requestBackgroundRefresh() {
        if (!guard.tryAcquire()) {
            return;
        }
        // Checked after taking the guard: a run that finishes in between has already started the cooldown.
        if (isCoolingDown()) {
            guard.release();
            return;
        }
        try {
            executor.execute(this::runInBackground);
        } catch (RejectedExecutionException e) {
            guard.release();
            LOGGER.warn("The background refresh was not started: the executor is shut down");
        }
    }

    /** Waits for any run in progress, then scrapes. Returns the feeds saved by this run. */
    public List<Feed> runNow() {
        try {
            guard.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the scrape run in progress", e);
        }
        try {
            return feedScraper.scrape();
        } finally {
            guard.release();
        }
    }

    /** Interrupts a run in progress and waits for it, so no feed is written after this returns. */
    @Override
    public void close() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                LOGGER.error("A scrape run did not stop within {}", SHUTDOWN_TIMEOUT);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void runInBackground() {
        try {
            feedScraper.scrape();
        } catch (RuntimeException e) {
            LOGGER.error("The background scrape run failed", e);
        } finally {
            lastBackgroundRunFinishedAt = clock.now();
            guard.release();
        }
    }

    private boolean isCoolingDown() {
        Instant finishedAt = lastBackgroundRunFinishedAt;
        return finishedAt != null && clock.now().isBefore(finishedAt.plus(cooldown));
    }
}
