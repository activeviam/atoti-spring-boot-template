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

# Security

## Multi-mode

- `AtotiSpringBootApplication OAuth (no OTEL)`: enables the configuration for OAuth.
- `AtotiSpringBootApplication SAML (no OTEL)`: enables the configuration for SAML.
- `AtotiSpringBootApplication Kerberos (no OTEL)`: enables the configuration for Kerberos.

## Kerberos Authentication Setup

### Configuration Overview

| Realm        | EXAMPLE.COM          |
|--------------|----------------------|
| KDC Hostname | kerberos.example.com |
| App Hostname | app.example.com      |
| Username     | admin                |
| Password     | admin                |

### Step-by-step instructions

#### DNS Records:

Add the KDC and application hostname at `/etc/hosts`.

```
127.0.0.1	kerberos.example.com
127.0.0.1	app.example.com
```

#### Kerberos Configuration

Adjust your Kerberos configuration at `/etc/krb5.conf`.
Uncomment, or add, the following lines:

```
[libdefaults]
    default_realm = EXAMPLE.COM
...

[realms]
EXAMPLE.COM = {
    kdc = kerberos.example.com
    admin_server = kerberos.example.com
}

[domain_realm]
.example.com = EXAMPLE.COM
example.com = EXAMPLE.COM
```

#### KDC Docker Container

Start the local KDC server and copy the generated keytab file for the application.

```bash
docker build -t kdc-image src/main/kerberos/docker
docker run --name=kdc -d -p 749:749/tcp -p 88:88/udp kdc-image
# Validate the server started correctly
docker logs kdc
docker cp kdc:/app.keytab src/main/kerberos/app.keytab
```

#### Initialize Kerberos Client

Initialize Kerberos and perform a test.
If you do not have `kinit` installed, you can install it via your package manager.

```bash
$ kinit admin
Password for admin@EXAMPLE.COM:

# Check the user ticket exists
$ klist
Ticket cache: FILE:/tmp/krb5cc_1000
Default principal: admin@EXAMPLE.COM

Valid starting     Expires            Service principal
24/10/25 18:55:03  25/10/25 18:55:01  krbtgt/EXAMPLE.COM@EXAMPLE.COM
	renew until 31/10/25 18:55:01

# Export the necessary environment variables, so curl and the browser can access the Kerberos setup
$ export KRB5CCNAME=/tmp/krb5cc_$(id -u)
$ export KRB5_CLIENT_KTNAME=./src/main/kerberos/app.keytab

# Test with curl and ensure the Kerberos Negotiate token is sent and accepted
$ curl -v -u : --negotiate http://app.example.com:9090/ui/index.html
* Host app.example.com:9090 was resolved.
* IPv6: (none)
* IPv4: 127.0.0.1
*   Trying 127.0.0.1:9090...
* Connected to app.example.com (127.0.0.1) port 9090
* using HTTP/1.x
* Server auth using Negotiate with user ''
> GET /ui/index.html HTTP/1.1
> Host: app.example.com:9090
> Authorization: Negotiate YIIDVgYGKwYBBQUCoIIDSjCCA0agDTALBgkqhkiG9xIBAgKiggMzBIIDL2CCAysGCSqGSIb3EgECAgEAboIDGjCCAxagAwIBBaEDAgEOogcDBQAgAAAAo4ICBmGCAgIwggH+oAMCAQWhDRsLRVhBTVBMRS5DT02iIjAgoAMCAQOhGTAXGwRIVFRQGw9hcHAuZXhhbXBsZS5jb22jggHCMIIBvqADAgESoQMCAQKiggGwBIIBrPTJeiwx2Iv+2dAW0g14jseaXdqQbhdolhOaIzb/iIf9rtlzXfeDyoiTtcPZw1wcjKqPqAtmnChOeOXWQRs3hnsl492ynWOmuGIMF4e0XBxsf2XtMPlxkoLEx0N9TGdXQMjas4+Yf43kwfVO59jievkDXibC7UuzcoksIF+/wqHYf6pW2s9S1R1a8g19xUTd63U56UX9Dqu5AVRBBKD5spECpr4DcvAsDAoLAuBFVrVPneJxk5lfFwc7XRfMh2CyCtX+BwMsFEp09iZUWzoA5tA7hzq15w7z+POWzEP8XsA7aAGcnw7yYJVy/4WJ+lRB7sswg24ViNGToaLdysZjtPTi4r66GLM1EjtQ5cgejevBq5pH+T+Lb9OzfRLB4INus7wCmzTft+x1wqtMIHVMr4maKV9NWyV89Cg+LAQBk2gwiuSUOxAtgSIXxftorAsmcIIPYMWOtI/ku5NjPsZemV/kmNERMvXtO+AOFtDjnLRu4P6RDcznuIf6kqKpbXvGjaNq1Ym7tNS/TUTMKRLDWvQ1HlTHgtRlCHiyEJRpu+iU1oQ5OlSdPGtEjrp7pIH2MIHzoAMCARKigesEgeintoF6ofLep1wdr1EvGkmbARi3WfccBCsdrB7L6THfu0Bza0LgS1PIwH0EsfjiNgQigIyu6dAHHLV9bolJQvvvLESLUba8tE/s0/zp9YSkV3ZyigGj/4z4IGLteTQDJaoVDeLuV92m60ulR4aBvQAS79ORtN9l99JERFwW/zdqn6wlZduhcP1aaNThisvxBr62oKseCK40/Xv4yywCbuNiR/1EVd15J9sJo74fj8sQWxrZnhn9RGacd9CxP9a7WszGD1otuovbmgWXPlgqOykwpKyZZLm7irTARrROJDljwOB6nxPMsM8r
> User-Agent: curl/8.11.1
> Accept: */*
> 
* Request completely sent off
< HTTP/1.1 200 
< Vary: Origin
< Vary: Access-Control-Request-Method
< Vary: Access-Control-Request-Headers
< Cache-Control: no-store
< Last-Modified: Thu, 31 Jul 2025 10:46:23 GMT
< Accept-Ranges: bytes
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 0
< Content-Type: text/html
< Content-Length: 618
< Date: Fri, 24 Oct 2025 10:55:26 GMT
< 
* Connection #0 to host app.example.com left intact
<!doctype html><html><head><title>Atoti</title><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="description" content="Collaborative data exploration"><script defer="defer" src="static/js/6737.8ab6f0a4.js"></script><script defer="defer" src="static/js/3306.d92c51d0.js"></script><script defer="defer" src="static/js/2535.4082db32.js"></script><script defer="defer" src="static/js/index.771277a0.js"></script><style>body{margin:0;overflow:hidden}</style><link rel="icon" href="favicon.ico"><script src="env.js"></script></head><body><div id="root"></div></body></html>%
```

#### Browser Configuration

The browser must be started from the terminal where you exported the Kerberos environment variables.
Once it is started, you have to configure it to allow SPNEGO authentication for the application hostname.
Check the following link for instructions on how to do this for different browsers:
https://docs.spring.io/spring-security-kerberos/reference/appendix.html#browserspnegoconfig

For Firefox, you need to set the value `.example.com` to the configuration key `network.negotiate-auth.trusted-uris`.

When testing locally and without HTTPS, you may need to adjust the `SameSite` and `Secure` attributes of the session
cookie.
That is why you will find the following configuration in the `application-kerberos.yml` file:

```yaml
server:
  servlet:
    session:
      cookie:
        same-site: lax
        secure: false
```

Ref: https://github.com/Kr0oked/spring-kerberos-demo/tree/master

