# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A minimal Atoti (ActiveViam's in-memory OLAP/analytics engine) application packaged as a Spring Boot app. Atoti loads CSV trade data into an in-memory datastore, exposes it as an OLAP cube queryable via MDX/REST/XMLA, and serves a bundled Atoti UI. This project is meant to be forked and customized as a starting point for real Atoti Java projects.

## Build & run

- Requires Java 21, Maven 3, and access to ActiveViam's commercial Atoti jars (via Maven repo credentials) plus a valid Atoti license to actually run the app.
- Build: `mvn clean install`
- Build skipping tests (useful if license/test infra isn't available): `mvn clean install -DskipTests`
- Run the fat jar: `java -jar ./target/atoti-spring-boot-template.jar`
  - On macOS add `-Dactiveviam.chunkAllocatorKey=mmap`
  - If startup fails, add `--add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED`
- Run from IntelliJ using the provided run configs (`.idea/runConfigurations`): `AtotiSpringBootApplication (no OTEL)` uses `application-local.yml` and is the simplest way to run locally; the `OTEL` variants require the OpenTelemetry stack described in `otel/doc/STACK.md`.
- Server runs on port 9090. Once up:
  - Atoti UI: http://localhost:9090/ui
  - AdminUI: http://localhost:9090/admin/ui
  - Swagger UI (REST endpoints): http://localhost:9090/swagger-ui/index.html
  - XMLA endpoint (e.g. for Excel): http://localhost:9090/xmla
  - Default credentials: `admin:admin`

## Tests

- Run all tests: `mvn test`
- Run a single test class: `mvn test -Dtest=MeasuresTest`
- Two distinct test styles are used, both under `src/test/java`:
  - **Cube-only tests** (e.g. `MeasuresTest`) spin up just the datastore + pivot manager via `CubeTestConfig` (no full Spring Boot context, no HTTP) using `@SpringJUnitConfig({CubeTestConfig.class})` and ActiveViam's `CubeTester` to run MDX/API queries against manually inserted data.
  - **Full application tests** (e.g. `AtotiSpringBootApplicationTest`) boot the whole app with `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` and hit real HTTP endpoints with `TestRestTemplate`.
- `.http` files under `src/test/resources` (`core-rest.http`, `custom-rest.http`) contain example REST requests against the core Atoti API and the custom endpoints — useful for manual exploration, not run by Maven.

## Code style

- Formatting is enforced by Spotless (`palantir-java-format`) bound to the `check` goal, so `mvn clean install` fails on style violations. Fix with `mvn spotless:apply` (covers Java, POM, Markdown, and YAML).
- Spotless is ratcheted from `origin/6.1` — only files changed relative to that branch are checked, so untouched legacy files won't fail the build.
- Every Java file carries an ActiveViam copyright header sourced from `.idea/copyright/spotless.license`; Spotless inserts/validates it automatically.
- Lombok is configured (`lombok.config`) to copy `@Autowired`/`@Qualifier`/`@Value` from fields to generated constructors — most config classes use `@RequiredArgsConstructor` with `final` fields for constructor injection rather than field injection.

## Architecture

The app wires together the Atoti stack purely through Spring `@Configuration` classes under `com.activeviam.apps.cfg`, composed via constructor-injected beans. The dependency chain from data model to running cube is:

1. **Datastore schema** (`cfg/datastore/datamodel/StoresConfiguration`) — declares stores (tables) and references (joins) using `IStoreDescription`/`IReferenceDescription` beans. Field/store name string constants live centrally in `constants/StoreAndFieldConstants`. Currently: `Trades` (key: AsOfDate+TradeID) referencing `TradeAttributes` (key: AsOfDate+TradeID).
2. **`DatastoreSchemaConfig`** collects all `IStoreDescription`/`IReferenceDescription` beans (Spring injects `List<? extends ...>` automatically) into one `IDatastoreSchemaDescription`.
3. **`DatastoreSelectionConfig`** builds an `ISelectionDescription` — the flattened view (base store + reachable fields via references) that the cube reads from.
4. **Cube definition**: `cfg/pivot/Measures` and `cfg/pivot/Dimensions` are plain `@Component`s (not `@Configuration`) that build measures (via the `Copper` DSL) and dimensions/hierarchies respectively; `CubeConfig` wires them into an `IActivePivotInstanceDescription` (aggregate provider, shared context like query timeout, drillthrough limits).
5. **`ActivePivotManagerConfig`** combines the selection + cube description(s) into the top-level `IActivePivotManagerDescription` (catalog/schema/manager names as public constants, also referenced from tests and REST controllers).
6. **`ActivePivotWithDatastoreConfig`** and **`DatastoreConfig`** assemble the datastore description + manager description + epoch policy into the running `ApplicationWithDatastore`/`IDatastore`, implementing ActiveViam's `IDatastoreConfig`/`IActivePivotManagerDescriptionConfig` SPI interfaces so the Atoti Spring Boot starters pick them up.
7. **Data loading**: `cfg/source/CsvSourceConfig` declares `CsvTopicDescription` beans mapping file-glob patterns (in `src/main/resources/data/`) to store names; `InitialCsvLoad` listens for `ApplicationReadyEvent` and triggers a one-off load of both topics via `DataLoadControllerService`.

When extending the data model, add stores/measures/dimensions by following this same chain — a new store needs a description bean, likely a reference if it joins existing stores, a CSV topic if loaded from file, and measure/dimension wiring if it should be queryable.

### Security

- `GlobalSecurityConfig` combines two `UserDetailsService`s — an in-memory one (`security.authentication.in-memory.users` in `application.yml`, bcrypt passwords) and a "technical user" one for machine-to-machine (`security.tech-user.passwords`) — into a `CompositeUserDetailsService`.
- `CustomWebSecurityFiltersConfig` defines ordered `SecurityFilterChain`s per concern: H2 console (dev only, order 4), Swagger UI (admin-only, order 5), and custom REST endpoints under `EndpointConstants.CUSTOM_REST_PATH` (any authenticated user, order 6), using ActiveViam's `HumanToMachineSecurityDsl`/`MachineToMachineSecurityDsl` helpers. Lower orders (built into the Atoti starters) handle the core Atoti REST/MDX/UI endpoints.
- JWT keys and the login/logout flow are configured in `application.yml`; regenerate the key pair for real deployments via the `JwtUtil` class mentioned in the README.

### Adding a REST endpoint

Custom endpoints live under `com.activeviam.apps.rest`, mounted under `EndpointConstants.CUSTOM_REST_PATH` and protected by the `customRestEndpointsSecurityFilterChain`. See `HelloController` (trivial) and `DayController` (queries the cube via `IQueriesService.retrieveMembers`) as templates.

## OpenTelemetry

OTEL is enabled by default in `application.yml` (traces/metrics/logs exported) but disabled in `application-local.yml` for local dev without the stack running. See `otel/doc/STACK.md` for standing up the collector stack when using the OTEL run configuration.
