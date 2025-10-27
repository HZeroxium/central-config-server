# Config Control Service

A centralized configuration control and monitoring service that provides drift detection, heartbeat tracking, and configuration management for microservices.

## Overview

The Config Control Service acts as an aggregator and policy enforcement layer for centralized configuration management. It works in conjunction with:

- **Config Server**: Source of truth for configuration
- **Consul**: Service discovery and health checks
- **Kafka**: Event bus for configuration refresh notifications
- **MongoDB**: Metadata storage for service instances and drift events
- **Redis**: Caching layer for improved performance

## Features

### 1. Heartbeat Tracking

- Receives periodic heartbeat from service instances via ZCM SDK
- Tracks service instance metadata (host, port, version, etc.)
- Monitors last-seen timestamp with TTL expiration

### 2. Configuration Drift Detection

- Compares applied config hash vs expected hash from Config Server
- Automatically detects configuration drift
- Creates drift events with severity levels
- Auto-resolves drift when instances sync

### 6. Automatic Drift Remediation

- Automatically triggers `/actuator/busrefresh` when drift is detected
- Publishes targeted refresh events to Kafka topic `config-refresh`
- Instances receive refresh and reconcile configuration within next ping cycle
- Drift resolution is tracked and logged

### 3. Service Discovery Integration

- Proxies requests to Consul for service discovery
- Lists all registered services and their instances
- Provides instance health status from Consul

### 4. Configuration Refresh Orchestration

- Publishes refresh events to Kafka topic
- Allows targeted refresh by service or instance
- Integrates with Spring Cloud Bus

### 5. Administrative Operations

- Cache management (clear specific or all caches)
- Drift statistics and reporting
- Configuration diff comparison

## API Endpoints

### Heartbeat

```
POST /api/heartbeat
```

Receives heartbeat from service instances with configuration hash for drift detection.

**Request Body:**

```json
{
  "serviceName": "user-watcher-service",
  "instanceId": "watcher-abc123",
  "configHash": "sha256-hash",
  "host": "10.0.1.5",
  "port": 8080,
  "environment": "development",
  "version": "1.0.0",
  "metadata": {
    "zone": "us-west-1"
  }
}
```

### Services

```
GET /api/services
```

List all registered services.

```
GET /api/services/{serviceName}
```

Get details about a specific service and its instances.

```
GET /api/services/{serviceName}/instances?passing=true
```

List healthy instances for a service from Consul.

### Drift Detection

```
GET /api/drift?serviceName={name}&unresolvedOnly=true
```

List configuration drift events.

```
GET /api/drift/instances?serviceName={name}
```

List all service instances currently experiencing configuration drift.

```
GET /api/drift/{serviceName}/diff?profile={profile}&appliedHash={hash}
```

Compare expected config vs applied config hash.

```
GET /api/drift/statistics
```

Get drift statistics (total events, unresolved, affected instances).

### Admin Operations

```
POST /api/admin/refresh?destination={pattern}
```

Trigger configuration refresh. Destination pattern examples:

- `user-watcher-service:**` - all instances of the service
- `user-watcher-service:instance-id` - specific instance
- Empty - all services

```
POST /api/admin/cache/clear?cacheName={name}
```

Clear cache. If cacheName is not specified, clears all caches.

## Configuration

The service is configured via environment variables:

```yaml
SERVER_PORT=8889
CONSUL_HOST=consul
CONSUL_PORT=8500
MONGODB_URI=mongodb://mongodb:27017/config-control
REDIS_HOST=redis
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENVIRONMENT=development
```

## Architecture

### Hexagonal Architecture Layers

```
┌─────────────────────────────────────────┐
│           API Layer (REST)              │
│   - HeartbeatController                 │
│   - ServiceDiscoveryController          │
│   - DriftController                     │
│   - AdminController                     │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│        Application Layer                │
│   - HeartbeatService                    │
│   - ConfigProxyService                  │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│          Domain Layer                   │
│   - ServiceInstance                     │
│   - DriftEvent                          │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│       Infrastructure Layer              │
│   - MongoDB Repositories                │
│   - Redis Cache                         │
│   - Consul Discovery Client             │
│   - Kafka Producer                      │
└─────────────────────────────────────────┘
```

