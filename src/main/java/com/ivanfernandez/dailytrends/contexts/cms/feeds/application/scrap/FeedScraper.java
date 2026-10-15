package com.ivanfernandez.dailytrends.contexts.cms.feeds.application.scrap;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FrontPageScraper;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.BusinessDay;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.EventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One scrape run: every scraper in parallel, then the feeds not already stored for their source on
 * the same business day are saved and published, in scraper order.
 */
public final class FeedScraper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedScraper.class);

    private final List<FrontPageScraper> scrapers;
    private final FeedRepository repository;
    private final EventBus eventBus;
    private final Clock clock;
    private final Executor executor;

    public FeedScraper(
            List<FrontPageScraper> scrapers,
            FeedRepository repository,
            EventBus eventBus,
            Clock clock,
            Executor executor) {
        this.scrapers = List.copyOf(scrapers);
        this.repository = repository;
        this.eventBus = eventBus;
        this.clock = clock;
        this.executor = executor;
    }

    /** Returns the feeds saved by this run. */
    public List<Feed> scrape() {
        List<CompletableFuture<List<Feed>>> runs =
                scrapers.stream().map(this::start).toList();
        var saved = new ArrayList<Feed>();
        for (CompletableFuture<List<Feed>> run : runs) {
            for (Feed feed : run.join()) {
                if (isNew(feed)) {
                    // Saving one by one lets a headline listed twice in this run find the first copy.
                    repository.save(feed);
                    eventBus.publish(feed.pullDomainEvents());
                    saved.add(feed);
                }
            }
        }
        return List.copyOf(saved);
    }

    private CompletableFuture<List<Feed>> start(FrontPageScraper scraper) {
        return CompletableFuture.supplyAsync(scraper::scrape, executor).exceptionally(failure -> {
            LOGGER.warn("The {} scraper failed; it adds no feeds to this run", scraper.source(), causeOf(failure));
            return List.of();
        });
    }

    private boolean isNew(Feed feed) {
        BusinessDay day = BusinessDay.of(feed.createdAt().value(), clock.zone());
        return !repository.existsHeadline(feed.source(), feed.title(), day.start(), day.end());
    }

    private static Throwable causeOf(Throwable failure) {
        return failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
    }
}
