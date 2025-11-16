# Metrics and Observability

## Overview

The heartbeat processing system provides comprehensive metrics and observability through Micrometer, enabling monitoring, alerting, and performance analysis. All metrics are exported to Prometheus and can be visualized in Grafana.

## HeartbeatMetrics Component

**Location:** `com.example.control.infrastructure.observability.heartbeat.HeartbeatMetrics`

**Responsibilities:**
- Collect and record all heartbeat-related metrics
- Provide counters, timers, and gauges for monitoring
- Export metrics to Prometheus via Micrometer

## Metric Categories

### 1. Ingestion Metrics

**Metrics:**
- `heartbeat.received` (Counter)
  - Description: Total number of heartbeats received (ingested)
  - Tags: None
  - Use Case: Monitor ingestion rate, track feature flag adoption

- `heartbeat.ingestion.time` (Timer)
  - Description: Time taken to ingest (enqueue) a heartbeat
  - Percentiles: p50, p90, p95, p99
  - Histogram: Enabled
  - Use Case: Monitor ingestion latency, detect Kafka performance issues

**Recording:**
- `recordReceived()` - Increments counter when heartbeat is received
- `recordIngestionTime(Duration)` - Records ingestion latency

### 2. Processing Metrics

**Metrics:**
- `heartbeat.processed` (Counter)
  - Description: Total number of heartbeats successfully processed
  - Tags: None
  - Use Case: Track processing success rate

- `heartbeat.processing.time` (Timer)
  - Description: Time taken to process a single heartbeat (sync mode)
  - Percentiles: p50, p90, p95, p99
  - Histogram: Enabled
  - Use Case: Monitor sync processing performance

**Recording:**
- `recordProcessed(Duration)` - Records successful processing with latency

### 3. Batch Processing Metrics

**Metrics:**
- `heartbeat.batch.processed` (Counter)
  - Description: Total number of batches successfully processed
  - Tags: None
  - Use Case: Track batch processing throughput

- `heartbeat.batch.processing.time` (Timer)
  - Description: Time taken to process a batch of heartbeats
  - Percentiles: p50, p90, p95, p99
  - Histogram: Enabled
  - Use Case: Monitor batch processing performance, optimize batch size

- `heartbeat.batch.size` (Gauge)
  - Description: Current batch processing size
  - Tags: None
  - Use Case: Monitor actual batch sizes, detect batch size variations

**Recording:**
- `recordBatchProcessed(Duration, int)` - Records batch processing with latency and size

### 4. Error Metrics

**Metrics:**
- `heartbeat.failed` (Counter)
  - Description: Total number of heartbeats that failed processing
  - Tags: None
  - Use Case: Monitor error rate, detect ingestion issues

- `heartbeat.batch.failed` (Counter)
  - Description: Total number of batches that failed processing
  - Tags: None
  - Use Case: Monitor batch processing failures, detect systemic issues

- `heartbeat.dlq.sent` (Counter)
  - Description: Total number of heartbeats sent to dead letter queue
  - Tags: None
  - Use Case: Monitor DLQ routing, detect persistent failures

**Recording:**
- `recordFailed()` - Records ingestion failure
- `recordBatchFailed()` - Records batch processing failure
- `recordDlqSent()` - Records DLQ routing

### 5. Business Metrics

**Metrics:**
- `heartbeat.drift.detected` (Counter)
  - Description: Total number of configuration drift detections
  - Tags: None
  - Use Case: Monitor drift detection rate, track config stability

- `heartbeat.mongodb.writes` (Counter)
  - Description: Total number of MongoDB write operations for heartbeats
  - Tags: None
  - Use Case: Monitor database write load, track bulk operation efficiency

**Recording:**
- `recordDriftDetected()` - Records drift detection
- `recordMongodbWrites(long)` - Records MongoDB write operations (count for bulk operations)

### 6. Queue Metrics

**Metrics:**
- `heartbeat.queue.size` (Gauge)
  - Description: Current Kafka queue depth (estimated)
  - Tags: None
  - Use Case: Monitor queue backlog, detect processing lag

**Recording:**
- `updateQueueSize(long)` - Updates estimated queue size (not currently used, placeholder)

## Prometheus Export

### Metric Names

All metrics are prefixed with `heartbeat.` and follow Prometheus naming conventions:
- Counters: `heartbeat.*` (e.g., `heartbeat.received`)
- Timers: `heartbeat.*.time` (e.g., `heartbeat.ingestion.time`)
- Gauges: `heartbeat.*.size` (e.g., `heartbeat.batch.size`)

### Metric Format

**Counter Example:**
```
heartbeat_received_total 12345
```

**Timer Example:**
```
heartbeat_ingestion_time_seconds_count 12345
heartbeat_ingestion_time_seconds_sum 123.45
heartbeat_ingestion_time_seconds_max 0.05
heartbeat_ingestion_time_seconds{quantile="0.5"} 0.01
heartbeat_ingestion_time_seconds{quantile="0.9"} 0.02
heartbeat_ingestion_time_seconds{quantile="0.95"} 0.03
heartbeat_ingestion_time_seconds{quantile="0.99"} 0.05
```

**Gauge Example:**
```
heartbeat_batch_size 50
```

## Key Performance Indicators (KPIs)

### Throughput

**Metrics:**
- `heartbeat.received` rate (heartbeats/second)
- `heartbeat.batch.processed` rate (batches/second)

