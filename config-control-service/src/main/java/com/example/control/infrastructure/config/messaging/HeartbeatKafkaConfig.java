package com.example.control.infrastructure.config.messaging;

import com.example.control.domain.model.HeartbeatPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for heartbeat ingestion.
 * <p>
 * Creates a dedicated KafkaTemplate for HeartbeatPayload serialization with
 * JSON serializer. Uses serviceName as partition key to ensure ordering per service.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HeartbeatKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final ObjectMapper objectMapper;

    /**
     * Producer factory for heartbeat messages.
     * <p>
     * Configures JSON serialization for HeartbeatPayload values and String
     * serialization for keys (serviceName).
     *
     * @return producer factory
     */
    @Bean
    public ProducerFactory<String, HeartbeatPayload> heartbeatProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip"); // Changed from snappy to gzip (no native libraries required)
        configProps.put(ProducerConfig.ACKS_CONFIG, "1"); // Leader acknowledgment

        // Configure JsonSerializer to use our ObjectMapper
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        DefaultKafkaProducerFactory<String, HeartbeatPayload> factory =
                new DefaultKafkaProducerFactory<>(configProps);
        factory.setValueSerializer(new JsonSerializer<>(objectMapper));

        log.info("Created heartbeat Kafka producer factory with JSON serialization");
        return factory;
    }

    /**
     * KafkaTemplate for sending heartbeat messages.
     * <p>
     * Uses serviceName as partition key to ensure ordering per service.
     *
     * @return KafkaTemplate for HeartbeatPayload
     */
    @Bean(name = "heartbeatKafkaTemplate")
    public KafkaTemplate<String, HeartbeatPayload> heartbeatKafkaTemplate() {
        KafkaTemplate<String, HeartbeatPayload> template =
                new KafkaTemplate<>(heartbeatProducerFactory());
        log.info("Created heartbeat KafkaTemplate");
        return template;
    }
}

