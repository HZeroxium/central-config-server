package com.example.control.infrastructure.config.messaging;

import com.example.control.domain.model.HeartbeatPayload;
import com.example.control.infrastructure.config.messaging.HeartbeatProperties;
import com.example.control.infrastructure.observability.heartbeat.HeartbeatMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka listener configuration for heartbeat batch processing.
 * <p>
 * Configures batch listener mode with manual acknowledgment and error handling
 * for dead letter queue routing.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HeartbeatKafkaListenerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final HeartbeatProperties heartbeatProperties;
    private final ObjectMapper objectMapper;
    private final HeartbeatMetrics heartbeatMetrics;
    private final KafkaTemplate<String, HeartbeatPayload> dlqKafkaTemplate;

    /**
     * Consumer factory for heartbeat messages.
     * <p>
     * Configures JSON deserialization for HeartbeatPayload values and String
     * deserialization for keys (serviceName).
     *
     * @return consumer factory
     */
    @Bean
    public ConsumerFactory<String, HeartbeatPayload> heartbeatConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "heartbeat-batch-processor");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit
        
        // Batch processing tuning (configurable via HeartbeatProperties)
        HeartbeatProperties.Kafka.Consumer consumerConfig = heartbeatProperties.getKafka().getConsumer();
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerConfig.getMaxPollRecords());
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, consumerConfig.getFetchMinBytes());
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, consumerConfig.getFetchMaxWaitMs());
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, consumerConfig.getMaxPollIntervalMs());

        // Error handling deserializers
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // Configure JsonDeserializer
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, HeartbeatPayload.class);

        DefaultKafkaConsumerFactory<String, HeartbeatPayload> factory =
                new DefaultKafkaConsumerFactory<>(configProps);
        factory.setValueDeserializer(new JsonDeserializer<>(HeartbeatPayload.class, objectMapper));

        log.info("Created heartbeat Kafka consumer factory with JSON deserialization");
        return factory;
    }

    /**
     * Kafka listener container factory for batch processing.
     * <p>
     * Configures batch listener mode, manual acknowledgment, and error handling
     * using Spring Kafka best practices:
     * <ul>
     * <li>DefaultErrorHandler with exponential backoff for retries</li>
     * <li>DeadLetterPublishingRecoverer for DLQ routing with metadata headers</li>
     * </ul>
     *
     * @return listener container factory
     */
    @Bean(name = "heartbeatKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, HeartbeatPayload> heartbeatKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, HeartbeatPayload> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(heartbeatConsumerFactory());
        factory.setBatchListener(true); // Enable batch mode
        factory.setConcurrency(heartbeatProperties.getKafka().getConsumer().getConcurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Configure error handler with Spring Kafka best practices
        CommonErrorHandler errorHandler = createErrorHandler();
        factory.setCommonErrorHandler(errorHandler);

        log.info("Created heartbeat Kafka listener container factory with batch mode, concurrency={}, using DefaultErrorHandler with exponential backoff",
                heartbeatProperties.getKafka().getConsumer().getConcurrency());
        return factory;
    }

    /**
     * Creates a CommonErrorHandler using Spring Kafka best practices.
     * <p>
     * Uses DefaultErrorHandler with ExponentialBackOff for retries and
     * DeadLetterPublishingRecoverer for DLQ routing with metadata headers.
     *
     * @return configured error handler
     */
    private CommonErrorHandler createErrorHandler() {
        HeartbeatProperties.Kafka.Consumer.Retry retryConfig = heartbeatProperties.getKafka().getConsumer().getRetry();
        
        // Configure exponential backoff
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(retryConfig.getInitialIntervalMs());
        backOff.setMultiplier(retryConfig.getMultiplier());
        backOff.setMaxInterval(retryConfig.getMaxIntervalMs());
        backOff.setMaxElapsedTime(retryConfig.getMaxRetries() * retryConfig.getMaxIntervalMs());

        // Create DeadLetterPublishingRecoverer with custom header extraction
        String dlqTopic = heartbeatProperties.getKafka().getDlqTopic();
        org.springframework.kafka.listener.DeadLetterPublishingRecoverer recoverer =
                new org.springframework.kafka.listener.DeadLetterPublishingRecoverer(
                        dlqKafkaTemplate,
                        (record, ex) -> {
                            // Use DLQ topic from configuration, partition 0 (DLQ typically uses single partition)
                            return new TopicPartition(dlqTopic, 0);
                        });

        // Configure header extraction for DLQ messages
        // HeadersFunction receives (record, exception) and returns new Headers
        recoverer.setHeadersFunction((record, ex) -> {
            // Create new headers based on original headers
            RecordHeaders headers = new RecordHeaders();
            
            // Copy original headers
            if (record.headers() != null) {
                for (Header header : record.headers()) {
                    headers.add(header);
                }
            }
            
            // Add original topic metadata
            headers.add("x-original-topic", record.topic().getBytes(StandardCharsets.UTF_8));
            headers.add("x-original-partition", String.valueOf(record.partition()).getBytes(StandardCharsets.UTF_8));
            headers.add("x-original-offset", String.valueOf(record.offset()).getBytes(StandardCharsets.UTF_8));
            
            // Add exception metadata
            if (ex != null) {
                String exceptionMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                headers.add("x-exception-message", exceptionMessage.getBytes(StandardCharsets.UTF_8));
                headers.add("x-exception-class", ex.getClass().getName().getBytes(StandardCharsets.UTF_8));
                
                // Retry count will be set by retry listener (we track max retries)
                headers.add("x-retry-count", String.valueOf(retryConfig.getMaxRetries()).getBytes(StandardCharsets.UTF_8));
            }
            
            return headers;
        });

        // Create DefaultErrorHandler with backoff and recoverer
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        
        // Record metrics on DLQ routing
        errorHandler.setCommitRecovered(true); // Commit offset after successful DLQ routing
        
        // Add listener to record metrics when records are sent to DLQ
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            if (deliveryAttempt == retryConfig.getMaxRetries() + 1) {
                // This is the attempt that will go to DLQ
                heartbeatMetrics.recordBatchFailed();
                log.warn("Heartbeat batch failed after {} retries, routing to DLQ", retryConfig.getMaxRetries());
            } else {
                log.debug("Heartbeat batch retry attempt {}/{}", deliveryAttempt, retryConfig.getMaxRetries());
            }
        });

        return errorHandler;
    }
}

