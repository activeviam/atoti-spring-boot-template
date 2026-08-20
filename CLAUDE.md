# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A minimal Atoti (ActiveViam's in-memory OLAP/analytics engine) application packaged as a Spring Boot app, exposing an OLAP cube queryable via MDX/REST/XMLA and serving a bundled Atoti UI. This project is meant to be forked and customized as a starting point for real Atoti Java projects.

This branch (`phase-4-single-table`) is no longer the original single-node CSV template: it runs trade data live from Dremio via **DirectQuery** (no in-memory datastore, no CSV loading), split into distributed **data-node** and **query-node** Spring profiles for high availability, with REST endpoints to rehearse masking a data node's data out of query rotation. See "Distributed architecture" below.

## Build & run

- Requires Java 21, Maven 3, and access to ActiveViam's commercial Atoti jars (via Maven repo credentials) plus a valid Atoti license to actually run the app.
- Build: `mvn clean install`
- Build skipping tests (useful if license/test infra isn't available): `mvn clean install -DskipTests`
- Run the fat jar with an explicit role profile — `-Dspring.profiles.active=data-node` or `-Dspring.profiles.active=query-node` (see "Distributed architecture" below); a `data-node` instance additionally needs `DREMIO_HOST`/`DREMIO_PORT`/`DREMIO_USERNAME`/`DREMIO_PASSWORD` and a license with DirectQuery entitlement. Give each instance its own `SERVER_PORT` (and `NODE_NAME`, used for OTEL disambiguation) to run several at once:
  `java -jar ./target/atoti-spring-boot-template.jar` (add the flags below as needed)
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

The app wires together the Atoti stack purely through Spring `@Configuration` classes under `com.activeviam.apps.cfg`, composed via constructor-injected beans. There is no in-memory datastore and no CSV loading — the cube reads trade data live from Dremio via DirectQuery, and the app runs as two distinct Spring-profile roles (`data-node` and `query-node`) that together form one distributed cluster.

### Distributed architecture (DirectQuery over Dremio + data/query nodes)

- **`data-node` profile** (`cfg/directquery/*`, `@Profile("data-node")`): the only role that talks to Dremio.
  - `DremioProperties` / `DremioConnectorConfig` — connection settings and the `DirectQueryConnector<GenericJdbcDatabaseSettings>` (Dremio SQL dialect, Arrow Flight SQL JDBC URL).
  - `DremioSchemaConfig` — discovers the Dremio table(s) via `connector.getDiscoverer().discoverTable(...)` and builds a `SchemaDescription`. As of Phase 4, `Trades`/`TradeAttributes` have been **merged into a single Dremio table, `TradesMerged`** (`StoreAndFieldConstants.TRADES_STORE_NAME`) — there is no join at all, unlike the earlier two-table model kept on the `6.1` branch.
  - `DremioSelectionConfig` — `StartBuilding.selection(schema).fromBaseStore(TRADES_STORE_NAME).withAllReachableFields()`.
  - `DirectQueryApplicationConfig extends ADirectQueryApplicationConfig` — the DirectQuery SPI equivalent of `IDatastoreConfig`; its one abstract method builds the `Application` bean (connector + schema + manager description) that supplies both the `IActivePivotManager` and the live database connection.
  - `DataCubeConfig` — builds the cube's `IActivePivotInstanceDescription` (measures/dimensions via `cfg/pivot/Measures`/`Dimensions`, a `jit` aggregate provider plus a named `ByAsOfDate` bitmap partial provider scoped to the `AsOfDate` level so date-level queries are served from an in-memory snapshot instead of live Dremio SQL), then joins it to the cluster via `.asDataCube().withClusterDefinition()...withApplicationId(APPLICATION_ID)...withProperty(IDataClusterDefinition.DATA_NODE_PRIORITY, ...)`. Several instances of this same config can run at once (different `SERVER_PORT`/`NODE_NAME`), all reading the same Dremio data, for HA — they share the same node priority so the query node picks one at random per query.
  - `DataNodeManagerConfig implements IActivePivotManagerDescriptionConfig` — `withSchema(selectionDescription).withCube(activePivotInstanceDescription)`.
- **`query-node` profile** (`cfg/pivot/QueryCubeConfig`/`QueryNodeManagerConfig`, `@Profile("query-node")`): holds **no data of its own** — no Dremio connection, no schema/selection. `QueryCubeConfig` builds an `IDistributedActivePivotInstanceDescription` via `.asQueryCube().withClusterDefinition()...withApplication(APPLICATION_ID).withDistributingLevels(AsOfDate).withProperty(IQueryClusterDefinition.HORIZONTAL_DATA_DUPLICATION_PROPERTY, "true")` — `AsOfDate` is the distributing level, and horizontal duplication tells the query node to treat every data node as a full replica (random per-query dispatch) rather than a disjoint partition. `QueryNodeManagerConfig` wires it via `.withDistributedCube(...)` only.
- **Cluster wiring**: both roles join the same JGroups cluster (`constants/DistributionConstants`: `CLUSTER_ID`, `APPLICATION_ID`, `JGROUPS_PROTOCOL_PATH` — `JDBC_PING` discovery against the shared `cluster-db` Postgres instance) and both set `QueryMonitoring().enableExecutionPlanningPrint().enableExecutionTimingPrint()` as a shared context value, so every node prints its own execution plan + per-node timing to its log for every query.
- **Manager lifecycle**: `ApplicationManagerService` (unconditional `@Component`) listens for `ApplicationStartedEvent` and calls `.init(null)`/`.start()` on whichever `IActivePivotManager` bean is present — any new manager config should build-and-expose the manager unstarted and let this service start it (Atoti's own registry auto-configuration requires the manager to still be unstarted when the Spring context finishes refreshing).
- **Running nodes locally**: build the fat jar once, then launch as many JVMs as needed, each with its own `SERVER_PORT`/`NODE_NAME` and `-Dspring.profiles.active=data-node` or `query-node`; `data-node` instances additionally need `DREMIO_*` env vars and a license with DirectQuery entitlement. See `constants/DistributionConstants`/`StoreAndFieldConstants` for the exact identifiers in play.

### HA masking and the data-maintenance/rehearsal REST endpoints

Because several `data-node` instances replicate the same Dremio data, Atoti's **masking API** lets you take one data node out of query rotation *for specific `AsOfDate` values* without any downtime, so its underlying rows can be safely deleted/changed on Dremio and the node brought back once it's consistent again (`docs.activeviam.com/engine/java-sdk/6.1/distributed/remove_data_overlap`).

**Masking is scoped both per-node and per-member**, not a whole-node on/off switch: a mask call targets one specific data-node process (only that node's `IMultiVersionDataActivePivot` exposes `maskMembers`/`unmaskMembers` — the query node's cube has no such data-cube instance) *and* a specific set of level members (here, specific `AsOfDate` values via `LevelMembers`). Masking dq1 for one date leaves dq1 still serving every other date, and has zero effect on dq2. Once masked, the query node's routing table stops treating that node as a candidate for the masked date(s) and serves them exclusively from the other replica; unmasking reverses it. No data is deleted by masking itself — it is pure in-memory routing bookkeeping.

This isn't just the mechanism's intended design — it's been directly confirmed against a real rehearsal's raw query-node log: parsing every `Horizontal dispatching=` line for the masked node's own JGroups address across the full mask→unmask window showed it assigned at least one date in **2171/2171** dispatch snapshots, with the masked date appearing in its own dispatched-members list **0/2171** times — i.e. the masked node kept actively serving every other date throughout, not just theoretically remaining "up."

Three custom REST controllers under `com.activeviam.apps.rest` implement this rehearsal workflow, all mounted under `EndpointConstants.CUSTOM_REST_PATH` (`/custom/rest`) and all `ROLE_ADMIN`-protected (separate, lower-`@Order` `SecurityFilterChain`s in `CustomWebSecurityFiltersConfig`, evaluated before the `ROLE_USER` catch-all since their matchers are subsets of it):

- **`MaskingController`** (`data-node` only, `/custom/rest/masking`): `POST /{date}` → `maskMembers`; `DELETE /{date}` → `unmaskMembers`. Both call `(IMultiVersionDataActivePivot) activePivotManager.getActivePivot(CUBE_NAME)` on `IEpoch.MASTER_BRANCH_NAME`, block via `.join()`, and return a `MaskingResult(successful, successfulQueryCubes, failedQueryCubesWithReasons)`. Each call has its own OTEL span (`masking.mask`/`masking.unmask`) and accepts an optional `Test-Run-Id` header for correlating a whole rehearsal's traces/logs/metrics.
- **`DataMaintenanceController`** (`data-node` only, `/custom/rest/data`): mutates the Dremio backend directly, since DirectQuery cannot detect external database changes on its own — every SQL mutation is followed by an explicit `Application.refresh(ChangeDescription)` call scoped to the affected date, so the calling node's own hierarchies/aggregate providers stay consistent with Dremio.
  - `DELETE /{date}` — backs the date's rows up into `TradesMerged_backup`, deletes them from `TradesMerged_iceberg`, then `refresh(REMOVE_ROWS, date)`.
  - `POST /{date}/restore` — reverses it (re-insert from `_backup`, clear it, `refresh(ADD_ROWS, date)`).
  - `POST /{date}/load?sourceDate={existingDate}` — clones an existing date's rows under a brand-new date (stands in for a real upstream feed) then `refresh(ADD_ROWS, date)`.
  - ⚠️ `TradesMerged_iceberg` is a **single Dremio table shared by every data node** — calling `delete`/`load` against one node's port still mutates data visible to every replica; only the node you called gets its in-memory snapshot refreshed. That is exactly the risk masking is meant to cover for the masked node during the window between the SQL change and the other replica also being refreshed.
- **`DistributionInfoController`** (`query-node` only, `GET /custom/rest/distribution/{date}`): reads the query node's own live `IDistributionInformation` (only exposed via `IDistributedActivePivotVersion` — a data node's cube has no such accessor) and returns `DistributionResult(date, servingDataNodes, maskedDataNodes)` — independent proof that a mask actually changed dispatch, rather than trusting the mask call's own success flag. Caveat: `maskedDataNodes` only reflects masks applied while *this* query-node process has been continuously running — a query-node restart after a mask silently drops it from this view (the mask/routing effect itself is unaffected).

Example rehearsal sequence, run against a specific data node's own port (e.g. dq1 on 9090) — masking/unmasking/data calls are always scoped to one node's process:

```bash
curl -u admin:admin -X POST http://localhost:9090/custom/rest/masking/2019-01-05
curl -u admin:admin -X DELETE http://localhost:9090/custom/rest/data/2019-01-05
curl -u admin:admin -X POST "http://localhost:9090/custom/rest/data/2019-01-13/load?sourceDate=2019-01-06"
curl -u admin:admin -X DELETE http://localhost:9090/custom/rest/masking/2019-01-05
curl -u admin:admin http://localhost:9091/custom/rest/distribution/2019-01-05   # against the query node, port 9091
```

### Security

- `GlobalSecurityConfig` combines two `UserDetailsService`s — an in-memory one (`security.authentication.in-memory.users` in `application.yml`, bcrypt passwords) and a "technical user" one for machine-to-machine (`security.tech-user.passwords`) — into a `CompositeUserDetailsService`.
- `CustomWebSecurityFiltersConfig` defines ordered `SecurityFilterChain`s per concern, using ActiveViam's `HumanToMachineSecurityDsl`/`MachineToMachineSecurityDsl` helpers: H2 console (dev only, order 4), Swagger UI (admin-only, order 5), the masking/data-maintenance/distribution-info rehearsal endpoints (admin-only, orders 6-8, see "HA masking and the data-maintenance/rehearsal REST endpoints" above), then the remaining custom REST endpoints under `EndpointConstants.CUSTOM_REST_PATH` as a catch-all (any authenticated user, order 9). Lower orders (built into the Atoti starters) handle the core Atoti REST/MDX/UI endpoints. Order is load-bearing here, not cosmetic: each admin-only chain's matcher is a subset of the catch-all's, so it must be evaluated first.
- JWT keys and the login/logout flow are configured in `application.yml`; regenerate the key pair for real deployments via the `JwtUtil` class mentioned in the README.

### Adding a REST endpoint

Custom endpoints live under `com.activeviam.apps.rest`, mounted under `EndpointConstants.CUSTOM_REST_PATH` and protected by the `customRestEndpointsSecurityFilterChain`. See `HelloController` (trivial) and `DayController` (queries the cube via `IQueriesService.retrieveMembers`) as templates.

## OpenTelemetry

OTEL is enabled by default in `application.yml` (traces/metrics/logs exported) but disabled in `application-local.yml` for local dev without the stack running. See `otel/doc/STACK.md` for standing up the collector stack when using the OTEL run configuration.
