# Async Ingestion Layer

## Overview

The async ingestion layer is responsible for accepting heartbeat requests from clients and enqueueing them to Kafka for asynchronous batch processing. This layer provides fast, non-blocking ingestion with resilience patterns to ensure reliability.

## Architecture

```
Client Request
    ↓
HeartbeatController.processHeartbeat()
    ↓
[Feature Flag Check: app.heartbeat.async.enabled]
    ↓
    ├─→ true: HeartbeatIngestionService.enqueue() → Kafka → 202 Accepted
    └─→ false: HeartbeatService.processHeartbeat() → 200 OK (sync mode)
```

## Components

### HeartbeatController

**Location:** `com.example.control.api.http.controller.infra.HeartbeatController`

**Responsibilities:**
- Receive HTTP POST requests at `/api/heartbeat`
- Validate heartbeat payload (via `@Valid` annotation)
- Route to async or sync processing based on feature flag
- Return appropriate HTTP response:
  - **Async mode:** `202 Accepted` with `{"status": "accepted", "message": "Heartbeat queued"}`
  - **Sync mode:** `200 OK` with processed instance details

**Key Method:**
- `processHeartbeat(HeartbeatPayload payload)` - Main entry point

**Feature Flag:**
- `app.heartbeat.async.enabled` (default: `true`)
- Allows gradual rollout and easy rollback

### HeartbeatIngestionService

**Location:** `com.example.control.application.service.infra.HeartbeatIngestionService`

**Responsibilities:**
- Enqueue heartbeat payloads to Kafka topic
- Use `serviceName` as partition key for ordering
- Record ingestion metrics (rate, latency)
- Handle Kafka send failures gracefully

**Key Method:**
- `enqueue(HeartbeatPayload payload)` - Enqueues payload to Kafka

**Partition Key Strategy:**
- Uses `payload.getServiceName()` as Kafka message key
- Ensures all heartbeats from the same service are:
  - Processed in order (within a partition)
  - Grouped together for batch processing
  - Maintained in sequence for drift detection consistency

**Resilience:**
- Protected by `ResilientKafkaProducer` wrapper
- Circuit breaker, bulkhead, and time limiter applied
- Failures are logged and recorded in metrics
- Does not throw exceptions to avoid impacting HTTP response

### HeartbeatKafkaConfig

**Location:** `com.example.control.infrastructure.config.messaging.HeartbeatKafkaConfig`

**Responsibilities:**
- Configure Kafka producer factory for heartbeat messages
- Set up JSON serialization for `HeartbeatPayload`
- Configure producer performance settings

**Key Configuration:**
- **Serializer:** `JsonSerializer<HeartbeatPayload>` with custom `ObjectMapper`
- **Key Serializer:** `StringSerializer` (for serviceName partition key)
- **Batch Size:** 16KB (default Kafka batch size)
- **Linger:** 10ms (wait time to fill batches)
- **Compression:** gzip (reduces network overhead)
- **Acknowledgment:** `acks=1` (leader acknowledgment, balanced durability/performance)

**Producer Factory:**
- `heartbeatProducerFactory()` - Creates producer factory with JSON serialization
- `heartbeatKafkaTemplate()` - Creates KafkaTemplate bean for dependency injection

## Resilience Patterns

### ResilientKafkaProducer

**Location:** `com.example.control.infrastructure.resilience.messaging.ResilientKafkaProducer`

**Responsibilities:**
- Wrap Kafka producer send operations with resilience patterns
- Apply circuit breaker, bulkhead, and time limiter
- Handle failures gracefully

**Resilience Patterns Applied:**
1. **Circuit Breaker:** Prevents cascading failures when Kafka is unavailable
2. **Bulkhead:** Limits concurrent Kafka operations to prevent resource exhaustion
3. **Time Limiter:** Enforces maximum time for Kafka send operations

**Service Name:** `kafka-producer` (used for resilience instance lookup)

**Note:** No retry mechanism (KafkaTemplate has built-in retry)

### Error Handling

**Strategy:**
- Failures are logged with context (serviceName, instanceId)
- Metrics are recorded (`heartbeat.failed` counter)
- CompletableFuture completion handler logs success/failure
- Exceptions are re-thrown to allow controller to handle error response

