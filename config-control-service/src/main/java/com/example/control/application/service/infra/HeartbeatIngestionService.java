package com.example.control.application.service.infra;

import com.example.control.domain.model.HeartbeatPayload;
import com.example.control.infrastructure.config.messaging.HeartbeatProperties;
import com.example.control.infrastructure.observability.heartbeat.HeartbeatMetrics;
import com.example.control.infrastructure.resilience.messaging.ResilientKafkaProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for ingesting (enqueueing) heartbeat payloads to Kafka.
 * <p>
 * This service provides a fast, non-blocking way to accept heartbeats and
 * enqueue them for asynchronous batch processing. Uses serviceName as partition
 * key to ensure ordering per service.
 * <p>
 * All operations are protected with resilience patterns (circuit breaker,
 * bulkhead, time limiter) via ResilientKafkaProducer.
 */
@Slf4j
@Service
public class HeartbeatIngestionService {

    private final ResilientKafkaProducer resilientKafkaProducer;
    private final HeartbeatMetrics heartbeatMetrics;
    private final KafkaTemplate<String, HeartbeatPayload> heartbeatKafkaTemplate;
    private final HeartbeatProperties heartbeatProperties;

    public HeartbeatIngestionService(
            ResilientKafkaProducer resilientKafkaProducer,
            HeartbeatMetrics heartbeatMetrics,
            @Qualifier("heartbeatKafkaTemplate") KafkaTemplate<String, HeartbeatPayload> heartbeatKafkaTemplate,
            HeartbeatProperties heartbeatProperties) {
        this.resilientKafkaProducer = resilientKafkaProducer;
        this.heartbeatMetrics = heartbeatMetrics;
        this.heartbeatKafkaTemplate = heartbeatKafkaTemplate;
        this.heartbeatProperties = heartbeatProperties;
    }

    /**
     * Enqueues a heartbeat payload to Kafka for asynchronous processing.
     * <p>
     * Uses serviceName as partition key to ensure ordering per service.
     * Records metrics for ingestion rate and latency.
     * <p>
     * This method is designed to be fast and non-blocking. Failures are logged
     * and recorded in metrics but do not throw exceptions to avoid impacting
     * the HTTP response.
     *
     * @param payload the heartbeat payload to enqueue
     * @throws RuntimeException if Kafka send fails (should be rare due to resilience)
     */
    public void enqueue(HeartbeatPayload payload) {
        Instant start = Instant.now();

        try {
            // Use serviceName as partition key to ensure ordering per service
            String partitionKey = payload.getServiceName();

            // Send to Kafka with resilience protection
            CompletableFuture<SendResult<String, HeartbeatPayload>> future =
                    resilientKafkaProducer.send(heartbeatKafkaTemplate, heartbeatProperties.getKafka().getTopic(), partitionKey, payload);

            // Record metrics
            heartbeatMetrics.recordReceived();
            Duration ingestionTime = Duration.between(start, Instant.now());
            heartbeatMetrics.recordIngestionTime(ingestionTime);

            log.debug("Enqueued heartbeat for {}:{} to topic {}", payload.getServiceName(),
                    payload.getInstanceId(), heartbeatProperties.getKafka().getTopic());

            // Optionally handle send result asynchronously
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to enqueue heartbeat for {}:{}", payload.getServiceName(),
                            payload.getInstanceId(), ex);
                    heartbeatMetrics.recordFailed();
                } else {
                    log.trace("Heartbeat enqueued successfully for {}:{}", payload.getServiceName(),
                            payload.getInstanceId());
                }
            });

        } catch (Exception e) {
            log.error("Failed to enqueue heartbeat for {}:{}", payload.getServiceName(),
                    payload.getInstanceId(), e);
            heartbeatMetrics.recordFailed();
            // Re-throw to allow controller to handle error response
            throw new RuntimeException("Failed to enqueue heartbeat: " + e.getMessage(), e);
        }
    }
}

