# Batch Processing Layer

## Overview

The batch processing layer consumes heartbeat messages from Kafka in batches and processes them efficiently using bulk database operations and in-memory processing. This layer is responsible for the core business logic of heartbeat processing, optimized for batch operations.

## Architecture

```
Kafka Topic (heartbeat-queue)
    ↓
HeartbeatBatchProcessor (Kafka batch consumer)
    ↓
HeartbeatBatchService.processBatch()
    ↓
1. Batch Load ServiceInstances
2. Batch Load ApplicationServices
3. Batch Load Config Hashes (grouped by service:env)
4. Process Heartbeats In-Memory
5. Bulk Upsert ServiceInstances
6. Bulk Save ApplicationServices
7. Bulk Save DriftEvents
8. Trigger Batch Bus Refresh
```

## Components

### HeartbeatBatchProcessor

**Location:** `com.example.control.application.service.infra.HeartbeatBatchProcessor`

**Responsibilities:**
- Consume batches of heartbeat messages from Kafka
- Extract `HeartbeatPayload` objects from `ConsumerRecord` list
- Delegate batch processing to `HeartbeatBatchService`
- Manually acknowledge after successful processing
- Record batch processing metrics

**Key Method:**
- `processBatch(List<ConsumerRecord<String, HeartbeatPayload>> records, Acknowledgment acknowledgment)`

**Kafka Listener Configuration:**
- **Topic:** `app.heartbeat.kafka.topic` (default: `heartbeat-queue`)
- **Concurrency:** `app.heartbeat.kafka.consumer.concurrency` (default: 10 threads)
- **Batch Mode:** Enabled (`batchListener = true`)
- **Acknowledgment:** Manual immediate (`MANUAL_IMMEDIATE`)
- **Container Factory:** `heartbeatKafkaListenerContainerFactory`

**Error Handling:**
- Exceptions are caught and logged
- Metrics record batch failures
- Error handler manages retries and DLQ routing
- Acknowledgment only occurs after successful processing

### HeartbeatBatchService

**Location:** `com.example.control.application.service.infra.HeartbeatBatchService`

**Responsibilities:**
- Orchestrate batch processing of heartbeat payloads
- Implement same business logic as `HeartbeatService` but optimized for batches
- Batch load all required data before processing
- Process heartbeats in memory
- Execute bulk database operations
- Trigger batch bus refresh for drifted instances

**Key Method:**
- `processBatch(List<HeartbeatPayload> payloads)` - Main batch processing orchestrator

**Processing Steps:**

1. **Batch Load ServiceInstances**
   - Extract all instance IDs from payloads
   - Single bulk query: `serviceInstanceRepository.findAllByIds(instanceIds)`
   - Build map: `instanceId → ServiceInstance`

2. **Batch Load ApplicationServices**
   - Extract all service names from payloads
   - Single bulk query: `applicationServiceQueryService.findByDisplayNamesMap(serviceNames)`
   - Create orphaned services for missing ones (collected for bulk save)
   - Build map: `serviceName → ApplicationService`

3. **Batch Load Config Hashes**
   - Group payloads by `serviceName:environment`
   - For each unique combination, call `configProxyService.getEffectiveConfigHash()`
   - Cache handles deduplication (same service:env in batch = 1 cache lookup)
   - Build map: `serviceName:environment → configHash`

4. **Process Heartbeats In-Memory**
   - For each payload, call `processHeartbeatInMemory()`
   - Updates ServiceInstance objects in memory
   - Collects drift events and refresh triggers
   - No database writes during this phase

5. **Bulk Save ApplicationServices**
   - Save orphaned services and environment merges
   - Single bulk operation: `applicationServiceCommandService.bulkSave()`

6. **Bulk Upsert ServiceInstances**
   - Single bulk operation: `serviceInstanceCommandService.bulkUpsert()`
   - Uses MongoDB `BulkOperations` with `UNORDERED` mode
   - Returns counts: inserted, modified, matched

7. **Bulk Save DriftEvents**
   - Save all drift events detected in batch
   - Single bulk operation: `driftEventService.bulkSave()`

8. **Trigger Batch Bus Refresh**
   - Group refresh destinations by service name
   - Trigger one refresh per unique service (Config Server broadcasts to all instances)
   - Reduces HTTP calls to Config Server

