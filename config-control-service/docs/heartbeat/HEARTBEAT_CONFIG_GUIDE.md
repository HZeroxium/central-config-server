# Heartbeat Configuration Guide

This guide documents all configuration properties related to heartbeat processing, including tuning recommendations and troubleshooting.

## Overview

The heartbeat system processes periodic ping messages from SDK clients to track service instances, detect configuration drift, and maintain service discovery state. The system uses Kafka for async processing with batch optimization.

## Client-Side Configuration (SDK)

### Core Ping Configuration

```yaml
zcm:
  sdk:
    ping:
      enabled: true                    # Enable/disable periodic ping
      fixed-delay: 30000              # Ping interval in milliseconds (default: 30s)
      protocol: KAFKA                  # Protocol: HTTP, THRIFT, GRPC, KAFKA
      service-discovery-name: config-control-service
```

### Config Hash Caching

```yaml
zcm:
  sdk:
    ping:
      hash-cache:
        enabled: true                  # Enable config hash caching (default: true)
        ttl: 30000                    # Cache TTL in milliseconds (default: 30s, matches ping interval)
        max-size: 1000                # Maximum cache entries (default: 1000)
```

**Tuning Recommendations:**
- Set `ttl` to match or slightly less than `fixed-delay` to ensure fresh hashes
- Increase `max-size` if you have many services with different profiles/labels
- Cache is automatically invalidated on refresh events

### Circuit Breaker Configuration

```yaml
zcm:
  sdk:
    ping:
      circuit-breaker:
        enabled: true                 # Enable circuit breaker (default: true)
        failure-rate-threshold: 50    # Failure rate % to open circuit (default: 50%)
        wait-duration-in-open-state: 30000  # Wait time before half-open (default: 30s)
        permitted-number-of-calls-in-half-open-state: 3  # Test calls in half-open (default: 3)
        sliding-window-size: 10      # Window size for failure rate calculation (default: 10)
```

**Tuning Recommendations:**
- Increase `failure-rate-threshold` if you have transient network issues (60-70%)
- Decrease `wait-duration-in-open-state` for faster recovery (20s) if Kafka is stable
- Increase `sliding-window-size` for more stable failure rate calculation (20-50)

### Kafka Configuration

```yaml
zcm:
  sdk:
    ping:
      kafka:
        bootstrap-servers: localhost:9092  # Fallback if config-control-service unavailable
        topic: heartbeat-queue            # Kafka topic (default: heartbeat-queue)
        config-refresh-interval: 300000   # Refresh interval for Kafka config (default: 5min)
```

**Note:** SDK automatically fetches Kafka configuration from config-control-service. These properties are fallback only.

## Server-Side Configuration

### Kafka Consumer Tuning

```yaml
app:
  heartbeat:
    kafka:
      topic: heartbeat-queue
      consumer:
        concurrency: 10                # Number of concurrent consumer threads
        max-retries: 3                 # Max retries before DLQ
        max-poll-records: 200          # Max records per poll (optimized from 100)
        fetch-max-wait-ms: 1000        # Max wait for fetch.min.bytes (optimized from 500ms)
        fetch-min-bytes: 8192          # Min bytes before returning (optimized from 1024)
        max-poll-interval-ms: 300000   # Max poll interval to prevent rebalance (default: 5min)
```

### Tuning Recommendations

#### Batch Size (max-poll-records)

**Conservative (Recommended):**
- `max-poll-records: 200` - Safe default, good for most workloads
- Provides 2x throughput improvement over default 100
- Low risk of timeout or memory issues

**Aggressive:**
- `max-poll-records: 500` - Maximum throughput
- Use only if:
  - Processing time per record < 10ms
  - Sufficient memory (heap > 2GB)
  - Monitoring shows no timeout issues

**Tuning Formula:**
```
max-poll-records = (max-poll-interval-ms / avg-processing-time-per-record) * 0.8
```

#### Fetch Wait Time (fetch-max-wait-ms)

- **Default:** 1000ms (1 second)
- **Purpose:** Allows Kafka to accumulate larger batches
- **Tuning:**
  - Increase to 2000ms if message rate is low (< 100 msg/sec)
  - Decrease to 500ms if latency is critical
  - Monitor batch sizes - should see batches > 50 records

#### Fetch Min Bytes (fetch-min-bytes)

- **Default:** 8192 bytes (8KB)
- **Purpose:** Reduces number of fetch requests
- **Tuning:**
  - Increase to 16384 (16KB) if messages are large (> 1KB each)
  - Decrease to 4096 (4KB) if messages are small (< 100 bytes)
  - Monitor network requests - should see fewer fetches

#### Max Poll Interval (max-poll-interval-ms)

- **Default:** 300000ms (5 minutes)
- **Purpose:** Prevents consumer rebalancing during long processing
- **Tuning:**
  - Set to: `(max-poll-records * avg-processing-time-per-record) * 2`
  - Example: 200 records * 50ms = 10s, set to 20000ms (20s) + buffer
  - Monitor for rebalance events - should be zero

#### Concurrency

- **Default:** 10 threads
- **Tuning:**
  - Set to number of Kafka topic partitions (optimal)
  - Maximum: 2x number of partitions
  - Monitor thread pool utilization - should be 60-80%

### Environment Variables

All server-side properties can be overridden via environment variables:

