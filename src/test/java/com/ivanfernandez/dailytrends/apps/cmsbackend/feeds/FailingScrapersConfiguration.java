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

/** Replaces the real scrapers with fakes whose every source fails. */
@TestConfiguration(proxyBeanMethods = false)
public class FailingScrapersConfiguration {

    @Bean
    FakeFrontPageScraper elMundoFake() {
        return FakeFrontPageScraper.failing(FeedSource.EL_MUNDO, new IllegalStateException("blocked"));
    }

    @Bean
    FakeFrontPageScraper elEspanolFake() {
        return FakeFrontPageScraper.failing(FeedSource.EL_ESPANOL, new IllegalStateException("blocked"));
    }

    @Bean
    @Primary
    FeedScraper failingFeedScraper(
            FakeFrontPageScraper elMundoFake,
            FakeFrontPageScraper elEspanolFake,
            FeedRepository repository,
            EventBus eventBus,
            Clock clock,
            ExecutorService virtualThreadExecutor) {
        return new FeedScraper(List.of(elMundoFake, elEspanolFake), repository, eventBus, clock, virtualThreadExecutor);
    }
}
