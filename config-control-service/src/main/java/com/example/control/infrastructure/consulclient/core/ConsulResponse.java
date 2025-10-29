package com.example.control.infrastructure.consulclient.core;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.Map;

/**
 * Generic response wrapper for Consul API calls with metadata.
 *
 * @param <T> the response body type
 */
@Data
@Builder
public class ConsulResponse<T> {

    /**
     * The response body.
     */
    private final T body;

    /**
     * Consul index for this response (X-Consul-Index header).
     */
    private final Long consulIndex;

    /**
     * Whether the response is from a known leader (X-Consul-KnownLeader header).
     */
    private final Boolean knownLeader;

    /**
     * Time since last contact with leader (X-Consul-LastContact header).
     */
    private final Duration lastContact;

    /**
     * All response headers.
     */
    private final Map<String, String> headers;

    /**
     * Create a response with just a body (no metadata).
     *
     * @param body the response body
     * @return ConsulResponse instance
     */
    public static <T> ConsulResponse<T> of(T body) {
        return ConsulResponse.<T>builder()
                .body(body)
                .build();
    }

    /**
     * Create a response with body and index.
     *
     * @param body        the response body
     * @param consulIndex the consul index
     * @return ConsulResponse instance
     */
    public static <T> ConsulResponse<T> of(T body, Long consulIndex) {
        return ConsulResponse.<T>builder()
                .body(body)
                .consulIndex(consulIndex)
                .build();
    }
}
