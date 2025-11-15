package com.vng.zing.zcm.pingconfig.strategy;

/**
 * Kafka configuration record for ping operations.
 * <p>
 * Represents the Kafka bootstrap servers and topic name needed to send
 * heartbeat messages via Kafka.
 */
public record KafkaConfig(
        /**
         * Kafka bootstrap servers (comma-separated list).
         * Example: "kafka:9092" or "kafka1:9092,kafka2:9092"
         */
        String bootstrapServers,

        /**
         * Kafka topic name for heartbeat messages.
         * Example: "heartbeat-queue"
         */
        String topic
) {
    /**
     * Validates that the Kafka config has required fields.
     *
     * @return true if both bootstrapServers and topic are non-null and non-empty
     */
    public boolean isValid() {
        return bootstrapServers != null && !bootstrapServers.isBlank()
                && topic != null && !topic.isBlank();
    }
}

