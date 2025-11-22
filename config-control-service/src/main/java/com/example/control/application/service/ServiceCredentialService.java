package com.example.control.application.service;

import com.example.control.api.http.exception.exceptions.ExternalServiceException;
import com.example.control.api.http.exception.exceptions.ServiceCredentialNotFoundException;
import com.example.control.api.http.exception.exceptions.ServiceCredentialNotActiveException;
import com.example.control.application.command.ServiceCredentialCommandService;
import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.ServiceCredentialQueryService;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.ServiceCredential;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.infrastructure.adapter.external.keycloak.KeycloakAdminRestService;
import com.example.control.infrastructure.config.keycloak.KeycloakAdminProperties;
import com.example.control.infrastructure.config.security.DomainPermissionEvaluator;
import com.example.control.infrastructure.config.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing service credentials for M2M authentication.
 * <p>
 * Orchestrates credential creation, activation, retrieval, and revocation.
 * Integrates with Keycloak Admin API for client management.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceCredentialService {

    private final ServiceCredentialCommandService commandService;
    private final ServiceCredentialQueryService queryService;
    private final ApplicationServiceQueryService applicationServiceQueryService;
    private final KeycloakAdminRestService keycloakAdmin;
    private final DomainPermissionEvaluator permissionEvaluator;
    private final KeycloakAdminProperties keycloakAdminProperties;

    /**
     * Create service credentials after approval workflow completes.
     * <p>
     * This is called automatically when an ApprovalRequest is approved
     * and ownership is transferred to a team.
     * </p>
     * <p>
     * <strong>Business Rules:</strong>
     * <ul>
     * <li>Service must exist and have an owner team</li>
     * <li>Credentials must not already exist for the service</li>
     * <li>Keycloak client is created with service account enabled</li>
     * <li>Credential status is set to PENDING - user must activate after config files are ready</li>
     * <li>Client secret is returned ONCE in response - save it securely</li>
     * </ul>
     * </p>
     *
     * @param serviceId the ApplicationService ID
     * @param userContext the user context (for audit)
     * @return ServiceCredentialResponse with clientId, clientSecret (one-time), status, and token endpoint
     * @throws IllegalArgumentException if service not found or has no owner
     * @throws IllegalStateException if credentials already exist
     * @throws ExternalServiceException if Keycloak client creation fails
     */
    @Transactional
    public ServiceCredentialResponse createCredentialsForService(
            ApplicationServiceId serviceId,
            UserContext userContext) {

        log.info("Creating credentials for service: {}", serviceId);

        // 1. Validate service exists and has owner
        ApplicationService service = applicationServiceQueryService.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        if (service.getOwnerTeamId() == null) {
            throw new IllegalStateException(
                    "Cannot create credentials for orphan service. Service must have owner team.");
        }

        // 2. Check if credentials already exist
        if (queryService.existsByServiceId(serviceId)) {
            log.warn("Credentials already exist for service: {}", serviceId);
            ServiceCredential existing = queryService.findByServiceId(serviceId)
                    .orElseThrow(() -> new ServiceCredentialNotFoundException(
                            "Credentials exist for service " + serviceId + " but could not be retrieved"));
            
            // Return response without secret (credentials already created previously)
            String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token",
                    keycloakAdminProperties.getUrl(), keycloakAdminProperties.getRealm());
            
            return new ServiceCredentialResponse(
                    existing.getKeycloakClientId(),
                    null, // Secret not returned for existing credentials
                    existing.getStatus(),
                    existing.getExpiresAt(),
                    tokenEndpoint
            );
        }

        // 3. Create Keycloak client
        String clientId = service.getDisplayName(); // Use displayName as clientId
        String description = "Service client for " + service.getDisplayName();

        KeycloakAdminRestService.KeycloakClientCreationResult keycloakResult;
        try {
            keycloakResult = keycloakAdmin.createServiceClient(clientId, description);
        } catch (ExternalServiceException e) {
            log.error("Failed to create Keycloak client for service: {}", serviceId, e);
            throw e; // Re-throw to trigger transaction rollback
        }

        // 4. Create ServiceCredential domain object
        Instant now = Instant.now();
        ServiceCredential credential = ServiceCredential.builder()
                .id(com.example.control.domain.valueobject.id.ServiceCredentialId.of(UUID.randomUUID().toString()))
                .serviceId(serviceId)
                .keycloakClientId(keycloakResult.clientId())
                .keycloakClientUuid(keycloakResult.clientUuid())
                .status(ServiceCredential.CredentialStatus.PENDING) // Created as PENDING - user must activate after config files are ready
                .createdBy(userContext.getUserId() != null ? userContext.getUserId() : "SYSTEM")
                .createdAt(now)
                .updatedAt(now)
                .version(0)
                .build();

        // 5. Save credential
        ServiceCredential saved = commandService.save(credential);

        log.info("Created credentials for service: {} with Keycloak clientId: {} (status: PENDING - activation required)",
                serviceId, keycloakResult.clientId());

        // 6. Build response with secret (ONE-TIME ONLY)
        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakAdminProperties.getUrl(), keycloakAdminProperties.getRealm());

        return new ServiceCredentialResponse(
                keycloakResult.clientId(),
                keycloakResult.clientSecret(), // ← SECRET RETURNED HERE (one-time only)
                saved.getStatus(),
                saved.getExpiresAt(),
                tokenEndpoint
        );
    }

    /**
     * Get credentials for a service (returns clientId and clientSecret).
     * <p>
     * Returns secret if credentials are ACTIVE or PENDING and user has permission.
     * The secret is retrieved from Keycloak (may require rotation if not available).
     * <p>
     * <strong>Note:</strong> Each time this method is called, if the secret is not available
     * from Keycloak, a new secret will be rotated. This invalidates the previous secret.
     * Users should retrieve and store the secret securely on first access.
     * </p>
     * </p>
     * <p>
     * <strong>Permissions:</strong>
     * <ul>
     * <li>Service owner team members can view credentials</li>
     * <li>SYS_ADMIN can view all credentials</li>
     * </ul>
     * </p>
     *
     * @param serviceId the ApplicationService ID
     * @param userContext the user context (for permission check)
     * @return ServiceCredentialResponse with clientId, clientSecret, status, and tokenEndpoint
     * @throws IllegalArgumentException if service or credentials not found
     * @throws IllegalStateException if user lacks permission or credentials not active
     */
    @Transactional(readOnly = true)
    public ServiceCredentialResponse getCredentialsForService(
            ApplicationServiceId serviceId,
            UserContext userContext) {

        ApplicationService service = applicationServiceQueryService.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        // Check permissions
        if (!permissionEvaluator.canViewService(userContext, service)) {
            throw new IllegalStateException("User does not have permission to view credentials for service: " + serviceId);
        }

        ServiceCredential credential = queryService.findByServiceId(serviceId)
                .orElseThrow(() -> new ServiceCredentialNotFoundException(
                        "Credentials not found for service: " + serviceId));

        // Retrieve client secret from Keycloak
        // Try to get existing secret first, fallback to rotation if not available
        String clientSecret;
        Optional<String> existingSecret = keycloakAdmin.getClientSecret(credential.getKeycloakClientUuid());
        if (existingSecret.isPresent()) {
            clientSecret = existingSecret.get();
            log.debug("Retrieved existing client secret for service: {}", serviceId);
        } else {
            // Rotate to get new secret (this invalidates the old one)
            log.warn("Existing secret not available for service: {}, rotating to get new secret", serviceId);
            clientSecret = keycloakAdmin.rotateClientSecret(credential.getKeycloakClientUuid());
        }

        // Construct token endpoint URL
        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakAdminProperties.getUrl(), keycloakAdminProperties.getRealm());

        return new ServiceCredentialResponse(
                credential.getKeycloakClientId(),
                clientSecret,
                credential.getStatus(),
                credential.getExpiresAt(),
                tokenEndpoint
        );
    }

    /**
     * Activate credentials after config files are ready.
     * <p>
     * Called when user confirms config files have been created/updated.
     * Changes status from PENDING to ACTIVE.
     * </p>
     *
     * @param serviceId the ApplicationService ID
     * @return the activated ServiceCredential
     * @throws IllegalArgumentException if credentials not found
     * @throws IllegalStateException if credentials are not in PENDING status
     */
    @Transactional
    public ServiceCredential activateCredentials(ApplicationServiceId serviceId) {
        ServiceCredential credential = queryService.findByServiceId(serviceId)
                .orElseThrow(() -> new ServiceCredentialNotFoundException(
                        "Credentials not found for service: " + serviceId));

        if (credential.getStatus() != ServiceCredential.CredentialStatus.PENDING) {
            throw new ServiceCredentialNotActiveException(
                    "Credentials are not in PENDING status. Current status: " + credential.getStatus() +
                            ". Only PENDING credentials can be activated.");
        }

        credential.setStatus(ServiceCredential.CredentialStatus.ACTIVE);
        credential.setUpdatedAt(Instant.now());

        ServiceCredential saved = commandService.save(credential);

        log.info("Activated credentials for service: {}", serviceId);

        return saved;
    }

    /**
     * Revoke credentials (disable Keycloak client and mark as REVOKED).
     * <p>
     * This disables the Keycloak client and marks credentials as revoked.
     * Existing tokens may still be valid until expiration.
     * </p>
     *
     * @param serviceId the ApplicationService ID
     * @param userContext the user context (for audit)
     * @return the revoked ServiceCredential
     * @throws IllegalArgumentException if credentials not found
     */
    @Transactional
    public ServiceCredential revokeCredentials(ApplicationServiceId serviceId, UserContext userContext) {
        ServiceCredential credential = queryService.findByServiceId(serviceId)
                .orElseThrow(() -> new ServiceCredentialNotFoundException(
                        "Credentials not found for service: " + serviceId));

        // Revoke Keycloak client
        try {
            keycloakAdmin.revokeClient(credential.getKeycloakClientUuid());
            log.info("Revoked Keycloak client for service: {}", serviceId);
        } catch (ExternalServiceException e) {
            log.error("Failed to revoke Keycloak client for service: {}", serviceId, e);
            // Continue with local revocation even if Keycloak fails
        }

        // Update credential status
        credential.setStatus(ServiceCredential.CredentialStatus.REVOKED);
        credential.setRevokedAt(Instant.now());
        credential.setUpdatedBy(userContext.getUserId() != null ? userContext.getUserId() : "SYSTEM");
        credential.setUpdatedAt(Instant.now());

        ServiceCredential saved = commandService.save(credential);

        log.info("Revoked credentials for service: {}", serviceId);

        return saved;
    }

    /**
     * Response DTO for service credentials.
     * <p>
     * Contains the client ID, secret, status, expiration, and token endpoint URL.
     * </p>
     * <p>
     * <strong>Important:</strong> The client secret is only returned once when credentials
     * are first created (in approval response). Subsequent retrievals via getCredentialsForService()
     * may rotate the secret if the original secret is not available from Keycloak.
     * </p>
     *
     * @param clientId the Keycloak client ID
     * @param clientSecret the client secret (one-time retrieval on creation, may be rotated on subsequent retrievals)
     * @param status the credential status (PENDING or ACTIVE)
     * @param expiresAt when credentials expire (optional)
     * @param tokenEndpoint the Keycloak token endpoint URL
     */
    public record ServiceCredentialResponse(
            String clientId,
            String clientSecret,
            ServiceCredential.CredentialStatus status,
            Instant expiresAt,
            String tokenEndpoint
    ) {}
}

