package com.ivanfernandez.dailytrends.contexts.cms.feeds.application.home;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.application.scrap.BackgroundRefresh;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedField;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.BusinessDay;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Criteria;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Direction;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Filter;
import java.util.List;

/**
 * Answers the front page from the stored feeds and, when its newest feed is from a previous business
 * day, asks for a background refresh without waiting for it.
 */
public final class FrontPageSearcher {

    private static final int PAGE_SIZE = 10;

    private final FeedRepository repository;
    private final Clock clock;
    private final BackgroundRefresh backgroundRefresh;

    public FrontPageSearcher(FeedRepository repository, Clock clock, BackgroundRefresh backgroundRefresh) {
        this.repository = repository;
        this.clock = clock;
        this.backgroundRefresh = backgroundRefresh;
    }

    public List<Feed> search() {
        List<Feed> page = repository.search(criteria());
        if (!page.isEmpty() && isStale(page.getFirst())) {
            backgroundRefresh.requestBackgroundRefresh();
        }
        return page;
    }

    private static Criteria<FeedField> criteria() {
        return Criteria.withFilters(List.of(
                        new Filter<>(FeedField.SOURCE, FeedSource.EL_MUNDO),
                        new Filter<>(FeedField.SOURCE, FeedSource.EL_ESPANOL)))
                .orderBy(FeedField.CREATED_AT, Direction.DESC)
                .limit(PAGE_SIZE);
    }

    private boolean isStale(Feed newest) {
        BusinessDay today = BusinessDay.of(clock.now(), clock.zone());
        return newest.createdAt().value().isBefore(today.start());
    }
}
