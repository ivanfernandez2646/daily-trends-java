package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Criteria;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Direction;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria.Filter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The behavior every {@link FeedRepository} shares, so the in-memory fake behaves like MongoDB. */
public abstract class FeedRepositoryContractTest {

    private static final Instant FROM = Instant.parse("2026-10-11T22:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-12T22:00:00Z");

    protected abstract FeedRepository repository();

    @Test
    void finds_a_saved_feed() {
        FeedPrimitives feed = FeedMother.builder()
                .updatedAt(Instant.parse("2026-10-13T08:00:00.123Z"))
                .primitives();

        repository().save(Feed.fromPrimitives(feed));

        assertThat(primitivesOf(repository().search(Criteria.all()))).containsExactly(feed);
    }

    @Test
    void finds_a_saved_feed_without_description_or_update_date() {
        FeedPrimitives feed = FeedMother.builder().description(null).primitives();

        repository().save(Feed.fromPrimitives(feed));

        assertThat(primitivesOf(repository().search(Criteria.all()))).containsExactly(feed);
    }

    @Test
    void replaces_a_feed_saved_again_with_the_same_id() {
        FeedPrimitives original = FeedPrimitivesMother.random();
        FeedPrimitives changed =
                FeedMother.builder().id(original.id()).title("Changed").primitives();

        repository().save(Feed.fromPrimitives(original));
        repository().save(Feed.fromPrimitives(changed));

        assertThat(primitivesOf(repository().search(Criteria.all()))).containsExactly(changed);
    }

    @Test
    void combines_filters_with_or() {
        Feed elMundo = FeedMother.builder().source(FeedSource.EL_MUNDO).build();
        Feed elEspanol = FeedMother.builder().source(FeedSource.EL_ESPANOL).build();
        Feed cms = FeedMother.builder().source(FeedSource.CMS).build();
        List.of(elMundo, elEspanol, cms).forEach(repository()::save);

        List<Feed> found = repository()
                .search(Criteria.withFilters(List.of(
                        new Filter<>(FeedField.SOURCE, FeedSource.EL_MUNDO),
                        new Filter<>(FeedField.SOURCE, FeedSource.EL_ESPANOL))));

        assertThat(found).containsExactlyInAnyOrder(elMundo, elEspanol);
    }

    @Test
    void orders_and_limits() {
        Feed oldest = FeedMother.builder().createdAt("2026-10-12T08:00:00Z").build();
        Feed middle = FeedMother.builder().createdAt("2026-10-12T09:00:00Z").build();
        Feed newest = FeedMother.builder().createdAt("2026-10-12T10:00:00Z").build();
        List.of(middle, oldest, newest).forEach(repository()::save);

        List<Feed> found = repository()
                .search(Criteria.<FeedField>all()
                        .orderBy(FeedField.CREATED_AT, Direction.DESC)
                        .limit(2));

        assertThat(found).containsExactly(newest, middle);
    }

    @Test
    void finds_a_headline_created_at_the_start_of_the_range() {
        repository().save(headline(FeedSource.EL_MUNDO, "Headline", FROM));

        assertThat(repository().existsHeadline(FeedSource.EL_MUNDO, new FeedTitle("Headline"), FROM, TO))
                .isTrue();
    }

    @Test
    void ignores_a_headline_created_at_the_end_of_the_range() {
        repository().save(headline(FeedSource.EL_MUNDO, "Headline", TO));

        assertThat(repository().existsHeadline(FeedSource.EL_MUNDO, new FeedTitle("Headline"), FROM, TO))
                .isFalse();
    }

    @Test
    void ignores_a_headline_created_just_before_the_range() {
        repository().save(headline(FeedSource.EL_MUNDO, "Headline", FROM.minus(Duration.ofMillis(1))));

        assertThat(repository().existsHeadline(FeedSource.EL_MUNDO, new FeedTitle("Headline"), FROM, TO))
                .isFalse();
    }

    @Test
    void ignores_a_headline_from_another_source_or_with_another_title() {
        repository().save(headline(FeedSource.EL_ESPANOL, "Headline", FROM));
        repository().save(headline(FeedSource.EL_MUNDO, "Headline!", FROM));

        assertThat(repository().existsHeadline(FeedSource.EL_MUNDO, new FeedTitle("Headline"), FROM, TO))
                .isFalse();
    }

    private static Feed headline(FeedSource source, String title, Instant createdAt) {
        return FeedMother.builder()
                .source(source)
                .title(title)
                .createdAt(createdAt)
                .build();
    }

    private static List<FeedPrimitives> primitivesOf(List<Feed> feeds) {
        return feeds.stream().map(feed -> feed.toPrimitives()).toList();
    }
}
