package com.example.control.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for User API operations.
 * <p>
 * These DTOs provide a clean separation between the domain model and the API layer,
 * allowing for versioning, validation, and documentation without affecting the domain.
 * </p>
 */
public class UserDtos {

    /**
     * Response DTO for current user information.
     */
    public record MeResponse(
            String userId,
            String username,
            String email,
            String firstName,
            String lastName,
            String managerId,
            List<String> teamIds,
            List<String> roles,
            Instant lastLoginAt,
            Instant createdAt
    ) {}

    /**
     * Response DTO for user profile data.
     */
    public record ProfileResponse(
            String userId,
            String username,
            String email,
            String firstName,
            String lastName,
            String managerId,
            List<String> teamIds,
            List<String> roles,
            Instant lastLoginAt,
            Instant createdAt,
            Instant updatedAt
    ) {}

    /**
     * Request DTO for updating user profile.
     */
    public record UpdateProfileRequest(
            @Size(max = 100, message = "First name must not exceed 100 characters")
            String firstName,
            
            @Size(max = 100, message = "Last name must not exceed 100 characters")
            String lastName,
            
            @Size(max = 255, message = "Email must not exceed 255 characters")
            String email
    ) {}

    /**
     * Request DTO for listing users with filters.
     */
    public record ListRequest(
            String search,
            List<String> teamIds,
            List<String> roles,
            Integer page,
            Integer size,
            String sort
    ) {}

    /**
     * Response DTO for paginated user list.
     */
    public record ListResponse(
            List<ProfileResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {}

    /**
     * Response DTO for user statistics.
     */
    public record StatsResponse(
            long totalUsers,
            long activeUsers,
            long usersByTeam,
            long usersByRole
    ) {}
}
