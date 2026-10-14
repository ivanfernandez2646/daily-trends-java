# S03 · Front page

**Status:** draft · **Depends on:** S01, S02. **Node origin:** `FeedHomeController`, `FeedSearcher`,
`home-feed.feature`.

## Goal
`GET /feed/home` is a fast read that never waits for external sites. When the front page is stale it
starts one scrape run in the background and answers with what is stored. At most one scrape run
happens at a time, and a recent run blocks new background runs for a while.

## `GET /feed/home`
1. Search with criteria: filter `source = EL_MUNDO` OR `source = EL_ESPANOL`, order `created_at`
   descending, limit 10.
2. If there are results and the **newest** was created on a business day before today (S00), request a
   **background refresh**.
3. If there are no results, request nothing.
4. Answer 200 at once with the array from step 1 (feed JSON, S01). Never wait for the refresh and never
   search again in the same request.

- `CMS` and `EL_PAIS` feeds never appear. (Node's filter was `EL_MUNDO` OR `EL_PAIS`; El Español
  replaces El País, S02.)
- An error of the search → 500. An invalid stored feed → 500 (S01).
- Errors of the background run are logged at `ERROR` and never reach any response.

## Background refresh

### One scrape run at a time
- Every scrape run in the process shares one single-flight guard: background refreshes and
  `GET /feed/scrap` (S02).
- A background refresh requested while any run is in progress is **ignored**. Nothing is queued.
- `GET /feed/scrap` **waits** for a run in progress to finish, then performs its own run. Because runs
  never overlap, deduplication (S02) sees the previous run's feeds.

### Cooldown
- A background refresh is **not started** if the previous background run **finished** less than
  `daily-trends.scraping.cooldown` (default `300s`, S01) ago, whatever its outcome: feeds saved,
  nothing saved, every source failed, or an exception.
- A refresh at exactly the cooldown or later starts. `0s` disables the cooldown.
- The cooldown is measured with the `Clock`.
- `GET /feed/scrap` ignores the cooldown and does not start it.

### Lifecycle
- The background run executes on the virtual-thread `ExecutorService` owned by the composition root
  (`AGENTS.md`), never on a request thread.
- On application shutdown, the executor is shut down (`shutdownNow()`), interrupting a run in progress,
  and awaited before the MongoDB client closes. Feeds it already saved stay saved.
- The guard and the cooldown live in the process. The app runs as a single instance.

## Ports and roles
- `FrontPageSearcher` (application): builds the home criteria, searches, decides staleness and requests
  a refresh.
- `ScrapeRunCoordinator` (application): owns the guard and the cooldown. `requestBackgroundRefresh()`
  returns at once; `runNow()` waits for the guard and returns the saved feeds. Both delegate the work to
  `FeedScraper` (S02). `GET /feed/scrap` goes through `runNow()`.
- A background executor port, so tests run the refresh synchronously or hold it to simulate a run in
  progress. The plan decides its shape.

## Edge cases
- First request on a stale day → 200 with yesterday's feeds; a request after the run finishes → 200 with
  today's feeds.
- Both sources fail → the run saves nothing; requests within the cooldown answer the stale page without
  a run; the first request after it starts a new run.
- Every headline is a same-day duplicate (S02) → the page stays stale; the cooldown limits runs.
- Many concurrent `/feed/home` requests on a stale day → exactly one background run.
- `/feed/home` during a `/feed/scrap` run → answers at once, no refresh queued.
- `/feed/scrap` during a background run → waits, runs, and usually answers `[]`.
- Business timezone `Europe/Madrid`, newest feed at `2026-10-08T22:30:00Z` (00:30 on 9 October in
  Madrid), now `2026-10-09T08:00:00Z` → the page is **fresh**. Under a UTC rule it would be stale.

## Acceptance criteria
- Use-case tests with a fixed clock, the fake repository, fake scrapers and a controllable executor:
  - stale page → refresh requested, the response is the first search, one search only;
  - fresh page or empty result → no refresh requested;
  - the Madrid local-midnight case above is fresh;
  - only `EL_MUNDO` and `EL_ESPANOL` feeds, newest first, at most 10;
  - a refresh requested while a run is in progress is ignored (one scraper call);
  - a refresh within the cooldown is ignored; at exactly the cooldown or later it starts; after success
    and after failure;
  - a background run that throws is logged, does not propagate, releases the guard and starts the
    cooldown;
  - `runNow()` waits for a background run in progress, ignores the cooldown and does not start it;
  - many concurrent requests on a stale page start one run (real threads, a latch to hold the run).
- Shutdown test: a run in progress is interrupted and awaited.
- Acceptance tests (Spring Boot, MongoDB in Testcontainers, fake scrapers), adapted from Node's
  `home-feed.feature` and `scrap-feed.feature`:
  - empty database → 200 `[]`, no scrape run;
  - the `home-feed.feature` rows with `EL_PAIS` changed to `EL_ESPANOL` and the empty `createdAt` set to
    now → 200 with the `EL_MUNDO` feed first and the `EL_ESPANOL` one second; no run (fresh page);
  - two `CMS` feeds only → `/feed/home` → `[]`; `/feed/scrap` → 10; `/feed/home` → 10;
  - one stale `EL_MUNDO` feed → 200 with that feed; after the background run completes, `/feed/home`
    returns the 10 scraped feeds;
  - every source fails → `/feed/home` keeps answering the stale feed.
- OpenAPI test: `GET /feed/home` appears in `/v3/api-docs` with a `200` array of the same feed schema
  as `/feed/scrap`.
