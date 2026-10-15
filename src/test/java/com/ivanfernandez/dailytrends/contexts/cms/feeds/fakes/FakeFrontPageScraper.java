package com.ivanfernandez.dailytrends.contexts.cms.feeds.fakes;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedAuthorMother;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedDescriptionMother;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedId;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedTitle;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FrontPageScraper;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** A scraper with a scripted outcome. It counts its calls and is thread-safe. */
public final class FakeFrontPageScraper implements FrontPageScraper {

    private final FeedSource source;
    private final Supplier<List<Feed>> outcome;
    private final AtomicInteger calls = new AtomicInteger();

    private FakeFrontPageScraper(FeedSource source, Supplier<List<Feed>> outcome) {
        this.source = source;
        this.outcome = outcome;
    }

    /** Every call creates new feeds, at the clock's time, with these titles in this order. */
    public static FakeFrontPageScraper returning(FeedSource source, Clock clock, String... titles) {
        return answering(
                source,
                () -> Arrays.stream(titles)
                        .map(title -> Feed.create(
                                FeedId.random(),
                                new FeedTitle(title),
                                FeedDescriptionMother.random(),
                                FeedAuthorMother.random(),
                                source,
                                clock.now()))
                        .toList());
    }

    public static FakeFrontPageScraper failing(FeedSource source, RuntimeException failure) {
        return answering(source, () -> {
            throw failure;
        });
    }

    public static FakeFrontPageScraper answering(FeedSource source, Supplier<List<Feed>> outcome) {
        return new FakeFrontPageScraper(source, outcome);
    }

    @Override
    public FeedSource source() {
        return source;
    }

    @Override
    public List<Feed> scrape() {
        calls.incrementAndGet();
        return outcome.get();
    }

    public int calls() {
        return calls.get();
    }
}
