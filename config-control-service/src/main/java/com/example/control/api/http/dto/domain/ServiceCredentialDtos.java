package com.example.control.api.http.dto.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTOs for ServiceCredential API operations.
 * <p>
 * Provides DTOs for service credential management and retrieval.
 * </p>
 */
@Schema(name = "ServiceCredentialDtos", description = "DTOs for service credential API operations")
public final class ServiceCredentialDtos {

    private ServiceCredentialDtos() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Response DTO for service credentials.
     * <p>
     * Contains the client ID, secret, status, expiration, and token endpoint URL.
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ServiceCredentialResponse", description = "Service credential details response")
    public static class ServiceCredentialResponse {
        @Schema(description = "Keycloak client ID", example = "sample-service", required = true)
        private String clientId;

        @Schema(description = "Keycloak client secret (one-time retrieval)", example = "abc123...", required = true)
        private String clientSecret;

        @Schema(description = "Credential status", example = "ACTIVE", allowableValues = {"PENDING", "ACTIVE", "EXPIRED", "REVOKED"}, required = true)
        private String status;

        @Schema(description = "When credentials expire (optional)", example = "2024-12-31T23:59:59Z")
        private Instant expiresAt;

        @Schema(description = "Keycloak token endpoint URL", example = "http://keycloak:8080/realms/config-control/protocol/openid-connect/token", required = true)
        private String tokenEndpoint;
    }
}

