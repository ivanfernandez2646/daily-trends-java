# daily-trends-java

Java port of the scraping and front page of
[daily-trends](https://github.com/ivanfernandez2646/daily-trends): a small news API that scrapes the
front pages of El Mundo and El Español and serves the newest headlines. It rehearses a Node → Java
migration with the same hexagonal architecture, and applies from the start the improvements already
made in the Python port (`daily-trends-py`).

**Status:** specs ready, implementation not started. Work follows the
[spec-driven workflow](#spec-driven-workflow).

## Stack

- Java 25, Spring Boot 4.1 (Web MVC, virtual threads), Maven wrapper
- MongoDB with Spring Data MongoDB (`MongoTemplate`)
- Jsoup (`Jsoup.connect`) for scraping
- springdoc-openapi (OpenAPI document and Swagger UI)
- JUnit Jupiter, AssertJ, Testcontainers 2, ArchUnit, Spotless
- Virtual threads with an explicit executor and `CompletableFuture`
- Hexagonal architecture with a single bounded context (`cms`, subdomain `feeds`)

Why Java 25 and Spring Boot 4: Java 25 is the current LTS and its virtual threads no longer pin the
carrier thread inside `synchronized` (JEP 491). Spring Boot 3.5 left open-source support in
June 2026, so a migration starting now targets the 4.x line.

## API (planned)

| Method and path | Description | Spec |
|---|---|---|
| `GET /status` | Health check | S01 |
| `GET /v3/api-docs`, `GET /swagger-ui.html` | OpenAPI document and Swagger UI, generated from the code | S01 |
| `GET /feed/scrap` | Scrape El Mundo and El Español, store new headlines, return the ones saved | S02 |
| `GET /feed/home` | 10 newest scraped feeds; a stale page triggers a background refresh | S03 |

## Getting started

Requirements: JDK 25 (for example `sdk install java 25-tem`), Docker (MongoDB and Testcontainers).

```bash
git config core.hooksPath .githooks   # once per clone: run ./mvnw verify before every push
docker compose up -d --wait   # MongoDB on localhost:27017
./mvnw verify                 # quality gate (available once S01 is implemented)
./mvnw spring-boot:run        # API on http://localhost:5000
```

Integration tests (`*IT`) start MongoDB with Testcontainers and need Docker. With OrbStack, which
does not create `/var/run/docker.sock` by default, point Testcontainers at its socket once in
`~/.testcontainers.properties`:

```properties
docker.host=unix:///Users/<you>/.orbstack/run/docker.sock
```

### Configuration

Standard Spring Boot profiles and properties. The active profile is `dev` unless
`SPRING_PROFILES_ACTIVE` says `test` or `production`.

| Property | Environment variable | Default |
|---|---|---|
| `spring.mongodb.uri` | `SPRING_MONGODB_URI` | `dev`: `mongodb://localhost:27017/daily-trends-dev`; `test`: `mongodb://localhost:27017/daily-trends-test`; `production`: required |
| `server.port` | `SERVER_PORT` | `5000` |
| `daily-trends.timezone` | `DAILY_TRENDS_TIMEZONE` | `Europe/Madrid` (business day) |
| `daily-trends.scraping.timeout` | `DAILY_TRENDS_SCRAPING_TIMEOUT` | `10s` (each scraping request) |
| `daily-trends.scraping.cooldown` | `DAILY_TRENDS_SCRAPING_COOLDOWN` | `300s` (between background refreshes; `0s` disables it) |

Local overrides go in `config/application.properties` (git-ignored, loaded by Spring Boot from the
working directory; tests never read it). For example, on macOS port 5000 is used by AirPlay
Receiver:

```properties
# config/application.properties
server.port=5123
```

## Project layout

```text
specs/
  feeds/            functional specs (S00 to S03), read in order
  improvements/     changes to implemented specs; candidates listed in its README
.agents/
  skills/           agent skills (ai-project-*)
  plans/            approved implementation plans
AGENTS.md           agent guide (single source of truth for rules and stack)
AGENT-DOCS.md       how agent files are organized
reference/          read-only copy of the Node project (git-ignored)
```

Once S01 is implemented:

```text
src/main/java/com/ivanfernandez/dailytrends/
  apps/cmsbackend/          entry point, composition root, controllers, error handling
  contexts/cms/
    feeds/{domain,application,infrastructure}
    shared/{domain,infrastructure}
src/test/java/...           mirrors src/main
```

## Spec-driven workflow

1. Pick the next spec in `specs/feeds/` and agree a plan with `/ai-project-create-plan`.
2. Implement one phase at a time with `/ai-project-implement-phase` (TDD, then review).
3. Commit with `/ai-project-conventional-commit`.

See [`AGENTS.md`](AGENTS.md) for the rules agents follow.
