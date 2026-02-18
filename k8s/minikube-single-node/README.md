# Deploying Atoti on Minikube with Helm

A minimal Helm chart for deploying a single-node Atoti application to Minikube.

## Prerequisites

- [Minikube](https://minikube.sigs.k8s.io/docs/start/)
- [Helm](https://helm.sh/docs/intro/install/) (v3+)
- [Docker](https://docs.docker.com/get-docker/)
- An Atoti application with a `Dockerfile`
- A valid ActiveViam license file

## 1. Start Minikube

```bash
minikube start
```

## 2. Build Your Docker Image Inside Minikube

Minikube runs its own Docker daemon. To avoid pushing images to a registry,
point your local shell at Minikube's Docker daemon and build directly there:

```bash
# Point your shell to minikube's Docker daemon
eval $(minikube docker-env)

# Build your image (run this from your project root containing the Dockerfile)
docker build -t atoti-application:latest .
```

> **Important:** You must run `eval $(minikube docker-env)` in every new
> terminal session before building. Images built against your host daemon are
> not visible to Minikube.

The chart defaults to `imagePullPolicy: IfNotPresent` so Kubernetes will use
the locally built image instead of trying to pull from a remote registry.

## 3. Configure Your License

Base64-encode your license file and paste the result into `values.yaml`. The
chart stores it in a Kubernetes Secret and injects it as the `ATOTI_LICENSE`
environment variable on the pod.

```bash
# Encode your license file
base64 -w 0 /path/to/your/license.json
```

Then edit `atoti-application/values.yaml`:

```yaml
license: "eyJmaWVsZCI6InZhbHVlIiwiLi4uIjoiYmFzZTY0IG91dHB1dCBoZXJlIn0="
```

Or pass it at install time:

```bash
helm install my-atoti ./atoti-application \
  --set license="$(base64 -w 0 /path/to/your/license.json)"
```

Your Atoti application should read the license from the `ATOTI_LICENSE`
environment variable.

## 4. Install the Chart

```bash
helm install my-atoti ./atoti-application
```

To install into a specific namespace:

```bash
helm install my-atoti ./atoti-application --namespace atoti --create-namespace
```

## 5. Access the Application

Since the service type is `NodePort`, use Minikube to open it:

```bash
minikube service my-atoti-atoti-application
```

This opens a browser to the application. Alternatively, get the URL manually:

```bash
minikube service my-atoti-atoti-application --url
```

## 6. Customizing Values

You can override any value at install time. Common overrides:

```bash
helm install my-atoti ./atoti-application \
  --set image.repository=my-custom-image \
  --set image.tag=1.0.0 \
  --set resources.limits.memory=8Gi
```

### All Configurable Values

| Parameter | Description | Default |
|---|---|---|
| `image.repository` | Docker image name | `atoti-application` |
| `image.tag` | Docker image tag | `latest` |
| `image.pullPolicy` | Image pull policy | `IfNotPresent` |
| `license` | Base64-encoded Atoti license (set as `ATOTI_LICENSE` env var) | `""` |
| `service.type` | Kubernetes service type | `NodePort` |
| `service.port` | Service and container port | `8080` |
| `resources.requests.memory` | Memory request | `1Gi` |
| `resources.requests.cpu` | CPU request | `500m` |
| `resources.limits.memory` | Memory limit | `4Gi` |
| `resources.limits.cpu` | CPU limit | `2` |

## Useful Commands

```bash
# Check deployment status
helm status my-atoti

# View pod logs
kubectl logs -l app.kubernetes.io/name=atoti-application

# Upgrade after changing values
helm upgrade my-atoti ./atoti-application

# Uninstall
helm uninstall my-atoti

# Stop minikube
minikube stop
```

## Troubleshooting

**Pod stuck in `ImagePullBackOff`:**
You likely built the image against your host Docker daemon instead of
Minikube's. Run `eval $(minikube docker-env)` and rebuild.

**Pod stuck in `CrashLoopBackOff`:**
Check the logs with `kubectl logs <pod-name>`. Common causes are a missing or
invalid license, or the application port not matching `service.port`.

**Cannot connect to the service:**
Make sure you are using `minikube service` to access NodePort services, as
Minikube runs in a VM/container and `localhost` won't work directly.
