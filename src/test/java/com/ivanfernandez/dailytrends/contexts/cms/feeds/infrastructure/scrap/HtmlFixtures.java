package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/** Hand-written front pages under {@code src/test/resources/scrap/}, parsed without any network. */
final class HtmlFixtures {

    private HtmlFixtures() {}

    static Document page(String name) {
        try (InputStream html = Objects.requireNonNull(
                HtmlFixtures.class.getResourceAsStream("/scrap/" + name), () -> "missing fixture " + name)) {
            return Jsoup.parse(html, "UTF-8", "https://fixture.test/");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
