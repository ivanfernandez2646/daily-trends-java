package com.ivanfernandez.dailytrends.apps.cmsbackend.config;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.application.home.FrontPageSearcher;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.application.scrap.FeedScraper;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.application.scrap.ScrapeRunCoordinator;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.persistence.mongo.MongoFeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap.ElEspanolFrontPageScraper;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap.ElMundoFrontPageScraper;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap.JsoupPageDownloader;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.EventBus;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration(proxyBeanMethods = false)
public class FeedsConfiguration {

    @Bean
    FeedRepository feedRepository(MongoTemplate mongoTemplate) {
        return new MongoFeedRepository(mongoTemplate);
    }

    @Bean
    JsoupPageDownloader pageDownloader(DailyTrendsProperties properties) {
        return new JsoupPageDownloader(properties.scraping().timeout());
    }

    /** The scraper order is the order of the {@code /feed/scrap} response. */
    @Bean
    FeedScraper feedScraper(
            JsoupPageDownloader pageDownloader,
            FeedRepository feedRepository,
            EventBus eventBus,
            Clock clock,
            ExecutorService virtualThreadExecutor) {
        return new FeedScraper(
                List.of(
                        new ElMundoFrontPageScraper(pageDownloader, clock),
                        new ElEspanolFrontPageScraper(pageDownloader, clock)),
                feedRepository,
                eventBus,
                clock,
                virtualThreadExecutor);
    }

    /** Its {@code close()} stops a run in progress; it runs before the MongoDB client closes. */
    @Bean
    ScrapeRunCoordinator scrapeRunCoordinator(
            FeedScraper feedScraper,
            Clock clock,
            DailyTrendsProperties properties,
            ExecutorService virtualThreadExecutor) {
        return new ScrapeRunCoordinator(
                feedScraper, clock, properties.scraping().cooldown(), virtualThreadExecutor);
    }

    @Bean
    FrontPageSearcher frontPageSearcher(
            FeedRepository feedRepository, Clock clock, ScrapeRunCoordinator scrapeRunCoordinator) {
        return new FrontPageSearcher(feedRepository, clock, scrapeRunCoordinator);
    }
}
