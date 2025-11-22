package com.example.control.infrastructure.config.messaging;

import com.example.control.domain.model.HeartbeatPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka listener configuration for DLQ heartbeat batch processing.
 * <p>
 * Configures a separate consumer factory and listener container factory for
 * processing failed heartbeats from the DLQ topic. Uses lower concurrency
 * to avoid flooding MongoDB with writes.
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HeartbeatDlqConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final HeartbeatProperties heartbeatProperties;
    private final ObjectMapper objectMapper;

    /**
     * Consumer factory for DLQ heartbeat messages.
     * <p>
     * Configures JSON deserialization for HeartbeatPayload values and String
     * deserialization for keys (serviceName).
     *
     * @return consumer factory for DLQ
     */
    @Bean
    public ConsumerFactory<String, HeartbeatPayload> heartbeatDlqConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "heartbeat-dlq-processor");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit

        // DLQ consumer tuning (lower concurrency than main consumer)
        HeartbeatProperties.Kafka.DlqConsumer dlqConsumerConfig = heartbeatProperties.getKafka().getDlqConsumer();
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, dlqConsumerConfig.getMaxPollRecords());
        
        // Use shorter intervals for DLQ consumer (less critical than main processing)
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024); // Lower than main consumer
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500); // Lower than main consumer
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 60000); // 1 minute (shorter than main)

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

        log.info("Created heartbeat DLQ Kafka consumer factory with JSON deserialization");
        return factory;
    }

    /**
     * Kafka listener container factory for DLQ batch processing.
     * <p>
     * Configures batch listener mode with manual acknowledgment and lower concurrency
     * to avoid flooding MongoDB with writes.
     *
     * @return listener container factory for DLQ
     */
    @Bean(name = "heartbeatDlqKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, HeartbeatPayload> heartbeatDlqKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, HeartbeatPayload> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(heartbeatDlqConsumerFactory());
        factory.setBatchListener(true); // Enable batch mode
        factory.setConcurrency(heartbeatProperties.getKafka().getDlqConsumer().getConcurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // No error handler for DLQ consumer - failures are logged but records remain in DLQ
        // This prevents infinite loops if MongoDB is down

        log.info("Created heartbeat DLQ Kafka listener container factory with batch mode, concurrency={}",
                heartbeatProperties.getKafka().getDlqConsumer().getConcurrency());
        return factory;
    }
}

