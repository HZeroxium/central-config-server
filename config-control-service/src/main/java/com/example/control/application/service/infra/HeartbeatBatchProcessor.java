package com.example.control.application.service.infra;

import com.example.control.domain.model.HeartbeatPayload;
import com.example.control.infrastructure.observability.MetricsNames;
import com.example.control.infrastructure.observability.heartbeat.HeartbeatMetrics;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kafka batch consumer for processing heartbeat payloads.
 * <p>
 * Consumes batches of heartbeat messages from Kafka and delegates to
 * HeartbeatBatchService for batch processing. Implements manual acknowledgment
 * and error handling via HeartbeatKafkaErrorHandler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatBatchProcessor {

    private final HeartbeatBatchService heartbeatBatchService;
    private final HeartbeatMetrics heartbeatMetrics;

    /**
     * Processes a batch of heartbeat records from Kafka.
     * <p>
     * Extracts payloads from ConsumerRecords, processes them in batch, and
     * manually acknowledges after successful processing.
     *
     * @param records list of consumer records containing heartbeat payloads
     * @param acknowledgment manual acknowledgment for offset commit
     */
    @KafkaListener(
            topics = "${app.heartbeat.kafka.topic:heartbeat-queue}",
            concurrency = "${app.heartbeat.kafka.consumer.concurrency:10}",
            containerFactory = "heartbeatKafkaListenerContainerFactory"
    )
    @Observed(name = MetricsNames.Heartbeat.BATCH_PROCESS, contextualName = "heartbeat-batch-process")
    public void processBatch(
            List<ConsumerRecord<String, HeartbeatPayload>> records,
            Acknowledgment acknowledgment) {

        if (records.isEmpty()) {
            log.debug("Received empty batch, skipping");
            return;
        }

        Instant start = Instant.now();
        int batchSize = records.size();

        try {
            log.debug("Processing heartbeat batch of {} records", batchSize);

            // Extract payloads from records
            List<HeartbeatPayload> payloads = records.stream()
                    .map(ConsumerRecord::value)
                    .collect(Collectors.toList());

            // Process batch
            heartbeatBatchService.processBatch(payloads);

            // Acknowledge after successful processing
            acknowledgment.acknowledge();

            // Record metrics
            Duration processingTime = Duration.between(start, Instant.now());
            heartbeatMetrics.recordBatchProcessed(processingTime, batchSize);

            log.debug("Successfully processed heartbeat batch of {} records in {}ms",
                    batchSize, processingTime.toMillis());

        } catch (Exception e) {
            log.error("Failed to process heartbeat batch of {} records", batchSize, e);
            heartbeatMetrics.recordBatchFailed();
            // Error handler will handle retry and DLQ routing
            throw new RuntimeException("Batch processing failed", e);
        }
    }
}

