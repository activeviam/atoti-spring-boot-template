<p align="center">
  <img width="80" src="./activeviam.svg" />
</p>
<h1 align="center">Atoti Spring Boot Template</h1>
<p align="center">A minimalist Atoti project built with Spring Boot for you to edit, customize and use as a base for your Atoti Java projects.</p>

---

## 📋 Details

This project aims to be an example of how to run Atoti as a [Spring Boot](https://spring.io/guides/gs/spring-boot)
application. Atoti was already a *Spring* application, but with the power of *Spring Boot* we can simplify our
dependency management, deployment model, and many other goodies that come with Spring Boot.

This project is a starting point for your own projects and implementations. You should be able to take this, customize
it and get a cube up and running in a few minutes.

## 📦 Installation

#### Requirements

- Java 21
- Maven 3
- Atoti jar files (commercial software)
- [Lombok](https://www.baeldung.com/lombok-ide)
- Running the application requires a license for the Atoti software.

Clone or download this repository and run `mvn clean install`. This will generate a jar file, which can be run using
standard java commands.

**Note:** If your build is unsuccessful, try skipping tests: `mvn clean install -DskipTests`

## 💻 Usage

#### Running the fat jar

The project contains, out of the box, an extremely simple datastore schema and small `trades.csv` file. You can find
this file in `src/main/resources/data`.<br>

```bash
java -jar ./target/atoti-spring-boot-template.jar
```

###### Running on macOS

Add the following argument `-Dactiveviam.chunkAllocatorKey=mmap` to your JVM, so it then becomes:

```bash
java -Dactiveviam.chunkAllocatorKey=mmap -jar ./target/atoti-spring-boot-template.jar
```

**Note:** If unable to start the Atoti Spring Boot application, [you may need to add some additional arguments as
well](https://docs.activeviam.com/products/atoti/server/latest/docs/configuration/java_version/#jvm-options), try the
following:

```bash
java --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED -Dactiveviam.chunkAllocatorKey=mmap -jar ./target/atoti-spring-boot-template.jar
```

#### Running from the IDE

We provide 3 run configurations for IntelliJ Idea Ultimate:

- `AtotiSpringBootApplication (no OTEL)`: does not use the OpenTelemetry config, uses the `application-local.yml`.
- `AtotiSpringBootApplication OTEL`: uses the OpenTelemetry config, uses the `application.yml`, start the OTEL stack
  else you will see some exceptions.

Similarly, you can find the following run configurations that work with IntelliJ Idea CE:

- `AtotiSpringBootApplication (no OTEL) App`
- `AtotiSpringBootApplication OTEL App`

#### Connecting to the Atoti Server

- Excel: you can connect to the cube from Excel, by connecting to an 'Analysis Services' source.
  The default URL to use when running locally is [http://localhost:9090/xmla](http://localhost:9090/xmla).

- AtotiUI, ActiveViam's user interface for exploring the cube, will be available
  from [http://localhost:9090/ui](http://localhost:9090/ui).

- AdminUI console: [http://localhost:9090/admin/ui](http://localhost:9090/admin/ui).

- List of REST endpoints provided can be found
  at [http://localhost:9090/swagger-ui/index.html](http://localhost:9090/swagger-ui/index.html).

The default security credentials are `admin:admin`, but can be modified in the `application.yml` file.<br>
For a real production deployment you should probably use LDAP instead of hardcoding the users in the `application.yml`
file.<br>
It is also recommended that you change the JWT key pair in `application.yml` by running the class `JwtUtil` and
generating a new key pair.

## 🌐 Distributed architecture (DirectQuery + data/query nodes)

This branch no longer loads CSVs into an in-memory datastore. Instead it reads trade data live from Dremio via
**DirectQuery**, and the app runs as two Spring-profile roles that together form one distributed cluster:

- **`data-node`** — the only role that connects to Dremio. Discovers a single merged `TradesMerged` table (Phase 4:
  the earlier `Trades`/`TradeAttributes` two-table model was merged into one table, so there is no join at all),
  builds the cube, and joins the cluster. Several `data-node` instances can run at once, all reading the exact same
  Dremio data, for high availability — the query node picks one of them at random per query, so one instance going
  down just means it stops being picked, no failover logic needed.
- **`query-node`** — holds no data of its own. It joins the same cluster and merges each data node's
  measures/dimensions into its own topology at query time, dispatching every query to one of the available data
  nodes and returning the result.

Launch each instance with an explicit profile, its own `SERVER_PORT`, and (for `data-node`) Dremio connection
details:

```bash
# data node(s) — repeat with a different SERVER_PORT for each replica
DREMIO_USERNAME=... DREMIO_PASSWORD=... DREMIO_HOST=localhost SERVER_PORT=9090 \
  java -Dspring.profiles.active=data-node -Dactiveviam.chunkAllocatorKey=mmap -jar ./target/atoti-spring-boot-template.jar

# query node — clients connect here
SERVER_PORT=9091 java -Dspring.profiles.active=query-node -jar ./target/atoti-spring-boot-template.jar
```

Query the *query node* the same way you'd query any Atoti cube (MDX/REST/XMLA/UI) — it's the client-facing entry
point; the data node(s) are internal.

### High-availability masking: taking a data node's data out of rotation, safely

Since multiple `data-node` instances replicate the same Dremio data, Atoti's **masking API** lets you pull one
node out of rotation *for specific dates* with zero downtime, so its backing rows can be safely changed on Dremio,
then bring it back once it's consistent again.

**Masking is scoped two ways at once — to one specific node process, and to specific values on that node:**
a mask call is sent to one data node's own port, and it targets a specific set of `AsOfDate` values on that node
only. Masking dq1 for `2019-01-05` does not take dq1 offline — dq1 keeps serving every other date normally — and
has no effect on dq2 at all. Once masked, the query node stops routing queries for that date to that node and
serves them from the other replica instead; unmasking reverses it. Masking never deletes data — it's purely an
in-memory routing decision, which is what makes it safe to do live.

**This has been verified against a live run, not just asserted from the mechanism's design.** Parsing every
dispatch-log line for a masked node's own address across a full mask→unmask window showed it still actively
serving *some* date in every single dispatch snapshot (2171/2171), while the masked date itself appeared in its
own dispatched-members list zero times (0/2171) over that same window.

### Custom REST endpoints

All custom endpoints live under `/custom/rest` and require the `admin`/`admin` credentials (or another
`ROLE_ADMIN`/`ROLE_USER` account) via HTTP Basic.

|                     Endpoint                      | Method | Role  |    Node    |                                                          What it does                                                           |
|---------------------------------------------------|--------|-------|------------|---------------------------------------------------------------------------------------------------------------------------------|
| `/custom/rest/hello`                              | GET    | user  | any        | Trivial example endpoint, returns a greeting + timestamp.                                                                       |
| `/custom/rest/day/loaded`                         | GET    | user  | any        | Returns how many `AsOfDate` members are currently loaded in the cube.                                                           |
| `/custom/rest/masking/{date}`                     | POST   | admin | data-node  | Masks `date` on **this** node — the query node stops routing that date's queries here.                                          |
| `/custom/rest/masking/{date}`                     | DELETE | admin | data-node  | Unmasks `date` on this node, reversing the above.                                                                               |
| `/custom/rest/data/{date}`                        | DELETE | admin | data-node  | Backs up and deletes `date`'s rows from the shared Dremio table, then refreshes this node's cube to match.                      |
| `/custom/rest/data/{date}/restore`                | POST   | admin | data-node  | Restores `date`'s rows from the backup made by `DELETE`, then refreshes.                                                        |
| `/custom/rest/data/{date}/load?sourceDate={date}` | POST   | admin | data-node  | Clones an existing date's rows under a brand-new date (simulates a new day's data arriving), then refreshes.                    |
| `/custom/rest/distribution/{date}`                | GET    | admin | query-node | Returns which data node(s) currently serve `date`, and which are masked for it — proof the mask actually changed query routing. |

**Example: mask a date on one data node, delete it from Dremio, bring in a new date, then unmask:**

```bash
# dq1 is listening on 9090, the query node on 9091
curl -u admin:admin -X POST http://localhost:9090/custom/rest/masking/2019-01-05

curl -u admin:admin -X DELETE http://localhost:9090/custom/rest/data/2019-01-05
curl -u admin:admin -X POST "http://localhost:9090/custom/rest/data/2019-01-13/load?sourceDate=2019-01-06"

curl -u admin:admin -X DELETE http://localhost:9090/custom/rest/masking/2019-01-05

# confirm from the query node's own routing table
curl -u admin:admin http://localhost:9091/custom/rest/distribution/2019-01-05
```

⚠️ `TradesMerged` is one Dremio table **shared by every data node** — a `delete`/`load` call issued against one
node's port still mutates data every replica ultimately reads from; only the node you called gets its own
in-memory snapshot refreshed immediately. That gap is exactly what masking the node beforehand protects against.

## ❤️ Using OpenTelemetry

Check [this section for OpenTelemetry](./otel/doc/STACK.md).

## Docker properties (specified in .env file)

DREMIO_USERNAME=
DREMIO_PASSWORD=
DREMIO_HOST=host.docker.internal
DREMIO_PORT=32010
DREMIO_JDBC_DRIVER=org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver

START_DATE=2020-02-27
END_DATE=2027-02-27

CLUSTER_DB_URL=jdbc:postgresql://localhost:5432/atoti_cluster
CLUSTER_DB_USERNAME=postgres
CLUSTER_DB_PASSWORD=
CLUSTER_DB_DRIVER=org.postgresql.Driver
