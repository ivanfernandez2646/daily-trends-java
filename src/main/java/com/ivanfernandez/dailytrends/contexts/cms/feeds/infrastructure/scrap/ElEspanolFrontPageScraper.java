package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FrontPageScraper;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import java.time.Instant;
import java.util.List;
import org.jsoup.nodes.Document;

public final class ElEspanolFrontPageScraper implements FrontPageScraper {

    private static final String URL = "https://www.elespanol.com/";
    private static final FrontPageExtractor EXTRACTOR =
            new FrontPageExtractor(".art__author", ".art__title", ".art__subtitle");

    private final JsoupPageDownloader downloader;
    private final Clock clock;

    public ElEspanolFrontPageScraper(JsoupPageDownloader downloader, Clock clock) {
        this.downloader = downloader;
        this.clock = clock;
    }

    @Override
    public FeedSource source() {
        return FeedSource.EL_ESPANOL;
    }

    @Override
    public List<Feed> scrape() {
        return extract(downloader.download(URL));
    }

    List<Feed> extract(Document page) {
        Instant now = clock.now();
        return EXTRACTOR.extract(page).stream()
                .map(article -> article.toFeed(source(), now))
                .toList();
    }
}
