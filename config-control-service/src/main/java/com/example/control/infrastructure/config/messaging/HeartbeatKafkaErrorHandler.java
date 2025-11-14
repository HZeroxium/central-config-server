package com.example.control.infrastructure.config.messaging;

import com.example.control.domain.model.HeartbeatPayload;
import com.example.control.infrastructure.observability.heartbeat.HeartbeatMetrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Error handler for heartbeat Kafka batch processing.
 * <p>
 * Implements retry logic with exponential backoff and routes failed batches
 * to dead letter queue after max retries.
 */
@Slf4j
public class HeartbeatKafkaErrorHandler implements CommonErrorHandler {

    private final int maxRetries;
    private final HeartbeatMetrics heartbeatMetrics;
    private final KafkaTemplate<String, HeartbeatPayload> dlqKafkaTemplate;
    private final String dlqTopic;
    private final AtomicInteger retryCount = new AtomicInteger(0);

    public HeartbeatKafkaErrorHandler(
            int maxRetries,
            HeartbeatMetrics heartbeatMetrics,
            KafkaTemplate<String, HeartbeatPayload> dlqKafkaTemplate,
            @Value("${app.heartbeat.kafka.dlq.topic:heartbeat-queue-dlq}") String dlqTopic) {
        this.maxRetries = maxRetries;
        this.heartbeatMetrics = heartbeatMetrics;
        this.dlqKafkaTemplate = dlqKafkaTemplate;
        this.dlqTopic = dlqTopic;
    }

    /**
     * Handles errors for batch listeners.
     * <p>
     * This method is called when a batch listener throws an exception.
     * Implements retry logic with exponential backoff and DLQ routing.
     *
     * @param thrownException the exception that was thrown
     * @param records the list of records that failed (can be null for non-batch listeners)
     * @param consumer the Kafka consumer
     * @param container the message listener container
     */
    @Override
    public void handleRemaining(
            Exception thrownException,
            List<ConsumerRecord<?, ?>> records,
            Consumer<?, ?> consumer,
            MessageListenerContainer container) {

        if (records == null || records.isEmpty()) {
            // Non-batch listener or empty batch
            log.error("Error in heartbeat consumer (non-batch)", thrownException);
            handleOtherException(thrownException, consumer, container, false);
            return;
        }

        log.error("Error processing heartbeat batch of {} records", records.size(), thrownException);

        int currentRetry = retryCount.incrementAndGet();
        if (currentRetry <= maxRetries) {
            // Retry with exponential backoff
            long backoffMs = (long) Math.pow(2, currentRetry - 1) * 1000; // 1s, 2s, 4s, ...
            log.warn("Retrying batch (attempt {}/{}) after {}ms", currentRetry, maxRetries, backoffMs);

            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted during retry backoff");
                return;
            }

            // Re-throw to trigger retry
            throw new RuntimeException("Batch processing failed, retrying", thrownException);
        } else {
            // Max retries exceeded, send to DLQ
            log.error("Max retries ({}) exceeded for batch, sending to DLQ: {}", maxRetries, dlqTopic);
            sendToDlq(records);
            if (heartbeatMetrics != null) {
                heartbeatMetrics.recordBatchFailed();
            }
            retryCount.set(0); // Reset for next batch
        }
    }

    private void sendToDlq(List<ConsumerRecord<?, ?>> records) {
        if (dlqKafkaTemplate == null || dlqTopic == null) {
            log.warn("DLQ not configured, skipping DLQ routing");
            return;
        }

        for (ConsumerRecord<?, ?> record : records) {
            try {
                if (record.value() instanceof HeartbeatPayload payload) {
                    String key = record.key() != null ? record.key().toString() : "unknown";
                    dlqKafkaTemplate.send(dlqTopic, key, payload);
                    if (heartbeatMetrics != null) {
                        heartbeatMetrics.recordDlqSent();
                    }
                }
            } catch (Exception e) {
                log.error("Failed to send record to DLQ", e);
            }
        }
    }

    @Override
    public void handleOtherException(Exception thrownException, Consumer<?, ?> consumer, MessageListenerContainer container, boolean batchListener) {
        log.error("Unexpected error in heartbeat consumer (batchListener={})", batchListener, thrownException);
    }
}

