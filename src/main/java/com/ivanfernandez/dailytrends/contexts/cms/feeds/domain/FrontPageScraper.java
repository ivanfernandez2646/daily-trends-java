package com.ivanfernandez.dailytrends.contexts.cms.feeds.domain;

import java.util.List;

/** Extracts the headlines of one source's front page. A failure is an unchecked exception. */
public interface FrontPageScraper {

    FeedSource source();

    List<Feed> scrape();
}
