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

## ❤️ Using OpenTelemetry

Check [this section for OpenTelemetry](./otel/doc/STACK.md).

## WebServices

Test with the following

```bash
curl -X POST http://localhost:9090/webservices/DummyService \
  -H "Content-Type: text/xml" \
  -d '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:qua="http://www.quartetfs.com">
   <soapenv:Header/>
   <soapenv:Body>
      <qua:executeMDX>
         <query>test query</query>
      </qua:executeMDX>
   </soapenv:Body>
</soapenv:Envelope>'
```

```bash
curl -X POST http://localhost:9090/webservices/VaRQueryService \
  -H "Content-Type: text/xml" \
  -d '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:qua="http://www.quartetfs.com">
   <soapenv:Header/>
   <soapenv:Body>
      <qua:testVarDataExtract>
        <varQueryDTO xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <confidenceLevel>0.99</confidenceLevel>
          <dtHeader>DealNum|TranDate|SourceSystem|Ptfolio|TrnFmly|TrnGrp|TrnType|PlCur|Cur|Instrument|TpCntrp|Currency|ScenarioValues</dtHeader>
          <dtQuery>
            <firstResult xmlns="http://www.quartetfs.com">0</firstResult>
            <maxResults xmlns="http://www.quartetfs.com">150000</maxResults>
            <contextValues xmlns="http://www.quartetfs.com">
              <subCubeProperties isAccessGranted="true">
                <hierarchyRestrictions>
                  <hierarchy name="DEF_HIE_Data Snapshot" dimension="Data Snapshot">
                    <allowedMember>
                      <memberPathPart xsi:type="xsd:string">AllMember</memberPathPart>
                      <memberPathPart xsi:type="xsd:string">EOD</memberPathPart>
                    </allowedMember>
                  </hierarchy>
                  <hierarchy name="DEF_HIE_Capital View" dimension="Capital View">
                    <allowedMember>
                      <memberPathPart xsi:type="xsd:string">AllMember</memberPathPart>
                      <memberPathPart xsi:type="xsd:string">NO</memberPathPart>
                    </allowedMember>
                    <allowedMember>
                      <memberPathPart xsi:type="xsd:string">AllMember</memberPathPart>
                      <memberPathPart xsi:type="xsd:string">N/A</memberPathPart>
                    </allowedMember>
                  </hierarchy>
                  <hierarchy name="DEF_HIE_Risk Portfolio Hierarchy" dimension="Risk Portfolio Hierarchy">
                    <allowedMember>
                      <memberPathPart xsi:type="xsd:string">AllMember</memberPathPart>
                      <memberPathPart xsi:type="xsd:string">ANZ Group</memberPathPart>
                      <memberPathPart xsi:type="xsd:string">TradedCAPM</memberPathPart>
                      <memberPathPart xsi:type="xsd:string">Combined Trading</memberPathPart>
                      <memberPathPart xsi:type="xsd:string">Traded</memberPathPart>
                      <memberPathPart xsi:type="xsd:string">LM Trading</memberPathPart>
                      <memberPathPart xsi:type="xsd:string">LM - India</memberPathPart>
                    </allowedMember>
                  </hierarchy>
                  <hierarchy name="DEF_HIE_Container" dimension="Container">
                    <allowedMember>
                      <memberPathPart xsi:type="xsd:string">AllMember</memberPathPart>
                      <memberPathPart xsi:type="xsd:string">VAR_1D</memberPathPart>
                    </allowedMember>
                  </hierarchy>
                </hierarchyRestrictions>
              </subCubeProperties>
            </contextValues>
            <isFormatted xmlns="http://www.quartetfs.com">false</isFormatted>
            <locations xmlns="http://www.quartetfs.com">
              <location>17/11/25|AllMember|AllMember|AllMember|AUD|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|AllMember|N/A|N/A|1|1|1|1|1|1|1|1|1|1|1|N/A|RegCapital|RegCapital</location>
            </locations>
            <pivotId xmlns="http://www.quartetfs.com">MarketRiskCube</pivotId>
          </dtQuery>
          <from>0</from>
          <to>0</to>
        </varQueryDTO>
      </qua:testVarDataExtract>
   </soapenv:Body>
</soapenv:Envelope>'
```

