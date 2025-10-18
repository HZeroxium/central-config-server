package com.example.control.api.dto;

import com.example.control.domain.ServiceShare;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for ServiceShare API operations.
 * <p>
 * These DTOs provide a clean separation between the domain model and the API layer,
 * allowing for versioning, validation, and documentation without affecting the domain.
 * </p>
 */
public class ServiceShareDtos {

    /**
     * Request DTO for granting a service share.
     */
    public record GrantRequest(
            @NotBlank(message = "Service ID is required")
            String serviceId,
            
            @NotNull(message = "Grantee type is required")
            ServiceShare.GranteeType grantToType,
            
            @NotBlank(message = "Grantee ID is required")
            String grantToId,
            
            @NotNull(message = "Permissions list is required")
            List<ServiceShare.SharePermission> permissions,
            
            @NotNull(message = "Environments list is required")
            List<@NotBlank String> environments,
            
            Instant expiresAt
    ) {}

    /**
     * Request DTO for updating a service share.
     */
    public record UpdateRequest(
            List<ServiceShare.SharePermission> permissions,
            List<String> environments,
            Instant expiresAt
    ) {}

    /**
     * Response DTO for service share data.
     */
    public record Response(
            String id,
            String serviceId,
            ServiceShare.GranteeType grantToType,
            String grantToId,
            List<ServiceShare.SharePermission> permissions,
            List<String> environments,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt,
            String grantedBy
    ) {}

    /**
     * Request DTO for listing service shares with filters.
     */
    public record ListRequest(
            String serviceId,
            ServiceShare.GranteeType grantToType,
            String grantToId,
            List<String> environments,
            String grantedBy,
            Integer page,
            Integer size,
            String sort
    ) {}

    /**
     * Response DTO for paginated service share list.
     */
    public record ListResponse(
            List<Response> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {}

    /**
     * Request DTO for revoking a service share.
     */
    public record RevokeRequest(
            @NotBlank(message = "Share ID is required")
            String shareId
    ) {}
}
