---
name: 's02-scrape-external-sources'
description: 'GET /feed/scrap scrapes El Mundo and El Español in parallel, deduplicates by source, title and business day, saves, publishes feed.created and returns the saved feeds'
spec: 'specs/feeds/S02-scrape-external-sources.md'
branch: 'feat/s02-scrape-external-sources'
created_at: '2026-10-14T16:19:31Z'
created_by:
  tool: 'Claude Code'
  model: 'claude-opus-5-5'
updated_at: '2026-10-14T16:27:06Z'
implemented_phases:
  - phase: 1
    implemented_at: '2026-10-14T16:27:06Z'
    tool: 'Claude Code'
    model: 'claude-opus-5-5'
---

# S02 · Scrape external sources plan

## Goal

Deliver `specs/feeds/S02-scrape-external-sources.md`. `GET /feed/scrap` runs the El Mundo and El
Español scrapers in parallel on virtual threads. A failing scraper is logged at `WARN` and adds no
feeds. The run skips headlines already stored for the same source on the same business day, saves
the rest, publishes `feed.created` and answers 200 with the saved feeds. The repository stays green
after every phase.

## Context

- S01 is merged into `main` (19602e3). Already available:
  - `Feed.create(...)` records `feed.created`;
  - `FeedRepository.existsHeadline(source, title, from, to)` with `[from, to)` semantics, in Mongo
    and in `InMemoryFeedRepository`;
  - `Clock` with `zone()`, `FixedClock`, `FeedMother`;
  - `FeedResponse`, `EventBus` / `InMemoryEventBus` and `DailyTrendsProperties.scraping().timeout()`;
  - jsoup 1.23.2 in the pom;
  - the `network` test group, excluded from surefire and failsafe unless `-Pnetwork` is used.
- Not available yet: an executor bean, a fake event bus and any `/feed/*` route.
- `OpenApiDocumentIT.leaves_the_framework_routes_out` asserts the paths are only `/status`. Phase 4
  changes it to also allow `/feed/scrap`.
- Node origin: `FeedScraperBase` (fetch, cheerio and the `article` walk), one subclass per source,
  `FeedScraperDomainService` (`Promise.allSettled`) and the `FeedScraper` use case.

### Decisions recorded in this plan

- **Text of a scraped field:** jsoup `Elements.text()`. Matches are joined with a space and
  whitespace is collapsed. This differs from Cheerio, so S02 was updated and S00 gained a
  differences row during planning (S02 is still a draft, so no improvement spec is needed).
- **HTTP stub for the request tests:** the JDK `com.sun.net.httpserver.HttpServer`, behind a small
  test helper. There is no new dependency, and the tests control the raw bytes, headers, status codes
  and delays.
- **Infrastructure shape: close to Node, composition instead of inheritance.** Node's
  `FeedScraperBase` is split into two classes because the spec requires download and extraction to
  be separate:
  - `JsoupPageDownloader` does the fetch;
  - `FrontPageExtractor` does the walk and returns `ScrapedArticle`s;
  - one class per source (`ElMundoFrontPageScraper`, `ElEspanolFrontPageScraper`) holds the URL,
    the selectors and the cleanup, and calls `Feed.create`.

  The cleanup runs before `Feed.create`. Node rebuilt the feed with `fromPrimitives` instead, which
  would lose `feed.created`.
- **Logging in `application`:** SLF4J, as `ApiExceptionHandler` already uses it. ArchUnit does not
  forbid it.
- **The use case takes an `Executor`, not an `ExecutorService`.** Tests pass `Runnable::run` or a
  real virtual-thread executor. The use case cannot shut down an executor it does not own.
- **Executor lifecycle:** one `ExecutorService` bean from `Executors.newVirtualThreadPerTaskExecutor()`
  in the composition root. Spring's inferred `close()` runs on shutdown, which waits for in-flight
  scrapes; each scrape is bounded by the request timeout. S03 replaces this with `shutdownNow()`
  awaited before the MongoDB client closes.
- **Acceptance tests use fake scrapers** through a `@TestConfiguration` with a `@Primary FeedScraper`
  built on `FakeFrontPageScraper`s and the real repository, event bus, clock and executor. Bean
  definition overriding is not enabled, and the real scrapers are created but never called.
- **Scraper order** is explicit: the `FeedScraper` bean gets `List.of(elMundo, elEspanol)`.

