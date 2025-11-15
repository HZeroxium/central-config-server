package com.example.control.infrastructure.config.misc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Consul client.
 * <p>
 * Maps properties from {@code consul.*} in application.yml.
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "consul")
public class ConsulProperties {

    /**
     * Consul server base URL, e.g. http://consul:8500
     */
    @NotBlank
    private String url = "http://localhost:8500";

    /**
     * Connection timeout in milliseconds.
     */
    @Positive
    private long timeout = 5000;

    /**
     * Retry configuration.
     */
    private Retry retry = new Retry();

    @Data
    public static class Retry {
        /**
         * Maximum number of retry attempts.
         */
        @Positive
        private int maxAttempts = 3;

        /**
         * Backoff delay between retries in milliseconds.
         */
        @Positive
        private long backoffDelay = 1000;
    }
}

