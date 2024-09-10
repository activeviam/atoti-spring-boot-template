<p align="center">
  <img width="80" src="./activeviam.svg" />
</p>
<h1 align="center">Atoti Spring Boot Template</h1>
<p align="center">A minimalist ActivePivot project built with Spring Boot for you to edit, customize and use as a base for your ActivePivot projects.</p>

---

## 📋 Details

This project aims to be an example of how to run ActivePivot as a [Spring Boot](https://spring.io/guides/gs/spring-boot)
application. ActivePivot was already a *Spring* application, but with the power of *Spring Boot* we can simplify our
dependency management, deployment model, and many other goodies that come with Spring Boot.

This project is a starting point for your own projects and implementations. You should be able to take this, customize
it and get a cube up and running in a few minutes.

## 📦 Installation

#### Requirements

- Java 21
- Maven 3
- ActivePivot jar files (commercial software)
- Running the application requires a license for the ActivePivot software.

Clone or download this repository and run `mvn clean install`. This will generate a jar file, which can be run using
standard java commands.

**Note:** If your build is unsuccessful, try skipping tests: `mvn clean install -DskipTests`

## 💻 Usage

### Running the fat jar

The project contains, out of the box, an extremely simple datastore schema and small `trades.csv` file. You can find
this file in `src/main/resources/data`.<br>

### Running on macOS

Add the following
argument `-Dactiveviam.chunkAllocatorKey=mmap` to your JVM, so it then becomes:

```bash
java -Dactiveviam.chunkAllocatorKey=mmap -Dfile.trades=<absolute path of trades.csv> -jar <fat jar path>
```

**Note:** If unable to start the ActivePivot Spring Boot application, you may need to add some additional arguments as
well, try the following:

```bash
java --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED -Dactiveviam.chunkAllocatorKey=mmap -Dfile.trades=<absolute path of trades.csv> -jar <fat jar path>
```

### Connecting to the ActivePivot

- Excel: you can connect to the cube from Excel, by connecting to an 'Analysis Services' source. The default URL to use
  when running locally is `http://localhost:9090/xmla`

- ActiveUI, ActiveViam's user interface for exploring the cube, will be available from `http://localhost:9090/ui`

- List of REST endpoints provided can be found at `http://localhost:9090/swagger-ui/index.html`

The default security credentials are `admin:admin`, but can be modified in the `SecurityConfig` class (we use Spring
Security). You should change this before going into production.<br>

It is also recommended that you change the JWT key pair in `application.yaml` by running the class `JwtUtil` and
generating a new key pair.