### Pieces

| Piece | Package | Kind | Node equivalent |
|---|---|---|---|
| `FrontPageScraper` | `contexts/cms/feeds/domain` | port | `FeedScrap` |
| `FeedId.random()` | `contexts/cms/feeds/domain` | static factory | `FeedId.random()` |
| `BusinessDay` | `contexts/cms/shared/domain` | record | — (server `TZ`) |
| `FeedScraper` | `contexts/cms/feeds/application/scrap` | use case | `FeedScraper` + `FeedScraperDomainService` |
| `JsoupPageDownloader` | `contexts/cms/feeds/infrastructure/scrap` | adapter | fetch half of `FeedScraperBase` |
| `FrontPageExtractor`, `ScrapedArticle` | `contexts/cms/feeds/infrastructure/scrap` | adapter, record | cheerio half of `FeedScraperBase` |
| `ElMundoFrontPageScraper`, `ElEspanolFrontPageScraper` | `contexts/cms/feeds/infrastructure/scrap` | port adapters | `ElMundoFeedScraper`, `ElPaisFeedScraper` |
| `FeedScrapGetController` | `apps/cmsbackend/feeds` | controller | `FeedScrapController` |
| `ExecutorService` and `FeedScraper` beans | `apps/cmsbackend/config` | composition root | DI container |
| `FakeFrontPageScraper`, `RecordingEventBus` | test `feeds/fakes`, `shared/fakes` | fakes | — |

## Phases

## Phase 1: Scrape run use case

`FeedScraper` runs every `FrontPageScraper` in parallel, tolerates failing scrapers, deduplicates by
source, title and business day, saves, publishes and returns the saved feeds. Only domain and
application code; nothing is wired yet.

### Public contracts

- `contexts/cms/feeds/domain/FrontPageScraper`:
  ```java
  public interface FrontPageScraper {
      FeedSource source();
      List<Feed> scrape(); // a failure is an unchecked exception
  }
  ```
- `FeedId.random()`: a new random UUID v4 (`UUID.randomUUID()`).
- `contexts/cms/shared/domain/BusinessDay`:
  ```java
  public record BusinessDay(LocalDate date, ZoneId zone) {
      public static BusinessDay of(Instant instant, ZoneId zone);
      public Instant start(); // local midnight of date
      public Instant end();   // local midnight of the next day
  }
  ```
- `contexts/cms/feeds/application/scrap/FeedScraper`:
  ```java
  public final class FeedScraper {
      public FeedScraper(List<FrontPageScraper> scrapers, FeedRepository repository,
                         EventBus eventBus, Clock clock, Executor executor);
      public List<Feed> scrape(); // saved feeds, scraper order then article order
  }
  ```
  - It starts every scraper with `CompletableFuture.supplyAsync(scraper::scrape, executor)`.
  - A failed future is logged at `WARN` with the scraper's `source()` and the cause, and gives
    `List.of()`.
  - The futures are joined in scraper order. Each feed is then handled in turn: skipped if
    `existsHeadline(source, title, day.start(), day.end())` is true, where
    `day = BusinessDay.of(feed.createdAt().value(), clock.zone())`; otherwise saved, followed by
    `eventBus.publish(feed.pullDomainEvents())`. Saving one at a time makes a duplicate within the
    run hit the repository check.
  - A save error propagates; feeds already saved stay saved.
- Test fakes:
  - `feeds/fakes/FakeFrontPageScraper`: `returning(FeedSource, Clock, String... titles)` creates
    fresh feeds with those titles on every call; `failing(FeedSource, RuntimeException)` throws;
  - `shared/fakes/RecordingEventBus`: keeps the published events and exposes them with `published()`.

### Tests first

- [x] `FeedIdTest`: `random()` is a valid UUID v4, and two calls differ.
- [x] `BusinessDayTest`:
  - `2026-10-08T21:30:00Z` and `2026-10-08T22:30:00Z` in `Europe/Madrid` are 8 and 9 October;
  - `start()` and `end()` are the local midnights;
  - a spring-forward day lasts 23 h and a fall-back day 25 h (`2026-03-29`, `2026-10-25`).
