package com.vng.zing.zcm.pingconfig.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vng.zing.zcm.pingconfig.HeartbeatPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer configuration for ping operations.
 * <p>
 * Creates a dedicated KafkaTemplate for HeartbeatPayload serialization with
 * JSON serializer. Uses serviceName as partition key to ensure ordering per service.
 * <p>
 * The bootstrap servers are obtained from KafkaConfigCache (which fetches from
 * config-control-service) or fallback to properties/env vars.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "zcm.sdk.ping", name = "protocol", havingValue = "KAFKA", matchIfMissing = false)
public class KafkaPingProducerConfig {

    private final KafkaConfigCache kafkaConfigCache;
    private final ObjectMapper objectMapper;

    /**
     * Producer factory for ping heartbeat messages.
     * <p>
     * Configures JSON serialization for HeartbeatPayload values and String
     * serialization for keys (serviceName). Bootstrap servers are resolved
     * dynamically from KafkaConfigCache or fallback configuration.
     *
     * @return producer factory
     */
    @Bean
    public ProducerFactory<String, HeartbeatPayload> pingKafkaProducerFactory() {
        // Get bootstrap servers from cache or fallback
        KafkaConfig config = kafkaConfigCache.get();
        if (config == null || !config.isValid()) {
            throw new IllegalStateException(
                    "Cannot create Kafka producer factory: no valid Kafka configuration available. "
                            + "Ensure config-control-service is accessible or configure zcm.sdk.ping.kafka.bootstrap-servers");
        }

        String bootstrapServers = config.bootstrapServers();
        log.info("Creating Kafka producer factory with bootstrap servers: {}", bootstrapServers);

        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip"); // No native deps required
        configProps.put(ProducerConfig.ACKS_CONFIG, "1"); // Leader acknowledgment for low latency
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // Retry up to 3 times
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100); // Exponential backoff base

        // Configure JsonSerializer to use our ObjectMapper
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        DefaultKafkaProducerFactory<String, HeartbeatPayload> factory =
                new DefaultKafkaProducerFactory<>(configProps);
        JsonSerializer<HeartbeatPayload> valueSerializer = new JsonSerializer<>(objectMapper);
        factory.setValueSerializer(valueSerializer);

        log.info("Created ping Kafka producer factory with JSON serialization");
        return factory;
    }

    /**
     * KafkaTemplate for sending ping heartbeat messages.
     * <p>
     * Uses serviceName as partition key to ensure ordering per service.
     *
     * @param producerFactory producer factory
     * @return KafkaTemplate for HeartbeatPayload
     */
    @Bean(name = "pingKafkaTemplate")
    public KafkaTemplate<String, HeartbeatPayload> pingKafkaTemplate(
            ProducerFactory<String, HeartbeatPayload> producerFactory) {
        KafkaTemplate<String, HeartbeatPayload> template = new KafkaTemplate<>(producerFactory);
        log.info("Created ping KafkaTemplate");
        return template;
    }
}

