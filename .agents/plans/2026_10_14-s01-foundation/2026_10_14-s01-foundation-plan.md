---
name: 's01-foundation'
description: 'Running Spring Boot app with validated configuration, error format, OpenAPI, GET /status, feed domain, criteria, clock and MongoDB repository'
spec: 'specs/feeds/S01-foundation.md'
branch: 'feat/s01-foundation'
created_at: '2026-10-14T12:29:53Z'
created_by:
  tool: 'Claude Code'
  model: 'claude-opus-5-5'
updated_at: '2026-10-14T13:03:24Z'
implemented_phases:
  - phase: 1
    implemented_at: '2026-10-14T14:10:00Z'
    tool: 'Claude Code'
    model: 'claude-opus-5-5'
  - phase: 2
    implemented_at: '2026-10-14T12:54:14Z'
    tool: 'Claude Code'
    model: 'claude-opus-5-5'
  - phase: 3
    implemented_at: '2026-10-14T12:58:38Z'
    tool: 'Claude Code'
    model: 'claude-opus-5-5'
  - phase: 4
    implemented_at: '2026-10-14T13:03:24Z'
    tool: 'Claude Code'
    model: 'claude-opus-5-5'
---

# S01 · Foundation plan

## Goal

Deliver the foundation of `specs/feeds/S01-foundation.md`. The app starts, reads and validates its
configuration (Spring profiles and properties) and connects to MongoDB. It answers `GET /status` and formats every error as
`{"error": "..."}`, and it publishes a generated OpenAPI document. It also contains the feed domain, `Criteria`, `Clock` and the MongoDB
`FeedRepository` that S02 and S03 use. The repository stays green after every phase.

## Context

- The repository has no Java code yet: only specs, skills, `docker-compose.yml` and the read-only
  Node reference.
- Versions, the latest when this plan was made:
  - Spring Boot **4.1.1**, which manages JUnit Jupiter 6.0.3, Jackson 3.1.5, Testcontainers 2.0.5,
    MongoDB driver 5.8.1 and JSpecify 1.0.1;
  - ArchUnit **1.5.1** (`archunit-junit6`, because Boot manages JUnit 6);
  - Spotless Maven plugin 3.10.4 with `palantir-java-format` 2.102.0;
  - Error Prone 2.50.0 and NullAway 0.14.2;
  - springdoc-openapi **3.1.1** (`springdoc-openapi-starter-webmvc-ui`, the 3.x line targets Spring
    Boot 4).
- Base package `com.ivanfernandez.dailytrends`, app `apps/cmsbackend`, contexts
  `contexts/cms/{feeds,shared}`.

### Decisions recorded in this plan (AGENTS.md leaves them to S01)

- **Compile-time null checking:** Error Prone with NullAway in the `maven-compiler-plugin`,
  `NullAway:AnnotatedPackages=com.ivanfernandez.dailytrends`. JSpecify `@Nullable` marks nullable
  fields and parameters. A violation fails compilation.
- **Coverage:** JaCoCo report bound to `verify` (`target/site/jacoco`), no thresholds.
- **Configuration binding and validation** (spec S01, Spring-idiomatic; no `.env`, no `ENV`):
  - Profiles `dev` (default through `spring.profiles.default=dev`), `test` and `production`.
    Defaults live in `application.properties` and `application-{profile}.properties`.
  - `daily-trends.*` binds to `@ConfigurationProperties("daily-trends") record DailyTrendsProperties`
    with a nested `Scraping` record. The compact constructors validate (no Bean Validation
    dependency), and Spring's binder turns a failure into a `BindException` that names the
    property.
  - `ConfigurationGuard`, an `EnvironmentPostProcessor` that runs after config data, checks two
    things Spring does not: exactly one of `dev`, `test`, `production` is active, and `production`
    defines `spring.mongodb.uri`. It throws `InvalidConfiguration(property, reason)`, which never
    contains a value.
  - A `FailureAnalyzer` prints the property and the reason. The uncaught startup exception ends the
    process with exit code 1.
- **Local overrides and test isolation** (agreed):
  - Developers override properties with environment variables or a git-ignored
    `config/application.properties` in the working directory, which Spring Boot loads by itself.
  - Every Spring test uses the meta-annotation `@IntegrationTest`, which wraps `@SpringBootTest`
    with `spring.config.location=classpath:/`, `@ActiveProfiles("test")` and
    `@Import(TestcontainersConfiguration.class)`. Tests therefore only read the classpath
    configuration.
  - An ArchUnit rule forbids a bare `@SpringBootTest` in test classes.
  - CI writes a decoy `config/application.properties` (`spring.profiles.active=production`,
    unreachable `spring.mongodb.uri`, `daily-trends.timezone=Pacific/Kiritimati`) before
    `./mvnw verify`. This proves the acceptance criterion.