- [x] `FeedScraperTest`, with `InMemoryFeedRepository`, `RecordingEventBus`, `FixedClock`,
  `FakeFrontPageScraper` and `Runnable::run` unless stated otherwise:
  - saves, publishes `feed.created` for, and returns the feeds of both scrapers, in scraper order
    then article order;
  - one scraper fails → the other's feeds are saved, published and returned, and a `WARN` naming
    the failed source is logged (a Logback `ListAppender`, see the notes below);
  - every scraper fails or finds nothing → `[]`, nothing saved, nothing published;
  - same-day duplicate already stored → skipped: not saved, not published, not returned;
  - duplicate within the run (a page listing a headline twice) → only the first is saved;
  - same title stored on the previous business day → saved;
  - same title stored today by another source (`CMS`) → saved;
  - Madrid midnight: stored at `2026-10-08T21:30:00Z`, scraped at `2026-10-08T22:30:00Z` → saved;
  - a save error propagates, and feeds saved before it stay saved (a test-local repository that
    fails on the n-th save);
  - the scrapers run in parallel: with a virtual-thread executor, two scrapers that each wait on a
    shared `CountDownLatch` of 2 (with a timeout) both finish.

### Implementation

- [x] Add `FrontPageScraper`, `FeedId.random()` and `BusinessDay`.
- [x] Add `FeedScraper` with the minimum code that makes each test pass.
- [x] Add `FakeFrontPageScraper` and `RecordingEventBus`.
- [x] Refactor without changing behavior.
- [x] Run `./mvnw verify` and fix failures, including ArchUnit (no Spring or Jsoup in `application`).
- [x] STOP for user review. Suggest three Conventional Commit messages.

### Implementation notes

- **Log capture without Spring.** The ArchUnit rule `domain_and_application_are_framework_free`
  also analyzes test classes, so `FeedScraperTest` (package `..application..`) cannot use Spring
  Boot's `OutputCaptureExtension`. It attaches a Logback `ListAppender` to the `FeedScraper` logger
  instead, which also checks the exact `WARN` level, the source in the message and the cause. The
  rule was not relaxed.
- **`FakeFrontPageScraper.answering(source, supplier)`** was added next to `returning` and `failing`.
  The parallel test uses it for scrapers that wait for each other on a latch. It also counts calls
  (`calls()`), which S03 needs.
- The failing-save repository is a nested fake in `FeedScraperTest` that delegates to
  `InMemoryFeedRepository`, because only that test uses it.

## Phase 2: Page download with jsoup

`JsoupPageDownloader` downloads a page with the agreed HTTP behavior and lets jsoup pick the
charset. It is verified against a local stub server; no real network.

### Public contracts

- `contexts/cms/feeds/infrastructure/scrap/JsoupPageDownloader`:
  ```java
  public final class JsoupPageDownloader {
      public JsoupPageDownloader(Duration timeout);
      public Document download(String url); // UncheckedIOException on any IOException
  }
  ```
  - Built with `Jsoup.connect(url)`, `.timeout(millis)` (the whole request), `.userAgent("daily-trends-java")`,
    `.followRedirects(true)` and `.ignoreHttpErrors(true)`, then `.get()`. No `Content-Type` is
    set. A duration that does not fit in an `int` of milliseconds is capped at `Integer.MAX_VALUE`.
  - The charset comes from jsoup (header, then BOM or `<meta>`, then UTF-8) and is not reimplemented.
- Test helper `StubHttpServer` (test sources, `infrastructure/scrap`): it wraps `HttpServer` on an
  ephemeral port, registers handlers per path with raw bytes, headers, status and an optional delay,
  and records the request headers. It is `AutoCloseable`.

### Tests first

- [ ] `JsoupPageDownloaderTest` (unit, `StubHttpServer`):
  - sends `User-Agent: daily-trends-java` and no `Content-Type` header;
  - follows a `301` to another path and parses the target page;
  - a `404` page is returned and parsed (its `<title>` is readable);
  - a server that waits longer than the timeout before answering fails with `UncheckedIOException`;
  - a server that sends the body in slow chunks, each faster than the timeout but longer in total,
    fails (the timeout covers the whole request);
  - an unreachable port fails with `UncheckedIOException`.
