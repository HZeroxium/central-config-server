package com.example.control.api.http.controller.domain;

import com.example.control.api.http.dto.domain.ServiceCredentialDtos;
import com.example.control.api.http.exception.ErrorResponse;
import com.example.control.application.service.ServiceCredentialService;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.infrastructure.config.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for ServiceCredential operations.
 * <p>
 * Provides endpoints for managing service credentials used in M2M authentication.
 * Requires authentication and appropriate permissions (service owner team or SYS_ADMIN).
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/services/{serviceId}/credentials")
@RequiredArgsConstructor
@Tag(name = "Service Credentials", description = "Manage service credentials for M2M authentication")
public class ServiceCredentialController {

    private final ServiceCredentialService serviceCredentialService;

    /**
     * Get service credentials (clientId and clientSecret).
     * <p>
     * Returns the client credentials for the service. The secret is retrieved from Keycloak
     * and may require rotation if not available.
     * </p>
     * <p>
     * <strong>Permissions:</strong>
     * <ul>
     * <li>Service owner team members can view credentials</li>
     * <li>SYS_ADMIN can view all credentials</li>
     * </ul>
     * </p>
     *
     * @param serviceId the service ID
     * @param jwt the JWT token
     * @return ServiceCredentialResponse with clientId, clientSecret, status, and tokenEndpoint
     */
    @GetMapping
    @Operation(summary = "Get service credentials", description = """
            Retrieve client credentials (clientId and clientSecret) for a service.
            
            **Access Control:**
            - Service owner team members can view credentials
            - SYS_ADMIN can view all credentials
            
            **Important:**
            - The client secret is retrieved from Keycloak (may require rotation if not available)
            - This is a one-time retrieval - user must save the secret immediately
            - Secret rotation invalidates the old secret
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "getServiceCredentials")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credentials retrieved successfully", content = @Content(schema = @Schema(implementation = ServiceCredentialDtos.ServiceCredentialResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid service ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Service or credentials not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ServiceCredentialDtos.ServiceCredentialResponse> getCredentials(
            @Parameter(description = "Service ID", required = true) @PathVariable String serviceId,
            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting credentials for service: {}", serviceId);

        UserContext userContext = UserContext.fromJwt(jwt);
        ApplicationServiceId appServiceId = ApplicationServiceId.of(serviceId);

        ServiceCredentialService.ServiceCredentialResponse response = 
                serviceCredentialService.getCredentialsForService(appServiceId, userContext);

        ServiceCredentialDtos.ServiceCredentialResponse dto = ServiceCredentialDtos.ServiceCredentialResponse.builder()
                .clientId(response.clientId())
                .clientSecret(response.clientSecret())
                .status(response.status().name())
                .expiresAt(response.expiresAt())
                .tokenEndpoint(response.tokenEndpoint())
                .build();

        return ResponseEntity.ok(dto);
    }

    /**
     * Activate service credentials.
     * <p>
     * Changes credential status from PENDING to ACTIVE after config files are ready.
     * </p>
     * <p>
     * <strong>Permissions:</strong>
     * <ul>
     * <li>Service owner team members can activate credentials</li>
     * <li>SYS_ADMIN can activate any credentials</li>
     * </ul>
     * </p>
     *
     * @param serviceId the service ID
     * @param jwt the JWT token
     * @return success response
     */
    @PostMapping("/activate")
    @Operation(summary = "Activate service credentials", description = """
            Activate service credentials after config files are ready.
            
            Changes credential status from PENDING to ACTIVE, allowing the SDK to authenticate.
            
            **Access Control:**
            - Service owner team members can activate credentials
            - SYS_ADMIN can activate any credentials
            
            **Workflow:**
            1. Service is approved and credentials are created (status: PENDING)
            2. User configures service config files (e.g., application.yml)
            3. User calls this endpoint to activate credentials
            4. SDK can now authenticate using client credentials
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "activateServiceCredentials")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credentials activated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid service ID or credentials not in PENDING status", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Service or credentials not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> activateCredentials(
            @Parameter(description = "Service ID", required = true) @PathVariable String serviceId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Activating credentials for service: {}", serviceId);

        ApplicationServiceId appServiceId = ApplicationServiceId.of(serviceId);

        serviceCredentialService.activateCredentials(appServiceId);

        return ResponseEntity.ok().build();
    }

    /**
     * Revoke service credentials.
     * <p>
     * Disables the Keycloak client and marks credentials as REVOKED.
     * Existing tokens may still be valid until expiration.
     * </p>
     * <p>
     * <strong>Permissions:</strong>
     * <ul>
     * <li>Service owner team members can revoke credentials</li>
     * <li>SYS_ADMIN can revoke any credentials</li>
     * </ul>
     * </p>
     *
     * @param serviceId the service ID
     * @param jwt the JWT token
     * @return success response
     */
    @PostMapping("/revoke")
    @Operation(summary = "Revoke service credentials", description = """
            Revoke service credentials by disabling the Keycloak client.
            
            This disables the client, preventing new tokens from being issued.
            Existing tokens may still be valid until expiration.
            
            **Access Control:**
            - Service owner team members can revoke credentials
            - SYS_ADMIN can revoke any credentials
            
            **Use Cases:**
            - Security incident response
            - Service decommissioning
            - Credential rotation
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "revokeServiceCredentials")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credentials revoked successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid service ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Service or credentials not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> revokeCredentials(
            @Parameter(description = "Service ID", required = true) @PathVariable String serviceId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Revoking credentials for service: {}", serviceId);

        UserContext userContext = UserContext.fromJwt(jwt);
        ApplicationServiceId appServiceId = ApplicationServiceId.of(serviceId);

        serviceCredentialService.revokeCredentials(appServiceId, userContext);

        return ResponseEntity.ok().build();
    }
}