### Data Model

**ServiceInstance**

- serviceName, instanceId
- host, port, environment, version
- configHash, lastAppliedHash
- status (HEALTHY, UNHEALTHY, DRIFT, UNKNOWN)
- lastSeenAt, createdAt, updatedAt
- metadata
- hasDrift, driftDetectedAt

**DriftEvent**

- serviceName, instanceId
- expectedHash, appliedHash
- severity (LOW, MEDIUM, HIGH, CRITICAL)
- status (DETECTED, ACKNOWLEDGED, RESOLVING, RESOLVED, IGNORED)
- detectedAt, resolvedAt
- detectedBy, resolvedBy

## Integration with ZCM SDK

Services using the ZCM SDK automatically:

1. **Register with Consul** on startup with TTL health checks
2. **Send periodic heartbeat** to Config Control Service (default: 30s)
   - Includes current config hash for drift detection
3. **Listen for refresh events** on Kafka topic `config-refresh`
4. **Auto-refresh configuration** when drift is detected or admin triggers refresh

### SDK Configuration Example

```yaml
zcm:
  sdk:
    service:
      name: user-watcher-service
    control:
      url: http://config-control-service:8889
    ping:
      enabled: true
      fixed-delay: 30000
    bus:
      refresh:
        enabled: true
        topic: config-refresh
```

## Deployment

### Docker

```bash
# Build
./gradlew :config-control-service:build -x test

# Build Docker image
docker build -t hzeroxium/config-control-service:latest config-control-service/

# Run
docker run -p 8889:8889 \
  -e CONSUL_HOST=consul \
  -e MONGODB_URI=mongodb://mongodb:27017/config-control \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  hzeroxium/config-control-service:latest
```

### Docker Compose

```yaml
config-control-service:
  image: hzeroxium/config-control-service:latest
  ports:
    - "8889:8889"
  environment:
    - CONSUL_HOST=consul
    - MONGODB_URI=mongodb://mongodb:27017/config-control
    - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
  depends_on:
    - config-server
    - consul
    - mongodb
    - kafka
    - redis
```

## Monitoring

### Health Check

```bash
curl http://localhost:8889/actuator/health
```

### Metrics

Prometheus metrics available at:

```bash
curl http://localhost:8889/actuator/prometheus
```

Key metrics:

- `api_heartbeat_process` - Heartbeat processing duration
- `api_drift_list` - Drift event query duration
- `api_admin_refresh` - Refresh trigger duration
- Service instance counts
- Drift event counts by status/severity

### OpenAPI/Swagger

API documentation available at:

```
http://localhost:8889/swagger-ui.html
```

## Development

### Build

```bash
./gradlew :config-control-service:build
```

### Run locally

```bash
./gradlew :config-control-service:bootRun
```

### Run tests

```bash
./gradlew :config-control-service:test
```

## Troubleshooting

### Service instances not appearing

- Check Consul is accessible at configured host/port
- Verify services are registering with Consul
- Check ZCM SDK configuration in client services

### Drift not detected

- Verify heartbeat is being received (check logs)
- Ensure Config Server is accessible
- Check config hash calculation logic

### Refresh not working

- Verify Kafka is accessible
- Check Kafka topic `config-refresh` exists
- Ensure client services have SDK configured with `bus.refresh.enabled=true`

## References

- [Spring Cloud Config](https://cloud.spring.io/spring-cloud-config/)
- [Spring Cloud Bus](https://docs.spring.io/spring-cloud-bus/)
- [Consul Service Discovery](https://developer.hashicorp.com/consul/docs/discovery)
- [ZCM SDK Documentation](../zcm-spring-sdk-starter/README.md)
