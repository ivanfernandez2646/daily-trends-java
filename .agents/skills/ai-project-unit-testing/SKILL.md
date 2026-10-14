---
name: ai-project-unit-testing
description: Write maintainable JUnit tests with explicit fake classes, Object Mothers, AssertJ and Given-When-Then structure.
disable-model-invocation: false
user-invocable: false
---

# Unit testing

Use automatically when writing unit tests.

## Test doubles

- Create a fake class that implements the real port (a Java `interface`), usually in memory.
- Store configured responses and call history explicitly (`List<...> saved()`, `int calls()`).
- Do not use Mockito, `@MockitoBean`, `@SpyBean` or spies.
- Keep shared fakes under `src/test/java/.../<subdomain>/fakes/` mirroring the port's package; a fake
  used by one test class can be a nested static class.
- Fakes used from several threads (S03) are thread-safe.

## Object Mothers

- Create `<Concept>Mother` classes for valid domain and DTO fixtures, next to the tests of that
  subdomain.
- Provide a valid default (`random()`) plus named variants (`fromSource(...)`, `createdAt(...)`).
- Prefer a small builder or overloads so tests state only relevant differences.
- Use one shared random-data helper (`MotherCreator`) for primitive defaults. The S01 plan picks the
  library (for example Datafaker).
- Never hide the behavior under test behind excessive fixture magic.

## Structure

Write tests in Given-When-Then order:

1. Given dependencies and input.
2. When invoking the public behavior.
3. Then assert result, state, and relevant calls.

- One behavior per test. Method names in snake case describe it:
  `returns_saved_feeds_when_one_scraper_fails`. Use `@DisplayName` only when the name cannot say it.
- AssertJ for assertions (`assertThat`, `assertThatThrownBy`). No JUnit `assertEquals`.
- `@ParameterizedTest` with `@MethodSource` or `@CsvSource` for input variations.
- Package-private test classes and methods.
- A fixed `Clock` in every test that depends on time. Never sleep to wait for time to pass: advance
  the fake clock.
- Concurrency tests use latches and timeouts (`CountDownLatch`, `assertTimeoutPreemptively`), never
  `Thread.sleep`.

## Kinds of tests

- **Unit** (`*Test`, Surefire): domain and application, no Spring context, no Docker.
- **Integration** (`*IT`, Failsafe): adapters against real infrastructure (MongoDB in Testcontainers,
  a local HTTP stub) and Spring Boot acceptance tests of the routes.
- **Network** (`@Tag("network")`): live smoke tests, only with `./mvnw verify -Pnetwork`.

Test use cases and application services; test domain objects separately only when they own
meaningful invariants.
