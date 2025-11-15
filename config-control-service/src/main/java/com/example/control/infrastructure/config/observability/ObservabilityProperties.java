package com.example.control.infrastructure.config.observability;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for observability (tracing, metrics, logging).
 * <p>
 * Maps properties from {@code app.observability.*} in application.yml.
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "app.observability")
public class ObservabilityProperties {

    /**
     * Application environment (development, test, production).
     */
    @NotBlank
    private String environment = "development";

    /**
     * OTLP (OpenTelemetry Protocol) configuration.
     */
    private Otlp otlp = new Otlp();

    @Data
    public static class Otlp {
        /**
         * OTLP tracing endpoint URL.
         */
        private String tracingEndpoint = "http://alloy:4318/v1/traces";

        /**
         * OTLP metrics endpoint URL.
         */
        private String metricsUrl = "http://alloy:4318/v1/metrics";
    }
}

