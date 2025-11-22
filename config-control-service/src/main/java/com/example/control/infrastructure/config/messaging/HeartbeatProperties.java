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

    /**
     * Drift detection configuration.
     */
    private DriftDetection driftDetection = new DriftDetection();

    @Data
    public static class DriftDetection {
        /**
         * Number of consecutive drift detections required before creating a DriftEvent.
         * <p>
         * Defaults to 3 to avoid creating events for transient drift issues.
         * Refresh events are still triggered immediately on first drift detection.
         */
        @Positive
        private int eventThreshold = 3;
    }

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

        /**
         * DLQ consumer configuration.
         */
        private DlqConsumer dlqConsumer = new DlqConsumer();

        @Data
        public static class Consumer {
            /**
             * Number of concurrent consumer threads.
             */
            @Positive
            private int concurrency = 10;

            /**
             * Maximum number of retries before sending to DLQ.
             * <p>
             * @deprecated Use {@link Retry#maxRetries} instead. This field is kept for backward compatibility.
             */
            @Deprecated
            @Positive
            private int maxRetries = 3;

            /**
             * Retry configuration for error handling.
             */
            private Retry retry = new Retry();

            /**
             * Maximum number of records to poll in a single batch.
             * <p>
             * Increased from default 100 to 200 for better throughput.
             * Can be tuned based on message size and processing time.
             */
            @Positive
            private int maxPollRecords = 200;

            /**
             * Maximum wait time for fetch.min.bytes to accumulate (milliseconds).
             * <p>
             * Increased from default 500ms to 1000ms to allow larger batches.
             */
            @Positive
            private int fetchMaxWaitMs = 1000;

            /**
             * Minimum bytes to accumulate before returning data (bytes).
             * <p>
             * Increased from default 1024 to 8192 for better batching.
             */
            @Positive
            private int fetchMinBytes = 8192;

            /**
             * Maximum poll interval in milliseconds.
             * <p>
             * Prevents consumer rebalancing when processing takes longer.
             * Should be set to max processing time + buffer.
             */
            @Positive
            private int maxPollIntervalMs = 300000; // 5 minutes

            @Data
            public static class Retry {
                /**
                 * Initial retry interval in milliseconds.
                 * <p>
                 * First retry will wait this amount before attempting again.
                 */
                @Positive
                private long initialIntervalMs = 1000;

                /**
                 * Multiplier for exponential backoff.
                 * <p>
                 * Each retry will wait: initialIntervalMs * (multiplier ^ retryAttempt).
                 * Example: 1000ms * (2.0 ^ 0) = 1000ms, 1000ms * (2.0 ^ 1) = 2000ms, etc.
                 */
                @Positive
                private double multiplier = 2.0;

                /**
                 * Maximum number of retries before sending to DLQ.
                 */
                @Positive
                private int maxRetries = 3;

                /**
                 * Maximum retry interval in milliseconds.
                 * <p>
                 * Caps the exponential backoff to prevent excessive wait times.
                 */
                @Positive
                private long maxIntervalMs = 4000;
            }
        }

        @Data
        public static class DlqConsumer {
            /**
             * Number of concurrent DLQ consumer threads.
             * <p>
             * Lower concurrency than main consumer to avoid flooding MongoDB.
             */
            @Positive
            private int concurrency = 2;

            /**
             * Maximum number of records to poll in a single batch for DLQ consumer.
             */
            @Positive
            private int maxPollRecords = 50;
        }
    }
}

