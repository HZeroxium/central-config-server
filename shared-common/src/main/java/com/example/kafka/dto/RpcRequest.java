package com.example.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generic RPC request envelope sent over Kafka from ThriftServerService to
 * WatcherService.
 */
public record RpcRequest(
        @JsonProperty("correlationId") String correlationId,
        @JsonProperty("action") String action,
        @JsonProperty("payload") Object payload) {

    /**
     * Creates an RPC request with a typed payload.
     * This method provides better type safety than using Object directly.
     */
    public static <T> RpcRequest of(String correlationId, String action, T payload) {
        return new RpcRequest(correlationId, action, payload);
    }
}
