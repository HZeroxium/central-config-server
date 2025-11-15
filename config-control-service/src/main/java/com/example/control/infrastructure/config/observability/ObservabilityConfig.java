package com.example.control.infrastructure.config.observability;

import com.example.control.infrastructure.config.observability.ObservabilityProperties;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observability configuration for distributed tracing support.
 * <p>
 * This configuration prepares for future distributed tracing support via OTLP.
 * Currently using Prometheus-only stack for metrics (scraping /actuator/prometheus).
 * </p>
 * <p>
 * Features:
 * - Configures span exporting for better trace sampling control
 * - Relies on Spring Boot's auto-configuration for OpenTelemetry setup
 * - Common tags are configured in application-observability.yml via
 * {@code management.metrics.tags}
 * </p>
 * <p>
 * <b>Current stack:</b> Prometheus scraping for metrics
 * <br>
 * <b>Future support:</b> When Tempo/Alloy enabled, tracing will automatically export via OTLP
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ObservabilityConfig {

    private final ObservabilityProperties observabilityProperties;

    /**
     * Configures span exporting predicate for better trace sampling control.
     * Only export spans for important operations to reduce noise.
     */
    @Bean
    public SpanExportingPredicate spanExportingPredicate() {
        return span -> {
            // Export all spans in development, filter in production
            String env = observabilityProperties.getEnvironment();
            if ("development".equals(env) || "test".equals(env)) {
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