- **MongoDB unreachable at startup:** a startup `ping` command in the composition root. The cluster
  server selection timeout is 5 s (`MongoClientSettingsBuilderCustomizer`), so the app fails fast
  with exit code 1.
- **HTTP tests:** `@IntegrationTest` with `MockMvcTester` (AssertJ). MongoDB comes from a
  Testcontainers `MongoDBContainer` `@Bean` with `@ServiceConnection` in a shared
  `@TestConfiguration` (`TestcontainersConfiguration`).
- **Docker image:** multi-stage `Dockerfile`. The build stage uses `eclipse-temurin:25-jdk` and runs
  `./mvnw -DskipTests package`. The runtime stage uses `eclipse-temurin:25-jre`, runs as a non-root
  user and uses Boot's layered jar extraction. `docker-compose.yml` gains an `api` service that
  depends on a healthy `mongo`.
- **CI:** GitHub Actions `.github/workflows/ci.yml` on push and pull request. Steps:
  1. `actions/setup-java` with Temurin 25 and the Maven cache;
  2. write the decoy `config/application.properties`;
  3. `./mvnw verify`;
  4. `docker build .` (added in Phase 6).

### Notes agreed during planning

- **`FeedUpdatedAt` has no failing value in Java.** Its type is `@Nullable Instant`, so every value
  is valid and the spec's `<FeedUpdatedAt> does not allow the value <…>` message cannot be thrown.
  Node validated text dates; here a non-date stored value is caught by the Mongo adapter as
  `InvalidStoredFeed`, which answers 500. No `throw`, no spec change. `FeedCreatedAt` only rejects
  `null`.
- **Criteria fields are typed.** The shared kernel defines `Criteria<F>`, and feeds defines
  `enum FeedField` with domain names. `FeedField.fromValue(String)` throws `InvalidArgumentError`
  for an unknown name. That is the spec's "unknown field" case. A misspelled constant does not
  compile.
- **OpenAPI is generated, not hand-written** (spec S01). Each later plan (S02, S03) documents its
  routes in the same phase that adds them and extends `OpenApiDocumentTest`.
- **Compression is tested by configuration only in S01.** No S01 response exceeds Tomcat's 2 KB
  threshold. The S03 plan must add a real `Accept-Encoding: gzip` test on `GET /feed/home`.

## Phases

## Phase 1: Walking skeleton and quality gate

The Maven project and the build tooling. The app answers `GET /status`, with open CORS and
compression on. It connects to MongoDB through Testcontainers in tests. The ArchUnit rule and CI run
the quality gate from the first commit. Configuration validation arrives in Phase 2.

### Public contracts

- `GET /status` returns 200 with an empty body.
- CORS: any origin (`Access-Control-Allow-Origin: *`) on every route.
- `server.compression.enabled=true`.
- Spring Boot property `spring.threads.virtual.enabled=true`.
- Build commands from `AGENTS.md`: `./mvnw spotless:apply`, `./mvnw test`, `./mvnw verify`,
  `./mvnw verify -Pnetwork` (profile declared, no tests yet).
- Test suites:
  - `apps/cmsbackend/status/StatusGetControllerIT`;
  - `apps/cmsbackend/CorsAndCompressionIT`;
  - `ArchitectureTest`;
  - shared `TestcontainersConfiguration` and the `@IntegrationTest` meta-annotation.

### Tests first

- [x] `StatusGetControllerIT`: `GET /status` returns 200 and an empty body.
- [x] `CorsAndCompressionIT`:
  - a preflight `OPTIONS /status` with `Origin: https://example.com` returns
    `Access-Control-Allow-Origin: *`;
  - the bound `ServerProperties` has compression enabled.
- [x] `ArchitectureTest` (ArchUnit):
  - classes in `..contexts..domain..` and `..contexts..application..` do not depend on
    `org.springframework..`, `org.bson..`, `com.mongodb..`, `tools.jackson..`,
    `com.fasterxml.jackson..`, `org.jsoup..`, `java.net..`, `org.springdoc..` or
    `io.swagger..`;
  - no class outside `..apps..` is annotated with `@Service`, `@Component` or `@Repository`;
  - no test class is annotated with `@SpringBootTest` directly (only through `@IntegrationTest`).
- [x] `CmsBackendApplicationIT`: the context starts against the Testcontainers MongoDB.

### Implementation

- [x] Maven wrapper (`./mvnw`, `.mvn/wrapper`) and `pom.xml`:
  - parent `spring-boot-starter-parent` 4.1.1, Java 25;
  - dependencies: `spring-boot-starter-webmvc`, `spring-boot-starter-data-mongodb`, `jsoup`,
    `jspecify`;
  - test dependencies: the Spring Boot test starters, `testcontainers-mongodb`,
    `spring-boot-testcontainers` and `archunit-junit6`;
  - plugins: Spotless (`palantir-java-format`, `check` bound to `verify`), Error Prone with NullAway,
    JaCoCo (report on `verify`) and Failsafe for `*IT`;
  - profile `network` that includes tests tagged `network` (excluded by default).
