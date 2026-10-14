package com.ivanfernandez.dailytrends.contexts.cms.feeds.fakes;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedRepositoryContractTest;

class InMemoryFeedRepositoryTest extends FeedRepositoryContractTest {

    private final InMemoryFeedRepository repository = new InMemoryFeedRepository();

    @Override
    protected FeedRepository repository() {
        return repository;
    }
}
