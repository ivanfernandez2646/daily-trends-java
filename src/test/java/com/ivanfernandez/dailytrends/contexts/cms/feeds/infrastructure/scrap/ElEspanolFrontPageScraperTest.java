package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedCreatedDomainEvent;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.shared.fakes.FixedClock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ElEspanolFrontPageScraperTest {

    private final FixedClock clock = FixedClock.at("2026-10-14T08:00:00Z");
    private final ElEspanolFrontPageScraper scraper =
            new ElEspanolFrontPageScraper(new JsoupPageDownloader(Duration.ofSeconds(1)), clock);

    @Test
    void creates_new_el_espanol_feeds_at_the_clock_time() {
        List<Feed> feeds = scraper.extract(HtmlFixtures.page("elespanol.html"));

        assertThat(scraper.source()).isEqualTo(FeedSource.EL_ESPANOL);
        assertThat(feeds).hasSize(2).allSatisfy(feed -> {
            assertThat(feed.source()).isEqualTo(FeedSource.EL_ESPANOL);
            assertThat(feed.createdAt().value()).isEqualTo(clock.now());
            assertThat(feed.updatedAt().value()).isNull();
            assertThat(UUID.fromString(feed.id().value()).version()).isEqualTo(4);
            assertThat(feed.pullDomainEvents()).singleElement().isInstanceOf(FeedCreatedDomainEvent.class);
        });
    }

    @Test
    void reads_the_author_title_and_subtitle_without_cleanup() {
        List<Feed> feeds = scraper.extract(HtmlFixtures.page("elespanol.html"));

        assertThat(feeds.getFirst().author().value()).isEqualTo("Marta Gil");
        assertThat(feeds.getFirst().title().value()).isEqualTo("El crucigrama de hoy");
        assertThat(feeds.getFirst().description().value()).isEqualTo("Crucigrama.");
        assertThat(feeds.get(1).description().value()).isNull();
    }
}
