# S01 · Foundation

**Status:** done · **Depends on:** nothing.

## Goal
A running app that starts, reads configuration, connects to Mongo, answers `GET /status`, formats
errors, publishes its OpenAPI document, and contains the feed domain, search criteria and clock that S02 and S03 use.

## Behavior
- `GET /status` → 200, empty body.
- Default port `5000` (`server.port`).
- Open CORS (any origin). Response compression.
- Startup error (invalid configuration, MongoDB unreachable when the app starts) → logged, process
  exits with code 1.
- Route order: the fixed routes `/feed/scrap` and `/feed/home` must never be read as an id. No
  `/feed/{id}` route exists yet, but S01 adds a test so a later one cannot shadow them.

## Configuration
Standard Spring Boot configuration: profiles and properties, no custom loader.

### Profiles
- Exactly one of `dev`, `test`, `production` is active. `dev` is the default
  (`spring.profiles.default=dev`). It is selected with `SPRING_PROFILES_ACTIVE` or
  `--spring.profiles.active`.
- `application.properties` holds the global defaults; `application-{profile}.properties` holds the
  defaults of that profile.

### Sources
Spring Boot's own precedence, from highest to lowest:
1. command-line arguments;
2. operating-system environment variables (relaxed binding: `DAILY_TRENDS_TIMEZONE` sets
   `daily-trends.timezone`);
3. a local, git-ignored `config/application.properties` (and `config/application-{profile}.properties`)
   in the working directory, if it exists. Spring Boot loads it by itself; it is the place for a
   developer's overrides;
4. `application-{profile}.properties` in the jar;
5. `application.properties` in the jar.

- The test suite never reads the working-directory `config/` files: tests run with
  `spring.config.location=classpath:/` and the `test` profile, so a developer's local overrides
  cannot point the tests at another database.
- `config/` is git-ignored. The README documents every property with its default.

### Properties
| Property | Environment variable | Type | Default | Notes |
|---|---|---|---|---|
| `spring.mongodb.uri` | `SPRING_MONGODB_URI` | MongoDB URI | `dev`: `mongodb://localhost:27017/daily-trends-dev`; `test`: `mongodb://localhost:27017/daily-trends-test`; `production`: **required** | |
| `server.port` | `SERVER_PORT` | integer | `5000` | |
| `daily-trends.timezone` | `DAILY_TRENDS_TIMEZONE` | IANA zone id | `Europe/Madrid` | Business timezone |
| `daily-trends.scraping.timeout` | `DAILY_TRENDS_SCRAPING_TIMEOUT` | `Duration` > 0 | `10s` | Timeout of each scraping request (S02) |
| `daily-trends.scraping.cooldown` | `DAILY_TRENDS_SCRAPING_COOLDOWN` | `Duration` ≥ 0 | `300s` | Minimum time between background runs (S03); `0s` disables it |

- Durations use Spring Boot's format (`10s`, `500ms`, `5m`, `PT10S`).
- Invalid configuration fails at startup with exit code 1: no active profile among `dev`, `test`,
  `production` (or more than one), non-integer `server.port`, unknown `daily-trends.timezone`, a
  non-positive timeout, a negative cooldown, or the `production` profile without
  `spring.mongodb.uri`.
- The error names the property and never echoes the value of `spring.mongodb.uri` (it can contain
  credentials).
- The `daily-trends.*` properties bind to one immutable, validated `@ConfigurationProperties` record
  in `apps/`. Domain and application code receive the values they need through constructors and
  never read configuration.
- The names follow Spring conventions on purpose; they do not match the Node or Python ports
  (`ENV`, `MONGO_URL`, `PORT`, `SCRAP_*`, `.env`), see S00.

