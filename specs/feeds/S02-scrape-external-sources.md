# S02 · Scrape external sources

**Status:** done · **Depends on:** S01. **Node origin:** `FeedScraper`, `FeedScraperBase`,
`ElMundoFeedScraper`, `ElPaisFeedScraper`, `scrap-feed.feature`.

## Goal
Download the El Mundo and El Español front pages, extract up to five headlines from each, and save
the ones not already stored on the same business day.

## `GET /feed/scrap`
- Runs both scrapers in parallel (`CompletableFuture` on the virtual-thread executor, `AGENTS.md`). A
  scraper that fails (network error, timeout, unexpected exception)
  is logged at `WARN` and contributes nothing; the other's feeds are still saved.
- Response 200 with a JSON array of the feeds **saved by this run** (feed JSON, S01), in scraper order
  (El Mundo, then El Español) and article order within each scraper.
- Every scraper fails or finds nothing new → 200 `[]`.
- A save error propagates as 500. Feeds already saved are not rolled back.
- After saving each feed, its domain events are published (`feed.created`).
- Waits for a scrape run in progress (S03) to finish, then performs its own run. It ignores the S03
  cooldown and does not start it. Until S03 exists there is nothing to wait for.

## Extraction
Same walk as Node's `FeedScraperBase`:

- Walk the `article` elements of the page in document order.
- Inside each article, read author, title and description with the source's selectors, as jsoup's
  `Elements.text()`: the text of every matching element, joined with a space, with whitespace
  collapsed and trimmed (`<a>Ana</a><a>Luis</a>` → `"Ana Luis"`). Cheerio's `.text()` joined them with
  no separator and kept inner whitespace (S00).
- Skip an article with no author or no title (no element, or blank text).
- Stop after **5 feeds per source**. The limit counts extracted articles, before deduplication.
- A missing or blank description is `null`. The rule applies after the source's cleanup.
- Each extracted article becomes a feed with `Feed.create`: random UUID v4, the scraper's source,
  `createdAt` from the `Clock`, `updatedAt` `null`.

## Sources
| Source | URL | Author | Title | Description | Cleanup |
|---|---|---|---|---|---|
| `EL_MUNDO` | `https://elmundo.es/` | `.ue-c-cover-content__byline-name .ue-c-cover-content__link` | `.ue-c-cover-content__headline` | `.ue-c-cover-content__kicker` | remove one trailing period |
| `EL_ESPANOL` | `https://www.elespanol.com/` | `.art__author` | `.art__title` | `.art__subtitle` | none |

- El Mundo cleanup, after trimming: `"Crucigrama."` → `"Crucigrama"`, `"Sudoku..."` → `"Sudoku.."`,
  `"¿Qué pasa?"` unchanged, `"."` → `null`. Node cut the last character whatever it was.
- El País is not scraped: since 2026-10 `https://elpais.com/` answers 403 with a bot-protection page to
  any non-browser client. Bypassing it is rejected. To restore it, add a scraper with
  `https://elpais.com/`, `.c_a`, `.c_h`, `.c_d` and no cleanup.

## HTTP requests
Each page is downloaded with `Jsoup.connect(url)`:

- Timeout `daily-trends.scraping.timeout` (default `10s`, S01) for the whole request. A timeout
  fails that scraper only.
- `User-Agent: daily-trends-java`. A GET sends no `Content-Type` header.
- Redirects are followed (`https://elmundo.es/` answers 301 to `https://www.elmundo.es/`).
- `ignoreHttpErrors(true)`: a non-2xx response is not an error; its body is parsed like any other page,
  so it usually yields no feeds.
- **Charset:** the `charset` of the response `Content-Type` header; else the document's BOM,
  `<meta charset>` or `<meta http-equiv="Content-Type">`; else UTF-8. An unknown charset name counts
  as absent. This is jsoup's behavior: verify it with the tests below, do not reimplement it.
  El Mundo currently declares `iso-8859-15`.

## Deduplication
- A scraped feed is **not saved** when a feed with the same `source` and the same `title` (exact,
  case-sensitive, after trimming) already exists with a `createdAt` on the same **business day** as
  the scraped feed's `createdAt`.
- The same rule applies within one run: if a page lists a headline twice, only the first is saved.
- Feeds of other sources never count: a `CMS` feed with the same title does not block a scraped one.
- Skipped feeds are not saved, not published and not part of the response.
- The id is never looked up: a feed is saved with the id it was created with. A UUID v4 collision is
  not handled.
- Concurrent runs cannot both save a headline because runs never overlap (S03). There is no unique
  index.

## Ports and roles
- `FrontPageScraper` (domain port): `FeedSource source()` and `List<Feed> scrape()`; one adapter per
  source. A failure is an unchecked exception.
- The download (`Jsoup.connect`, returns a jsoup `Document`) and the extraction (`Document` → feeds) are
  separate infrastructure classes, so extraction is tested with HTML fixtures parsed with
  `Jsoup.parse` and no network. The plan names the classes.
- `FeedScraper` (application): runs every scraper, deduplicates, saves, publishes the events and
  returns the saved feeds.

## Edge cases
- A second `/feed/scrap` on the same business day with unchanged front pages → 200 `[]`, nothing
  saved.
- A headline stored yesterday (business day) and still on the front page → saved again today.
- Madrid timezone: the same headline scraped at `2026-10-08T21:30:00Z` (23:30 local) and
  `2026-10-08T22:30:00Z` (00:30 local) → two business days, both saved.
- A response that declares one charset in the header and another in `<meta>` → the header wins.
- A page with no `article` (markup changed, bot protection) → that scraper saves nothing, no error.

## Acceptance criteria
- Extraction tests with hand-written HTML fixtures (`src/test/resources`), never the real network:
  document order, the limit of 5, skipping articles without author or title, `null` and blank
  descriptions, El Mundo period removal for each example above.
- Charset tests through the HTTP stub (the header path only exists with a real response):
  `iso-8859-15` declared in the header, and only in `<meta>`, decode `€` correctly; no declaration
  decodes UTF-8; header wins over `<meta>`.
- Request tests against a local HTTP stub (the plan picks it: the JDK `HttpServer` or WireMock): the
  `User-Agent` is sent and no `Content-Type`; redirects are followed; a 404 page is still parsed; a
  slow server makes the scraper fail with a timeout.
- Use-case tests with fake scrapers, the fake repository, a fake event bus and a fixed clock:
  - one scraper fails, the other's feeds are saved, published and returned;
  - a same-day duplicate (stored, or within the run) is skipped and not returned;
  - a duplicate from a previous business day, or from another source, is saved;
  - the Madrid midnight case above.
- Acceptance test (Spring Boot, MongoDB in Testcontainers, fake scrapers returning 5 feeds each),
  adapted from Node's `scrap-feed.feature`:
  - two `CMS` feeds seeded → `GET /feed/scrap` → 200 with 10 feeds, 12 stored;
  - a second `GET /feed/scrap` → 200 `[]`, still 12 stored.
  The `/feed/home` and `/feed/list` steps of the Node scenario are covered by S03, or by reading the
  repository while `/feed/list` is out of scope.
- OpenAPI test: `GET /feed/scrap` appears in `/v3/api-docs` with a `200` array of the feed schema
  (S01 feed JSON, `description` and `updated_at` nullable).
- One live smoke test per scraper against the real front page, tagged `network` and run only with
  `./mvnw verify -Pnetwork`: 5 feeds with the scraper's source and non-blank author and title.
