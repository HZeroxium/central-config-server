package com.example.control.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Domain model representing a public application service.
 * <p>
 * This entity contains metadata about services that can be viewed by all users
 * and serves as the basis for service ownership and sharing. Each service
 * belongs to one team and can be requested for ownership transfer.
 * </p>
 * 
 * @see ServiceInstance for runtime instances of this service
 * @see ApprovalRequest for ownership transfer requests
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationService {

    /** Unique service identifier (slug format: svc_payments). */
    @NotBlank(message = "Service ID is required")
    @Size(max = 100, message = "Service ID must not exceed 100 characters")
    private String id;

    /** Human-readable display name. */
    @NotBlank(message = "Display name is required")
    @Size(max = 200, message = "Display name must not exceed 200 characters")
    private String displayName;

    /** Team that owns this service (Keycloak group ID). */
    @NotBlank(message = "Owner team ID is required")
    private String ownerTeamId;

    /** List of environments where this service is deployed. */
    @NotNull(message = "Environments list cannot be null")
    @Size(min = 1, message = "At least one environment must be specified")
    private List<String> environments;

    /** Tags for categorization and filtering. */
    private List<String> tags;

    /** Repository URL for source code. */
    private String repoUrl;

    /** Service lifecycle status. */
    @Builder.Default
    private ServiceLifecycle lifecycle = ServiceLifecycle.ACTIVE;

    /** Timestamp when the service was first created. */
    private Instant createdAt;

    /** Timestamp when the service was last updated. */
    private Instant updatedAt;

    /** User who created this service (Keycloak user ID). */
    private String createdBy;

    /** Additional attributes as key-value pairs. */
    private Map<String, String> attributes;

    /**
     * Service lifecycle enumeration.
     */
    public enum ServiceLifecycle {
        /** Service is actively developed and deployed. */
        ACTIVE,
        
        /** Service is deprecated, no new features, maintenance only. */
        DEPRECATED,
        
        /** Service is retired, no longer in use. */
        RETIRED
    }
}