## OpenAPI
- The OpenAPI 3.1 document is generated from the code with springdoc-openapi (the version that
  supports the project's Spring Boot line). No hand-written YAML.
- `GET /v3/api-docs` → the document as JSON. `GET /swagger-ui.html` → Swagger UI. Both are public in
  every profile, like the rest of the API.
- Document info: title `daily-trends API`, the project version.
- Every route in scope appears with its method, path, success response and body schema. S01
  documents `GET /status`; S02 and S03 document their routes when they add them.
- The error body `{"error": "<message>"}` is a named schema (`ErrorResponse`) and every operation
  documents its `500`. Framework routes (`/v3/api-docs`, Swagger UI) do not appear in the document.
- springdoc and Swagger annotations live only in `apps/`; `domain` and `application` never import
  them.

## Error format
Body `{"error": "<message>"}` for every error, on every route, mapped by error kind:

| Error | Status | Message |
|---|---|---|
| `InvalidArgumentError` and subclasses | 400 | the error's message |
| `NotFoundError` and subclasses | 404 | the error's message |
| `ConflictError` and subclasses | 409 | the error's message |
| Framework errors: unknown route, method not allowed | their status (404, 405) | a short description |
| Anything else, including `InvalidStoredFeed` and MongoDB errors | 500 | `Internal server error` |

- A 500 is logged once at `ERROR` with the stack trace, the HTTP method and the path. The client never
  sees the internal message.
- No route in scope produces a 4xx from domain errors today; the mapping exists for every route so
  S02, S03 and later specs do not add per-route lists.

## Clock
- Port `Clock` with `Instant now()` and `ZoneId zone()` (the business timezone).
- `SystemClock` adapter in infrastructure. Tests use a fixed clock.
- Nothing outside the `Clock` adapter reads the system time. This includes `Feed.create` and domain
  events: they receive `now` as an argument.

## Feed domain

### Value objects
| Type | Rule | Error (`InvalidArgumentError`) |
|---|---|---|
| `FeedId` | Required UUID, versions 1–5 or the nil UUID (a v7 UUID is invalid, as in Node's `uuid@9`) | `<FeedId> does not allow the value <{value}>` |
| `FeedTitle`, `FeedAuthor` | Required string: fails if null, empty or only whitespace | `<{ClassName}> is mandatory. Current value: <{value}>` |
| `FeedDescription` | Optional string: `null` or any string, including `""` | — |
| `FeedSource` | Enum `CMS`, `EL_PAIS`, `EL_MUNDO`, `EL_ESPANOL` | — |
| `FeedCreatedAt` | Required `Instant`, truncated to milliseconds | `<FeedCreatedAt> does not allow the value <{value}>` |
| `FeedUpdatedAt` | Optional `Instant`, truncated to milliseconds | `<FeedUpdatedAt> does not allow the value <{value}>` |

- A `null` value is rendered as `<null>` in messages.
- Equality by value. Value objects do not normalize strings (no trim). Callers trim before building
  them (S02).

### `Feed` aggregate
- Fields as in S00. `Feed.create(id, title, description, author, source, now)` sets `createdAt = now`,
  `updatedAt = null`, and records a `FeedCreatedDomainEvent`.
- `Feed.fromPrimitives(...)` rebuilds a stored feed without recording events.
- Equality by `id`.

### Domain events
- Base fields: `eventId` (UUID), `eventName`, `aggregateId`, `occurredOn` (from the `Clock`).
- The aggregate accumulates events; `pullDomainEvents()` returns them and clears the list.
- Only event: `feed.created`, attribute `title`.
- `EventBus` port with `publish(List<DomainEvent>)`. In-memory adapter: it dispatches to subscribers;
  there are none yet. Kept because later features will subscribe.

### Feed JSON
Every route that returns feeds uses this shape, keys in this order:

```json
{ "id": "uuid", "title": "string", "description": "string|null", "author": "string",
  "source": "CMS|EL_PAIS|EL_MUNDO|EL_ESPANOL", "created_at": "...", "updated_at": "...|null" }
```

- Dates in UTC with `Z`, ISO-8601 as Jackson writes an `Instant` by default: no fractional part
  when it is zero, milliseconds otherwise: `2026-10-12T08:00:00Z`, `2026-10-12T08:00:00.123Z`. No
  custom serializer (a deliberate difference from the Python port, S00).
- `null` fields are written as `null`, never omitted.
- The DTO lives in `apps/`; the domain exposes primitives, not JSON.

## Search criteria
- `Criteria` is a domain value object:
  - `filters`: zero or more *field equals value* conditions, combined with OR. No filters means every
    feed; it is the only way to say that.
  - `order`: optional field and direction (`ASC` | `DESC`).
  - `limit`: optional positive integer. Absent means no limit.
- Fields are domain names (`id`, `title`, `source`, `created_at`, …). Only the Mongo adapter knows
  storage names.
- Building criteria with a limit ≤ 0 or an unknown field throws `InvalidArgumentError`. Criteria are
  built by code, never from a request, so this is a programming error and answers 500 if it reaches
  a route.

## Persistence (MongoDB)
- Collection `feeds`. `_id` is the feed UUID as a string; no separate `id` field.
- Storage fields: `title`, `description`, `author`, `source`, `createdAt`, `updatedAt`.
- `createdAt` and `updatedAt` are BSON dates (UTC, millisecond precision). `updatedAt: null` is stored
  as `null`. Ordering by `createdAt` is chronological.
- `FeedRepository` port:
  - `save(Feed)`: upsert by `_id`.
  - `search(Criteria) → List<Feed>`.
  - `existsHeadline(FeedSource, FeedTitle, Instant from, Instant to) → boolean`: a feed with that source
    and exact title whose `createdAt` is in `[from, to)`. Used by S02.
- A stored document that cannot become a `Feed` (missing field, unknown `source`, `createdAt` that is
  not a BSON date, a value object rejecting it) → `InvalidStoredFeed` with the id and the reason. It
  is not an `InvalidArgumentError`: it is a server error (500).
- An in-memory fake repository in tests applies filters, order and limit, so use-case tests assert
  results instead of the criteria they passed.

## Delivery
- `docker-compose.yml` with MongoDB for local runs (exists in the scaffold).
- A Dockerfile and a CI workflow running the quality gate. The plan decides their shape.

## Acceptance criteria
- `GET /status` → 200, empty body.
- Configuration tests: each default per profile, an environment variable overriding a profile
  default, each invalid value above, the production error does not contain the value of
  `spring.mongodb.uri`. The quality gate passes with a local `config/application.properties` present
  that points `spring.mongodb.uri` elsewhere.
- A startup test: invalid configuration makes the app fail to start.
- Value-object tests: valid values, invalid values and exact messages, equality.
- Aggregate tests: `create` records `feed.created` with the given `now`; pulling returns and clears
  events; `fromPrimitives` records none.
- `Criteria` tests: no filters, OR filters, order, missing limit, invalid limit, unknown field.
- Feed JSON test: key names and order, a date with and without milliseconds, `null` description and
  `updated_at`.
- Error-mapping tests: each error kind → status and body; an unexpected exception → 500 with
  `Internal server error`; an unknown route → 404 JSON.
- Repository integration tests against MongoDB (Testcontainers): save and search with OR filter,
  order and limit; dates stored as BSON dates and sorted chronologically; `existsHeadline` at the
  edges of the range; a string `createdAt` raises `InvalidStoredFeed`.
- OpenAPI test: `GET /v3/api-docs` → 200 JSON with `openapi` 3.1.x, the info title, the path
  `/status` with its `200` response and the `ErrorResponse` schema; Swagger UI answers 200.
- ArchUnit test: `domain` and `application` do not depend on the libraries listed in `AGENTS.md`.
- `./mvnw verify` green.