**Calculation:**
```
rate(heartbeat_received_total[5m])  # Ingestion rate
rate(heartbeat_batch_processed_total[5m])  # Batch processing rate
```

**Target:**
- Ingestion rate: 5000+ heartbeats/second
- Batch processing rate: 100+ batches/second (with batch size 50)

### Latency

**Metrics:**
- `heartbeat.ingestion.time` (p50, p90, p95, p99)
- `heartbeat.batch.processing.time` (p50, p90, p95, p99)

**Target:**
- Ingestion latency: <50ms p99
- Batch processing latency: <500ms p99

### Error Rate

**Metrics:**
- `heartbeat.failed` rate
- `heartbeat.batch.failed` rate
- `heartbeat.dlq.sent` rate

**Calculation:**
```
rate(heartbeat_failed_total[5m]) / rate(heartbeat_received_total[5m])  # Error rate
rate(heartbeat_batch_failed_total[5m]) / rate(heartbeat_batch_processed_total[5m])  # Batch failure rate
```

**Target:**
- Error rate: <1%
- Batch failure rate: <5%

### Efficiency

**Metrics:**
- `heartbeat.mongodb.writes` (write operations)
- `heartbeat.received` (total heartbeats)

**Calculation:**
```
rate(heartbeat_mongodb_writes_total[5m]) / rate(heartbeat_received_total[5m])  # Writes per heartbeat
```

**Target:**
- Writes per heartbeat: <0.1 (10 heartbeats per write, due to batching)

## Grafana Dashboards

### Recommended Panels

1. **Throughput Panel**
   - Ingestion rate (line graph)
   - Batch processing rate (line graph)
   - Overlay for comparison

2. **Latency Panel**
   - Ingestion latency p50, p90, p95, p99 (line graph)
   - Batch processing latency p50, p90, p95, p99 (line graph)

3. **Error Rate Panel**
   - Error rate percentage (line graph)
   - Batch failure rate percentage (line graph)
   - DLQ routing rate (line graph)

4. **Efficiency Panel**
   - Writes per heartbeat (line graph)
   - Batch size distribution (histogram)
   - Drift detection rate (line graph)

5. **Queue Depth Panel**
   - Kafka queue size (gauge)
   - Processing lag (calculated)

## Alerting Recommendations

### Critical Alerts

1. **High Error Rate**
   - Condition: `rate(heartbeat_failed_total[5m]) / rate(heartbeat_received_total[5m]) > 0.01`
   - Severity: Critical
   - Action: Investigate Kafka connectivity, check circuit breaker status

2. **High Batch Failure Rate**
   - Condition: `rate(heartbeat_batch_failed_total[5m]) / rate(heartbeat_batch_processed_total[5m]) > 0.05`
   - Severity: Critical
   - Action: Investigate batch processing failures, check database connectivity

3. **DLQ Growth**
   - Condition: `rate(heartbeat_dlq_sent_total[5m]) > 10`
   - Severity: Warning
   - Action: Investigate root cause, check DLQ for patterns

4. **High Latency**
   - Condition: `histogram_quantile(0.99, rate(heartbeat_ingestion_time_seconds_bucket[5m])) > 0.05`
   - Severity: Warning
   - Action: Investigate Kafka performance, check network latency

### Warning Alerts

1. **Low Throughput**
   - Condition: `rate(heartbeat_received_total[5m]) < 1000`
   - Severity: Warning
   - Action: Check client connectivity, verify feature flag

2. **High Drift Detection Rate**
   - Condition: `rate(heartbeat_drift_detected_total[5m]) > 100`
   - Severity: Warning
   - Action: Investigate config stability, check Config Server

## Observability Integration

### Micrometer Tracing

**Integration:**
- `@Observed` annotations on key methods
- Tracing spans for heartbeat processing
- W3C trace context propagation

**Spans:**
- `process-heartbeat` - Single heartbeat processing (sync mode)
- `heartbeat-batch-process` - Batch processing
- `config.get_effective_hash` - Config hash retrieval

### Logging

**Structured Logging:**
- JSON format for easy parsing
- Context included (serviceName, instanceId, batch size)
- Appropriate log levels (DEBUG, INFO, WARN, ERROR)

**Key Log Events:**
- Heartbeat received (DEBUG)
- Batch processing started/completed (DEBUG)
- Errors with context (ERROR)
- Drift detection (WARN)
- DLQ routing (ERROR)

## Best Practices

1. **Metric Naming:**
   - Use consistent prefixes
   - Follow Prometheus naming conventions
   - Include units in descriptions

2. **Metric Cardinality:**
   - Avoid high-cardinality tags
   - Use gauges for current state, counters for events
   - Use histograms for latency distributions

3. **Monitoring:**
   - Set up dashboards for key metrics
   - Configure alerts for critical thresholds
   - Review metrics regularly for trends

4. **Performance:**
   - Minimize metric collection overhead
   - Use async metric recording where possible
   - Avoid blocking operations in metric recording

## Configuration

### Micrometer Configuration

```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        heartbeat.ingestion.time: true
        heartbeat.batch.processing.time: true
      percentiles:
        heartbeat.ingestion.time: 0.5, 0.9, 0.95, 0.99
        heartbeat.batch.processing.time: 0.5, 0.9, 0.95, 0.99
```

## Related Documentation

- [Architecture Overview](ARCHITECTURE.md)
- [Async Ingestion](ASYNC_INGESTION.md)
- [Batch Processing](BATCH_PROCESSING.md)
- [Error Handling](ERROR_HANDLING.md)