- [x] `apps/cmsbackend/CmsBackendApplication` (`@SpringBootApplication`).
- [x] `apps/cmsbackend/status/StatusGetController`.
- [x] `apps/cmsbackend/WebConfiguration`: CORS mapping.
- [x] `application.properties`: `spring.profiles.default=dev`, `server.port=5000`, compression,
  virtual threads.
- [x] `application-dev.properties` and `application-test.properties` with their
  `spring.mongodb.uri`; `application-production.properties` without one.
- [x] `TestcontainersConfiguration` (`MongoDBContainer` `@Bean` with `@ServiceConnection`, image
  `mongo:8`) and `@IntegrationTest`.
- [x] `.github/workflows/ci.yml`: Temurin 25, Maven cache, decoy `config/application.properties`,
  `./mvnw verify`.
- [x] Refactor without changing behavior.
- [x] Run `./mvnw spotless:apply` and `./mvnw verify`, and fix failures.
- [x] STOP for user review. Suggest three Conventional Commit messages.

### Implementation notes

- Tests that start Spring or Testcontainers are `*IT` (Failsafe), as `ai-project-unit-testing`
  requires; the plan's earlier `*Test` names for them are renamed. Later phases follow the same rule.
- Maven wrapper 3.3.4 (`only-script`) pinned to Maven 3.9.16. `.mvn/jvm.config` exports the
  `jdk.compiler` packages that Error Prone needs on JDK 25.
- NullAway runs as `ERROR` on `com.ivanfernandez.dailytrends`; checked with a probe that returns
  `null` from a non-null method (compilation fails). The `@SpringBootTest` ArchUnit rule was checked
  with a probe class too.
- The domain/application ArchUnit rule used `allowEmptyShould(true)` while those packages were
  empty; Phase 3 removed it.
- JaCoCo merges unit and integration data (`jacoco.exec`, `jacoco-it.exec`) into one report.
- `./mvnw verify` passes with a decoy `config/application.properties` (production profile,
  unreachable MongoDB) in the working directory.
- Local Docker: OrbStack without `/var/run/docker.sock` needs
  `DOCKER_HOST=unix://$HOME/.orbstack/run/docker.sock` for Testcontainers (or OrbStack's admin
  setting that creates the standard socket). CI runners have the standard socket.

## Phase 2: Configuration and startup failures

One immutable, validated `DailyTrendsProperties` record bound from Spring properties, with
per-profile defaults. Invalid configuration or an unreachable MongoDB make startup fail with exit
code 1. Tests never read the working-directory `config/` files.

### Public contracts

- Profiles `dev` (default), `test`, `production`.
- `apps/cmsbackend/config/DailyTrendsProperties`:
  `@ConfigurationProperties("daily-trends") record DailyTrendsProperties(ZoneId timezone, Scraping scraping)`
  and `record Scraping(Duration timeout, Duration cooldown)`.
  - The compact constructors validate: `timeout` > 0 and `cooldown` ≥ 0.
  - An unknown zone fails conversion. Both failures name the property.
- `apps/cmsbackend/config/InvalidConfiguration extends RuntimeException`:
  `InvalidConfiguration(String property, String reason)`. It never contains a value.
- `ConfigurationGuard` (`EnvironmentPostProcessor`, registered in `META-INF/spring.factories`):
  - exactly one known profile is active;
  - `production` requires `spring.mongodb.uri`.
- `InvalidConfigurationFailureAnalyzer`, registered in `META-INF/spring.factories`.
- Startup MongoDB `ping` with a 5 s server selection timeout.
- Defaults:

  | Property | Default |
  |---|---|
  | `spring.mongodb.uri` | `dev`: `mongodb://localhost:27017/daily-trends-dev`; `test`: `mongodb://localhost:27017/daily-trends-test`; `production`: required |
  | `server.port` | `5000` |
  | `daily-trends.timezone` | `Europe/Madrid` |
  | `daily-trends.scraping.timeout` | `10s` |
  | `daily-trends.scraping.cooldown` | `300s` |

### Tests first

- [x] `DailyTrendsPropertiesTest` (unit, Spring `Binder` over a `MapConfigurationPropertySource`):
  - the values bind;
  - `500ms` and `PT10S` are accepted;
  - `cooldown=0s` is valid;
  - `timeout` of `0s` and of `-1s`, `cooldown=-1s` and an unknown `timezone` each fail, naming the
    property.
