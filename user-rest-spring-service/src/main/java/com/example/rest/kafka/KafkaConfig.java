package com.example.rest.kafka;

import com.example.kafka.config.KafkaCommonConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Kafka configuration for Orchestrator service
 * Imports shared configuration and adds orchestrator-specific beans
 */
@Configuration
@Import(KafkaCommonConfig.class)
public class KafkaConfig {
    // Orchestrator-specific Kafka configuration can be added here
    // Currently using shared configuration from KafkaCommonConfig
}
