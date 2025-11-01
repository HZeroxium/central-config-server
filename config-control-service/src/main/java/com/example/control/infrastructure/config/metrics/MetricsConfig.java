package com.example.control.infrastructure.config.metrics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Core metrics configuration for config-control-service.
 * <p>
 * This configuration file is kept for future extensibility.
 * <p>
 * Note: Common tags are configured in application-observability.yml via
 * {@code management.metrics.tags}. Annotation scanning for @Observed/@Timed/@Counted
 * is enabled via {@code management.observations.annotations.enabled=true} in
 * application-observability.yml. JVM metrics are auto-configured by Spring Boot
 * when {@code management.metrics.enable.jvm=true} (already enabled).
 * <p>
 * All metrics configuration is now handled via Spring Boot auto-configuration
 * and YAML properties to avoid duplication and follow best practices.
 */
@Slf4j
@Configuration
public class MetricsConfig {
    // Metrics configuration is now handled via:
    // - application-observability.yml (common tags, distribution, histogram)
    // - Spring Boot auto-configuration (JVM, system, HTTP, cache, database metrics)
    // - management.observations.annotations.enabled=true (annotation scanning)
}
