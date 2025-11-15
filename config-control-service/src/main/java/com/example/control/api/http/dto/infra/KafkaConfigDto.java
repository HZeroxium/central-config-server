package com.example.control.api.http.dto.infra;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTOs for Kafka infrastructure configuration.
 * <p>
 * Provides data transfer objects for exposing Kafka configuration to SDK clients
 * for heartbeat ping operations.
 * </p>
 */
@Schema(name = "KafkaConfigDto", description = "DTOs for Kafka infrastructure configuration")
public final class KafkaConfigDto {

    private KafkaConfigDto() {
        throw new UnsupportedOperationException("Utility class");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "KafkaConfigResponse", description = "Kafka configuration response for SDK clients")
    public static class KafkaConfigResponse {
        @Schema(
                description = "Kafka bootstrap servers (comma-separated list)",
                example = "kafka:9092",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String bootstrapServers;

        @Schema(
                description = "Kafka topic for heartbeat messages",
                example = "heartbeat-queue",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String topic;
    }
}

