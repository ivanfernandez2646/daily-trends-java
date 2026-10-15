---
name: 'front-page'
description: 'Implement stale front-page refresh and shared scraper coordination for GET /feed/home'
spec: 'specs/feeds/S03-front-page.md'
branch: 'feat/s03-front-page'
created_at: '2026-10-15T13:52:42+02:00'
created_by:
  tool: 'Copilot SDK'
  model: 'default'
---

# Goal

Deliver the front-page read path for `GET /feed/home` while enforcing the single-flight guard, cooldown policy, and "never block on a background refresh" behavior described in S03. The implementation must preserve the shared scraper contract from S02 and keep the background refresh isolated from request threads.

# Context

- This work depends on S01 and S02 and should align with the project architecture in `AGENTS.md`.
- The home page is a fast read: it searches the feed store, responds immediately, and only requests a background refresh when the newest feed is stale.
- The same scrape coordination logic is shared with `GET /feed/scrap`, so the guard and cooldown must live in the application layer rather than in controller code.
- The runtime uses a single virtual-thread `ExecutorService` owned by the composition root and must not allow overlapping scraping runs.
- Acceptance tests must validate the EL_MUNDO/EL_ESPAÑOL filter, ordering, stale-day logic, concurrency, shutdown behavior, and OpenAPI response contract.

# Phases

## Phase 1: Front-page search and stale-page decision

Description.

Implement the use case that searches the feed store, filters to `EL_MUNDO` and `EL_ESPAÑOL`, limits the result set to 10 newest items, and decides whether the page is stale enough to request a background refresh. The response must still return the current stored data immediately without waiting for any refresh.

### Public contracts

- `FrontPageSearcher` application service: returns the current page payload and requests a refresh when stale.
- Home-page criteria contract: `source IN (EL_MUNDO, EL_ESPAÑOL)`, `created_at` descending, limit 10.
- `GET /feed/home` response contract remains the same feed JSON structure as the S01 feed API; no extra response fields are introduced.

### Tests first

- [x] Add a failing use-case test for a stale page: one search, one refresh request, immediate response.
- [x] Add a failing use-case test for a fresh page or empty result: no refresh requested.
- [x] Add a failing use-case test for the Madrid local-midnight case: 2026-10-09 00:30 in Madrid is considered fresh.
- [x] Add a failing use-case test for the filter and ordering rules: only `EL_MUNDO` and `EL_ESPAÑOL`, newest first, max 10.

### Implementation

- [x] Add the minimum front-page search and stale detection logic.
- [x] Wire the application service to a refresh-request port without blocking the HTTP request.
- [x] Reuse the feed schema and validation rules from S01.
- [x] Run the relevant unit tests.
- [ ] STOP for user review. Suggest three Conventional Commit messages.

### Verification

- `./mvnw test`
- `./mvnw verify` once the slice is stable

## Phase 2: Single-flight guard, cooldown, and scraper coordination

Description.

Introduce the `ScrapeRunCoordinator` that owns the single-flight guard and cooldown timers. This phase ensures background refreshes are ignored when a run is in flight or within the cooldown window, while `GET /feed/scrap` waits for the active run instead of overlapping with it.

### Public contracts

- `ScrapeRunCoordinator` API: `requestBackgroundRefresh()` and `runNow()`.
- Background executor port abstraction for controllable execution in tests and a clean boundary from the application layer.
- Lifecycle contract: on shutdown, an in-progress run is interrupted and awaited before the MongoDB client closes, without losing already-saved feeds.
- No public response contract changes for `GET /feed/home`; only the internal orchestration changes.

### Tests first

- [x] Add a failing unit test for refresh requests while a run is in progress: the refresh is ignored and only one scraper call happens.
- [x] Add a failing unit test for cooldown behavior: ignored inside the cooldown window and allowed at or after the threshold.
- [x] Add a failing unit test for `runNow()`: it waits for a current background run, ignores the cooldown, and does not queue a duplicate run.
- [x] Add a failing unit test for background-run exceptions: they are logged, do not propagate, the guard is released, and the cooldown starts.
- [x] Add a failing shutdown test: in-progress runs are interrupted and awaited.

### Implementation

- [x] Add the single-flight guard and cooldown state in the application-layer coordinator.
- [x] Add a background executor abstraction that can be swapped in tests.
- [x] Delegate the actual scraping work to the existing `FeedScraper` contract from S02.
- [x] Ensure `GET /feed/scrap` uses the coordinator but bypasses the cooldown.
- [x] Run the relevant unit and integration tests.
- [ ] STOP for user review. Suggest three Conventional Commit messages.

### Verification

- `./mvnw test`
- `./mvnw verify`

## Phase 3: Spring Boot acceptance and OpenAPI contract

Description.

Wire the front-page use case into the controller, verify the acceptance scenarios through Spring Boot and Testcontainers, and confirm the OpenAPI schema exposes `GET /feed/home` with the same feed array schema as `/feed/scrap`.

### Public contracts

- HTTP contract for `GET /feed/home`: 200 with the feed array, immediate response without waiting on background refresh, background refresh as a side effect only.
- Controller and exception-handling contract remains aligned with the app-wide error policy from `AGENTS.md`.
- OpenAPI contract: `GET /feed/home` appears in `/v3/api-docs` with a response array of the same feed schema as `/feed/scrap`.

### Tests first

- [x] Add a failing acceptance test for empty database: `200 []` and no scrape run.
- [x] Add a failing acceptance test mirroring the home-feed rows with `EL_PAIS` replaced by `EL_ESPAÑOL` and no run for a fresh page.
- [x] Add a failing acceptance test for CMS-only data: home returns `[]`, scrap returns 10, then home returns 10.
- [x] Add a failing acceptance test for a stale `EL_MUNDO` feed: stale page returns the stale feed, then the background run updates it.
- [x] Add a failing acceptance test for every source failing: the stale feed continues to be served.
- [x] Add a failing OpenAPI test: `GET /feed/home` has the expected 200 response schema.

### Implementation

- [x] Expose the controller and DTO mapping for the front page.
- [x] Configure the composition root to assemble the front page service with the coordinator and executor.
- [x] Add the app-level integration tests and validate the API contract.
- [x] Run the full repository quality gate and fix any issues introduced by the change.
- [ ] STOP for user review. Suggest three Conventional Commit messages.

### Verification

- `./mvnw verify`
- `./mvnw verify -Pnetwork` only after the normal quality gate passes for live smoke coverage

# Next step

Phase 3 is implemented and awaiting review: `GET /feed/home` (`FeedHomeGetController`), the `FrontPageSearcher` bean with the coordinator as its `BackgroundRefresh`, the acceptance tests and the OpenAPI contract. The full quality gate passes. `./mvnw verify -Pnetwork` was not run (live sites); run it manually if wanted. After review and commit, S03 is complete: set the spec status accordingly.

Decisions: acceptance tests turn the cooldown off (`daily-trends.scraping.cooldown=0s`) and use `ScrapeRunCoordinator.runNow()` as an exact barrier for a background run, because a refresh holds the guard from the moment it is accepted; fake scrapers are beans so tests count their calls.

Implementation metadata: Phases 1–3 implemented 2026-10-15 by Claude Sonnet 5.5.
