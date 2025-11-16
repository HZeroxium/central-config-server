# Error Handling and Resilience

## Overview

The heartbeat processing system implements comprehensive error handling with retry logic, exponential backoff, dead letter queue (DLQ) routing, and resilience patterns to ensure reliability and fault tolerance.

## Error Handling Layers

### 1. Ingestion Layer Errors

**Location:** `HeartbeatIngestionService.enqueue()`

**Error Scenarios:**
- Kafka connectivity issues
- Serialization failures
- Network timeouts
- Circuit breaker open

**Handling Strategy:**
- Logs error with context (serviceName, instanceId)
- Records metrics (`heartbeat.failed`)
- Re-throws exception to allow controller to return error response
- Client receives 500 error (does not receive 202 Accepted)

**Resilience Patterns:**
- **Circuit Breaker:** Prevents cascading failures when Kafka is unavailable
- **Bulkhead:** Limits concurrent Kafka operations
- **Time Limiter:** Enforces maximum time for Kafka send

**Recovery:**
- Automatic retry via Kafka producer retry mechanism
- Circuit breaker recovers after half-open state
- Bulkhead releases resources after operation completes

### 2. Batch Processing Errors

**Location:** `HeartbeatBatchProcessor.processBatch()`

**Error Scenarios:**
- Batch processing failures
- Database connection issues
- Config Server unavailability
- Deserialization errors

**Handling Strategy:**
- Catches exceptions at batch level
- Logs error with batch size
- Records metrics (`heartbeat.batch.failed`)
- Re-throws exception to trigger error handler
- No acknowledgment (messages remain in Kafka)

**Recovery:**
- Retry via `HeartbeatKafkaErrorHandler`
- Exponential backoff between retries
- DLQ routing after max retries

### 3. Individual Heartbeat Errors

**Location:** `HeartbeatBatchService.processHeartbeatInMemory()`

**Error Scenarios:**
- Invalid payload data
- Missing ApplicationService
- Config hash retrieval failure

**Handling Strategy:**
- Catches exceptions per heartbeat
- Logs error with context (serviceName, instanceId)
- Continues processing other heartbeats in batch
- Failed heartbeat is skipped (not saved to database)

**Impact:**
- Batch processing continues
- Other heartbeats in batch are processed successfully
- Failed heartbeat is lost (no retry for individual failures)

## HeartbeatKafkaErrorHandler

**Location:** `com.example.control.infrastructure.config.messaging.HeartbeatKafkaErrorHandler`

**Responsibilities:**
- Handle batch processing failures
- Implement retry logic with exponential backoff
- Route failed batches to dead letter queue
- Record error metrics

### Retry Logic

**Configuration:**
- **Max Retries:** `app.heartbeat.kafka.consumer.max-retries` (default: 3)
- **Backoff:** Exponential (1s, 2s, 4s, ...)

**Implementation:**
1. **Retry Attempt:** Increment retry counter
2. **Backoff Calculation:** `backoffMs = 2^(retry-1) * 1000` (1s, 2s, 4s, ...)
3. **Sleep:** Wait for backoff duration
4. **Re-throw:** Re-throw exception to trigger retry
5. **Max Retries:** After max retries, send to DLQ

**Example:**
```
Attempt 1: Fail → wait 1s → retry
Attempt 2: Fail → wait 2s → retry
Attempt 3: Fail → wait 4s → retry
Attempt 4: Fail → send to DLQ
```

### Dead Letter Queue (DLQ)

**Configuration:**
- **Topic:** `app.heartbeat.kafka.dlq.topic` (default: `heartbeat-queue-dlq`)
- **Enabled:** `app.heartbeat.kafka.dlq.enabled` (default: `true`)

**Routing:**
- After max retries, batch is sent to DLQ
- Each record in batch is sent individually to DLQ
- Original partition key (serviceName) is preserved
- Metrics record DLQ routing (`heartbeat.dlq.sent`)

**DLQ Processing:**
- DLQ messages can be manually inspected
- Can be reprocessed after root cause is fixed
- Supports manual replay or automated retry (future enhancement)

### Error Handler Flow

```
Batch Processing Failure
    ↓
HeartbeatKafkaErrorHandler.handleRemaining()
    ↓
Check retry count
    ↓
    ├─→ retryCount <= maxRetries
    │   ↓
    │   Calculate exponential backoff
    │   ↓
    │   Sleep (backoff duration)
    │   ↓
    │   Re-throw exception (triggers retry)
    │
    └─→ retryCount > maxRetries
        ↓
        Send to DLQ
        ↓
        Record metrics
        ↓
        Reset retry count
```

## Resilience Patterns

### ResilientKafkaProducer

**Location:** `com.example.control.infrastructure.resilience.messaging.ResilientKafkaProducer`

**Resilience Patterns Applied:**
1. **Circuit Breaker**
   - Opens after failure threshold
   - Half-open state for recovery testing
   - Prevents cascading failures

