package com.example.control.infrastructure.config.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Simplified OpenTelemetry configuration using Spring Boot auto-configuration.
 * <p>
 * This configuration:
 * - Configures meter registry with exemplars support for trace correlation
 * - Adds common tags for service identification
 * - Configures span exporting for better trace sampling
 * - Relies on Spring Boot's auto-configuration for OpenTelemetry setup
 */
@Slf4j
@Configuration
public class OpenTelemetryConfig {

    @Value("${app.name:config-control-service}")
    private String serviceName;

    @Value("${app.version:1.0.0}")
    private String serviceVersion;

    @Value("${app.environment:development}")
    private String environment;

    /**
     * Configures meter registry with exemplars support for trace correlation.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsConfigCustomizer() {
        return registry -> {
            log.info("Configuring MeterRegistry with exemplars support");

            // Configure meter filters for better metrics
            registry.config().meterFilter(MeterFilter.accept());

            // Add common tags for service identification
            registry.config().commonTags(
                    "service.name", serviceName,
                    "service.version", serviceVersion,
                    "deployment.environment", environment
            );

            log.info("MeterRegistry configured with service tags: service.name={}, service.version={}, environment={}",
                    serviceName, serviceVersion, environment);
        };
    }

    /**
     * Configures span exporting predicate for better trace sampling control.
     * Only export spans for important operations to reduce noise.
     */
    @Bean
    public SpanExportingPredicate spanExportingPredicate() {
        return span -> {
            // Export all spans in development, filter in production
            if ("development".equals(environment) || "test".equals(environment)) {
                return true;
            }

            // In production, only export spans for important operations
            String spanName = span.getName();
            return spanName != null && (
                    spanName.startsWith("http.server.requests") ||
                            spanName.startsWith("config_control") ||
                            spanName.startsWith("heartbeat") ||
                            spanName.contains("error")
            );
        };
    }
}
