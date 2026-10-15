package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FrontPageScraper;
import com.ivanfernandez.dailytrends.contexts.cms.shared.infrastructure.SystemClock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Live smoke tests against the real front pages. Run only with {@code ./mvnw verify -Pnetwork}. */
@Tag("network")
class FrontPageScrapersNetworkIT {

    private final JsoupPageDownloader downloader = new JsoupPageDownloader(Duration.ofSeconds(10));
    private final SystemClock clock = new SystemClock(ZoneId.of("Europe/Madrid"));

    @Test
    void scrapes_five_headlines_from_el_mundo() {
        assertScrapesFiveHeadlines(new ElMundoFrontPageScraper(downloader, clock), FeedSource.EL_MUNDO);
    }

    @Test
    void scrapes_five_headlines_from_el_espanol() {
        assertScrapesFiveHeadlines(new ElEspanolFrontPageScraper(downloader, clock), FeedSource.EL_ESPANOL);
    }

    private static void assertScrapesFiveHeadlines(FrontPageScraper scraper, FeedSource source) {
        List<Feed> feeds = scraper.scrape();

        assertThat(feeds).hasSize(5).allSatisfy(feed -> {
            assertThat(feed.source()).isEqualTo(source);
            assertThat(feed.author().value()).isNotBlank();
            assertThat(feed.title().value()).isNotBlank();
        });
    }
}
