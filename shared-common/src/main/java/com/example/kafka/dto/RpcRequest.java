package com.example.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Generic RPC request envelope sent over Kafka from ThriftServerService to WatcherService. */
public record RpcRequest(
    @JsonProperty("correlationId") String correlationId,
    @JsonProperty("action") String action,
    @JsonProperty("payload") Object payload
) {}


