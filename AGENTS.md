# Agent guide

This repository is a Spec-First Java project. It ports the scraping and front page of a
TypeScript/Node project (`daily-trends`) and keeps its hexagonal architecture with bounded contexts.
The specs already include the improvements made in the Python port (`daily-trends-py`): this port
does not reproduce the Node defects.

## Required workflow

1. Write or update `specs/<subdomain>/<feature>.md` (bounded context `cms`, subdomain `feeds`:
   `specs/feeds/S0N-*.md`, read in order from `S00`). Changes to an implemented spec go in
   `specs/improvements/I0N-*.md`.
2. Run `/ai-project-create-plan` to agree public contracts and vertical phases.
3. After approving the plan, the agent creates a feature branch automatically.
4. Run `/ai-project-implement-phase`. It implements exactly one phase using TDD, then stops for review.
5. Run `/ai-project-conventional-commit` only when ready. Commits are never automatic.

`/ai-project-create-plan`, `/ai-project-implement-phase`, and `/ai-project-conventional-commit`
are user-invoked. Supporting skills under `.agents/skills/` are selected by the agent.

## Reference and scope

- The original Node project is a read-only reference in `reference/daily-trends-node/`
  (git-ignored). Its selectors and `.feature` files are the origin of the behavior.
- The specs in `specs/feeds/` are the source of truth. Where they differ from Node, the difference is
  intentional and listed in `S00`. Never "fix" a spec back to the Node behavior.
- Scope is S00–S03: scraping (`GET /feed/scrap`) and the front page (`GET /feed/home`). The CMS
  routes are out of scope. Do not implement anything without a spec.

## Stack

Fixed decisions. A change needs a spec or an approved plan that says why.

| Concern | Choice |
|---|---|
| Language | Java 25 (LTS) |
| Framework | Spring Boot 4.1.x (latest patch when the S01 plan is created), Spring Web MVC (`spring-boot-starter-webmvc`) |
| Concurrency | Virtual threads (`spring.threads.virtual.enabled=true`); no reactive stack. Background and parallel work run on an `ExecutorService` from `Executors.newVirtualThreadPerTaskExecutor()` owned by the composition root, composed with `CompletableFuture`. No `@Async`, no `StructuredTaskScope` (preview in Java 25) |
| Build | Maven, through the wrapper (`./mvnw`); single module |
| Persistence | MongoDB through Spring Data MongoDB `MongoTemplate` (no `MongoRepository`, no mapping annotations on domain classes), only in infrastructure adapters. Connection property `spring.mongodb.uri` |
| Scraping | Jsoup: `Jsoup.connect` for requests (timeout, `User-Agent`, redirects, charset) and its selectors for parsing |
| JSON | Jackson 3 as managed by Spring Boot (packages `tools.jackson.*`; annotations stay in `com.fasterxml.jackson.annotation`). `Instant` uses Jackson's default ISO-8601 format |
| Tests | JUnit Jupiter and AssertJ (from the Spring Boot test starters), Testcontainers 2 (`testcontainers-mongodb`) with `@ServiceConnection` on a container `@Bean` |
| Architecture tests | ArchUnit 1.5.x (`archunit-junit5` or `archunit-junit6`, matching the JUnit version Spring Boot manages) |
| Formatting | Spotless with `palantir-java-format` |
| Local services | Docker Compose (MongoDB) |
| Nullness | `Optional` for absent results; JSpecify `@Nullable` on nullable fields and parameters |

Decisions left to the plan of the spec that first needs them: compile-time null checking (Error
Prone with NullAway), coverage (JaCoCo), how settings bind and validate, the Docker image and CI
(S01); the local HTTP stub for request tests (S02). Record each decision in the plan.

## Architecture

Base package `com.ivanfernandez.dailytrends`.

- `apps/<app>`: entry point (`@SpringBootApplication`), the composition root (`@Configuration`
  classes that build use cases and adapters), controllers, response DTOs and the
  `@RestControllerAdvice`. Bounded contexts have no HTTP layer.
- `contexts/<context>/<subdomain>/application`: use-case orchestration.
- `contexts/<context>/<subdomain>/domain`: framework-free business types, rules and ports.
- `contexts/<context>/<subdomain>/infrastructure`: MongoDB, HTTP and HTML adapters.
- `contexts/<context>/shared`: shared kernel for that context (value object bases, `Criteria`,
  `Clock`, domain events, error kinds).

In this project: `apps/cmsbackend` and `contexts/cms/{feeds,shared}`.

- Application code depends on ports (Java `interface`s in `domain`). Infrastructure adapters contain
  only technology details.
- `domain` and `application` never import Spring, Spring Data, the MongoDB driver, Jackson, Jsoup or
  `java.net`. JSpecify annotations and `java.util.concurrent` are allowed. An ArchUnit test enforces it, and it is part of the quality gate.
- No Spring stereotype annotations (`@Service`, `@Component`, `@Repository`) outside `apps/`. Use
  cases and adapters are plain classes created with `@Bean` methods in the composition root.
- Every error goes through one `@RestControllerAdvice`: a new domain error picks its status by
  extending an error kind (`InvalidArgumentError`, `NotFoundError`, `ConflictError`). Every 500 is
  generic to the client and detailed only in the log.

## Quality rules

- Java 25 managed with SDKMAN or a JDK installed outside the project. Never commit a JDK.
- Always build through `./mvnw`, never a global `mvn`.
- Value objects are `record`s that validate in their compact constructor. Entities and aggregates are
  `final` classes with private fields, equality by id.
- No `null` returns from public methods: use `Optional` for absent results. A nullable field is
  documented in the spec (`description`, `updatedAt`).
- Dates are `Instant` in UTC in the domain. "Today" and "the same day" use the business `ZoneId` from
  the `Clock`. Never call `Instant.now()`, `LocalDate.now()` or `ZonedDateTime.now()` outside the
  `Clock` adapter.
- Prefer immutability: `final` fields, `List.copyOf`, no setters.
- No checked exceptions in `domain` or `application` ports; adapters translate them.
- English in code, docs, tests, errors, and commits.
- Test behavior first: red, green, refactor.
- Use fake classes that implement the port instead of Mockito, `@MockitoBean` or spies.
- Use Object Mothers for expressive domain fixtures.
- Tests never reach the real network, except the live smoke tests tagged `network`, which are not
  part of the quality gate.
- Read existing code before introducing a pattern. Prefer simple classes over speculative
  abstractions.
- Add comments only when they explain a non-obvious reason.
- Keep changes small, independently verifiable, and tied to an approved spec and plan phase.
- Never implement or commit on `main` or `master`.
- Never commit secrets or real credentials.

## Commands

These exist once S01 creates the Maven project.

```bash
./mvnw spotless:apply          # format
./mvnw test                    # unit tests
./mvnw verify                  # quality gate: format check, unit, integration and architecture tests
./mvnw verify -Pnetwork        # also the live scraper smoke tests
./mvnw spring-boot:run         # run the API on PORT (default 5000)
docker compose up -d --wait    # start MongoDB for local runs
```

Integration tests use Testcontainers and need Docker running; they do not use the Compose MongoDB.

Quality gate:

```bash
./mvnw verify
```

After each coherent slice, run the relevant tests. Before handing off a phase, run the full quality
gate and fix errors introduced by the change.
