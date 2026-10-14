# S01 · Foundation

**Status:** draft · **Depends on:** nothing.

## Goal
A running app that starts, reads configuration, connects to Mongo, answers `GET /status`, formats
errors, and contains the feed domain, search criteria and clock that S02 and S03 use.

## Behavior
- `GET /status` → 200, empty body.
- Default port `5000` (`PORT`).
- Open CORS (any origin). Response compression.
- Startup error (invalid configuration, MongoDB unreachable when the app starts) → logged, process
  exits with code 1.
- Route order: the fixed routes `/feed/scrap` and `/feed/home` must never be read as an id. No
  `/feed/{id}` route exists yet, but S01 adds a test so a later one cannot shadow them.

## Configuration
Read, from highest to lowest priority, from:
1. process environment variables;
2. a `.env` file in the working directory, if it exists (a missing file is not an error);
3. defaults for the current `ENV`;
4. global defaults.

- `ENV` itself can come from the environment or from `.env`.
- The test suite never reads `.env`: tests set their settings explicitly, so a developer's `.env`
  cannot point the tests at another database.
- `.env.example` lists every variable with its default (exists in the scaffold). `.env` is
  git-ignored.
- Implementation hint: `spring.config.import=optional:file:.env[.properties]` loads `.env` as a
  properties file; operating-system environment variables still take precedence over it.

| Variable | Type | Default | Notes |
|---|---|---|---|
| `ENV` | `production` \| `dev` \| `test` | `dev` | Selects the default `MONGO_URL` |
| `MONGO_URL` | string | `dev`: `mongodb://localhost:27017/daily-trends-dev`; `test`: `mongodb://localhost:27017/daily-trends-test`; `production`: **required** | |
| `PORT` | integer | `5000` | |
| `TIMEZONE` | IANA zone id | `Europe/Madrid` | Business timezone |
| `SCRAP_TIMEOUT_SECONDS` | number > 0 | `10` | Timeout of each scraping request (S02) |
| `SCRAP_COOLDOWN_SECONDS` | number ≥ 0 | `300` | Minimum time between background runs (S03); `0` disables it |

- Invalid configuration fails at startup with exit code 1: unknown `ENV`, non-integer `PORT`, unknown
  `TIMEZONE`, a non-positive timeout, a negative cooldown, or `ENV=production` without `MONGO_URL`.
- The error names the variable and never echoes the value of `MONGO_URL` (it can contain
  credentials).
- Settings are one immutable, validated object built in `apps/`. Domain and application code receive
  the values they need through constructors and never read settings.
- The Python names are kept on purpose (`SCRAP_*`), so both ports run with the same environment.

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
- Settings tests: each precedence level, each default per `ENV`, each invalid value above, the
  production error does not contain the value of `MONGO_URL`. The quality gate passes with a `.env`
  present that points `MONGO_URL` elsewhere.
- A startup test: invalid settings make the app fail to start.
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
- ArchUnit test: `domain` and `application` do not depend on the libraries listed in `AGENTS.md`.
- `./mvnw verify` green.