- [x] `ConfigurationGuardTest` (unit, `MockEnvironment`):
  - `dev`, `test`, and `production` with a URI pass;
  - an unknown profile (`prod`) fails;
  - two known profiles fail;
  - `production` without `spring.mongodb.uri` fails, naming the property.
  - When `spring.mongodb.uri=mongodb://user:secret@host/db` is set and the profile is invalid, the
    message does not contain `secret`.
- [x] `ProfileDefaultsIT` (`SpringApplication` with a fake `systemEnvironment` source and
  `spring.config.location=classpath:/`, no web server, no MongoDB auto-configuration):
  - global defaults;
  - the URI default for `dev` (no profile given) and for `test`;
  - an environment variable (`DAILY_TRENDS_TIMEZONE`, `SPRING_MONGODB_URI`) beats the profile
    default.
- [x] `ApplicationStartupIT`:
  - `SpringApplication` run with `--spring.profiles.active=prod` fails with `InvalidConfiguration`
    naming `spring.profiles.active`;
  - run with `--server.port=abc` fails;
  - run with `spring.mongodb.uri=mongodb://localhost:1/x` fails on startup with a
    `MongoTimeoutException` (after the 5 s server selection timeout).
- [x] `ConfigIsolationIT` (`@IntegrationTest`): no property source in the environment comes from a
  `file:` location, and the active profile is `test`.

### Implementation

- [x] `DailyTrendsProperties` and `Scraping`, enabled from the composition root
  (`@EnableConfigurationProperties`).
- [x] `InvalidConfiguration`, `ConfigurationGuard`, `InvalidConfigurationFailureAnalyzer`.
- [x] `daily-trends.*` defaults in `application.properties`.
- [x] `apps/cmsbackend/config/MongoConfiguration`:
  - `MongoClientSettingsBuilderCustomizer` with the 5 s server selection timeout;
  - a startup `ping` bean that fails startup if MongoDB is unreachable.
- [x] Refactor without changing behavior.
- [x] Run `./mvnw verify` with and without a decoy `config/application.properties` in the working
  directory, and fix failures.
- [x] STOP for user review. Suggest three Conventional Commit messages.

### Implementation notes

- Spring-starting tests are `*IT`. The isolation test is `ConfigIsolationIT`, not `TestIsolationIT`:
  Surefire includes `Test*` classes by default, so that name ran it twice.
- Added `InvalidConfigurationFailureAnalyzerTest` (unit): the description names the property and the
  action names the environment variable.
- `DailyTrendsProperties` is enabled with `@EnableConfigurationProperties` on
  `CmsBackendApplication`. The records validate in their compact constructors and their messages
  name the full property (`daily-trends.scraping.timeout must be greater than zero`).
- `ConfigurationGuard` reads `getActiveProfiles()`, falling back to the default profiles
  (`spring.profiles.default=dev`). Its messages name the property and the allowed profiles, never a
  configured value.
- The Mongo startup check is a `SmartInitializingSingleton`, so it runs before the web server
  accepts requests. The 5 s server selection timeout also applies to every repository call.
- NullAway runs with `HandleTestAssertionLibraries=true`, so AssertJ `isNotNull()` counts as a null
  check in tests.
- Checked by hand with the packaged jar, each case exiting with code 1:
  - `--spring.profiles.active=prod` prints the failure analysis;
  - the `production` profile without `SPRING_MONGODB_URI`;
  - MongoDB unreachable after 5 s.
- `./mvnw verify` passes with and without the decoy `config/application.properties`.

## Phase 3: Error format

The shared error kinds and one `@RestControllerAdvice` that answers `{"error": "..."}` on every
route. A 500 is generic to the client and logged once at `ERROR` with the stack trace, method and
path. A guard test keeps `/feed/scrap` and `/feed/home` from being shadowed by a future
`/feed/{id}`.

### Public contracts

- In `contexts/cms/shared/domain`:
  - `class InvalidArgumentError extends RuntimeException`, concrete because value objects throw it
    directly;
  - `abstract class NotFoundError extends RuntimeException`;
  - `abstract class ConflictError extends RuntimeException`.

  Each has a `(String message)` constructor.
- `apps/cmsbackend/errors/ErrorResponse`: `record ErrorResponse(String error)`.
- `apps/cmsbackend/errors/ApiExceptionHandler` (`@RestControllerAdvice`):

  | Exception | Status | Body |
  |---|---|---|
  | `InvalidArgumentError` | 400 | the error's message |
  | `NotFoundError` | 404 | the error's message |
  | `ConflictError` | 409 | the error's message |
  | Spring `ErrorResponse` exceptions (`NoResourceFoundException` / `NoHandlerFoundException`, `HttpRequestMethodNotSupportedException`) | their status | short description, the status reason phrase (`Not Found`, `Method Not Allowed`) |
  | Any other `Throwable` | 500 | `Internal server error` |

- `spring.web.resources.add-mappings=false`, so an unknown route is a routing 404 and not a
  static-resource lookup.

