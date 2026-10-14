# S00 · Vision and glossary

**Status:** draft · **General rule:** the port starts from the Node behavior (`reference/daily-trends-node`)
and applies, from the start, the improvements already made in the Python port (`daily-trends-py`).
The Node defects are not reproduced. Every difference from Node is listed below.

## Goal
Re-implement the scraping and front page of daily-trends in Java (Spring Boot + MongoDB) as a
rehearsal of a Node → Java migration: the domain and its ports translate almost one to one; the
adapters and the composition root are new.

## What the system is
A news API ("feeds"). A feed is a headline with an author and an optional description. Sources:
`CMS` (created by hand; its routes are out of scope here), `EL_MUNDO` and `EL_ESPANOL` (scraped from
their front pages), and `EL_PAIS` (scraped by Node; no longer scraped, see S02).

## Bounded context
A single one, `cms/feeds`.

## How the specs are organized
Each spec is a **capability** and is self-contained: HTTP contract, domain rules, persistence needs,
edge cases and acceptance criteria. A rule is defined in the first spec that needs it and referenced
by the later ones.

| Spec | Capability | Depends on | Node origin |
|---|---|---|---|
| S01 | Foundation: app startup, configuration, Mongo, error format, OpenAPI, `GET /status`, feed domain, criteria, clock | — | `apps/cms/backend`, `contexts/cms/shared`, `feeds/domain` |
| S02 | Scrape external sources: `GET /feed/scrap` | S01 | `feeds/application/scrap`, `feeds/infrastructure/scrap`, `scrap-feed.feature` |
| S03 | Front page: `GET /feed/home` | S01, S02 | `FeedHomeController`, `feeds/application/search`, `home-feed.feature` |

Implementation order is the table order.

## Glossary
- **Feed:** aggregate root. Fields: `id`, `title`, `description` (nullable), `author`, `source`,
  `createdAt`, `updatedAt` (nullable).
- **Source:** `CMS` | `EL_PAIS` | `EL_MUNDO` | `EL_ESPANOL`.
- **Scraped source:** `EL_MUNDO` | `EL_ESPANOL`.
- **Scrape run:** one execution of every scraper plus the save of the feeds they produced.
- **Front page (home):** the 10 most recent feeds from scraped sources.
- **Business timezone:** `daily-trends.timezone` property, default `Europe/Madrid`.
- **Business day:** a calendar day in the business timezone, from local midnight to the next local
  midnight (23 or 25 hours on DST days).
- **Stale front page:** the newest front-page feed was created on a business day before today.

## Out of scope
- CMS routes: `PUT`, `GET`, `PATCH`, `DELETE /feed/{id}` and `GET /feed/list`. Tests seed feeds
  through the repository.
- Request body validation (no route in scope reads a body).
- Mongo migrations and indexes, authentication, pagination, scheduled jobs,
  several instances, production deployment.

## Differences from Node
Each row is intentional. The origin column names the Python port spec that introduced it.

| Node behavior | This port | Origin |
|---|---|---|
| Scrapes El País (now answers 403 to any non-browser client) | Scrapes El Español instead; `EL_PAIS` stays a valid source | Python S06 |
| `/feed/scrap` answers 200 with an empty body | 200 with the array of feeds saved by the run | I01 |
| Scraped description `""` when missing | `null` | I02 |
| Every run re-inserts the same headlines | Deduplicated by source + title + business day | I02, I04 |
| Loop that regenerates an id while it exists | Removed; the id is never looked up | I02 |
| El Mundo always decoded as iso-8859-1 and its last description character cut | Declared charset; only one trailing period removed | I02 |
| No timeout, no `User-Agent`, a meaningless `Content-Type` request header | Configurable timeout, `User-Agent`, no `Content-Type` | I02, I04 |
| `/feed/home` scrapes inside the request and retries on every request | Background refresh, single flight, cooldown | I03 |
| "Today" uses the server's `TZ` | Business timezone | I04 |
| `NODE_ENV` and `dev.json` / `test.json` never loaded | Spring profiles `dev`, `test`, `production` with per-profile defaults | I04 |
| Hand-written `docs/openapi.yml` served with Swagger UI at `/` | OpenAPI document generated from the code (springdoc) at `/v3/api-docs`, Swagger UI at `/swagger-ui.html` | this port |
| Settings from `ENV`, `MONGO_URL`, `PORT`, `SCRAP_*` and a `.env` file (also in the Python port) | Spring Boot properties (`spring.mongodb.uri`, `server.port`, `daily-trends.*`), overridden by environment variables or a git-ignored `config/application.properties`; no `.env` | this port |
| Criteria shaped like a Mongo query; dates stored and sorted as text | Domain `Criteria`; BSON dates, chronological order | I05 |
| A 500 shows the internal error message; invalid stored data can answer 400 | Generic 500 body; invalid stored data is always a 500 | I01, I08 |
| Date-time errors name `<Function>` | They name the value object | I01 |
| Dates in JSON always with milliseconds (`.000Z`) | Java's ISO format: no fraction when zero (`08:00:00Z`), milliseconds otherwise (`.123Z`). The Python port writes six digits (`.123000Z`); not followed, on purpose | this port |

## Source of truth
The specs in this folder. Node (`reference/daily-trends-node`) explains where a rule comes from:
selectors, the `article` walk, the `.feature` scenarios. If Node and a spec disagree and the row is
not in the table above, ask before implementing.
