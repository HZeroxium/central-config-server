package com.example.configserver.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Conditional configuration to disable Kafka when not needed
 */
@Configuration
@ConditionalOnProperty(
    name = "spring.cloud.bus.enabled", 
    havingValue = "false", 
    matchIfMissing = false
)
public class KafkaConditionalConfiguration {
    // This class exists to provide a place for @ConditionalOnProperty
    // The actual Kafka auto-configuration will be disabled by the property
}