### Tests first

- [x] `ErrorHandlingIT`, using a test-only controller imported through `@TestConfiguration` that
  throws each kind. Each case checks the status and the exact body:
  - an `InvalidArgumentError` subclass returns 400 with its message;
  - a `NotFoundError` subclass returns 404 with its message;
  - a `ConflictError` subclass returns 409 with its message;
  - an `IllegalStateException("db password=x")` returns 500 with `Internal server error`, and the
    body does not contain `password`;
  - the 500 is logged once at `ERROR` with the method, the path and the stack trace (captured with
    `OutputCaptureExtension`).
- [x] `ErrorHandlingIT`, framework errors:
  - `GET /nope` returns 404 with `{"error": "Not Found"}`;
  - `POST /status` returns 405 with `{"error": "Method Not Allowed"}`.
- [x] `FixedFeedRoutesIT`: for `GET /feed/scrap` and `GET /feed/home`, every handler mapping in
  `RequestMappingHandlerMapping` that matches the path has a literal pattern (no `{…}`). It passes
  trivially today and guards S02, S03 and a later `/feed/{id}`.

### Implementation

- [x] Error kinds in `contexts/cms/shared/domain`.
- [x] `ErrorResponse` and `ApiExceptionHandler`. Log the 500 with SLF4J at `ERROR`, with the method,
  the path and the exception.
- [x] `spring.web.resources.add-mappings=false` in `application.properties`.
- [x] Refactor without changing behavior.
- [x] Run `./mvnw verify` and fix failures.
- [x] STOP for user review. Suggest three Conventional Commit messages.

### Implementation notes

- Spring-starting tests are `*IT`: `ErrorHandlingIT` and `FixedFeedRoutesIT`. The probe controller
  and the probe error subclasses are nested in `ErrorHandlingIT` and imported with `@Import`.
- One `@ExceptionHandler(Exception.class)` handles both framework errors and unexpected ones:
  - any exception implementing Spring's `org.springframework.web.ErrorResponse` keeps its status and
    headers (for example `Allow` on a 405), and its body is the reason phrase;
  - everything else is logged once at `ERROR` (`Unhandled error on GET /path`, with the stack trace)
    and answers the generic 500.
- NullAway treats `Throwable.getMessage()` as nullable, so the handler falls back to `""`. The error
  kinds always receive a message.
- `FixedFeedRoutesIT` was checked with a temporary `/feed/{id}` controller: it fails, naming the
  pattern.
- The domain/application ArchUnit rule no longer needs `allowEmptyShould`, because
  `contexts/cms/shared/domain` now has classes.

## Phase 4: Feed domain, events, clock and JSON

The framework-free feed domain: value objects, the `Feed` aggregate, domain events and the
`EventBus`, plus the `Clock` port and its system adapter. The `FeedResponse` DTO fixes the JSON
shape for S02 and S03.

### Public contracts

- `contexts/cms/shared/domain`:
  - `interface Clock { Instant now(); ZoneId zone(); }`.
  - `abstract class AggregateRoot`: `protected void record(DomainEvent event)`,
    `public List<DomainEvent> pullDomainEvents()` (returns a copy and clears the list).
  - `abstract class DomainEvent`:
    - constructor `(String eventName, String aggregateId, Instant occurredOn)`, with
      `eventId = UUID.randomUUID()`;
    - getters `eventId()`, `eventName()`, `aggregateId()`, `occurredOn()`;
    - `abstract Map<String, Object> attributes()`.
  - `interface DomainEventSubscriber<E extends DomainEvent> { Class<E> subscribedTo(); void on(E event); }`.
  - `interface EventBus { void publish(List<DomainEvent> events); }`.
- `contexts/cms/shared/infrastructure`:
  - `SystemClock(ZoneId zone)`, the only class that calls `Instant.now()`;
  - `InMemoryEventBus(List<DomainEventSubscriber<?>> subscribers)`, which dispatches synchronously
    to the subscribers of each event's class.
- `contexts/cms/feeds/domain`:
  - `record FeedId(String value)`: a UUID v1–v5 (case-insensitive) or the nil UUID. Otherwise
    `<FeedId> does not allow the value <{value}>`.
  - `record FeedTitle(String value)` and `record FeedAuthor(String value)`: fail on `null`, `""` or
    blank with `<{ClassName}> is mandatory. Current value: <{value}>`. No trim.
  - `record FeedDescription(@Nullable String value)`: always valid.
  - `enum FeedSource { CMS, EL_PAIS, EL_MUNDO, EL_ESPANOL }`.
  - `record FeedCreatedAt(Instant value)`: truncated to milliseconds. `null` throws
    `<FeedCreatedAt> does not allow the value <null>`.
  - `record FeedUpdatedAt(@Nullable Instant value)`: truncated to milliseconds, always valid (see
    the planning notes). Factory `FeedUpdatedAt.none()`.
  - `record FeedPrimitives(String id, String title, @Nullable String description, String author, FeedSource source, Instant createdAt, @Nullable Instant updatedAt)`.
  - `final class Feed extends AggregateRoot`, with equality by `id`:
    - `static Feed create(FeedId id, FeedTitle title, FeedDescription description, FeedAuthor author, FeedSource source, Instant now)`;
    - `static Feed fromPrimitives(FeedPrimitives primitives)`;
    - `FeedPrimitives toPrimitives()`;
    - getters for each value object.
  - `final class FeedCreatedDomainEvent extends DomainEvent`: `EVENT_NAME = "feed.created"`,
    attribute `title`.
