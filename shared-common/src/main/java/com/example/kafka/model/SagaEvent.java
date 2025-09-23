package com.example.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Base model for Saga events and commands
 * Used across all services for consistent event structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaEvent {

    @JsonProperty("type")
    private String type;

    @JsonProperty("sagaId")
    private String sagaId;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("causationId")
    private String causationId;

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("source")
    private String source;

    @JsonProperty("ts")
    private long timestamp;

    @JsonProperty("data")
    private Map<String, Object> data;

    @JsonProperty("error")
    private String error;

    @JsonProperty("result")
    private Map<String, Object> result;

    // Factory methods for common event types
    public static SagaEvent command(String type, String sagaId, String correlationId, Map<String, Object> data) {
        return SagaEvent.builder()
                .type(type)
                .sagaId(sagaId)
                .correlationId(correlationId)
                .causationId(java.util.UUID.randomUUID().toString())
                .eventId(java.util.UUID.randomUUID().toString())
                .source("thrift-client-service")
                .timestamp(Instant.now().toEpochMilli())
                .data(data)
                .build();
    }

    public static SagaEvent event(String type, String sagaId, String source, Map<String, Object> result) {
        return SagaEvent.builder()
                .type(type)
                .sagaId(sagaId)
                .correlationId(sagaId) // Default to sagaId for events
                .causationId(java.util.UUID.randomUUID().toString())
                .eventId(java.util.UUID.randomUUID().toString())
                .source(source)
                .timestamp(Instant.now().toEpochMilli())
                .result(result)
                .build();
    }

    public static SagaEvent failed(String type, String sagaId, String source, String error) {
        return SagaEvent.builder()
                .type(type)
                .sagaId(sagaId)
                .correlationId(sagaId)
                .causationId(java.util.UUID.randomUUID().toString())
                .eventId(java.util.UUID.randomUUID().toString())
                .source(source)
                .timestamp(Instant.now().toEpochMilli())
                .error(error)
                .build();
    }
}
