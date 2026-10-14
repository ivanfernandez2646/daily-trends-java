package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Downloads and parses a page. Jsoup picks the charset: the response header, else the BOM or
 * {@code <meta>}, else UTF-8. A non-2xx response is parsed like any other page.
 */
public final class JsoupPageDownloader {

    private static final String USER_AGENT = "daily-trends-java";

    private final int timeoutMillis;

    public JsoupPageDownloader(Duration timeout) {
        this.timeoutMillis = (int) Math.min(timeout.toMillis(), Integer.MAX_VALUE);
    }

    public Document download(String url) {
        try {
            return Jsoup.connect(url)
                    .timeout(timeoutMillis)
                    .userAgent(USER_AGENT)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get();
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not download " + url, exception);
        }
    }
}
