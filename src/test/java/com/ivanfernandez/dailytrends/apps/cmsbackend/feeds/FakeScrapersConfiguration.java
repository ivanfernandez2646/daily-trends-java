package com.ivanfernandez.dailytrends.apps.cmsbackend.feeds;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.application.scrap.FeedScraper;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.fakes.FakeFrontPageScraper;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.EventBus;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the real scrapers with fakes that return five fixed headlines each, so acceptance tests
 * never reach the network. The repository, event bus, clock and executor are the application's.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakeScrapersConfiguration {

    static final List<String> EL_MUNDO_TITLES = List.of("Mundo 1", "Mundo 2", "Mundo 3", "Mundo 4", "Mundo 5");
    static final List<String> EL_ESPANOL_TITLES =
            List.of("Español 1", "Español 2", "Español 3", "Español 4", "Español 5");

    @Bean
    FakeFrontPageScraper elMundoFake(Clock clock) {
        return FakeFrontPageScraper.returning(FeedSource.EL_MUNDO, clock, EL_MUNDO_TITLES.toArray(String[]::new));
    }

    @Bean
    FakeFrontPageScraper elEspanolFake(Clock clock) {
        return FakeFrontPageScraper.returning(FeedSource.EL_ESPANOL, clock, EL_ESPANOL_TITLES.toArray(String[]::new));
    }

    @Bean
    @Primary
    FeedScraper fakeFeedScraper(
            FakeFrontPageScraper elMundoFake,
            FakeFrontPageScraper elEspanolFake,
            FeedRepository repository,
            EventBus eventBus,
            Clock clock,
            ExecutorService virtualThreadExecutor) {
        return new FeedScraper(List.of(elMundoFake, elEspanolFake), repository, eventBus, clock, virtualThreadExecutor);
    }
}
