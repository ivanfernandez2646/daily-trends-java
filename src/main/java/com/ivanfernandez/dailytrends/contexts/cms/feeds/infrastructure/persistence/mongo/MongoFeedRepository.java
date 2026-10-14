package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.persistence.mongo;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedField;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedPrimitives;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedTitle;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.InvalidStoredFeed;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Criteria;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Direction;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Filter;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.bson.Document;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/** Maps feeds to documents by hand: {@code _id} is the feed id and dates are BSON dates. */
public final class MongoFeedRepository implements FeedRepository {

    private static final String COLLECTION = "feeds";
    private static final String ID = "_id";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String AUTHOR = "author";
    private static final String SOURCE = "source";
    private static final String CREATED_AT = "createdAt";
    private static final String UPDATED_AT = "updatedAt";

    private final MongoTemplate mongoTemplate;

    public MongoFeedRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(Feed feed) {
        mongoTemplate.save(toDocument(feed.toPrimitives()), COLLECTION);
    }

    @Override
    public List<Feed> search(Criteria<FeedField> criteria) {
        var query = new Query();
        if (!criteria.filters().isEmpty()) {
            query.addCriteria(new org.springframework.data.mongodb.core.query.Criteria()
                    .orOperator(criteria.filters().stream()
                            .map(MongoFeedRepository::toCondition)
                            .toList()));
        }
        criteria.order()
                .ifPresent(order -> query.with(Sort.by(
                        order.direction() == Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC,
                        storageName(order.field()))));
        criteria.limit().ifPresent(query::limit);
        return mongoTemplate.find(query, Document.class, COLLECTION).stream()
                .map(MongoFeedRepository::toFeed)
                .toList();
    }

    @Override
    public boolean existsHeadline(FeedSource source, FeedTitle title, Instant from, Instant to) {
        var query = Query.query(org.springframework.data.mongodb.core.query.Criteria.where(SOURCE)
                .is(source.name())
                .and(TITLE)
                .is(title.value())
                .and(CREATED_AT)
                .gte(Date.from(from))
                .lt(Date.from(to)));
        return mongoTemplate.exists(query, COLLECTION);
    }

    private static org.springframework.data.mongodb.core.query.Criteria toCondition(Filter<FeedField> filter) {
        return org.springframework.data.mongodb.core.query.Criteria.where(storageName(filter.field()))
                .is(toBson(filter.value()));
    }

    private static String storageName(FeedField field) {
        return switch (field) {
            case ID -> ID;
            case TITLE -> TITLE;
            case DESCRIPTION -> DESCRIPTION;
            case AUTHOR -> AUTHOR;
            case SOURCE -> SOURCE;
            case CREATED_AT -> CREATED_AT;
            case UPDATED_AT -> UPDATED_AT;
        };
    }

    private static Object toBson(Object value) {
        return switch (value) {
            case FeedSource source -> source.name();
            case Instant instant -> Date.from(instant);
            default -> value;
        };
    }

    private static Document toDocument(FeedPrimitives feed) {
        return new Document(ID, feed.id())
                .append(TITLE, feed.title())
                .append(DESCRIPTION, feed.description())
                .append(AUTHOR, feed.author())
                .append(SOURCE, feed.source().name())
                .append(CREATED_AT, Date.from(feed.createdAt()))
                .append(UPDATED_AT, feed.updatedAt() == null ? null : Date.from(feed.updatedAt()));
    }

    private static Feed toFeed(Document document) {
        Object id = document.get(ID);
        try {
            return Feed.fromPrimitives(new FeedPrimitives(
                    requiredString(document, ID),
                    requiredString(document, TITLE),
                    optional(document, DESCRIPTION, String.class),
                    requiredString(document, AUTHOR),
                    source(requiredString(document, SOURCE)),
                    required(document, CREATED_AT, Date.class).toInstant(),
                    toInstant(optional(document, UPDATED_AT, Date.class))));
        } catch (RuntimeException exception) {
            throw new InvalidStoredFeed(
                    id == null ? null : id.toString(),
                    Objects.requireNonNullElse(
                            exception.getMessage(), exception.getClass().getSimpleName()));
        }
    }

    private static String requiredString(Document document, String field) {
        return required(document, field, String.class);
    }

    private static <T> T required(Document document, String field, Class<T> type) {
        T value = optional(document, field, type);
        if (value == null) {
            throw new IllegalStateException("field '" + field + "' is missing");
        }
        return value;
    }

    private static <T> @Nullable T optional(Document document, String field, Class<T> type) {
        Object value = document.get(field);
        if (value != null && !type.isInstance(value)) {
            throw new IllegalStateException("field '" + field + "' is not a " + type.getSimpleName());
        }
        return type.cast(value);
    }

    private static FeedSource source(String value) {
        return Arrays.stream(FeedSource.values())
                .filter(source -> source.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("field 'source' has the unknown value <" + value + ">"));
    }

    private static @Nullable Instant toInstant(@Nullable Date date) {
        return date == null ? null : date.toInstant();
    }
}
