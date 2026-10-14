package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Criteria;
import java.time.Instant;
import java.util.List;

public interface FeedRepository {

    /** Inserts the feed, or replaces the stored one with the same id. */
    void save(Feed feed);

    List<Feed> search(Criteria<FeedField> criteria);

    /** Whether a feed with that source and exact title was created in {@code [from, to)}. */
    boolean existsHeadline(FeedSource source, FeedTitle title, Instant from, Instant to);
}
