package com.example.control.application.service.infra;

import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.FailedHeartbeat;
import com.example.control.domain.model.HeartbeatPayload;
import com.example.control.domain.valueobject.id.FailedHeartbeatId;
import com.example.control.infrastructure.observability.heartbeat.HeartbeatMetrics;
import com.example.control.domain.port.repository.FailedHeartbeatRepositoryPort;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Kafka batch consumer for processing failed heartbeats from DLQ.
 * <p>
 * Consumes batches of failed heartbeat messages from the DLQ topic, extracts
 * metadata from Kafka headers, and persists FailedHeartbeat documents to MongoDB.
 * Implements deduplication by serviceName + instanceId to avoid duplicate entries.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatDlqConsumer {

    private final FailedHeartbeatRepositoryPort failedHeartbeatRepository;
    private final ApplicationServiceQueryService applicationServiceQueryService;
    private final HeartbeatMetrics heartbeatMetrics;

    /**
     * Processes a batch of failed heartbeat records from DLQ.
     * <p>
     * Extracts metadata from Kafka headers, resolves serviceId and teamId from
     * ApplicationService, and persists FailedHeartbeat documents. Implements
     * deduplication to update existing records instead of creating duplicates.
     *
     * @param records       list of consumer records containing failed heartbeat payloads
     * @param acknowledgment manual acknowledgment for offset commit
     */
    @KafkaListener(
            topics = "${app.heartbeat.kafka.dlqTopic:heartbeat-queue-dlq}",
            concurrency = "${app.heartbeat.kafka.dlqConsumer.concurrency:2}",
            containerFactory = "heartbeatDlqKafkaListenerContainerFactory"
    )
    @Observed(name = "heartbeat.dlq.consume", contextualName = "heartbeat-dlq-consume")
    @Timed("heartbeat.dlq.consume.time")
    public void processDlqBatch(
            List<ConsumerRecord<String, HeartbeatPayload>> records,
            Acknowledgment acknowledgment) {

        if (records.isEmpty()) {
            log.debug("Received empty DLQ batch, skipping");
            return;
        }

        Instant start = Instant.now();
        int batchSize = records.size();
        int successCount = 0;
        int errorCount = 0;

        log.debug("Processing DLQ batch of {} failed heartbeats", batchSize);

        for (ConsumerRecord<String, HeartbeatPayload> record : records) {
            try {
                processDlqRecord(record);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                log.error("Failed to process DLQ record for {}:{}",
                        record.value() != null ? record.value().getServiceName() : "unknown",
                        record.value() != null ? record.value().getInstanceId() : "unknown", e);
                // Continue processing other records - don't fail entire batch
            }
        }

        // Acknowledge after processing all records (even if some failed)
        acknowledgment.acknowledge();

        // Record metrics
        heartbeatMetrics.recordDlqConsumed(successCount);
        if (errorCount > 0) {
            log.warn("DLQ batch processing completed with {} successes and {} errors", successCount, errorCount);
        } else {
            log.debug("Successfully processed DLQ batch of {} records in {}ms",
                    batchSize, java.time.Duration.between(start, Instant.now()).toMillis());
        }
    }

    /**
     * Processes a single DLQ record.
     * <p>
     * Extracts metadata from Kafka headers, resolves serviceId and teamId,
     * and creates or updates a FailedHeartbeat document.
     *
     * @param record the consumer record containing failed heartbeat
     */
    private void processDlqRecord(ConsumerRecord<String, HeartbeatPayload> record) {
        HeartbeatPayload payload = record.value();
        if (payload == null) {
            log.warn("DLQ record has null payload, skipping");
            return;
        }

        // Extract metadata from Kafka headers
        String originalTopic = extractHeader(record, "x-original-topic", record.topic());
        String originalPartitionStr = extractHeader(record, "x-original-partition", String.valueOf(record.partition()));
        String originalOffsetStr = extractHeader(record, "x-original-offset", String.valueOf(record.offset()));
        String exceptionMessage = extractHeader(record, "x-exception-message", "Unknown error");
        String exceptionClass = extractHeader(record, "x-exception-class", "UnknownException");
        String retryCountStr = extractHeader(record, "x-retry-count", "0");

        Integer originalPartition = parseInteger(originalPartitionStr);
        Long originalOffset = parseLong(originalOffsetStr);
        Integer retryCount = parseInteger(retryCountStr);

        // Resolve serviceId and teamId from ApplicationService
        String serviceId = null;
        String teamId = null;
        Optional<ApplicationService> appService = applicationServiceQueryService.findByDisplayName(payload.getServiceName());
        if (appService.isPresent()) {
            ApplicationService service = appService.get();
            serviceId = service.getId() != null ? service.getId().id() : null;
            teamId = service.getOwnerTeamId();
        }

        // Check if FailedHeartbeat already exists (deduplication)
        Optional<FailedHeartbeat> existing = failedHeartbeatRepository.findByServiceNameAndInstanceId(
                payload.getServiceName(), payload.getInstanceId());

        Instant now = Instant.now();
        FailedHeartbeat failedHeartbeat;

        if (existing.isPresent()) {
            // Update existing record (deduplication)
            FailedHeartbeat existingRecord = existing.get();
            failedHeartbeat = FailedHeartbeat.builder()
                    .id(existingRecord.getId())
                    .serviceName(payload.getServiceName())
                    .instanceId(payload.getInstanceId())
                    .serviceId(serviceId)
                    .teamId(teamId)
                    .environment(payload.getEnvironment())
                    .payload(payload)
                    .originalTopic(originalTopic)
                    .originalPartition(originalPartition)
                    .originalOffset(originalOffset)
                    .exceptionMessage(exceptionMessage)
                    .exceptionClass(exceptionClass)
                    .retryCount(retryCount)
                    .status(FailedHeartbeat.FailedHeartbeatStatus.NEW) // Reset to NEW if updated
                    .firstSeenAt(existingRecord.getFirstSeenAt() != null ? existingRecord.getFirstSeenAt() : now)
                    .lastSeenAt(now)
                    .resolvedAt(existingRecord.getResolvedAt())
                    .resolvedBy(existingRecord.getResolvedBy())
                    .notes(existingRecord.getNotes())
                    .build();
            log.debug("Updating existing FailedHeartbeat for {}:{}", payload.getServiceName(), payload.getInstanceId());
        } else {
            // Create new record
            failedHeartbeat = FailedHeartbeat.builder()
                    .id(FailedHeartbeatId.of(UUID.randomUUID().toString()))
                    .serviceName(payload.getServiceName())
                    .instanceId(payload.getInstanceId())
                    .serviceId(serviceId)
                    .teamId(teamId)
                    .environment(payload.getEnvironment())
                    .payload(payload)
                    .originalTopic(originalTopic)
                    .originalPartition(originalPartition)
                    .originalOffset(originalOffset)
                    .exceptionMessage(exceptionMessage)
                    .exceptionClass(exceptionClass)
                    .retryCount(retryCount)
                    .status(FailedHeartbeat.FailedHeartbeatStatus.NEW)
                    .firstSeenAt(now)
                    .lastSeenAt(now)
                    .build();
            log.debug("Creating new FailedHeartbeat for {}:{}", payload.getServiceName(), payload.getInstanceId());
        }

        // Persist to MongoDB
        failedHeartbeatRepository.save(failedHeartbeat);
    }

    /**
     * Extracts a header value from Kafka record headers.
     *
     * @param record       the consumer record
     * @param headerName   the header name
     * @param defaultValue default value if header not found
     * @return header value or default
     */
    private String extractHeader(ConsumerRecord<String, HeartbeatPayload> record, String headerName, String defaultValue) {
        Header header = record.headers().lastHeader(headerName);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return defaultValue;
    }

    /**
     * Parses an integer from string, returning null on error.
     */
    private Integer parseInteger(String value) {
        try {
            return value != null ? Integer.parseInt(value) : null;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse integer: {}", value);
            return null;
        }
    }

    /**
     * Parses a long from string, returning null on error.
     */
    private Long parseLong(String value) {
        try {
            return value != null ? Long.parseLong(value) : null;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse long: {}", value);
            return null;
        }
    }
}

