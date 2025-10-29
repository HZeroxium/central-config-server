package com.example.control.infrastructure.consulclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

/**
 * Health service entry combining node, service, and health checks.
 */
@Builder
public record HealthService(
        @JsonProperty("Node")
        Node node,

        @JsonProperty("Service")
        Service service,

        @JsonProperty("Checks")
        List<HealthCheck> checks
) {

    /**
     * Check if all health checks are passing.
     *
     * @return true if all checks are passing
     */
    public boolean isHealthy() {
        if (checks == null || checks.isEmpty()) {
            return true; // No checks means healthy
        }
        return checks.stream().allMatch(HealthCheck::isPassing);
    }

    /**
     * Check if any health checks are critical.
     *
     * @return true if any check is critical
     */
    public boolean hasCriticalChecks() {
        if (checks == null || checks.isEmpty()) {
            return false;
        }
        return checks.stream().anyMatch(HealthCheck::isCritical);
    }

    /**
     * Get the service address, falling back to node address.
     *
     * @return service address or node address
     */
    public String getEffectiveAddress() {
        if (service != null && service.address() != null && !service.address().isEmpty()) {
            return service.address();
        }
        if (node != null && node.address() != null) {
            return node.address();
        }
        return null;
    }

    /**
     * Get the service port.
     *
     * @return service port or 0 if not set
     */
    public int getEffectivePort() {
        if (service != null) {
            return service.port();
        }
        return 0;
    }
}
