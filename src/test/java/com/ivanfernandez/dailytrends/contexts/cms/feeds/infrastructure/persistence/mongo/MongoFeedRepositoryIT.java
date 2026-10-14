package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ivanfernandez.dailytrends.apps.cmsbackend.IntegrationTest;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedField;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedMother;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepositoryContractTest;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.InvalidStoredFeed;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Criteria;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Direction;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

@IntegrationTest
class MongoFeedRepositoryIT extends FeedRepositoryContractTest {

    @Autowired
    private FeedRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    protected FeedRepository repository() {
        return repository;
    }

    @BeforeEach
    void emptyTheCollection() {
        mongoTemplate.getCollection("feeds").deleteMany(new Document());
    }

    @Test
    void stores_dates_as_bson_dates_and_a_missing_update_date_as_null() {
        Feed updated = FeedMother.builder()
                .createdAt("2026-10-12T08:00:00.123Z")
                .updatedAt(Instant.parse("2026-10-13T08:00:00Z"))
                .build();
        Feed neverUpdated = FeedMother.builder().build();

        repository.save(updated);
        repository.save(neverUpdated);

        Document updatedDocument = rawDocument(updated);
        assertThat(updatedDocument.get("createdAt")).isEqualTo(Date.from(Instant.parse("2026-10-12T08:00:00.123Z")));
        assertThat(updatedDocument.get("updatedAt")).isEqualTo(Date.from(Instant.parse("2026-10-13T08:00:00Z")));
        Document neverUpdatedDocument = rawDocument(neverUpdated);
        assertThat(neverUpdatedDocument).containsEntry("updatedAt", null);
        assertThat(neverUpdatedDocument).doesNotContainKey("id");
    }

    @Test
    void orders_by_creation_date_chronologically() {
        // As text, ".500Z" sorts before "Z"; as dates, the whole second comes first.
        Feed wholeSecond =
                FeedMother.builder().createdAt("2026-10-12T08:00:00Z").build();
        Feed halfSecond =
                FeedMother.builder().createdAt("2026-10-12T08:00:00.500Z").build();
        repository.save(halfSecond);
        repository.save(wholeSecond);

        List<Feed> found = repository.search(Criteria.<FeedField>all().orderBy(FeedField.CREATED_AT, Direction.ASC));

        assertThat(found).containsExactly(wholeSecond, halfSecond);
    }

    @Test
    void rejects_a_stored_creation_date_that_is_not_a_date() {
        insertRaw(validDocument("9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b").append("createdAt", "2026-10-12T08:00:00Z"));

        assertThatThrownBy(() -> repository.search(Criteria.all()))
                .isInstanceOf(InvalidStoredFeed.class)
                .hasMessageContaining("9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b")
                .hasMessageContaining("createdAt");
    }

    @Test
    void rejects_a_stored_unknown_source() {
        insertRaw(validDocument("9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b").append("source", "EL_PAIZ"));

        assertThatThrownBy(() -> repository.search(Criteria.all()))
                .isInstanceOf(InvalidStoredFeed.class)
                .hasMessageContaining("9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b")
                .hasMessageContaining("EL_PAIZ");
    }

    @Test
    void rejects_a_stored_feed_without_title() {
        Document document = validDocument("9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b");
        document.remove("title");
        insertRaw(document);

        assertThatThrownBy(() -> repository.search(Criteria.all()))
                .isInstanceOf(InvalidStoredFeed.class)
                .hasMessageContaining("9b2f3a4e-1c5d-4e6f-8a7b-0c1d2e3f4a5b")
                .hasMessageContaining("title");
    }

    private Document rawDocument(Feed feed) {
        Document document = mongoTemplate
                .getCollection("feeds")
                .find(new Document("_id", feed.id().value()))
                .first();
        assertThat(document).isNotNull();
        return document;
    }

    private void insertRaw(Document document) {
        mongoTemplate.getCollection("feeds").insertOne(document);
    }

    private static Document validDocument(String id) {
        return new Document("_id", id)
                .append("title", "Title")
                .append("description", null)
                .append("author", "Author")
                .append("source", "EL_MUNDO")
                .append("createdAt", Date.from(Instant.parse("2026-10-12T08:00:00Z")))
                .append("updatedAt", null);
    }
}
