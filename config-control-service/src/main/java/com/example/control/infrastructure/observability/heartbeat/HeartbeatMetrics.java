package com.example.control.infrastructure.observability.heartbeat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive metrics component for heartbeat processing.
 * <p>
 * Tracks all aspects of heartbeat ingestion, processing, and batch operations:
 * <ul>
 * <li>Ingestion rate (heartbeats received)</li>
 * <li>Processing latency and throughput</li>
 * <li>Kafka queue depth</li>
 * <li>Batch processing metrics</li>
 * <li>Drift detection rate</li>
 * <li>MongoDB write operations</li>
 * </ul>
 * <p>
 * All metrics are exported to Prometheus and can be visualized in Grafana.
 */
@Slf4j
@Component
public class HeartbeatMetrics {

    private final MeterRegistry meterRegistry;

    // Counters
    private final Counter heartbeatReceived;
    private final Counter heartbeatProcessed;
    private final Counter heartbeatFailed;
    private final Counter heartbeatDriftDetected;
    private final Counter heartbeatMongodbWrites;
    private final Counter heartbeatBatchProcessed;
    private final Counter heartbeatBatchFailed;
    private final Counter heartbeatDlqSent;

    // Timers
    private final Timer heartbeatProcessingTime;
    private final Timer heartbeatBatchProcessingTime;
    private final Timer heartbeatIngestionTime;

    // Gauges (mutable state)
    private final AtomicLong queueSize = new AtomicLong(0);
    private final AtomicLong currentBatchSize = new AtomicLong(0);

    /**
     * Constructor that initializes all metrics.
     *
     * @param meterRegistry the Micrometer meter registry
     */
    public HeartbeatMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.heartbeatReceived = Counter.builder("heartbeat.received")
                .description("Total number of heartbeats received (ingested)")
                .register(meterRegistry);

        this.heartbeatProcessed = Counter.builder("heartbeat.processed")
                .description("Total number of heartbeats successfully processed")
                .register(meterRegistry);

        this.heartbeatFailed = Counter.builder("heartbeat.failed")
                .description("Total number of heartbeats that failed processing")
                .register(meterRegistry);

        this.heartbeatDriftDetected = Counter.builder("heartbeat.drift.detected")
                .description("Total number of configuration drift detections")
                .register(meterRegistry);

        this.heartbeatMongodbWrites = Counter.builder("heartbeat.mongodb.writes")
                .description("Total number of MongoDB write operations for heartbeats")
                .register(meterRegistry);

        this.heartbeatBatchProcessed = Counter.builder("heartbeat.batch.processed")
                .description("Total number of batches successfully processed")
                .register(meterRegistry);

        this.heartbeatBatchFailed = Counter.builder("heartbeat.batch.failed")
                .description("Total number of batches that failed processing")
                .register(meterRegistry);

        this.heartbeatDlqSent = Counter.builder("heartbeat.dlq.sent")
                .description("Total number of heartbeats sent to dead letter queue")
                .register(meterRegistry);

        // Initialize timers
        this.heartbeatProcessingTime = Timer.builder("heartbeat.processing.time")
                .description("Time taken to process a single heartbeat")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        this.heartbeatBatchProcessingTime = Timer.builder("heartbeat.batch.processing.time")
                .description("Time taken to process a batch of heartbeats")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        this.heartbeatIngestionTime = Timer.builder("heartbeat.ingestion.time")
                .description("Time taken to ingest (enqueue) a heartbeat")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(meterRegistry);

        // Initialize gauges
        Gauge.builder("heartbeat.queue.size", queueSize, AtomicLong::get)
                .description("Current Kafka queue depth (estimated)")
                .register(meterRegistry);

        Gauge.builder("heartbeat.batch.size", currentBatchSize, AtomicLong::get)
                .description("Current batch processing size")
                .register(meterRegistry);
    }

    /**
     * Record that a heartbeat was received (ingested).
     */
    public void recordReceived() {
        heartbeatReceived.increment();
    }

    /**
     * Record that a heartbeat was successfully processed.
     *
     * @param duration the processing duration
     */
    public void recordProcessed(Duration duration) {
        heartbeatProcessed.increment();
        heartbeatProcessingTime.record(duration);
    }

    /**
     * Record that a heartbeat processing failed.
     */
    public void recordFailed() {
        heartbeatFailed.increment();
    }

    /**
     * Record that configuration drift was detected.
     */
    public void recordDriftDetected() {
        heartbeatDriftDetected.increment();
    }

    /**
     * Record a MongoDB write operation.
     *
     * @param count the number of writes (for bulk operations)
     */
    public void recordMongodbWrites(long count) {
        heartbeatMongodbWrites.increment(count);
    }

    /**
     * Record that a batch was successfully processed.
     *
     * @param duration the batch processing duration
     * @param batchSize the number of heartbeats in the batch
     */
    public void recordBatchProcessed(Duration duration, int batchSize) {
        heartbeatBatchProcessed.increment();
        heartbeatBatchProcessingTime.record(duration);
        currentBatchSize.set(batchSize);
    }

    /**
     * Record that a batch processing failed.
     */
    public void recordBatchFailed() {
        heartbeatBatchFailed.increment();
    }

    /**
     * Record that a heartbeat was sent to dead letter queue.
     */
    public void recordDlqSent() {
        heartbeatDlqSent.increment();
    }

    /**
     * Record heartbeat ingestion (enqueue) time.
     *
     * @param duration the ingestion duration
     */
    public void recordIngestionTime(Duration duration) {
        heartbeatIngestionTime.record(duration);
    }

    /**
     * Update the estimated Kafka queue size.
     *
     * @param size the estimated queue size
     */
    public void updateQueueSize(long size) {
        queueSize.set(size);
    }
}

