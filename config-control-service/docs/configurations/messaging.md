# Messaging Configuration

## Overview

Kafka producer/consumer configuration, Spring Cloud Stream/Bus, and heartbeat processing configuration. These settings control how messages are produced to and consumed from Kafka, affecting network I/O, memory usage (batching), and CPU usage (compression, serialization).

**Configuration File**: `application-messaging.yml`

## Kafka Producer Configuration

### `spring.kafka.producer.batch.size`
- **Default**: `16384` (16KB)
- **What it does**: Maximum number of bytes to batch together before sending to Kafka
- **Resource impact**:
  - **Memory**: Buffers up to 16KB per partition before sending, increasing memory usage during batching
  - **Network I/O**: Larger batches reduce network round-trips, improving throughput
  - **CPU**: Lower CPU overhead per message due to fewer network operations
- **Configuration location**: `spring.kafka.producer.batch.size` in `application-messaging.yml`
- **Notes**:
  - Batching improves throughput but increases latency (see `linger.ms`)
  - Actual batch size may be smaller if `linger.ms` timeout expires before reaching this size

### `spring.kafka.producer.linger.ms`
- **Default**: `10`
- **What it does**: Time to wait before sending a batch if batch is not full
- **Resource impact**:
  - **Memory**: Holds messages in memory longer while waiting for batch to fill
  - **Latency**: Adds up to 10ms latency per message (trade-off for throughput)
  - **Network I/O**: Allows batching to accumulate more messages, reducing network calls
- **Configuration location**: `spring.kafka.producer.linger.ms` in `application-messaging.yml`
- **Notes**:
  - Set to 0 to disable batching (immediate send, higher network overhead)
  - Combined with `batch.size`, determines batching behavior

### `spring.kafka.producer.compression.type`
- **Default**: `gzip`
- **What it does**: Compression algorithm applied to message batches before sending
- **Resource impact**:
  - **CPU**: Compression requires CPU cycles (gzip is CPU-intensive)
  - **Network I/O**: Reduces network bandwidth usage by compressing messages
  - **Memory**: Temporary buffers for compression/decompression
- **Configuration location**: `spring.kafka.producer.compression.type` in `application-messaging.yml`
- **Notes**:
  - Options: `none`, `gzip`, `snappy`, `lz4`, `zstd`
  - `gzip` provides good compression ratio but higher CPU usage
  - `snappy` provides faster compression with lower ratio (requires native libraries, not used here)

### `spring.kafka.producer.acks`
- **Default**: `1`
- **What it does**: Number of acknowledgments required from Kafka brokers before considering message sent
- **Resource impact**:
  - **Latency**: Lower acks (0 or 1) provide lower latency, higher acks (all/-1) provide higher latency but stronger durability
  - **Network I/O**: Higher acks require more network round-trips for acknowledgments
  - **CPU**: Minimal impact
- **Configuration location**: `spring.kafka.producer.acks` in `application-messaging.yml`
- **Notes**:
  - `0`: No acknowledgment (fastest, but messages may be lost)
  - `1`: Leader acknowledgment (balanced, default)
  - `all` or `-1`: All in-sync replicas acknowledgment (slowest, strongest durability)

### `spring.kafka.producer.key-serializer` / `spring.kafka.producer.value-serializer`
- **Default**: `org.apache.kafka.common.serialization.StringSerializer` / `org.apache.kafka.common.serialization.ByteArraySerializer`
- **What it does**: Serializers for message keys and values
- **Resource impact**:
  - **CPU**: Serialization consumes CPU cycles (ByteArraySerializer is fast, but requires pre-serialization)
  - **Memory**: May create temporary objects during serialization
- **Configuration location**: `spring.kafka.producer.key-serializer` / `spring.kafka.producer.value-serializer` in `application-messaging.yml`
- **Notes**:
  - ByteArraySerializer requires objects to be pre-serialized (e.g., to JSON bytes)
  - JSON serializer would handle serialization but adds overhead

## Kafka Consumer Configuration

### `spring.kafka.consumer.group-id`
- **Default**: `config-control-group`
- **What it does**: Consumer group ID for coordinating consumers and managing partition assignment
- **Resource impact**: None (logical grouping only)
- **Configuration location**: `spring.kafka.consumer.group-id` in `application-messaging.yml`
- **Notes**: Consumers with the same group-id share partitions; different group-ids receive all messages independently

### `spring.kafka.consumer.auto-offset-reset`
- **Default**: `earliest`
- **What it does**: Where to start reading from if no committed offset exists
- **Resource impact**: 
  - **Network I/O**: `earliest` may process historical messages, increasing initial load
  - **Disk I/O**: May read from older log segments
