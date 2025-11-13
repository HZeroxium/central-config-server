package com.example.control.infrastructure.config.messaging;

import com.example.control.domain.model.HeartbeatPayload;
import com.example.control.infrastructure.observability.heartbeat.HeartbeatMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

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
public class HeartbeatKafkaListenerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.heartbeat.kafka.consumer.concurrency:10}")
    private int concurrency;

    @Value("${app.heartbeat.kafka.consumer.max-retries:3}")
    private int maxRetries;

    @Value("${app.heartbeat.kafka.dlq.topic:heartbeat-queue-dlq}")
    private String dlqTopic;

    private final ObjectMapper objectMapper;
    private final HeartbeatMetrics heartbeatMetrics;
    private final KafkaTemplate<String, HeartbeatPayload> dlqKafkaTemplate;

    public HeartbeatKafkaListenerConfig(
            ObjectMapper objectMapper,
            HeartbeatMetrics heartbeatMetrics,
            @Qualifier("heartbeatKafkaTemplate") KafkaTemplate<String, HeartbeatPayload> dlqKafkaTemplate) {
        this.objectMapper = objectMapper;
        this.heartbeatMetrics = heartbeatMetrics;
        this.dlqKafkaTemplate = dlqKafkaTemplate;
    }

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
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

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
     * Configures batch listener mode, manual acknowledgment, and error handling.
     *
     * @return listener container factory
     */
    @Bean(name = "heartbeatKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, HeartbeatPayload> heartbeatKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, HeartbeatPayload> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(heartbeatConsumerFactory());
        factory.setBatchListener(true); // Enable batch mode
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Configure error handler for DLQ routing
        HeartbeatKafkaErrorHandler errorHandler = new HeartbeatKafkaErrorHandler(
                maxRetries, heartbeatMetrics, dlqKafkaTemplate, dlqTopic);
        factory.setCommonErrorHandler(errorHandler);

        log.info("Created heartbeat Kafka listener container factory with batch mode, concurrency={}", concurrency);
        return factory;
    }
}