- [ ] Charset cases (same class, `€` encoded as the byte `0xA4` in iso-8859-15):
  - `Content-Type: text/html; charset=iso-8859-15` decodes `€`;
  - only `<meta charset="iso-8859-15">` decodes `€`;
  - only `<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-15">` decodes `€`;
  - no declaration with UTF-8 bytes decodes `€`;
  - header `iso-8859-15` and `<meta charset="utf-8">` with iso-8859-15 bytes → `€` (the header wins);
  - an unknown header charset name with `<meta charset="iso-8859-15">` → `€` (counts as absent).
  If jsoup disagrees with a case, stop and report: the spec says to verify jsoup's behavior, not
  reimplement it.

### Implementation

- [ ] Add `StubHttpServer`.
- [ ] Add `JsoupPageDownloader` with the minimum code that makes each test pass.
- [ ] Refactor without changing behavior.
- [ ] Run `./mvnw verify` and fix failures.
- [ ] STOP for user review. Suggest three Conventional Commit messages.

## Phase 3: Front page extraction and source scrapers

`FrontPageExtractor` walks the `article` elements. `ElMundoFrontPageScraper` and
`ElEspanolFrontPageScraper` implement `FrontPageScraper`: they download the page, extract, clean up
and create feeds. Extraction is tested with HTML fixtures, and live smoke tests are tagged
`network`.

### Public contracts

- `contexts/cms/feeds/infrastructure/scrap/ScrapedArticle`:
  `record ScrapedArticle(String author, String title, @Nullable String description)`. The author
  and title are non-blank. A missing or blank description is `null`.
- `contexts/cms/feeds/infrastructure/scrap/FrontPageExtractor`:
  ```java
  public final class FrontPageExtractor {
      public FrontPageExtractor(String authorSelector, String titleSelector, String descriptionSelector);
      public List<ScrapedArticle> extract(Document page); // document order, at most 5
  }
  ```
  For each `article`, it reads `article.select(selector).text()`. It skips an article with a blank
  author or title and stops after 5 extracted articles.
- `ElMundoFrontPageScraper implements FrontPageScraper`:
  - `ElMundoFrontPageScraper(JsoupPageDownloader downloader, Clock clock)`, `source()` → `EL_MUNDO`;
  - `scrape()` is `extract(downloader.download("https://elmundo.es/"))`;
  - package-private `List<Feed> extract(Document page)` uses the selectors
    `.ue-c-cover-content__byline-name .ue-c-cover-content__link`, `.ue-c-cover-content__headline` and
    `.ue-c-cover-content__kicker`;
  - it removes one trailing period from the description, then turns a blank one into `null`;
  - it calls `Feed.create(FeedId.random(), …, EL_MUNDO, clock.now())`.
- `ElEspanolFrontPageScraper implements FrontPageScraper`: same shape, `EL_ESPANOL`,
  `https://www.elespanol.com/`, `.art__author`, `.art__title`, `.art__subtitle`, no cleanup.
- Fixtures in `src/test/resources/scrap/`: `front-page-walk.html`, `elmundo.html` and
  `elespanol.html`, written by hand with the real class names.

### Tests first

- [ ] `FrontPageExtractorTest` (`Jsoup.parse` of fixtures):
  - articles in document order;
  - at most 5, counting only extracted articles (skipped ones do not count);
  - skips an article with no author element, a blank author, no title element or a blank title;
  - a missing description and a blank description are both `null`;
  - several matches of one selector are joined with a space (`<a>Ana</a><a>Luis</a>` → `Ana Luis`);
  - inner whitespace is collapsed and the text is trimmed;
  - a page with no `article` → `[]`.
- [ ] `ElMundoFrontPageScraperTest` (`extract(Jsoup.parse(fixture))`, `FixedClock`):
  - the feeds have source `EL_MUNDO`, `createdAt` from the clock, `updatedAt` `null`, distinct v4
    ids and a recorded `feed.created`;
  - the El Mundo selectors read author, title and description;
  - cleanup, parameterized: `"Crucigrama."` → `"Crucigrama"`, `"Sudoku..."` → `"Sudoku.."`,
    `"¿Qué pasa?"` unchanged, `"."` → `null`.
- [ ] `ElEspanolFrontPageScraperTest`: source `EL_ESPANOL`, its selectors, and `"Crucigrama."`
  kept as is.
- [ ] `FrontPageScrapersNetworkIT`, `@Tag("network")`, real `JsoupPageDownloader` with a 10 s
  timeout. One test per scraper: 5 feeds with the scraper's source and non-blank author and title.
  It runs only with `./mvnw verify -Pnetwork`.

