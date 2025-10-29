package com.example.control.domain.object;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Payload for heartbeat requests from service instances.
 * Contains instance metadata and configuration hash for drift detection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeartbeatPayload {
    /**
     * Service name identifier
     */
    @NotBlank(message = "Service name is required")
    private String serviceName;

    /**
     * Unique instance identifier
     */
    @NotBlank(message = "Instance ID is required")
    private String instanceId;

    /**
     * SHA-256 hash of applied configuration
     */
    private String configHash;

    /**
     * Instance host address
     */
    private String host;

    /**
     * Instance port number
     */
    @Positive(message = "Port must be a positive number")
    private Integer port;

    /**
     * Deployment environment (e.g., dev, staging, prod)
     */
    private String environment;

    /**
     * Service version
     */
    private String version;

    /**
     * Additional instance metadata
     */
    private Map<String, String> metadata;
}