- `apps/cmsbackend/feeds/FeedResponse`:
  `record FeedResponse(String id, String title, @Nullable String description, String author, String source, @JsonProperty("created_at") Instant createdAt, @JsonProperty("updated_at") @Nullable Instant updatedAt)`,
  with `static FeedResponse from(FeedPrimitives)`. The key order is the component order, and `null`
  is always written.
- Test fixtures:
  - Object Mothers `FeedMother`, `FeedIdMother`, `FeedTitleMother`, …;
  - `FixedClock implements Clock`.

### Tests first

- [x] `FeedIdTest`:
  - v1, v4, v5, upper case and nil are accepted;
  - v7, a non-UUID, `""` and `null` are rejected with the exact message (`<null>` for `null`);
  - equality by value.
- [x] `FeedTitleTest` and `FeedAuthorTest`:
  - `null`, `""` and `"   "` are rejected with the exact message;
  - `" a "` is kept untrimmed;
  - equality.
- [x] `FeedDescriptionTest`: `null` and `""` are accepted; equality.
- [x] `FeedCreatedAtTest`:
  - a value with nanoseconds is truncated to milliseconds;
  - `null` is rejected with `<FeedCreatedAt> does not allow the value <null>`;
  - equality.
- [x] `FeedUpdatedAtTest`: `null` is accepted; truncation; equality.
- [x] `FeedTest`:
  - `create` sets `createdAt = now` (truncated) and `updatedAt = null`, and records one
    `feed.created` event with `aggregateId`, `title` and `occurredOn = now`;
  - `pullDomainEvents` returns the events, and a second pull is empty;
  - `fromPrimitives` records no events and round-trips with `toPrimitives`;
  - equality by `id` only.
- [x] `InMemoryEventBusTest`:
  - a subscriber of `FeedCreatedDomainEvent` receives the published event;
  - publishing with no subscribers is a no-op.
- [x] `SystemClockTest`: `zone()` returns the configured zone, and `now()` is between two
  `Instant.now()` readings.
