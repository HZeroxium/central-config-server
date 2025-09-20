package com.example.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generic RPC response envelope sent over Kafka from WatcherService to
 * ThriftServerService.
 */
public record RpcResponse(
        @JsonProperty("correlationId") String correlationId,
        @JsonProperty("status") String status,
        @JsonProperty("error") String error,
        @JsonProperty("payload") Object payload) {

    /**
     * Creates a successful RPC response with a typed payload.
     * This method provides better type safety than using Object directly.
     */
    public static <T> RpcResponse success(String correlationId, T payload) {
        return new RpcResponse(correlationId, "ok", null, payload);
    }

    /**
     * Creates an error RPC response.
     */
    public static RpcResponse error(String correlationId, String error) {
        return new RpcResponse(correlationId, "error", error, null);
    }

    /**
     * Creates a not found RPC response.
     */
    public static RpcResponse notFound(String correlationId, String message) {
        return new RpcResponse(correlationId, "not_found", message, null);
    }
}
