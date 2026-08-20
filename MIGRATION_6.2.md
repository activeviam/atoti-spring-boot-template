# Migrating this project from Atoti 6.1 to 6.2

This document records exactly what changed in this repository to move from Atoti Server 6.1.23 to
6.2.0, and why. It is meant as a companion to ActiveViam's own
[6.2 migration notes](https://docs.activeviam.com/engine/java-sdk/6.2/release/migration_notes) —
that page covers the whole product; this page covers only what this specific template actually
hit, confirmed by getting `mvn clean install` green again on JDK 25 / Atoti 6.2.0 / Spring Boot
4.1.0. If you maintain a fork of this template, the sections below are close to a checklist for
your own upgrade.

## Prerequisites

* **JDK 25** is required (Atoti 6.2's new baseline; JDK 21 cannot compile with `--release 25` and
  will fail with `error: release version 25 not supported`). Any JDK 25 distribution works; this
  was verified with GraalVM CE 25.0.2.
* **Maven repository access to the `mvn-prod` channel.** Atoti 6.2.0 is only published to
  ActiveViam's client-facing production channel
  (`https://activeviam.jfrog.io/artifactory/mvn-prod/`), not to the `mvn-internal` channel this
  repository's build environment may already be configured to use for 6.1.x. See
  [Repository access](#repository-access) below.

## pom.xml changes

|                  Property / dependency                   |    6.1    |                                                                     6.2                                                                      |                                                                                                                         Why                                                                                                                          |
|----------------------------------------------------------|-----------|----------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `spring-boot-starter-parent`                             | 3.5.16    | **4.1.0**                                                                                                                                    | Atoti 6.2's mandatory baseline (brings Spring Framework 7 and Jackson 3)                                                                                                                                                                             |
| `java.version`                                           | 21        | **25**                                                                                                                                       | Atoti 6.2's mandatory baseline                                                                                                                                                                                                                       |
| `atoti-server.version`                                   | 6.1.23    | **6.2.0**                                                                                                                                    | The upgrade itself                                                                                                                                                                                                                                   |
| `springdoc.version`                                      | 2.8.17    | **3.1.0**                                                                                                                                    | springdoc 2.x does not support Spring Boot 4/Spring 7; 3.x does                                                                                                                                                                                      |
| `atoti-server-apm-starter` dependency                    | present   | **removed**                                                                                                                                  | The starter no longer exists in 6.2 — its auto-configuration was folded into `atoti-server-starter` (node-instance-name logging is now always on; the blocked-thread watchdog is opt-in via `atoti.server.monitoring.blockedThreadWatchdog.enabled`) |
| `spring-boot-resttestclient` dependency (test scope)     | —         | **added**                                                                                                                                    | `TestRestTemplate` moved out of `spring-boot-test` into this new module and is no longer auto-configured by default (see item 6 below)                                                                                                               |
| `tomcat.version` / `netty.version` / `httpcore5.version` | inherited | **pinned** to 11.0.24 / 4.2.16.Final / 5.4.3                                                                                                 | ActiveViam's migration notes call these out as security fixes ahead of what Spring Boot 4.1.0 itself manages (CVE-2026-55276, CVE-2026-53434, CVE-2026-53404, CVE-2026-44891, CVE-2026-54399, CVE-2026-54428)                                        |
| `flight-sql-jdbc-driver` dependency                      | shaded    | **swapped** for unshaded `flight-sql-jdbc-core` + `arrow-memory-unsafe` (excludes `arrow-memory-netty`, also excluded from `dremio-dialect`) | Not an Atoti change, but required on Java 25 — see item 8 below                                                                                                                                                                                      |

### Repository access

Add a repository pointing at the production channel (the internal/legacy channel most existing
setups use for 6.1.x does not carry 6.2.0):

```xml
<repositories>
  <repository>
    <id>ActiveViamProdRepository</id>
    <name>ActiveViam Production Repository</name>
    <url>https://activeviam.jfrog.io/artifactory/mvn-prod/</url>
  </repository>
</repositories>
```

Credentials for this repository id must exist under `<servers>` in your `~/.m2/settings.xml` (the
same JFrog account used for the existing internal repository works). Credentials are deliberately
**not** committed to `pom.xml`.

## Code changes

Six source files needed changes to compile against 6.2, plus two more (#7, #8 below) found only by
actually running the upgraded app end to end against a live Dremio — `mvn clean install` alone does
not catch these, since no test in this repo boots the full app with the real logging config or a
live external database connection. None of the ⚠️ items below are called out by name in
ActiveViam's own migration notes — they were found by compiling and running this codebase against
6.2.0, not by reading documentation, so double-check your own usage of these even if your code
looks unrelated to what's listed here.

### 1. `PathRequest` moved package (Spring Boot 4 modularization)

Spring Boot 4 split the old monolithic `spring-boot-autoconfigure` into per-concern modules; the
security ones now live in `spring-boot-security`.

```diff
- import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
+ import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
```

File: `src/main/java/com/activeviam/apps/cfg/security/filter/CustomWebSecurityFiltersConfig.java`

### 2. Masking API package moved (Atoti 6.2 distributed-cube split)

`activepivot-dist-impl` no longer exists as a single artifact in 6.2; it split into
`distribution-data-node` and `distribution-query-node`, and the masking types moved with it:

```diff
- import com.activeviam.activepivot.dist.impl.api.cube.IMultiVersionDataActivePivot;
- import com.activeviam.activepivot.dist.impl.api.cube.IMultiVersionDataActivePivot.IMaskingOperationReport;
- import com.activeviam.activepivot.dist.impl.api.cube.IMultiVersionDataActivePivot.LevelMembers;
+ import com.activeviam.activepivot.dist.datanode.impl.api.cube.IMultiVersionDataActivePivot;
+ import com.activeviam.activepivot.dist.datanode.impl.api.cube.IMultiVersionDataActivePivot.IMaskingOperationReport;
+ import com.activeviam.activepivot.dist.datanode.impl.api.cube.IMultiVersionDataActivePivot.LevelMembers;
```

File: `src/main/java/com/activeviam/apps/rest/MaskingController.java`. No pom change was needed —
the `distribution-data-node` jar is already pulled in transitively.

### 3. ⚠️ `INativeMeasureBuilder#withAlias(String)` removed, no replacement

`withAlias(String)` — used to rename a native measure (e.g. the built-in contributor count from
`contributors.COUNT` to a friendlier `Count`) — has been removed from
`ICanStartBuildingMeasures.INativeMeasureBuilder` with **no replacement method** anywhere in the
builder hierarchy. This is not mentioned in ActiveViam's migration notes or changelog; it was
found by decompiling `activepivot-core-6.1.23.jar` vs `activepivot-core-6.2.0.jar` and comparing.
ActiveViam's own 6.2 documentation example for `withContributorsCount()` no longer calls
`.withAlias(...)` either, which is consistent with this being deliberate rather than an oversight.

**Behavior change:** native measures built this way now surface under their default engine names
(`contributors.COUNT`, `UPDATE.TIMESTAMP`) instead of the custom names this project previously gave
them (`Count`, `Update.Timestamp`). Any MDX query, dashboard, or Atoti UI bookmark that referenced
the measure by its old aliased name will need updating to the new default name. This project had
no such references outside the two definition sites, but check your own fork.

```diff
  .withContributorsCount()
  .withinFolder(NATIVE_MEASURES)
- .withAlias("Count")
  .withFormatter(INT_FORMATTER)
  .withUpdateTimestamp()
  .withinFolder(NATIVE_MEASURES)
- .withAlias("Update.Timestamp")
  .withFormatter(TIMESTAMP_FORMATTER)
```

Files: `src/main/java/com/activeviam/apps/cfg/pivot/DataCubeConfig.java`,
`src/test/java/com/activeviam/apps/cfg/pivot/CubeTestConfig.java`.

**If you need the old display name back**, the only option found is renaming downstream (e.g. via
a Copper alias measure that delegates to the native one), since the native-measure builder itself
no longer exposes a rename hook. Consider filing feedback with ActiveViam if this affects you —
this looks like an undocumented regression, not an intentional simplification with a documented
alternative.

### 4. ⚠️ `withAllMeasures()` removed from the data-cluster builder chain

The data-cluster definition builder used to require an explicit `.withAllMeasures()` step between
`.withApplicationId(...)` and `.withConcealedBranches()`. That step no longer exists in the 6.2
builder interface chain at all — `withApplicationId(String)` now returns a builder whose only next
step is `withAllHierarchies()`, which flows directly to `withConcealedBranches()`.

This lines up with a change ActiveViam **does** document: the
`activeviam.distribution.cube.throwOnDifferentMeasureNames` property was removed, and "data cubes
sharing an application id must now expose the same measure names" — i.e. partial measure exposure
per data cube is gone; a data cube joining an application now always exposes its full measure set.

```diff
  .withApplicationId(APPLICATION_ID)
  .withAllHierarchies()
- .withAllMeasures()
  .withConcealedBranches()
```

File: `src/main/java/com/activeviam/apps/cfg/pivot/DataCubeConfig.java`

### 5. Spring Security 7: `DaoAuthenticationProvider` constructor

Spring Security 7 removed the no-arg `DaoAuthenticationProvider()` constructor and the
`setUserDetailsService(...)` setter; the `UserDetailsService` must now be passed to the
constructor (this is a Spring Security change, not an Atoti one, but it ships as part of the same
Spring Boot 4 upgrade).

```diff
- var authenticationProvider = new DaoAuthenticationProvider();
+ var authenticationProvider = new DaoAuthenticationProvider(basicUserDetailsService);
  authenticationProvider.setPasswordEncoder(passwordEncoder);
- authenticationProvider.setUserDetailsService(basicUserDetailsService);
  return authenticationProvider;
```

Files: `src/main/java/com/activeviam/apps/cfg/security/auth/InMemoryAuthenticationConfig.java`,
`src/main/java/com/activeviam/apps/cfg/security/auth/TechnicalAuthenticationSecurityConfig.java`

### 6. Test changes: `TestRestTemplate` relocated and no longer auto-configured

Spring Boot 4 moved `TestRestTemplate` out of `spring-boot-test` into a new, separate module
(`spring-boot-resttestclient`), under a new package, and stopped auto-configuring it by default —
`RestTestClient` is the forward-looking replacement, but `TestRestTemplate` is still available for
projects (like this one) that don't want to rewrite existing tests around a new fluent API.

```diff
+ <dependency>
+   <groupId>org.springframework.boot</groupId>
+   <artifactId>spring-boot-resttestclient</artifactId>
+   <scope>test</scope>
+ </dependency>
```

```diff
- import org.springframework.boot.test.web.client.TestRestTemplate;
+ import org.springframework.boot.resttestclient.TestRestTemplate;
+ import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;

  @SpringBootTest(classes = AtotiSpringBootApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
+ @AutoConfigureTestRestTemplate
  class AtotiSpringBootApplicationTest {
```

File: `src/test/java/com/activeviam/apps/AtotiSpringBootApplicationTest.java`. `LocalServerPort`
did **not** move and needed no change.

### 7. ⚠️ `logback-spring.xml`'s `LogUserConverter` moved package (only surfaces at runtime)

`com.activeviam.apm.api.logging.LogUserConverter` no longer exists — it moved to
`com.activeviam.tech.logging.logback.spring.api.LogUserConverter` (in the `logging-logback-spring`
artifact) when `atoti-server-apm-starter` was folded into other 6.2 artifacts (see the pom.xml table
above). Since this is a resource file referenced by class name string, not a Java import, `mvn
clean install` compiles fine either way — every node crashes at startup with a Logback
configuration error until this is fixed, and no test in this repo boots the full app with the real
`logback-spring.xml`, so this was only caught by actually running the app.

```diff
- <conversionRule conversionWord="user" converterClass="com.activeviam.apm.api.logging.LogUserConverter"/>
+ <conversionRule conversionWord="user" converterClass="com.activeviam.tech.logging.logback.spring.api.LogUserConverter"/>
```

File: `src/main/resources/logback-spring.xml`. **This directly contradicts** the "Deliberately not
done" section's original claim that "this project's own logging config didn't need changes" — that
claim was wrong; it was written before the app was actually run end to end post-upgrade.

### 8. ⚠️ `arrow-flight-sql-jdbc-driver:19.0.0` doesn't work on Java 25 — swap for unshaded modules

Not an Atoti issue at all, but blocks every `data-node` instance from starting (any DirectQuery
scenario, not just this template's Dremio dialect) once you're actually on Java 25. The shaded
driver's bundled Netty allocator (`PooledByteBufAllocatorL`) unconditionally probes an empty
buffer's native memory address at class-init time; on Java 25 this throws
`UnsupportedOperationException` from `EmptyByteBuf.memoryAddress()`, because Netty 4.x's
`sun.misc.Unsafe`-based direct-memory path breaks under JEP 471 (Unsafe memory-access methods
deprecated for removal). This is a real, externally-documented bug — see
[apache/arrow-java#728](https://github.com/apache/arrow-java/issues/728) and
[apache/iceberg#15930](https://github.com/apache/iceberg/issues/15930) — not a configuration
mistake, confirmed by reproducing it standalone outside this app before touching any code. No newer
`flight-sql-jdbc-driver` release exists on Maven Central (19.0.0 is latest) as of this writing, and
no JVM flag works around it (tried `-Dio.netty.tryReflectionSetAccessible=true`,
`-Dio.netty.noUnsafe=true`, extra `--add-opens`, `-XX:MaxDirectMemorySize`).

**Fix**: depend on the unshaded `flight-sql-jdbc-core` module instead of the shaded
`flight-sql-jdbc-driver` uber-jar, explicitly add `arrow-memory-unsafe`, and exclude
`arrow-memory-netty` (`flight-sql-jdbc-core`'s default transitive allocator, which carries the
identical bug in unshaded form) — both from this new dependency and from `dremio-dialect` itself,
which also transitively pulls in the shaded driver. The unshaded module ships the exact same driver
class and `META-INF/services/java.sql.Driver` registration
(`org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver`), so no application code or JDBC URL changes
were needed.

```diff
- <dependency>
-   <groupId>org.apache.arrow</groupId>
-   <artifactId>flight-sql-jdbc-driver</artifactId>
-   <version>${flight-sql-jdbc-driver.version}</version>
- </dependency>
+ <dependency>
+   <groupId>org.apache.arrow</groupId>
+   <artifactId>flight-sql-jdbc-core</artifactId>
+   <version>${flight-sql-jdbc-driver.version}</version>
+   <exclusions>
+     <exclusion>
+       <groupId>org.apache.arrow</groupId>
+       <artifactId>arrow-memory-netty</artifactId>
+     </exclusion>
+   </exclusions>
+ </dependency>
+ <dependency>
+   <groupId>org.apache.arrow</groupId>
+   <artifactId>arrow-memory-unsafe</artifactId>
+   <version>${flight-sql-jdbc-driver.version}</version>
+ </dependency>
```

Also add the same exclusion to the existing `dremio-dialect` dependency:

```diff
  <dependency>
    <groupId>com.activeviam.database.jdbc.dialect.dremio</groupId>
    <artifactId>dremio-dialect</artifactId>
    <version>${atoti-server.version}</version>
+   <exclusions>
+     <exclusion>
+       <groupId>org.apache.arrow</groupId>
+       <artifactId>flight-sql-jdbc-driver</artifactId>
+     </exclusion>
+   </exclusions>
  </dependency>
```

File: `pom.xml`. Verified end to end, not just compiled: a real `data-node` process started and
successfully queried live Dremio data over this driver on JDK 25 (see the Scenario 1 re-run evidence
at `~/atoti-rehearsal-evidence/phase4-single-table-6.2.0-rehearsal-2026-08-20/`).

## Not affected

These are worth naming because they're the kind of thing a 6.2 upgrade elsewhere might hit, but
this project's actual usage turned out to be already 6.2-safe:

* **Distributing-levels API.** 6.2 removed the deprecated `String`-based distributing-level methods
  (`withDistributingFields(String...)`, `getDistributingFields()`, the 2-arg
  `DistributedApplicationDefinition(String, List<String>)` constructor) in favor of
  `LevelIdentifier`-based ones. `QueryCubeConfig.java` already used
  `.withDistributingLevels(new LevelIdentifier(...))` — no change needed.
* **`IActivePivotManagerDescriptionConfig` / `IActivePivotConfig` / `IDatastoreConfig`.** All three
  are now deprecated (not removed) in 6.2, in favor of exposing the manager/pivot config as a plain
  `@Bean` without implementing the interface. `DataNodeManagerConfig`, `QueryNodeManagerConfig`,
  and `QueryNodeActivePivotConfig` still implement these interfaces and compiled and ran correctly
  as-is — this is on the "clean up later" list (see below), not a blocker.

## Deliberately not done as part of this pass

* **Jackson 2 → 3 namespace migration** (`com.fasterxml.jackson.*` → `tools.jackson.*`). This
  project doesn't touch Jackson directly in its own code, so nothing broke, but if you add custom
  REST DTOs/serializers to a fork, check ActiveViam's migration notes for this.
* **JUL → SLF4J logging switch.** Atoti Server now logs through SLF4J instead of JUL. This
  project's `logback-spring.xml` *did* need one change — see item 7 above — so don't assume your
  own logging config is unaffected just because it compiles; only running the app catches this.
* **Cleaning up the now-deprecated `IActivePivotManagerDescriptionConfig` /
  `IActivePivotConfig` / `IDatastoreConfig` usages** described above — they still work, they're
  just marked for eventual removal.
* **ActiveViam's official OpenRewrite migration recipe**
  (`com.activeviam.migration:6_1_20-to-6_2_0`) was not run. Everything in this document was found
  and fixed by hand, then confirmed with a real `mvn clean install`. Running the recipe on a fresh
  fork might surface additional mechanical clean-ups (e.g. the deprecated-interface removals above)
  that a hand pass doesn't bother with unless something actually fails to compile.

## Verification performed

* `mvn clean install -DskipTests` — clean compile of `src/main` and `src/test`, Spotless passes.
* `mvn test` — `MeasuresTest` (cube-only) passes; `AtotiSpringBootApplicationTest` (full Spring
  Boot context) is skipped as before (requires a live Dremio connection via `DREMIO_USERNAME`).
* Atoti Server confirmed reporting `Atoti Server version: 6.2.0` in the test log output.
* **Full live cluster rerun** (this is what actually caught items 7 and 8 above): built the fat jar
  and ran a real 3-node cluster (two `data-node` replicas + one `query-node`) against a live Dremio
  instance on JDK 25, repeating the existing masking/data-maintenance rehearsal sequence
  (mask → drop → external insert → load → unmask → drop → load) end to end. Every step matched the
  pre-upgrade (6.1.23) behavior exactly, including the aggregate cache staying active and correct
  throughout. Full evidence and findings:
  `~/atoti-rehearsal-evidence/phase4-single-table-6.2.0-rehearsal-2026-08-20/README.md`.

All of the above ran with `JAVA_HOME` pointed at a JDK 25 distribution (GraalVM CE 25.0.2 was used
here; any JDK 25 build should work equally).
