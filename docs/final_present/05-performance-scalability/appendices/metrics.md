# Metrics Reference
## Prometheus Metrics and SLO/SLI Tracking

---

## Overview

The system exports comprehensive metrics to Prometheus for monitoring, alerting, and SLO/SLI tracking.

---

## Custom Business Metrics

### Heartbeat Metrics

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/observability/heartbeat/HeartbeatMetrics.java`

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `heartbeat.received` | Counter | Total heartbeats received | - |
| `heartbeat.processed` | Counter | Successfully processed heartbeats | - |
| `heartbeat.failed` | Counter | Failed heartbeats | - |
| `heartbeat.drift.detected` | Counter | Drift detection count | - |
| `heartbeat.processing.time` | Timer | Processing latency | p50, p95, p99 |
| `heartbeat.batch.processing.time` | Timer | Batch processing latency | p50, p95, p99 |
| `heartbeat.ingestion.time` | Timer | Ingestion (enqueue) latency | p50, p95, p99 |
| `heartbeat.queue.size` | Gauge | Kafka queue depth | - |
| `heartbeat.batch.size` | Gauge | Current batch size | - |

**SLO Targets:**
- Processing latency: p95 < 100ms
- Batch processing: p95 < 500ms
- Ingestion latency: p95 < 10ms

---

### Drift Event Metrics

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `drift.event.count` | Counter | Total drift events | status, severity |
| `drift.event.unresolved` | Gauge | Unresolved drift events | - |
| `drift.instance.count` | Gauge | Instances with drift | - |

**Labels:**
- `status`: DETECTED, ACKNOWLEDGED, RESOLVING, RESOLVED, IGNORED
- `severity`: LOW, MEDIUM, HIGH, CRITICAL

---

### API Metrics

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `api.heartbeat.process` | Timer | Heartbeat endpoint latency | p50, p95, p99 |
| `api.drift.list` | Timer | Drift list query latency | - |
| `api.admin.refresh` | Timer | Refresh trigger latency | - |

**SLO Targets:**
- API latency: p95 < 200ms

---

## Resilience4j Metrics

### Circuit Breaker Metrics

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `resilience4j.circuitbreaker.calls` | Counter | Circuit breaker calls | name, kind (successful, failed, not_permitted) |
| `resilience4j.circuitbreaker.state` | Gauge | Circuit breaker state | name (0=CLOSED, 1=HALF_OPEN, 2=OPEN) |
| `resilience4j.circuitbreaker.failure_rate` | Gauge | Failure rate percentage | name |

**Instances:**
- `configserver`
- `consul`
- `keycloak`
- `mongodb`
- `kafka-producer`
- `email`
- `cache-redis`

---

### Retry Metrics

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `resilience4j.retry.calls` | Counter | Retry attempts | name, kind (successful_with_retry, successful_without_retry, failed) |
| `retry.budget.allowed` | Counter | Retries allowed | service |
| `retry.budget.rejected` | Counter | Retries rejected | service |
| `retry.budget.utilization` | Gauge | Retry budget usage % | service |

---

### Bulkhead Metrics

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `resilience4j.bulkhead.available.concurrent.calls` | Gauge | Available concurrent calls | name |
| `resilience4j.bulkhead.max.allowed.concurrent.calls` | Gauge | Max allowed concurrent calls | name |

---

### Rate Limiter Metrics

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `resilience4j.ratelimiter.available.permissions` | Gauge | Available permissions | name |
| `resilience4j.ratelimiter.waiting.threads` | Gauge | Waiting threads | name |
| `ratelimit.allowed` | Counter | Allowed requests | endpoint |
| `ratelimit.rejected` | Counter | Rejected requests | endpoint |

---

## Infrastructure Metrics

### HTTP Server Metrics

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `http.server.requests` | Timer | HTTP request latency | method, uri, status | p50, p95, p99 |
| `http.server.requests.active` | Gauge | Active HTTP requests | - |

**SLO Targets:**
- p95 < 200ms
- p99 < 500ms

---

### Database Metrics

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `mongodb.operations` | Timer | MongoDB operation latency | operation |
| `mongodb.connections.active` | Gauge | Active connections | - |

---

### Cache Metrics

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `cache.gets` | Counter | Cache get operations | cache, result (hit, miss) |
| `cache.evictions` | Counter | Cache evictions | cache |
| `cache.size` | Gauge | Cache size | cache |

**Target:** Cache hit rate > 80%

---

## SLO/SLI Configuration

**Reference:** `application-observability.yml:32-50`

**Histogram Configuration:**
```yaml
management:
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
        heartbeat.process: true
        heartbeat.batch.processing.time: true
        heartbeat.ingestion.time: true
      percentiles:
        http.server.requests: 0.5, 0.9, 0.95, 0.99
        heartbeat.process: 0.5, 0.9, 0.95, 0.99
      slo:
        http.server.requests: 50ms, 100ms, 200ms, 500ms, 1s, 2s, 5s
        heartbeat.process: 10ms, 50ms, 100ms, 200ms, 500ms, 1s
```

---

## Alerting Rules

### Critical Alerts

1. **Circuit Breaker OPEN (Critical Services)**
   ```promql
   resilience4j_circuitbreaker_state{name=~"configserver|consul"} == 2
   ```

2. **High API Latency**
   ```promql
   histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 0.5
   ```

3. **High Drift Detection Rate**
   ```promql
   rate(heartbeat_drift_detected_total[5m]) > 10
   ```

### Warning Alerts

1. **Circuit Breaker HALF_OPEN**
   ```promql
   resilience4j_circuitbreaker_state == 1
   ```

2. **Low Cache Hit Rate**
   ```promql
   rate(cache_gets_total{result="hit"}[5m]) / rate(cache_gets_total[5m]) < 0.7
   ```

3. **High Retry Budget Utilization**
   ```promql
   retry_budget_utilization{service="configserver"} > 15
   ```

---

## Metrics Export

**Endpoint:** `/actuator/prometheus`

**Format:** Prometheus text format

**Scraping:** Prometheus scrapes every 15 seconds (default)

**Configuration:** `application-observability.yml`

---

## References

- [Performance Metrics](../README.md#key-performance-metrics)
- [Observability Configuration](../../../config-control-service/src/main/resources/application-observability.yml)
- [Heartbeat Metrics](../../../config-control-service/src/main/java/com/example/control/infrastructure/observability/heartbeat/HeartbeatMetrics.java)