### In-Memory Processing

**Method:** `processHeartbeatInMemory()`

**Logic:**
Replicates the business logic from `HeartbeatService.processHeartbeat()` but operates entirely in memory:

1. Get or create ServiceInstance from map
2. Handle first-time heartbeat (set createdAt)
3. Sync serviceId and teamId from ApplicationService
4. Update metadata (host, port, version, lastAppliedHash, lastSeenAt)
5. Get expected hash from configHashesMap
6. Detect drift (compare expectedHash vs lastAppliedHash)
7. Handle drift cases:
   - **Case A:** New drift detected → set hasDrift=true, create drift event
   - **Case B:** Drift resolved → set hasDrift=false, clear drift state
   - **Case C:** Normal steady state → ensure HEALTHY status
   - **Case D:** Persistent drift → apply exponential backoff

**Drift Backoff Algorithm:**
- Maintains retry count and backoff power per instance
- Threshold = 2^power (1, 2, 4, 8, 16 cycles)
- Triggers refresh when threshold reached
- Increments power after refresh (max power = 4)

## Bulk Database Operations

### ServiceInstance Bulk Upsert

**Location:** `com.example.control.application.command.ServiceInstanceCommandService.bulkUpsert()`

**Implementation:**
- Uses MongoDB `BulkOperations` with `UNORDERED` mode
- For each instance:
  - Query: `_id = instanceId`
  - Update: Set all fields (serviceId, teamId, host, port, environment, version, configHash, lastAppliedHash, expectedHash, status, hasDrift, driftDetectedAt, lastSeenAt, updatedAt, metadata)
  - Upsert: `setOnInsert("createdAt", ...)`
- Executes single bulk operation
- Returns `BulkWriteResult` with counts

**Cache Eviction:**
- Programmatically evicts cache entries for processed instances
- Uses `CacheManager.getCache("service-instances")`
- Evicts by instance ID (not entire cache)

### ApplicationService Bulk Save

**Location:** `com.example.control.application.command.ApplicationServiceCommandService.bulkSave()`

**Implementation:**
- Uses MongoDB `BulkOperations` with `UNORDERED` mode
- Upserts ApplicationServices (by displayName)
- Handles orphaned service creation and environment merges

### DriftEvent Bulk Save

**Location:** `com.example.control.application.service.DriftEventService.bulkSave()`

**Implementation:**
- Bulk inserts drift events
- Only saves events for newly detected drift (not persistent drift)

## Batch Optimization Strategies

### 1. Grouping by Service:Environment

**Config Hash Loading:**
- Groups payloads by `serviceName:environment` before loading hashes
- Reduces cache lookups: 50 heartbeats from same service:env = 1 cache lookup
- Minimizes Config Server calls: 1 call per unique service:env per batch

**Example:**
```
Batch: 50 heartbeats
- service-a:dev (30 heartbeats) → 1 config hash lookup
- service-a:prod (10 heartbeats) → 1 config hash lookup
- service-b:dev (10 heartbeats) → 1 config hash lookup
Total: 3 config hash lookups (instead of 50)
```

### 2. Bulk Queries

**ServiceInstances:**
- Single query: `findAllByIds(Set<ServiceInstanceId>)`
- Returns all instances in one database round-trip

**ApplicationServices:**
- Single query: `findByDisplayNamesMap(Set<String>)`
- Returns all services in one database round-trip

### 3. In-Memory Processing

**Benefits:**
- No database writes during processing
- Fast iteration over payloads
- Can collect all changes before committing

**Trade-offs:**
- Increased memory usage (all instances in memory)
- Transaction spans entire batch (longer transaction time)

### 4. Bulk Writes

**MongoDB BulkOperations:**
- Single network round-trip for all writes
- Unordered mode allows parallel execution
- Significantly faster than individual writes

**Write Reduction:**
- Before: 50 heartbeats = 50 writes
- After: 50 heartbeats = 1 bulk write operation

## Batch Refresh Strategy

**Method:** `triggerBatchBusRefresh(Set<String> destinations)`

**Optimization:**
- Groups destinations by service name
- Triggers one refresh per unique service
- Config Server's `/busrefresh` endpoint broadcasts to all instances of that service

**Example:**
```
Destinations: ["service-a:instance-1", "service-a:instance-2", "service-b:instance-1"]
Unique services: ["service-a", "service-b"]
Refresh calls: 2 (instead of 3)
```