- **Configuration location**: `spring.kafka.consumer.auto-offset-reset` in `application-messaging.yml`
- **Notes**:
  - `earliest`: Start from beginning of topic
  - `latest`: Start from end (only new messages)
  - Useful for recovery scenarios

### `spring.kafka.consumer.max-poll-records`
- **Default**: `100`
- **What it does**: Maximum number of records returned in a single poll() call
- **Resource impact**:
  - **Memory**: Larger values consume more memory per poll (batch of records)
  - **Processing**: Larger batches reduce polling overhead but increase processing batch size
  - **Network I/O**: Fewer network calls to fetch messages
- **Configuration location**: `spring.kafka.consumer.max-poll-records` in `application-messaging.yml`
- **Notes**:
  - Must process within `max-poll-interval-ms` or consumer is considered dead
  - Larger values improve throughput but require faster processing

### `spring.kafka.consumer.fetch-min-size`
- **Default**: `1024` (1KB)
- **What it does**: Minimum number of bytes to fetch per request
- **Resource impact**:
  - **Network I/O**: Waits until at least 1KB is available, reducing small network calls
  - **Latency**: May add small delay waiting for minimum bytes
- **Configuration location**: `spring.kafka.consumer.fetch-min-size` in `application-messaging.yml`
- **Notes**: Combined with `fetch-max-wait` to balance latency vs throughput

### `spring.kafka.consumer.fetch-max-wait`
- **Default**: `500ms`
- **What it does**: Maximum time to wait for `fetch-min-size` bytes before returning whatever is available
- **Resource impact**:
  - **Latency**: Adds up to 500ms latency per fetch if minimum size not reached
  - **Network I/O**: Allows batching of small messages
- **Configuration location**: `spring.kafka.consumer.fetch-max-wait` in `application-messaging.yml`
- **Notes**: Prevents indefinite waiting for minimum bytes

### `spring.kafka.consumer.enable-auto-commit`
- **Default**: `false`
- **What it does**: Whether to automatically commit offsets after consuming messages
- **Resource impact**:
  - **Reliability**: Manual commit (false) provides better at-least-once delivery guarantees
  - **CPU**: Manual commit requires explicit calls but provides better control
- **Configuration location**: `spring.kafka.consumer.enable-auto-commit` in `application-messaging.yml`
- **Notes**: 
  - Manual commit allows committing only after successful processing
  - Auto-commit may commit before processing completes (at-most-once guarantee)

## Heartbeat Kafka Consumer Configuration

Heartbeat processing uses a dedicated Kafka consumer with custom configuration for high-throughput message processing.

### `app.heartbeat.kafka.consumer.concurrency`
- **Default**: `15`
- **What it does**: Number of concurrent consumer threads processing heartbeat messages
- **Resource impact**:
  - **Threads**: Creates 15 threads per instance for heartbeat processing
  - **CPU**: More threads allow parallel processing, improving throughput
  - **Memory**: Each thread consumes stack memory (~1MB per thread)
- **Configuration location**: `app.heartbeat.kafka.consumer.concurrency` in `application-app.yml` (referenced via `application-infrastructure.yml`)
- **Notes**:
  - Higher concurrency improves throughput but increases thread overhead
  - Each thread processes messages from assigned partitions

### `app.heartbeat.kafka.consumer.max-retries`
- **Default**: `3`
- **What it does**: Maximum number of retry attempts for failed heartbeat processing
- **Resource impact**:
  - **Network I/O**: Retries consume additional network resources
  - **CPU**: Re-processing consumes CPU cycles
  - **Latency**: Retries add latency to message processing
- **Configuration location**: `app.heartbeat.kafka.consumer.max-retries` in `application-app.yml`
- **Notes**: Failed messages after max retries are sent to DLQ (see below)

### `app.heartbeat.kafka.consumer.max-poll-records`
- **Default**: `500`
- **What it does**: Maximum number of heartbeat records fetched per poll
- **Resource impact**:
  - **Memory**: Larger batches consume more memory per poll
  - **Processing**: Enables batch processing of heartbeats, improving efficiency
  - **Network I/O**: Fewer network calls to fetch messages
- **Configuration location**: `app.heartbeat.kafka.consumer.max-poll-records` in `application-app.yml`
- **Notes**: Higher than default Kafka consumer (100) to optimize heartbeat throughput

### `app.heartbeat.kafka.consumer.fetch-max-wait-ms`
- **Default**: `2000` (2s)
- **What it does**: Maximum time to wait for `fetch-min-bytes` before returning available messages
- **Resource impact**:
  - **Latency**: Adds up to 2s latency per fetch
  - **Network I/O**: Allows batching for better throughput
- **Configuration location**: `app.heartbeat.kafka.consumer.fetch-max-wait-ms` in `application-app.yml`
- **Notes**: Longer than default (500ms) to allow more batching

