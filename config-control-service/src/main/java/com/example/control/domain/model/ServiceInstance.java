package com.example.control.domain.model;

import com.example.control.domain.valueobject.id.ServiceInstanceId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Domain model representing a runtime instance of a service within the system.
 * <p>
 * Each instance corresponds to a running process registered in service
 * discovery (e.g., Consul, Eureka).
 * The model includes both configuration and runtime metadata, allowing drift
 * detection and health monitoring.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInstance {

    /**
     * Composite identifier combining serviceId and instanceId.
     */
    private ServiceInstanceId id;

    /**
     * Service ID from {@link ApplicationService} (for team-based access control).
     */
    private String serviceId;

    /**
     * Team ID that owns this service instance (from
     * ApplicationService.ownerTeamId).
     */
    private String teamId;

    /**
     * Hostname or IP address of the instance.
     */
    private String host;

    /**
     * TCP port exposed by the instance.
     */
    private Integer port;

    /**
     * Active Spring profile or environment name (e.g., dev, staging, prod).
     */
    private String environment;

    /**
     * Service version reported by the instance.
     */
    private String version;

    /**
     * Current configuration hash reported by the instance (computed via
     * {@code ConfigHashCalculator}).
     */
    private String configHash;

    /**
     * Expected configuration hash from the source of truth (Config Server).
     */
    private String expectedHash;

    /**
     * Last configuration hash applied from the system of truth (SoT).
     */
    private String lastAppliedHash;

    /**
     * Current health or drift status of the instance.
     */
    private InstanceStatus status;

    /**
     * Timestamp of the last heartbeat ("ping") received from this instance.
     */
    private Instant lastSeenAt;

    /**
     * Timestamp when the instance record was first created.
     */
    private Instant createdAt;

    /**
     * Timestamp of the last update.
     */
    private Instant updatedAt;

    /**
     * Arbitrary metadata associated with the instance (from Consul, Eureka, etc.).
     */
    private Map<String, String> metadata;

    /**
     * Indicates whether configuration drift has been detected.
     */
    private Boolean hasDrift;

    /**
     * Timestamp when configuration drift was detected.
     */
    private Instant driftDetectedAt;

    /**
     * Gets the instance ID from the ID.
     *
     * @return the instance ID
     */
    public String getInstanceId() {
        return id != null ? id.instanceId() : null;
    }

    /**
     * Determines whether this instance has drifted configuration.
     *
     * @return {@code true} if drift detected, otherwise {@code false}
     */
    public boolean isDrifted() {
        return Boolean.TRUE.equals(hasDrift);
    }

    /**
     * Marks this instance as drifted, recording detection time and updating status.
     *
     * @param expectedHash the expected configuration hash from the source of truth
     *                     (Config Server)
     */
    public void markDrift(String expectedHash) {
        this.hasDrift = true;
        this.driftDetectedAt = Instant.now();
        this.status = InstanceStatus.DRIFT;
        this.expectedHash = expectedHash;
    }

    /**
     * Clears the drift flag and resets status to {@link InstanceStatus#HEALTHY} if
     * previously marked as DRIFT.
     */
    public void clearDrift() {
        this.hasDrift = false;
        this.driftDetectedAt = null;
        if (this.status == InstanceStatus.DRIFT) {
            this.status = InstanceStatus.HEALTHY;
        }
    }

    /**
     * Enumeration representing the operational state of the instance.
     */
    public enum InstanceStatus {
        /**
         * Instance is reachable and healthy.
         */
        HEALTHY,

        /**
         * Instance failed ping or health check.
         */
        UNHEALTHY,

        /**
         * Instance has configuration drift (mismatch between applied and expected
         * hash).
         */
        DRIFT,

        /**
         * Instance state could not be determined.
         */
        UNKNOWN,

        /**
         * Instance has not sent a heartbeat within the stale threshold.
         * Used by cleanup service to track instances that should be cleaned up.
         */
        STALE
    }
}