## Error Handling

### Individual Heartbeat Failures

**Strategy:**
- Catch exceptions per heartbeat in batch
- Log error with context (serviceName, instanceId)
- Continue processing other heartbeats
- Failed heartbeats are skipped (not saved)

**Impact:**
- Batch processing continues
- Other heartbeats in batch are processed successfully
- Metrics record individual failures

### Batch-Level Failures

**Strategy:**
- If batch processing fails, entire batch is retried
- Retry logic handled by `HeartbeatKafkaErrorHandler`
- After max retries, batch is sent to DLQ

**Impact:**
- No acknowledgment until batch succeeds
- Messages remain in Kafka for retry
- DLQ routing for persistent failures

## Metrics

**Component:** `HeartbeatMetrics`

**Metrics Recorded:**
- `heartbeat.batch.processed` - Counter for successful batches
- `heartbeat.batch.processing.time` - Timer for batch processing duration (p50, p90, p95, p99)
- `heartbeat.batch.size` - Gauge for current batch size
- `heartbeat.mongodb.writes` - Counter for MongoDB write operations
- `heartbeat.drift.detected` - Counter for drift detections

## Configuration

### Kafka Consumer Settings

```yaml
app:
  heartbeat:
    kafka:
      consumer:
        concurrency: 10  # Number of consumer threads
        batch-size: 50   # Target batch size (MAX_POLL_RECORDS)
        max-retries: 3
        retry-backoff-ms: 1000
```

### MongoDB Bulk Operations

- **Mode:** `UNORDERED` (allows parallel execution)
- **Write Concern:** Default (acknowledged)
- **Transaction:** Single transaction per batch

## Performance Characteristics

### Throughput
- **Batch Processing Rate:** 50-100 heartbeats per batch
- **Concurrent Batches:** 10 threads × batches = high parallelism
- **Total Throughput:** 5000+ heartbeats/second

### Latency
- **Batch Processing Time:** 100-500ms per batch (depending on batch size)
- **Per-Heartbeat Latency:** 2-10ms (amortized over batch)
- **Database Write Latency:** Single bulk operation vs 50 individual writes

### Resource Usage
- **Memory:** Higher during batch processing (all instances in memory)
- **CPU:** Efficient (bulk operations, in-memory processing)
- **Database:** Reduced load (bulk writes vs individual writes)

## Flow Diagram

```
┌──────────────────────┐
│ Kafka Topic          │
│ (heartbeat-queue)    │
└──────┬───────────────┘
       │ Batch of records
       ↓
┌──────────────────────┐
│ HeartbeatBatchProcessor│
│ - Extract payloads   │
│ - Delegate to service│
└──────┬───────────────┘
       │
       ↓
┌──────────────────────┐
│ HeartbeatBatchService│
│ processBatch()       │
└──────┬───────────────┘
       │
       ├─→ 1. Batch Load ServiceInstances
       │   (findAllByIds)
       │
       ├─→ 2. Batch Load ApplicationServices
       │   (findByDisplayNamesMap)
       │
       ├─→ 3. Batch Load Config Hashes
       │   (grouped by service:env)
       │
       ├─→ 4. Process In-Memory
       │   (for each payload)
       │
       ├─→ 5. Bulk Save ApplicationServices
       │   (bulkSave)
       │
       ├─→ 6. Bulk Upsert ServiceInstances
       │   (bulkUpsert)
       │
       ├─→ 7. Bulk Save DriftEvents
       │   (bulkSave)
       │
       └─→ 8. Trigger Batch Bus Refresh
           (grouped by service)
```

## Best Practices

1. **Batch Size:** Tune based on memory and latency requirements (default: 50)
2. **Concurrency:** Match consumer threads to available CPU cores
3. **Monitoring:** Track batch processing time and success rate
4. **Error Handling:** Monitor DLQ for persistent failures
5. **Cache Warming:** Pre-warm config hash cache to reduce batch processing time

## Related Documentation

- [Architecture Overview](ARCHITECTURE.md)
- [Async Ingestion](ASYNC_INGESTION.md)
- [Cache Optimization](CACHE_OPTIMIZATION.md)
- [Error Handling](ERROR_HANDLING.md)
- [Metrics](METRICS.md)

