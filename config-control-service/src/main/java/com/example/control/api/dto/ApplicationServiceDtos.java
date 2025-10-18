package com.example.control.api.dto;

import com.example.control.domain.ApplicationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTOs for ApplicationService API operations.
 * <p>
 * These DTOs provide a clean separation between the domain model and the API layer,
 * allowing for versioning, validation, and documentation without affecting the domain.
 * </p>
 */
public class ApplicationServiceDtos {

    /**
     * Request DTO for creating a new application service.
     */
    public record CreateRequest(
            @NotBlank(message = "Service ID is required")
            @Size(max = 100, message = "Service ID must not exceed 100 characters")
            String id,
            
            @NotBlank(message = "Display name is required")
            @Size(max = 255, message = "Display name must not exceed 255 characters")
            String displayName,
            
            @NotBlank(message = "Owner team ID is required")
            @Size(max = 100, message = "Owner team ID must not exceed 100 characters")
            String ownerTeamId,
            
            @NotNull(message = "Environments list is required")
            List<@NotBlank String> environments,
            
            List<@Size(max = 50) String> tags,
            
            @Size(max = 500, message = "Repository URL must not exceed 500 characters")
            String repoUrl,
            
            Map<String, String> attributes
    ) {}

    /**
     * Request DTO for updating an existing application service.
     */
    public record UpdateRequest(
            @NotBlank(message = "Display name is required")
            @Size(max = 255, message = "Display name must not exceed 255 characters")
            String displayName,
            
            ApplicationService.ServiceLifecycle lifecycle,
            
            List<@Size(max = 50) String> tags,
            
            @Size(max = 500, message = "Repository URL must not exceed 500 characters")
            String repoUrl,
            
            Map<String, String> attributes
    ) {}

    /**
     * Response DTO for application service data.
     */
    public record Response(
            String id,
            String displayName,
            String ownerTeamId,
            List<String> environments,
            List<String> tags,
            String repoUrl,
            ApplicationService.ServiceLifecycle lifecycle,
            Instant createdAt,
            Instant updatedAt,
            String createdBy,
            Map<String, String> attributes
    ) {}

    /**
     * Request DTO for listing application services with filters.
     */
    public record ListRequest(
            String ownerTeamId,
            ApplicationService.ServiceLifecycle lifecycle,
            List<String> tags,
            String search,
            Integer page,
            Integer size,
            String sort
    ) {}

    /**
     * Response DTO for paginated application service list.
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
}
