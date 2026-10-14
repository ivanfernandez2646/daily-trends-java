package com.ivanfernandez.dailytrends.contexts.cms.feeds.fakes;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedField;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedPrimitives;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedTitle;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Criteria;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Direction;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Order;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Applies filters (OR), order and limit like MongoDB does, so use-case tests assert results. It stores
 * primitives, so a found feed is a new instance with no recorded events. Thread-safe.
 */
public final class InMemoryFeedRepository implements FeedRepository {

    private final Map<String, FeedPrimitives> feeds = new LinkedHashMap<>();

    @Override
    public synchronized void save(Feed feed) {
        FeedPrimitives primitives = feed.toPrimitives();
        feeds.put(primitives.id(), primitives);
    }

    @Override
    public synchronized List<Feed> search(Criteria<FeedField> criteria) {
        Stream<FeedPrimitives> found = feeds.values().stream()
                .filter(feed -> criteria.filters().isEmpty()
                        || criteria.filters().stream()
                                .anyMatch(filter -> Objects.equals(valueOf(feed, filter.field()), filter.value())));
        if (criteria.order().isPresent()) {
            found = found.sorted(comparator(criteria.order().get()));
        }
        if (criteria.limit().isPresent()) {
            found = found.limit(criteria.limit().getAsInt());
        }
        return found.map(Feed::fromPrimitives).toList();
    }

    @Override
    public synchronized boolean existsHeadline(FeedSource source, FeedTitle title, Instant from, Instant to) {
        return feeds.values().stream()
                .anyMatch(feed -> feed.source() == source
                        && feed.title().equals(title.value())
                        && !feed.createdAt().isBefore(from)
                        && feed.createdAt().isBefore(to));
    }

    public synchronized List<Feed> all() {
        return feeds.values().stream().map(Feed::fromPrimitives).toList();
    }

    // MongoDB sorts null before any value in ascending order.
    @SuppressWarnings("rawtypes")
    private static Comparator<FeedPrimitives> comparator(Order<FeedField> order) {
        Comparator<FeedPrimitives> ascending = Comparator.comparing(
                feed -> (Comparable) valueOf(feed, order.field()), Comparator.nullsFirst(Comparator.naturalOrder()));
        return order.direction() == Direction.ASC ? ascending : ascending.reversed();
    }

    private static @Nullable Object valueOf(FeedPrimitives feed, FeedField field) {
        return switch (field) {
            case ID -> feed.id();
            case TITLE -> feed.title();
            case DESCRIPTION -> feed.description();
            case AUTHOR -> feed.author();
            case SOURCE -> feed.source();
            case CREATED_AT -> feed.createdAt();
            case UPDATED_AT -> feed.updatedAt();
        };
    }
}
