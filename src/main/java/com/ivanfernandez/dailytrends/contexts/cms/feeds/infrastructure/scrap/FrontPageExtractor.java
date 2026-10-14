package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jspecify.annotations.Nullable;

/**
 * Walks the {@code article} elements of a front page in document order and reads each field as
 * {@code Elements.text()}: every match joined with a space, whitespace collapsed and trimmed.
 */
public final class FrontPageExtractor {

    private static final int MAX_ARTICLES = 5;

    private final String authorSelector;
    private final String titleSelector;
    private final String descriptionSelector;

    public FrontPageExtractor(String authorSelector, String titleSelector, String descriptionSelector) {
        this.authorSelector = authorSelector;
        this.titleSelector = titleSelector;
        this.descriptionSelector = descriptionSelector;
    }

    /** At most five articles; one without author or title is skipped and does not count. */
    public List<ScrapedArticle> extract(Document page) {
        var articles = new ArrayList<ScrapedArticle>();
        for (Element article : page.select("article")) {
            String author = article.select(authorSelector).text();
            String title = article.select(titleSelector).text();
            if (author.isEmpty() || title.isEmpty()) {
                continue;
            }
            articles.add(new ScrapedArticle(
                    author,
                    title,
                    emptyToNull(article.select(descriptionSelector).text())));
            if (articles.size() == MAX_ARTICLES) {
                break;
            }
        }
        return List.copyOf(articles);
    }

    private static @Nullable String emptyToNull(String text) {
        return text.isEmpty() ? null : text;
    }
}