- [x] `FeedResponseJsonIT` (the app's Jackson `JsonMapper`):
  - keys in order `id, title, description, author, source, created_at, updated_at`;
  - `2026-10-12T08:00:00Z` without a fraction and `2026-10-12T08:00:00.123Z` with milliseconds;
  - `description` and `updated_at` written as `null`.

### Implementation

- [x] Shared kernel types and adapters listed above.
- [x] Feed value objects, `FeedPrimitives`, `Feed`, `FeedCreatedDomainEvent`.
- [x] `FeedResponse`.
- [x] Composition root `apps/cmsbackend/config/SharedConfiguration`: `Clock` `@Bean`
  (`new SystemClock(properties.timezone())`) and `EventBus` `@Bean` (`new InMemoryEventBus(List.of())`).
- [x] Object Mothers and `FixedClock` in test sources.
- [x] Refactor without changing behavior.
- [x] Run `./mvnw verify` (including `ArchitectureTest`) and fix failures.
- [x] STOP for user review. Suggest three Conventional Commit messages.

### Implementation notes

- **Object Mothers:** `MotherCreator` (`contexts/cms/shared/domain` in test sources) uses only the
  JDK (`UUID`, `ThreadLocalRandom`, a small word list); no Datafaker dependency. The mothers are
  `FeedMother`, `FeedPrimitivesMother`, `FeedIdMother`, `FeedTitleMother`, `FeedAuthorMother` and
  `FeedDescriptionMother`:
  - `FeedMother.random()` is a stored feed with no events;
  - `FeedMother.created(now)` records `feed.created`.
- **`FixedClock`** lives in `contexts/cms/shared/fakes`. It has `at(String)` and
  `advance(Duration)`, and is synchronized for the S03 concurrency tests.
- **JSON test:** `FeedResponseJsonIT` replaces `FeedResponseJsonTest`. It uses the application's
  own `JsonMapper` and compares the exact string, which also checks key order. Jackson 3's defaults
  already give `…:00Z` / `…:00.123Z` and write `null`; no Jackson configuration was needed.
- **Tests that pass `null` to a non-null parameter** (`FeedId`, `FeedTitle`, `FeedAuthor`,
  `FeedCreatedAt`) carry `@SuppressWarnings("NullAway")`: rejecting that `null` is the behavior
  under test.
- **`DomainEvent.eventId()`** returns `java.util.UUID`, and `occurredOn` is the given `now`
  untruncated. `FeedCreatedAt` truncates to milliseconds.
- **Composition root:** `SharedConfiguration` provides `Clock` (`SystemClock` in the configured
  zone) and `EventBus` (`InMemoryEventBus` with no subscribers). `CmsBackendApplicationIT` checks
  both beans.

## Phase 5: Criteria and MongoDB persistence

The domain `Criteria`, the `FeedRepository` port, its MongoDB adapter with BSON dates and
`InvalidStoredFeed`, and an in-memory fake that behaves like MongoDB. One contract test runs against
both repositories.

### Public contracts

- `contexts/cms/shared/domain/criteria`:
  - `record Criteria<F>(List<Filter<F>> filters, Optional<Order<F>> order, OptionalInt limit)`:
    - factories `static <F> Criteria<F> all()` and
      `static <F> Criteria<F> withFilters(List<Filter<F>> filters)`;
    - methods `Criteria<F> orderBy(F field, Direction direction)` and `Criteria<F> limit(int limit)`;
    - a limit of 0 or less throws `InvalidArgumentError`;
    - filters are combined with OR, and no filters means every element.
  - `record Filter<F>(F field, Object value)`, `record Order<F>(F field, Direction direction)`,
    `enum Direction { ASC, DESC }`.
- `contexts/cms/feeds/domain`:
  - `enum FeedField`: `ID("id")`, `TITLE("title")`, `DESCRIPTION("description")`,
    `AUTHOR("author")`, `SOURCE("source")`, `CREATED_AT("created_at")`,
    `UPDATED_AT("updated_at")`. It has `String value()` and `static FeedField fromValue(String)`,
    which throws `InvalidArgumentError` for an unknown name.
  - `interface FeedRepository`:
    - `void save(Feed feed)`: upsert by id;
    - `List<Feed> search(Criteria<FeedField> criteria)`;
    - `boolean existsHeadline(FeedSource source, FeedTitle title, Instant from, Instant to)`: exact
      title, `createdAt` in `[from, to)`.
  - `final class InvalidStoredFeed extends RuntimeException`: `InvalidStoredFeed(String id, String reason)`.
    It is not an error kind, so it answers 500.
- `contexts/cms/feeds/infrastructure/persistence/mongo/MongoFeedRepository(MongoTemplate mongoTemplate)`:
  - collection `feeds`, `_id` is the UUID string;
  - fields `title`, `description`, `author`, `source`, `createdAt`, `updatedAt`;
  - dates are BSON dates, and `updatedAt: null` is stored as `null`;
  - `FeedField` maps to storage names, and `FeedSource` and `Instant` filter values map to their
    BSON form;
  - documents are mapped by hand, and any mapping failure throws `InvalidStoredFeed` with the id
    and the reason.
- Composition root `apps/cmsbackend/config/FeedsConfiguration`: `FeedRepository` `@Bean`
  (`new MongoFeedRepository(mongoTemplate)`).
- Test fixture `InMemoryFeedRepository implements FeedRepository`, which applies filters, order and
  limit.

### Tests first

- [ ] `CriteriaTest`:
  - `all()` has no filters, no order and no limit;
  - several filters are kept (OR);
  - order;
  - a missing limit is empty;
  - `limit(0)` and `limit(-1)` throw `InvalidArgumentError`.
- [ ] `FeedFieldTest`: `fromValue("created_at")` returns `CREATED_AT`; `fromValue("tittle")` throws
  `InvalidArgumentError`.
- [ ] `FeedRepositoryContractTest` (abstract), run by `InMemoryFeedRepositoryTest` and
  `MongoFeedRepositoryIT`:
  - save then search `all()` returns the feed with equal primitives;
  - save twice with the same id upserts;
  - an OR filter on `source` returns only matching feeds;
  - order by `created_at` `DESC` with limit 2;
  - `existsHeadline`: `createdAt == from` is true, `createdAt == to` is false, a millisecond before
    `from` is false, and a different source or title is false.
- [ ] `MongoFeedRepositoryIT` (MongoDB-specific, against the Testcontainers MongoDB):
  - `createdAt` and `updatedAt` are stored as BSON `Date`, and a `null` `updatedAt` is stored as
    `null` (raw document check);
  - dates that sort differently as text and chronologically come back in chronological order;
  - a raw document with a string `createdAt` makes `search` throw `InvalidStoredFeed` naming the id;
  - an unknown `source` and a missing `title` also throw `InvalidStoredFeed`.
- [ ] `ErrorHandlingTest`: add a case where an `InvalidStoredFeed` returns 500 with
  `Internal server error`.

### Implementation

- [ ] `Criteria`, `Filter`, `Order`, `Direction`, `FeedField`.
- [ ] `FeedRepository`, `InvalidStoredFeed`.
- [ ] `MongoFeedRepository` using `MongoTemplate` with `org.bson.Document`. No mapping annotations
  on domain classes.
- [ ] `FeedsConfiguration` `@Bean`.
- [ ] `InMemoryFeedRepository` in test sources.
- [ ] Refactor without changing behavior.
- [ ] Run `./mvnw verify` and fix failures.
- [ ] STOP for user review. Suggest three Conventional Commit messages.

## Phase 6: OpenAPI and delivery

The generated OpenAPI document and Swagger UI, a container image for the API, the API in Docker
Compose, the image build in CI, and updated docs.

### Public contracts

- `GET /v3/api-docs`: OpenAPI 3.1 JSON generated by springdoc-openapi 3.1.1.
  - Info title `daily-trends API`, version from the build (`${project.version}`).
  - Only the API routes are included: `/status` now, `/feed/scrap` and `/feed/home` in S02/S03.
- `GET /swagger-ui.html`: Swagger UI, which redirects to `/swagger-ui/index.html`. Public in every
  profile.
- `apps/cmsbackend/config/OpenApiConfiguration`: the `OpenAPI` `@Bean` with the info, and an
  `OpenApiCustomizer` that adds a `500` response with the `ErrorResponse` schema to every operation.
- `StatusGetController` annotated with `@Operation` and `@ApiResponse(responseCode = "200")`, empty
  body. `ErrorResponse` annotated with `@Schema(name = "ErrorResponse")`.
- `Dockerfile` (multi-stage):
  - the build stage uses `eclipse-temurin:25-jdk` and runs `./mvnw -DskipTests package`;
  - the runtime stage uses `eclipse-temurin:25-jre` with Boot's layered jar extraction, a non-root
    user, `EXPOSE 5000` and `SPRING_PROFILES_ACTIVE=production` by default.
- `.dockerignore` (`target/`, `config/`, `reference/`, `.git/`).
- `docker-compose.yml`: service `api`, built from the `Dockerfile`, `depends_on: mongo` with
  `condition: service_healthy`, `SPRING_MONGODB_URI=mongodb://mongo:27017/daily-trends-dev`,
  `SPRING_PROFILES_ACTIVE=production`, port `5000:5000`.
- CI: add a `docker build .` step after `./mvnw verify`.

### Tests first

- [ ] `OpenApiDocumentTest` (`@IntegrationTest`, `MockMvcTester`):
  - `GET /v3/api-docs` returns 200 JSON with `openapi` starting with `3.1` and `info.title`
    `daily-trends API`;
  - `paths./status.get.responses` has `200` and `500`, and the `500` refers to
    `#/components/schemas/ErrorResponse`, which has a required string property `error`;
  - `/v3/api-docs` and the Swagger UI paths are not in `paths`.
- [ ] `SwaggerUiTest`: `GET /swagger-ui/index.html` returns 200 (it must keep working with
  `spring.web.resources.add-mappings=false`).
- [ ] Manual check, recorded in the phase summary:
  - `docker compose up -d --wait --build`, then `curl -i localhost:5000/status` returns 200 and
    `curl -s localhost:5000/v3/api-docs` returns the document;
  - `docker run --rm <image>` (profile `production`) without `SPRING_MONGODB_URI` exits with code 1,
    and the log names `spring.mongodb.uri`.

### Implementation

- [ ] Dependency `springdoc-openapi-starter-webmvc-ui` 3.1.1. Resource filtering for the project
  version in `application.properties`, or `BuildProperties` from `spring-boot-maven-plugin`'s
  `build-info` goal.
- [ ] `OpenApiConfiguration`, annotations on `StatusGetController` and `ErrorResponse`.
  `springdoc.paths-to-match=/status,/feed/**`.
- [ ] `Dockerfile`, `.dockerignore`.
- [ ] `api` service in `docker-compose.yml`, and its header comment updated.
- [ ] CI `docker build` step.
- [ ] `README.md`:
  - status line and getting started now that `./mvnw verify` exists (the configuration table and
    the API docs routes are already there);
  - Docker usage;
  - `GET /status` marked as implemented.
- [ ] Refactor without changing behavior.
- [ ] Run `./mvnw verify` and the manual Docker checks, and fix failures.
- [ ] STOP for user review. Suggest three Conventional Commit messages.

## Next step

Run `/ai-project-implement-phase .agents/plans/2026_10_14-s01-foundation/2026_10_14-s01-foundation-plan.md`
to implement Phase 5 (criteria and MongoDB persistence). Phase 4 is done and awaits review.