2. **Bulkhead**
   - Limits concurrent Kafka operations
   - Prevents resource exhaustion
   - Isolates failures

3. **Time Limiter**
   - Enforces maximum time for Kafka send
   - Fails fast on timeout
   - Prevents hanging operations

**Service Name:** `kafka-producer` (used for resilience instance lookup)

**Configuration:**
- Resilience settings in `application-resilience.yml`
- Per-service configuration for circuit breaker, bulkhead, time limiter

### Resilience Configuration

**Circuit Breaker:**
- Failure rate threshold: 50%
- Wait duration in open state: 60s
- Sliding window size: 10 requests

**Bulkhead:**
- Max concurrent calls: 50
- Max wait duration: 1s

**Time Limiter:**
- Timeout duration: 5s

## Error Scenarios and Handling

### Scenario 1: Kafka Unavailable

**Symptoms:**
- Circuit breaker opens
- Ingestion failures
- Client receives 500 errors

**Handling:**
- Circuit breaker prevents further attempts
- Bulkhead limits resource usage
- Automatic recovery when Kafka is available

**Recovery:**
- Circuit breaker transitions to half-open
- Test requests sent to verify recovery
- Circuit closes when successful

### Scenario 2: Config Server Unavailable

**Symptoms:**
- Config hash retrieval failures
- Batch processing fails
- Retries triggered

**Handling:**
- Individual heartbeat failures logged
- Batch processing continues for other heartbeats
- Failed heartbeats skipped (not saved)

**Recovery:**
- Retry via error handler
- Exponential backoff prevents overwhelming Config Server
- DLQ routing after max retries

### Scenario 3: Database Connection Issues

**Symptoms:**
- Bulk write operations fail
- Batch processing fails
- Retries triggered

**Handling:**
- Batch-level error handling
- No acknowledgment (messages remain in Kafka)
- Retry with exponential backoff

**Recovery:**
- Automatic retry when database is available
- DLQ routing for persistent failures
- Manual intervention may be required

### Scenario 4: Deserialization Errors

**Symptoms:**
- Invalid message format
- JSON parsing failures
- Batch processing fails

**Handling:**
- Error handler catches deserialization errors
- Logs error with message details
- Routes to DLQ (cannot be retried)

**Recovery:**
- Manual inspection of DLQ messages
- Fix root cause (client-side serialization issue)
- Manual replay after fix

## Metrics and Monitoring

### Error Metrics

**Component:** `HeartbeatMetrics`

**Metrics:**
- `heartbeat.failed` - Counter for ingestion failures
- `heartbeat.batch.failed` - Counter for batch processing failures
- `heartbeat.dlq.sent` - Counter for DLQ routing

### Monitoring Recommendations

1. **Alert on High Failure Rate:**
   - `heartbeat.failed` rate > 1% of total
   - Indicates Kafka connectivity issues

2. **Alert on Batch Failures:**
   - `heartbeat.batch.failed` rate > 5% of batches
   - Indicates processing issues

3. **Monitor DLQ Size:**
   - Track `heartbeat.dlq.sent` counter
   - Alert if DLQ size grows continuously

4. **Circuit Breaker Status:**
   - Monitor circuit breaker state
   - Alert when circuit is open

## Best Practices

1. **Retry Strategy:**
   - Use exponential backoff to prevent overwhelming services
   - Set max retries based on error type (transient vs permanent)

2. **DLQ Management:**
   - Monitor DLQ size regularly
   - Investigate root causes of DLQ routing
   - Implement manual replay mechanism

3. **Error Logging:**
   - Include context (serviceName, instanceId, batch size)
   - Use structured logging for easy analysis
   - Log at appropriate levels (ERROR for failures, WARN for retries)

4. **Resilience Configuration:**
   - Tune circuit breaker thresholds based on service characteristics
   - Adjust bulkhead limits based on resource capacity
   - Set timeouts based on expected operation duration

5. **Monitoring:**
   - Track error rates and trends
   - Set up alerts for critical failures
   - Review DLQ regularly for patterns

## Configuration Reference

### Error Handling Configuration

```yaml
app:
  heartbeat:
    kafka:
      consumer:
        max-retries: 3
        retry-backoff-ms: 1000
      dlq:
        topic: heartbeat-queue-dlq
        enabled: true
```

### Resilience Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      kafka-producer:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        sliding-window-size: 10
  bulkhead:
    instances:
      kafka-producer:
        max-concurrent-calls: 50
        max-wait-duration: 1s
  timelimiter:
    instances:
      kafka-producer:
        timeout-duration: 5s
```

## Related Documentation

- [Architecture Overview](ARCHITECTURE.md)
- [Async Ingestion](ASYNC_INGESTION.md)
- [Batch Processing](BATCH_PROCESSING.md)
- [Metrics](METRICS.md)

