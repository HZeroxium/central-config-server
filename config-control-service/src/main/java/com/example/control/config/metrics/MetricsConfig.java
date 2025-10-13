package com.example.control.config.metrics;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Core metrics configuration for config-control-service.
 * <p>
 * This configuration:
 * <ul>
 *   <li>Sets up Prometheus MeterRegistry with custom configuration</li>
 *   <li>Configures common tags for all metrics</li>
 *   <li>Sets up histogram percentiles and SLO boundaries</li>
 *   <li>Enables TimedAspect for @Timed annotation support</li>
 * </ul>
 */
@Slf4j
@Configuration
public class MetricsConfig {

    @Value("${app.environment:development}")
    private String environment;

    /**
     * Note: Using default MeterRegistry from Spring Boot AutoConfiguration
     * with Prometheus support enabled in application.yml
     */

    /**
     * Customizer for MeterRegistry to add common tags and configuration.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            // Add common tags
            registry.config().commonTags(
                "environment", environment,
                "application", "config-control-service",
                "service", "config-control-service"
            );

            log.info("Configured common tags for metrics: environment={}", environment);
        };
    }

    /**
     * TimedAspect for @Timed annotation support.
     * <p>
     * Enables automatic timing of methods annotated with @Timed.
     * This aspect works with the configured MeterRegistry to create
     * Timer metrics for method execution times.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        log.info("Enabled TimedAspect for @Timed annotation support");
        return new TimedAspect(registry);
    }

}
