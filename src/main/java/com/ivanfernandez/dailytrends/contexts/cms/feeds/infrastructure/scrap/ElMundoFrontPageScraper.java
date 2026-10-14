package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FrontPageScraper;
import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.Clock;
import java.time.Instant;
import java.util.List;
import org.jsoup.nodes.Document;

public final class ElMundoFrontPageScraper implements FrontPageScraper {

    private static final String URL = "https://elmundo.es/";
    private static final FrontPageExtractor EXTRACTOR = new FrontPageExtractor(
            ".ue-c-cover-content__byline-name .ue-c-cover-content__link",
            ".ue-c-cover-content__headline",
            ".ue-c-cover-content__kicker");

    private final JsoupPageDownloader downloader;
    private final Clock clock;

    public ElMundoFrontPageScraper(JsoupPageDownloader downloader, Clock clock) {
        this.downloader = downloader;
        this.clock = clock;
    }

    @Override
    public FeedSource source() {
        return FeedSource.EL_MUNDO;
    }

    @Override
    public List<Feed> scrape() {
        return extract(downloader.download(URL));
    }

    List<Feed> extract(Document page) {
        Instant now = clock.now();
        return EXTRACTOR.extract(page).stream()
                .map(article -> article.cleanDescription(ElMundoFrontPageScraper::removeTrailingPeriod)
                        .toFeed(source(), now))
                .toList();
    }

    private static String removeTrailingPeriod(String description) {
        return description.endsWith(".") ? description.substring(0, description.length() - 1) : description;
    }
}
