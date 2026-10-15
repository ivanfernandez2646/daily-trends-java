package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedAuthor;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedDescription;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedId;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedTitle;
import java.time.Instant;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/** The text of one front page article. Author and title are non-blank; a blank description is {@code null}. */
public record ScrapedArticle(
        String author, String title, @Nullable String description) {

    /** Applies a source's cleanup to the description; a blank result means no description. */
    ScrapedArticle cleanDescription(UnaryOperator<String> cleanup) {
        if (description == null) {
            return this;
        }
        String cleaned = cleanup.apply(description);
        return new ScrapedArticle(author, title, cleaned.isBlank() ? null : cleaned);
    }

    Feed toFeed(FeedSource source, Instant now) {
        return Feed.create(
                FeedId.random(),
                new FeedTitle(title),
                new FeedDescription(description),
                new FeedAuthor(author),
                source,
                now);
    }
}
