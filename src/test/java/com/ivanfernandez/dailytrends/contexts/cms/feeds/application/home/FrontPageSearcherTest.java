package com.ivanfernandez.dailytrends.contexts.cms.feeds.application.home;

import static com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource.CMS;
import static com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource.EL_ESPANOL;
import static com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource.EL_MUNDO;
import static com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource.EL_PAIS;
import static org.assertj.core.api.Assertions.assertThat;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.Feed;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedMother;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.domain.FeedSource;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.fakes.InMemoryFeedRepository;
import com.ivanfernandez.dailytrends.contexts.cms.feeds.fakes.RecordingBackgroundRefresh;
import com.ivanfernandez.dailytrends.contexts.cms.shared.fakes.FixedClock;
import java.util.List;
import org.junit.jupiter.api.Test;

class FrontPageSearcherTest {

    private final FixedClock clock = FixedClock.at("2026-10-09T08:00:00Z");
    private final InMemoryFeedRepository repository = new InMemoryFeedRepository();
    private final RecordingBackgroundRefresh refresh = new RecordingBackgroundRefresh();
    private final FrontPageSearcher searcher = new FrontPageSearcher(repository, clock, refresh);

    @Test
    void requests_one_refresh_and_answers_the_stored_feeds_when_the_newest_is_from_a_previous_day() {
        Feed stale = store(EL_MUNDO, "2026-10-08T09:00:00Z");

        List<Feed> page = searcher.search();

        assertThat(page).containsExactly(stale);
        assertThat(refresh.requests()).isEqualTo(1);
    }

    @Test
    void requests_no_refresh_when_the_newest_feed_is_from_today() {
        Feed fresh = store(EL_ESPANOL, "2026-10-09T06:00:00Z");

        assertThat(searcher.search()).containsExactly(fresh);
        assertThat(refresh.requests()).isZero();
    }

    @Test
    void requests_no_refresh_when_there_are_no_feeds() {
        assertThat(searcher.search()).isEmpty();
        assertThat(refresh.requests()).isZero();
    }

    @Test
    void considers_the_business_day_fresh_after_local_midnight_even_if_it_is_the_previous_day_in_utc() {
        // 00:30 on 9 October in Madrid
        store(EL_MUNDO, "2026-10-08T22:30:00Z");

        searcher.search();

        assertThat(refresh.requests()).isZero();
    }

    @Test
    void requests_a_refresh_when_the_newest_feed_is_before_local_midnight() {
        // 23:30 on 8 October in Madrid
        store(EL_MUNDO, "2026-10-08T21:30:00Z");

        searcher.search();

        assertThat(refresh.requests()).isEqualTo(1);
    }

    @Test
    void answers_only_el_mundo_and_el_espanol_feeds_newest_first() {
        Feed older = store(EL_MUNDO, "2026-10-09T05:00:00Z");
        Feed newer = store(EL_ESPANOL, "2026-10-09T06:00:00Z");
        store(CMS, "2026-10-09T07:00:00Z");
        store(EL_PAIS, "2026-10-09T07:30:00Z");

        assertThat(searcher.search()).containsExactly(newer, older);
    }

    @Test
    void answers_at_most_the_ten_newest_feeds() {
        for (int minute = 0; minute < 12; minute++) {
            store(EL_MUNDO, "2026-10-09T06:%02d:00Z".formatted(minute));
        }

        List<Feed> page = searcher.search();

        assertThat(page).hasSize(10);
        assertThat(page.getFirst().createdAt().value()).hasToString("2026-10-09T06:11:00Z");
        assertThat(page.getLast().createdAt().value()).hasToString("2026-10-09T06:02:00Z");
    }

    @Test
    void ignores_cms_feeds_when_deciding_staleness() {
        store(EL_MUNDO, "2026-10-09T06:00:00Z");
        store(CMS, "2026-10-01T06:00:00Z");

        searcher.search();

        assertThat(refresh.requests()).isZero();
    }

    private Feed store(FeedSource source, String createdAt) {
        Feed feed = FeedMother.builder().source(source).createdAt(createdAt).build();
        repository.save(feed);
        return feed;
    }
}
