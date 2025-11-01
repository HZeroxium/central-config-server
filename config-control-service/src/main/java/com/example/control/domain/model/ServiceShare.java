package com.example.control.domain.model;

import com.example.control.domain.valueobject.id.ServiceShareId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Domain model representing access control for service sharing.
 * <p>
 * This entity implements ACL (Access Control List) to allow teams to share
 * specific permissions for their services with other teams or individual users
 * without granting full team membership.
 * </p>
 *
 * @see ApplicationService for the service being shared
 * @see SharePermission for available permissions
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ServiceShare {

    /**
     * Unique share identifier.
     */
    @NotNull(message = "Share ID is required")
    private ServiceShareId id;

    /**
     * Level of resource being shared.
     */
    @NotNull(message = "Resource level is required")
    private ResourceLevel resourceLevel;

    /**
     * Service ID being shared.
     */
    @NotBlank(message = "Service ID is required")
    private String serviceId;

    /**
     * Instance ID if sharing at instance level (optional).
     */
    private String instanceId;

    /**
     * Type of grantee (TEAM or USER).
     */
    @NotNull(message = "Grantee type is required")
    private GranteeType grantToType;

    /**
     * ID of the grantee (team ID or user ID).
     */
    @NotBlank(message = "Grantee ID is required")
    private String grantToId;

    /**
     * Permissions being granted.
     */
    @NotNull(message = "Permissions cannot be null")
    @Size(min = 1, message = "At least one permission must be specified")
    private List<SharePermission> permissions;

    /**
     * Environment filter (optional, null means all environments).
     */
    private List<String> environments;

    /**
     * User who created this share (Keycloak user ID).
     */
    @NotBlank(message = "Granted by user ID is required")
    private String grantedBy;

    /**
     * Timestamp when the share was created.
     */
    private Instant createdAt;

    /**
     * Timestamp when the share was last updated.
     */
    private Instant updatedAt;

    /**
     * Optional expiration timestamp.
     */
    private Instant expiresAt;

    /**
     * Check if this share applies to the given environment.
     * <p>
     * If environments is null or empty, the share applies to all environments.
     * If environments has values, the share only applies to those specific
     * environments.
     *
     * @param environment the environment to check
     * @return true if the share applies to the environment, false otherwise
     */
    public boolean appliesToEnvironment(String environment) {
        if (environments == null || environments.isEmpty()) {
            return true; // No environment filter means all environments
        }
        return environments.contains(environment);
    }

    /**
     * Resource level enumeration.
     */
    public enum ResourceLevel {
        /**
         * Share all instances of a service.
         */
        SERVICE,

        /**
         * Share a specific instance.
         */
        INSTANCE
    }

    /**
     * Grantee type enumeration.
     */
    public enum GranteeType {
        /**
         * Share with a team.
         */
        TEAM,

        /**
         * Share with an individual user.
         */
        USER
    }

    /**
     * Share permission enumeration.
     */
    public enum SharePermission {
        /**
         * Can view service metadata.
         */
        VIEW_SERVICE,

        /**
         * Can view service instances and metadata.
         */
        VIEW_INSTANCE,

        /**
         * Can view drift events.
         */
        VIEW_DRIFT,

        /**
         * Can edit service metadata.
         */
        EDIT_SERVICE,

        /**
         * Can edit service instances and configuration.
         */
        EDIT_INSTANCE,

        /**
         * Can trigger instance restarts.
         */
        RESTART_INSTANCE
    }
}
