# Drift Detection Implementation
## Technical Deep Dive

---

## Overview

Configuration drift detection compares the applied configuration hash (from service instances) with the expected hash (from Config Server) to identify mismatches in real-time.

---

## Hash Calculation

### Canonical Configuration Snapshot

**Purpose:** Create a deterministic representation of configuration for hashing

**Implementation:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/configsnapshot/ConfigSnapshotBuilder.java`

**Process:**
1. Collect all `@ConfigurationProperties` beans
2. Collect all `@Value` properties
3. Sort properties alphabetically
4. Create canonical JSON representation
5. Calculate SHA-256 hash

**Example:**
```json
{
  "app.name": "user-service",
  "app.version": "1.0.0",
  "database.url": "jdbc:postgresql://localhost:5432/users",
  "server.port": 8080
}
```

**Hash:** `sha256(canonical_json)` → `abc123def456...`

---

## Drift Detection Flow

### Single Heartbeat Processing

**Reference:** `config-control-service/src/main/java/com/example/control/application/service/infra/HeartbeatService.java:97-138`

**Steps:**
1. Receive heartbeat with `configHash`
2. Load or create `ServiceInstance`
3. Fetch expected hash from Config Server (cached)
4. Compare `appliedHash` vs `expectedHash`
5. If mismatch:
   - Create `DriftEvent` (Status: DETECTED)
   - Mark instance as DRIFT
   - Trigger refresh via Kafka
6. If match:
   - Clear drift flag (if exists)
   - Update instance status to HEALTHY

### Batch Processing

**Reference:** `config-control-service/src/main/java/com/example/control/application/service/infra/HeartbeatBatchService.java:110-175`

**Optimization:**
- Batch load config hashes (grouped by service:env)
- Process all heartbeats in memory
- Bulk save drift events

**Performance:** 5x throughput improvement

---

## Drift Event Model

**Reference:** `config-control-service/src/main/java/com/example/control/domain/model/DriftEvent.java`

**Fields:**
- `id` - Unique event identifier
- `serviceName` - Service name
- `instanceId` - Instance identifier
- `expectedHash` - Hash from Config Server
- `appliedHash` - Hash from instance
- `severity` - LOW, MEDIUM, HIGH, CRITICAL
- `status` - DETECTED, ACKNOWLEDGED, RESOLVING, RESOLVED, IGNORED
- `detectedAt` - Detection timestamp
- `resolvedAt` - Resolution timestamp

**Severity Levels:**
- **LOW**: Minor configuration differences
- **MEDIUM**: Significant differences, non-critical
- **HIGH**: Critical differences, may cause issues
- **CRITICAL**: Critical differences, immediate action required

---

## Auto-Remediation

### Refresh Orchestration

**Reference:** `config-control-service/src/main/java/com/example/control/application/service/infra/ConfigRefreshOrchestrator.java`

**Process:**
1. Drift detected
2. Publish refresh event to Kafka topic `config-refresh`
3. Event contains: `{destination: "service:instance", timestamp}`
4. SDK receives event
5. SDK fetches new configuration from Config Server
6. SDK reloads Spring context (`@RefreshScope` beans)
7. SDK sends next heartbeat with new hash
8. Drift resolved automatically

**Kafka Topic:** `config-refresh`

**Event Format:**
```json
{
  "destination": "user-service:instance-1",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

---

## Metrics

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/observability/heartbeat/HeartbeatMetrics.java`

**Metrics:**
- `heartbeat.drift.detected` - Drift detection count
- `drift.event.count{status, severity}` - Drift event statistics
- `heartbeat.processing.time` - Processing latency

**SLO Targets:**
- Detection latency: p95 < 100ms
- Auto-remediation time: < 1 minute

---

## Configuration

**Heartbeat Interval:** 30 seconds (default)

**Batch Size:** 50-100 heartbeats

**Cache TTL:** 30 minutes (config hashes)

**Reference:** `application-app.yml:126-143`

---

## Troubleshooting

### Drift Not Detected

**Possible Causes:**
1. Config hash calculation mismatch
2. Cache serving stale expected hash
3. Heartbeat not being received

**Solutions:**
1. Verify hash calculation logic
2. Clear config hash cache
3. Check heartbeat logs

### Drift Not Resolved

**Possible Causes:**
1. Refresh event not received
2. Configuration not changed in Config Server
3. SDK refresh listener not enabled

**Solutions:**
1. Check Kafka topic `config-refresh`
2. Verify Config Server has latest config
3. Verify SDK `bus.refresh.enabled=true`

---

## References

- [Heartbeat Processing Flow](../README.md#heartbeat-processing-flow)
- [Batch Processing](../README.md#batch-heartbeat-processing)
- [Config Control Service README](../../../config-control-service/README.md)