### `app.heartbeat.kafka.consumer.fetch-min-bytes`
- **Default**: `16384` (16KB)
- **What it does**: Minimum number of bytes to fetch per request
- **Resource impact**:
  - **Network I/O**: Waits for 16KB, reducing small network calls
  - **Latency**: May add delay waiting for minimum bytes
- **Configuration location**: `app.heartbeat.kafka.consumer.fetch-min-bytes` in `application-app.yml`
- **Notes**: Higher than default (1024) to optimize network efficiency

### `app.heartbeat.kafka.consumer.max-poll-interval-ms`
- **Default**: `300000` (5 minutes)
- **What it does**: Maximum time between poll() calls before consumer is considered dead
- **Resource impact**:
  - **Reliability**: Allows longer processing time per batch before timeout
  - **CPU**: Prevents consumer from being kicked out during long batch processing
- **Configuration location**: `app.heartbeat.kafka.consumer.max-poll-interval-ms` in `application-app.yml`
- **Notes**: Longer than default to accommodate batch processing of large heartbeat batches

### `app.heartbeat.kafka.dlq.topic`
- **Default**: `heartbeat-queue-dlq`
- **What it does**: Dead Letter Queue topic for failed heartbeat messages after max retries
- **Resource impact**:
  - **Network I/O**: Additional topic for failed messages
  - **Disk I/O**: Kafka stores DLQ messages on disk
- **Configuration location**: `app.heartbeat.kafka.dlq.topic` in `application-app.yml`
- **Notes**: Allows recovery and analysis of failed messages

## Spring Cloud Stream Configuration

### `spring.cloud.stream.kafka.binder.auto-create-topics`
- **Default**: `true`
- **What it does**: Automatically create Kafka topics if they don't exist
- **Resource impact**: None (administrative setting)
- **Configuration location**: `spring.cloud.stream.kafka.binder.auto-create-topics` in `application-messaging.yml`
- **Notes**: Useful for development; production may prefer manual topic management

### `spring.cloud.stream.kafka.binder.auto-add-partitions`
- **Default**: `true`
- **What it does**: Automatically add partitions to topics if needed
- **Resource impact**: None (administrative setting)
- **Configuration location**: `spring.cloud.stream.kafka.binder.auto-add-partitions` in `application-messaging.yml`
- **Notes**: Partitions determine parallelism; more partitions allow more concurrent consumers

## Spring Cloud Bus Configuration

### `spring.cloud.bus.enabled`
- **Default**: `true`
- **What it does**: Enables Spring Cloud Bus for configuration refresh via Kafka
- **Resource impact**:
  - **Network I/O**: Bus messages broadcast configuration changes
  - **CPU**: Processes bus events for configuration refresh
- **Configuration location**: `spring.cloud.bus.enabled` in `application-messaging.yml`
- **Notes**: Allows remote refresh of configuration via `/actuator/busrefresh` endpoint

### `spring.cloud.bus.refresh.enabled`
- **Default**: `true`
- **What it does**: Enables configuration refresh via bus events
- **Resource impact**: Same as `spring.cloud.bus.enabled`
- **Configuration location**: `spring.cloud.bus.refresh.enabled` in `application-messaging.yml`
- **Notes**: Controls whether refresh events are processed

### `spring.cloud.bus.destination`
- **Default**: `springCloudBus`
- **What it does**: Kafka topic name for bus events
- **Resource impact**: None (logical topic name)
- **Configuration location**: `spring.cloud.bus.destination` in `application-messaging.yml`
- **Notes**: All services subscribe to this topic for broadcast events

## Resource Usage Summary

| Setting | CPU Impact | Memory Impact | Network I/O Impact | Thread Impact |
|---------|-----------|---------------|-------------------|---------------|
| `producer.batch.size` | Low | Medium (buffers) | High (efficiency) | - |
| `producer.linger.ms` | Low | Medium (hold time) | High (batching) | - |
| `producer.compression.type` | High (gzip) | Low | High (reduction) | - |
| `consumer.max-poll-records` | Low | Medium (batch size) | High (efficiency) | - |
| `heartbeat.consumer.concurrency` | Medium | Medium (stacks) | High (parallelism) | Direct (15 threads) |
| `heartbeat.consumer.max-poll-records` | Low | High (large batches) | High (efficiency) | - |

## See Also

- [Executor Pools](executor-pools.md) - Thread pools used for message processing
- [Resilience](resilience.md) - Circuit breakers and retries for Kafka operations
- [Kafka Producer Configuration](https://kafka.apache.org/documentation/#producerconfigs)
- [Kafka Consumer Configuration](https://kafka.apache.org/documentation/#consumerconfigs)