### Implementation

- [ ] Add `ScrapedArticle` and `FrontPageExtractor`.
- [ ] Add both source scrapers.
- [ ] Refactor: if the `ScrapedArticle` → `Feed` mapping is duplicated, move it to a method on
  `ScrapedArticle` (for example `toFeed(FeedSource, Instant)`).
- [ ] Run `./mvnw verify` and fix failures. Run `./mvnw verify -Pnetwork` once and report the result.
  If El Mundo yields fewer than 5 feeds, check whether jsoup's default `maxBodySize` truncates the page
  before changing anything.
- [ ] STOP for user review. Suggest three Conventional Commit messages.

## Phase 4: `GET /feed/scrap`, wiring and acceptance

Expose the use case over HTTP, wire everything in the composition root, and document the route in
OpenAPI.

### Public contracts

- `apps/cmsbackend/feeds/FeedScrapGetController`:
  ```java
  @GetMapping("/feed/scrap")
  public List<FeedResponse> scrap(); // FeedResponse.from(feed.toPrimitives()) for each saved feed
  ```
  - `@Operation(summary = "Scrape the external front pages and save the new headlines")`;
  - a `200` array of `FeedResponse`; the `500` comes from `OpenApiConfiguration`.
- Composition root:
  - `SharedConfiguration`: `@Bean ExecutorService virtualThreadExecutor()` →
    `Executors.newVirtualThreadPerTaskExecutor()`;
  - `FeedsConfiguration`:
    - `@Bean JsoupPageDownloader` (`properties.scraping().timeout()`);
    - `@Bean FeedScraper feedScraper(...)` with
      `List.of(new ElMundoFrontPageScraper(...), new ElEspanolFrontPageScraper(...))`.
- OpenAPI: `/feed/scrap` with `200` (array of `#/components/schemas/FeedResponse`) and `500`.
  `FeedResponse.description` and `updated_at` are nullable (`["string", "null"]` in OpenAPI 3.1).
- Test configuration `FakeScrapersConfiguration` (`@TestConfiguration`): a `@Primary FeedScraper`
  with two `FakeFrontPageScraper`s, `EL_MUNDO` and `EL_ESPANOL`, each with 5 fixed titles, using the
  real `FeedRepository`, `EventBus`, `Clock` and executor.

### Tests first

- [ ] `FeedScrapGetControllerIT` (`@IntegrationTest`, `@Import(FakeScrapersConfiguration.class)`,
  feeds collection emptied before each test), adapted from `scrap-feed.feature`:
  - two `CMS` feeds seeded → `GET /feed/scrap` → 200 with 10 feeds, the 5 `EL_MUNDO` first, then
    the 5 `EL_ESPANOL`, in feed JSON; 12 stored;
  - a second `GET /feed/scrap` → 200 `[]`, still 12 stored.
- [ ] `OpenApiDocumentIT`:
  - `leaves_the_framework_routes_out` → the paths are exactly `/status` and `/feed/scrap`;
  - `/feed/scrap` `get` has responses `200` and `500`, and `200` is an array of `FeedResponse`;
  - the `FeedResponse` schema marks `description` and `updated_at` as nullable.
- [ ] `FixedFeedRoutesIT` keeps passing (`/feed/scrap` is a literal pattern).

### Implementation

- [ ] Add the executor, downloader and `FeedScraper` beans.
- [ ] Add `FeedScrapGetController` with its OpenAPI annotations. Adjust `FeedResponse` schema
  annotations only if springdoc does not mark the JSpecify `@Nullable` components as nullable.
- [ ] Add `FakeScrapersConfiguration`.
- [ ] Refactor without changing behavior.
- [ ] Run `./mvnw verify` and fix failures.
- [ ] Manual check: `docker compose up -d --wait`, `./mvnw spring-boot:run`, then
  `curl localhost:5000/feed/scrap`; a second call returns `[]`.
- [ ] STOP for user review. Suggest three Conventional Commit messages.

## Next step

Phase 1 is implemented. After review, run `/ai-project-implement-phase` for Phase 2. After the last phase, review and commit, push
`feat/s02-scrape-external-sources` (the pre-push hook runs the quality gate) and open a pull request
to `main`. Then plan S03 with `/ai-project-create-plan specs/feeds/S03-front-page.md`.
