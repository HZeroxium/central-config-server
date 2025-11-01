package com.example.control.infrastructure.config.observability;

import io.micrometer.tracing.exporter.SpanExportingPredicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Simplified OpenTelemetry configuration using Spring Boot auto-configuration.
 * <p>
 * This configuration:
 * - Configures span exporting for better trace sampling
 * - Relies on Spring Boot's auto-configuration for OpenTelemetry setup
 * - Common tags are configured in application-observability.yml via
 * {@code management.metrics.tags}
 * </p>
 */
@Slf4j
@Configuration
public class OpenTelemetryConfig {

    @Value("${app.environment:development}")
    private String environment;

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
            return spanName != null && (spanName.startsWith("http.server.requests") ||
                    spanName.startsWith("config_control") ||
                    spanName.startsWith("heartbeat") ||
                    spanName.contains("error"));
        };
    }
}