**Client Impact:**
- If Kafka send fails, client receives 500 error
- If Kafka send succeeds but async processing fails later, client has already received 202 Accepted

## Metrics

**Component:** `HeartbeatMetrics`

**Metrics Recorded:**
- `heartbeat.received` - Counter for heartbeats ingested
- `heartbeat.ingestion.time` - Timer for ingestion latency (p50, p90, p95, p99)
- `heartbeat.failed` - Counter for ingestion failures

**Usage:**
- Monitor ingestion rate and latency
- Detect Kafka connectivity issues
- Track feature flag adoption (async vs sync mode)

## Flow Diagram

```
┌─────────────┐
│ SDK Client  │
└──────┬──────┘
       │ HTTP POST /api/heartbeat
       ↓
┌──────────────────────┐
│ HeartbeatController  │
│ - Validate payload   │
│ - Check feature flag │
└──────┬───────────────┘
       │
       ├─→ [async.enabled = true]
       │   ↓
       │   ┌──────────────────────────┐
       │   │ HeartbeatIngestionService │
       │   │ - Extract serviceName     │
       │   │ - Send to Kafka           │
       │   │ - Record metrics          │
       │   └──────┬────────────────────┘
       │          │
       │          ↓
       │   ┌──────────────────────────┐
       │   │ ResilientKafkaProducer    │
       │   │ - Circuit Breaker         │
       │   │ - Bulkhead                 │
       │   │ - Time Limiter             │
       │   └──────┬────────────────────┘
       │          │
       │          ↓
       │   ┌──────────────────────────┐
       │   │ Kafka Topic             │
       │   │ (heartbeat-queue)       │
       │   │ Partition Key: serviceName│
       │   └──────────────────────────┘
       │
       │   Return: 202 Accepted
       │
       └─→ [async.enabled = false]
           ↓
           ┌──────────────────────┐
           │ HeartbeatService      │
           │ (sync processing)     │
           └──────┬───────────────┘
                  │
                  Return: 200 OK
```

## Configuration

### Application Properties

```yaml
app:
  heartbeat:
    async:
      enabled: true  # Feature flag
    kafka:
      topic: heartbeat-queue
```

### Kafka Producer Settings

Configured in `HeartbeatKafkaConfig`:
- Batch size: 16KB
- Linger: 10ms
- Compression: gzip
- Acks: 1 (leader acknowledgment)

## Performance Characteristics

### Latency
- **Ingestion Time:** <10ms p99 (Kafka send operation)
- **Client Response Time:** <50ms p99 (including HTTP overhead)
- **Total Client-Facing Latency:** 10-50x faster than sync mode

### Throughput
- **Ingestion Rate:** Limited by Kafka producer throughput
- **Typical:** 10,000+ messages/second per producer
- **Bottleneck:** Network bandwidth and Kafka broker capacity

### Resource Usage
- **Memory:** Minimal (payload serialization only)
- **CPU:** Low (JSON serialization, Kafka send)
- **Network:** Compressed batches reduce bandwidth

## Error Scenarios

### Kafka Unavailable
- Circuit breaker opens after threshold failures
- Bulkhead prevents resource exhaustion
- Client receives 500 error
- Metrics record failure

### Kafka Slow Response
- Time limiter enforces maximum wait time
- Operation fails if timeout exceeded
- Client receives 500 error

### Serialization Failure
- Caught by error handler
- Logged with payload details
- Client receives 400 error (validation failure)

## Best Practices

1. **Partition Key:** Always use `serviceName` to maintain ordering
2. **Monitoring:** Track ingestion rate and latency metrics
3. **Feature Flag:** Use gradual rollout for production deployments
4. **Error Handling:** Monitor DLQ for persistent failures
5. **Capacity Planning:** Size Kafka cluster based on expected heartbeat rate

## Related Documentation

- [Architecture Overview](ARCHITECTURE.md)
- [Batch Processing](BATCH_PROCESSING.md)
- [Error Handling](ERROR_HANDLING.md)
- [Metrics](METRICS.md)

