package com.example.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Generic RPC response envelope sent over Kafka from WatcherService to ThriftServerService. */
public record RpcResponse(
    @JsonProperty("correlationId") String correlationId,
    @JsonProperty("status") String status,
    @JsonProperty("error") String error,
    @JsonProperty("payload") Object payload
) {}


