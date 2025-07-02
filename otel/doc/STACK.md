## The OTEL stack

![alt text](./img/stack_schema.png)

### Run

We assume that you know how to use Docker and that Docker is installed on your machine,
see [https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/).<br>
Run `docker compose up -d` from the folder `otel/otel-stack`. This will start the whole environment defined in the
schema
above.

### Configuration

The `docker-compose.yaml` located under `otel/otel-stack` is the file having the configuration of all the
containers that you can see in the schema above.<br>
Note that thanks to the OpenTelemetry collector, the Spring Boot app should not be aware of all the observability tools.
The Spring Boot app will send the traces, metrics and logs to the collector on the default port `4318` with the OTLP (
OpenTelemetry protocol) over http.<br>
The configuration is telling your application where the collector is (http://localhost:4318), what we are sending to the
collector (traces, logs and metrics) and how (OTLP).

OpenTelemetry set up is done with the provided launchers `AtotiSpringBootApplication OTEL` for the IntelliJ users.

## Collector

Let's check how the collector is configured:

![alt text](./img/otel-collector.svg)

In our case the collector we are using is based on this
image:
`otel/opentelemetry-collector-contrib` [https://github.com/open-telemetry/opentelemetry-collector-contrib](https://github.com/open-telemetry/opentelemetry-collector-contrib),
the configuration of the collector is located in this file `otel/otel-stack/collector-config-local.yaml`.<br>
As you say in the schema above, we need to define the `receivers`, the source of the collector, in my case this is the
Spring Boot app which sends the traces, logs and metrics with OTLP over gRCP.<br>
The received traces, logs and metrics are exported to different tools:

- The traces are exported to Tempo (with OTLP).
- The logs are exported to Loki (push to an endpoint).
- We DO NOT push the metrics to Prometheus, the collector provides an endpoint to Prometheus and every 15 seconds
  Prometheus scrapes the metrics.

Note that we can have more than one application sending information to the collector.<br>
You can use any tools compatible with OpenTelemetry, you need for that to amend the `collector-config-local.yaml` file.

### Traces

[Traces](./TRACES.md)

### Metrics

[Metrics](./METRICS.md)

### Logs

[Logs](./LOGS.md)

## Links

- [https://opentelemetry.io/docs/collector/](https://opentelemetry.io/docs/collector/)
- [https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#exporters](https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#exporters)
- [https://github.com/open-telemetry/opentelemetry-java-examples](https://github.com/open-telemetry/opentelemetry-java-examples)
- [https://opentelemetry.io/ecosystem/vendors/](https://opentelemetry.io/ecosystem/vendors/)
- [https://opentelemetry.io/docs/concepts/sdk-configuration/otlp-exporter-configuration/](https://opentelemetry.io/docs/concepts/sdk-configuration/otlp-exporter-configuration/)
- [https://opentelemetry.io/docs/collector/scaling/](https://opentelemetry.io/docs/collector/scaling/)