```bash
# Batch tuning
HEARTBEAT_KAFKA_CONSUMER_MAX_POLL_RECORDS=200
HEARTBEAT_KAFKA_CONSUMER_FETCH_MAX_WAIT_MS=1000
HEARTBEAT_KAFKA_CONSUMER_FETCH_MIN_BYTES=8192
HEARTBEAT_KAFKA_CONSUMER_MAX_POLL_INTERVAL_MS=300000

# Concurrency
HEARTBEAT_KAFKA_CONSUMER_CONCURRENCY=10
```

## Performance Tuning Checklist

### Initial Setup

- [ ] Set `max-poll-records` to 200 (conservative)
- [ ] Set `fetch-max-wait-ms` to 1000ms
- [ ] Set `fetch-min-bytes` to 8192
- [ ] Set `max-poll-interval-ms` based on processing time estimate
- [ ] Set `concurrency` to match Kafka topic partition count

### Monitoring Phase (Week 1)

- [ ] Monitor batch processing time (p95, p99)
- [ ] Monitor batch sizes (should average > 50 records)
- [ ] Monitor consumer lag (should be < 1000 messages)
- [ ] Monitor rebalance events (should be zero)
- [ ] Monitor thread pool utilization (60-80% optimal)

### Optimization Phase (Week 2+)

- [ ] If batch sizes < 50: increase `fetch-max-wait-ms` to 2000ms
- [ ] If processing time < 50% of `max-poll-interval-ms`: increase `max-poll-records`
- [ ] If rebalance events > 0: increase `max-poll-interval-ms`
- [ ] If thread utilization < 50%: decrease `concurrency`
- [ ] If thread utilization > 90%: increase `concurrency` or partitions

## Troubleshooting

### High Consumer Lag

**Symptoms:**
- Consumer lag continuously increasing
- Batch processing time > 1 second

**Solutions:**
1. Increase `concurrency` (if < partition count)
2. Increase `max-poll-records` (if processing time allows)
3. Optimize batch processing logic (check MongoDB queries)
4. Scale out: add more consumer instances

### Frequent Rebalances

**Symptoms:**
- Consumer group rebalancing every few minutes
- "The coordinator is not available" errors

**Solutions:**
1. Increase `max-poll-interval-ms` (current processing time * 2)
2. Check for long-running operations in batch processing
3. Verify network stability to Kafka brokers
4. Check Kafka broker health

### Small Batch Sizes

**Symptoms:**
- Batch sizes consistently < 20 records
- Low throughput despite high message rate

**Solutions:**
1. Increase `fetch-max-wait-ms` to 2000ms
2. Increase `fetch-min-bytes` to 16384
3. Check message rate - may be too low for batching
4. Verify Kafka topic has sufficient messages

### High Memory Usage

**Symptoms:**
- JVM heap usage > 80%
- OutOfMemoryError during batch processing

**Solutions:**
1. Decrease `max-poll-records` to 100
2. Increase JVM heap size
3. Check for memory leaks in batch processing
4. Optimize payload size (remove unnecessary metadata)

### Circuit Breaker Frequently Open

**Symptoms:**
- Circuit breaker state: OPEN
- Many "circuit breaker is OPEN" log messages

**Solutions:**
1. Check Kafka broker connectivity
2. Verify network stability
3. Check Kafka topic exists and is accessible
4. Review circuit breaker thresholds (may be too sensitive)
5. Check for Kafka producer errors in logs

### Config Hash Cache Misses

**Symptoms:**
- High CPU usage from hash calculation
- Cache hit rate < 80%

**Solutions:**
1. Increase cache `ttl` to match ping interval
2. Increase cache `max-size` if many services
3. Verify cache invalidation on refresh events
4. Check for too many unique service:profile:label combinations

## Metrics to Monitor

### Client-Side (SDK)

- `zcm.ping.send.total` - Total ping attempts
- `zcm.ping.send.success` - Successful pings
- `zcm.ping.send.failure` - Failed pings
- `zcm.ping.send.latency` - Ping latency histogram
- `zcm.ping.kafka.config.fetch` - Kafka config fetch attempts
- Circuit breaker state (via Resilience4j metrics)

### Server-Side

- `heartbeat.batch.process` - Batch processing time
- `heartbeat.batch.config-hash-fetch` - Config hash fetch time
- `heartbeat.ingestion` - Ingestion time
- `heartbeat.mongodb.writes` - MongoDB write operations
- `heartbeat.drift.detected` - Drift detection count
- Kafka consumer lag (via Kafka metrics)

## Performance Benchmarks

### Baseline (Before Optimization)

- Batch size: ~50-100 records
- Processing time: 200-500ms per batch
- Throughput: ~500-1000 heartbeats/second
- Config hash fetch: Sequential, ~50ms per service

### Optimized (After Implementation)

- Batch size: ~150-200 records
- Processing time: 300-600ms per batch (slightly higher due to larger batches)
- Throughput: ~1000-2000 heartbeats/second (2x improvement)
- Config hash fetch: Parallel, ~20ms per service (2.5x improvement)

## Migration Guide

### Upgrading from Previous Version

1. **No Breaking Changes:** All new properties have sensible defaults
2. **Gradual Rollout:** Enable optimizations one at a time:
   - Week 1: Enable config hash caching (client)
   - Week 2: Enable async Kafka send (client)
   - Week 3: Increase batch size to 200 (server)
   - Week 4: Enable parallel config fetch (server)

3. **Monitoring:** Monitor metrics after each change
4. **Rollback:** All changes are configurable - revert to defaults if issues occur

## References

- [Kafka Consumer Configuration](https://kafka.apache.org/documentation/#consumerconfigs)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)

