package com.ivanfernandez.dailytrends.contexts.cms.feeds.application.scrap;

/** Asks for a scrape run in the background; returns at once and never reports the outcome. */
public interface BackgroundRefresh {

    void requestBackgroundRefresh();
}
