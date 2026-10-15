package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedCreatedDomainEvent;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.shared.fakes.FixedClock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ElMundoFrontPageScraperTest {

    private final FixedClock clock = FixedClock.at("2026-10-14T08:00:00Z");
    private final ElMundoFrontPageScraper scraper =
            new ElMundoFrontPageScraper(new JsoupPageDownloader(Duration.ofSeconds(1)), clock);

    @Test
    void creates_new_el_mundo_feeds_at_the_clock_time() {
        List<Feed> feeds = scraper.extract(HtmlFixtures.page("elmundo.html"));

        assertThat(scraper.source()).isEqualTo(FeedSource.EL_MUNDO);
        assertThat(feeds).hasSize(5).allSatisfy(feed -> {
            assertThat(feed.source()).isEqualTo(FeedSource.EL_MUNDO);
            assertThat(feed.createdAt().value()).isEqualTo(clock.now());
            assertThat(feed.updatedAt().value()).isNull();
            assertThat(UUID.fromString(feed.id().value()).version()).isEqualTo(4);
            assertThat(feed.pullDomainEvents()).singleElement().isInstanceOf(FeedCreatedDomainEvent.class);
        });
        assertThat(feeds).extracting(feed -> feed.id()).doesNotHaveDuplicates();
    }

    @Test
    void reads_the_byline_headline_and_kicker() {
        Feed first = scraper.extract(HtmlFixtures.page("elmundo.html")).getFirst();

        assertThat(first.author().value()).isEqualTo("Ana López");
        assertThat(first.title().value()).isEqualTo("El crucigrama de hoy");
        assertThat(first.description().value()).isEqualTo("Crucigrama");
    }

    @ParameterizedTest(name = "{1} -> {2}")
    @CsvSource(
            nullValues = "null",
            value = {
                "El crucigrama de hoy, 'Crucigrama.', Crucigrama",
                "El sudoku de hoy, 'Sudoku...', Sudoku..",
                "Qué pasa en Madrid, '¿Qué pasa?', ¿Qué pasa?",
                "Solo un punto, '.', null",
                "Sin antetítulo, '', null"
            })
    void removes_one_trailing_period_from_the_description(String title, String kicker, @Nullable String description) {
        List<Feed> feeds = scraper.extract(HtmlFixtures.page("elmundo.html"));

        assertThat(feeds)
                .filteredOn(feed -> feed.title().value().equals(title))
                .singleElement()
                .satisfies(feed -> assertThat(feed.description().value()).isEqualTo(description));
    }
}
