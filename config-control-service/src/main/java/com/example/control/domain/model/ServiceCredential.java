package com.example.control.domain.model;

import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.valueobject.id.ServiceCredentialId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain model representing service credentials for M2M authentication.
 * <p>
 * This entity stores metadata about Keycloak client credentials used for
 * service-to-service authentication. The actual client secret is managed by
 * Keycloak and retrieved via Admin API when needed.
 * </p>
 * <p>
 * Credentials lifecycle:
 * <ul>
 * <li>PENDING: Created after approval, waiting for config files to be ready</li>
 * <li>ACTIVE: Credentials are active and can be used for authentication</li>
 * <li>EXPIRED: Credentials have expired (if expiration is configured)</li>
 * <li>REVOKED: Credentials have been revoked and cannot be used</li>
 * </ul>
 * </p>
 *
 * @see ApplicationService for the service this credential belongs to
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCredential {

    /**
     * Unique credential identifier.
     */
    @NotNull(message = "Credential ID is required")
    private ServiceCredentialId id;

    /**
     * Foreign key to ApplicationService.
     * Unique constraint: one credential per service.
     */
    @NotNull(message = "Service ID is required")
    private ApplicationServiceId serviceId;

    /**
     * Keycloak client ID (must match serviceName or custom naming).
     * Used to extract from JWT 'azp' or 'aud' claim.
     */
    @NotBlank(message = "Keycloak client ID is required")
    private String keycloakClientId;

    /**
     * Keycloak client UUID (internal Keycloak ID).
     * Used for Admin API operations (update, delete, rotate secret).
     */
    @NotBlank(message = "Keycloak client UUID is required")
    private String keycloakClientUuid;

    /**
     * Credential status.
     */
    @Builder.Default
    private CredentialStatus status = CredentialStatus.PENDING;

    /**
     * When credentials expire (optional, for rotation).
     */
    private Instant expiresAt;

    /**
     * When credentials were revoked (if status = REVOKED).
     */
    private Instant revokedAt;

    /**
     * User who created these credentials.
     */
    private String createdBy;

    /**
     * User who last updated/revoked these credentials.
     */
    private String updatedBy;

    /**
     * Timestamp when the credential was first created.
     */
    private Instant createdAt;

    /**
     * Timestamp when the credential was last updated.
     */
    private Instant updatedAt;

    /**
     * Version for optimistic locking.
     */
    @Builder.Default
    private Integer version = 0;

    /**
     * Credential status enumeration.
     */
    public enum CredentialStatus {
        /**
         * Credentials created but not yet activated (e.g., waiting for config files).
         */
        PENDING,

        /**
         * Credentials are active and can be used for authentication.
         */
        ACTIVE,

        /**
         * Credentials have expired.
         */
        EXPIRED,

        /**
         * Credentials have been revoked and cannot be used.
         */
        REVOKED
    }
}

