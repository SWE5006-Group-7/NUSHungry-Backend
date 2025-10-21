# NUSHungry Helm Chart

A comprehensive Helm chart for deploying the NUSHungry microservices platform on Kubernetes.

## Features

- 8 microservices (Eureka, Config Server, Gateway, Admin, Cafeteria, Review, Media, Preference)
- 4 PostgreSQL databases (one per service)
- MongoDB for Review Service
- Redis for caching
- RabbitMQ for message queue
- NGINX Ingress with TLS support
- Configurable resources and replicas
- Persistent storage for databases and media files

## Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- PV provisioner support in the underlying infrastructure (for persistent volumes)
- Ingress controller (NGINX recommended)
- cert-manager (optional, for TLS certificates)

## Installing the Chart

### Add the Helm repository (if published)

```bash
helm repo add nushungry https://charts.nushungry.com
helm repo update
```

### Install from local chart

```bash
# From the kubernetes/charts directory
helm install nushungry ./nushungry -n nushungry --create-namespace
```

### Install with custom values

```bash
helm install nushungry ./nushungry \
  -n nushungry \
  --create-namespace \
  -f custom-values.yaml
```

## Configuration

The following table lists the configurable parameters and their default values.

### Global Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `global.namespace` | Kubernetes namespace | `nushungry` |
| `global.environment` | Environment (dev/prod) | `production` |
| `global.imageRegistry` | Global Docker registry | `docker.io` |

### Microservices

| Parameter | Description | Default |
|-----------|-------------|---------|
| `eurekaServer.enabled` | Enable Eureka Server | `true` |
| `eurekaServer.replicaCount` | Number of replicas | `2` |
| `gatewayService.enabled` | Enable Gateway Service | `true` |
| `gatewayService.replicaCount` | Number of replicas | `3` |

### Databases

| Parameter | Description | Default |
|-----------|-------------|---------|
| `postgresql.enabled` | Enable PostgreSQL databases | `true` |
| `postgresql.admin.persistence.size` | Admin DB storage size | `5Gi` |
| `mongodb.enabled` | Enable MongoDB | `true` |
| `mongodb.persistence.size` | MongoDB storage size | `10Gi` |

### Ingress

| Parameter | Description | Default |
|-----------|-------------|---------|
| `ingress.enabled` | Enable ingress | `true` |
| `ingress.className` | Ingress class | `nginx` |
| `ingress.hosts[0].host` | Primary hostname | `api.nushungry.example.com` |

### Secrets

**IMPORTANT**: Change all default passwords before deploying to production!

| Parameter | Description | Default |
|-----------|-------------|---------|
| `secrets.jwt.secret` | JWT secret key | `CHANGE_ME_*` |
| `secrets.postgresql.admin.password` | Admin DB password | `CHANGE_ME_*` |
| `secrets.mongodb.password` | MongoDB password | `CHANGE_ME_*` |
| `secrets.redis.password` | Redis password | `CHANGE_ME_*` |

## Customization Examples

### Development Environment

```yaml
# values-dev.yaml
global:
  environment: development

gatewayService:
  replicaCount: 1

cafeteriaService:
  replicaCount: 1

postgresql:
  admin:
    persistence:
      size: 1Gi
```

```bash
helm install nushungry ./nushungry -f values-dev.yaml
```

### Production Environment with Custom Domain

```yaml
# values-prod.yaml
global:
  environment: production

ingress:
  hosts:
    - host: api.mycompany.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: mycompany-tls
      hosts:
        - api.mycompany.com

secrets:
  jwt:
    secret: "my-super-secure-jwt-secret-key-256-bits-minimum"
  postgresql:
    admin:
      password: "secure-admin-db-password"
```

```bash
helm install nushungry ./nushungry -f values-prod.yaml
```

## Upgrading

```bash
helm upgrade nushungry ./nushungry -n nushungry
```

## Uninstalling

```bash
helm uninstall nushungry -n nushungry
```

**Note**: PersistentVolumeClaims are not deleted automatically. Delete them manually if needed:

```bash
kubectl delete pvc -n nushungry --all
```

## Troubleshooting

### Check pod status

```bash
kubectl get pods -n nushungry
```

### View pod logs

```bash
kubectl logs -n nushungry <pod-name>
```

### Describe pod for events

```bash
kubectl describe pod -n nushungry <pod-name>
```

### Access services internally

```bash
# Port forward to a service
kubectl port-forward -n nushungry svc/gateway-service 8080:80

# Port forward to Eureka
kubectl port-forward -n nushungry svc/eureka-server 8761:8761
```

## Values File Structure

See [values.yaml](values.yaml) for the complete configuration reference.

## License

Copyright © 2025 NUSHungry Team
