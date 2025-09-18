package com.example.user.kafka;

import com.example.kafka.config.KafkaCommonConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Kafka configuration for Worker service
 * Imports shared configuration and adds worker-specific beans
 */
@Configuration
@Import(KafkaCommonConfig.class)
public class KafkaConfig {
    // Worker-specific Kafka configuration can be added here
    // Currently using shared configuration from KafkaCommonConfig
}
