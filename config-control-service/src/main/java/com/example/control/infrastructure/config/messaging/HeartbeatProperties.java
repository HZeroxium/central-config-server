package com.example.control.infrastructure.config.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for heartbeat processing.
 * <p>
 * Maps properties from {@code app.heartbeat.*} in application.yml.
 * Consolidates all heartbeat-related configuration.
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "app.heartbeat")
public class HeartbeatProperties {

    /**
     * Enable async heartbeat processing via Kafka.
     */
    private boolean asyncEnabled = true;

    /**
     * Kafka configuration for heartbeat messages.
     */
    private Kafka kafka = new Kafka();

    @Data
    public static class Kafka {
        /**
         * Kafka topic for heartbeat messages.
         */
        @NotBlank
        private String topic = "heartbeat-queue";

        /**
         * Consumer configuration.
         */
        private Consumer consumer = new Consumer();

        /**
         * Dead letter queue topic for failed heartbeat messages.
         */
        @NotBlank
        private String dlqTopic = "heartbeat-queue-dlq";

        @Data
        public static class Consumer {
            /**
             * Number of concurrent consumer threads.
             */
            @Positive
            private int concurrency = 10;

            /**
             * Maximum number of retries before sending to DLQ.
             */
            @Positive
            private int maxRetries = 3;
        }
    }
}

