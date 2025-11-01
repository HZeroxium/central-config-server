package com.example.control.api.http.controller.domain;

import com.example.control.api.http.dto.domain.IamUserDtos;
import com.example.control.api.http.mapper.domain.IamUserApiMapper;
import com.example.control.application.service.IamUserService;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.criteria.IamUserCriteria;
import com.example.control.domain.valueobject.id.IamUserId;
import com.example.control.domain.model.IamUser;
import com.example.control.api.http.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for IAM user management.
 * <p>
 * Provides endpoints for managing cached user projections from Keycloak.
 * All endpoints require SYS_ADMIN role.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/iam/users")
@RequiredArgsConstructor
@Tag(name = "IAM Users", description = "User management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class IamUserController {

    private final IamUserService iamUserService;
    private final IamUserApiMapper apiMapper;

    /**
     * List IAM users with pagination and filtering.
     *
     * @param criteria    filter criteria
     * @param pageable    pagination information
     * @return page of IAM users
     */
    @GetMapping
    @Operation(
            summary = "List IAM users",
            description = """
                    Retrieve a paginated list of cached IAM user projections from Keycloak.
                    This endpoint is restricted to system administrators.
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "findAllIamUsers"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved IAM users",
                    content = @Content(schema = @Schema(implementation = IamUserDtos.IamUserPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - SYS_ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<IamUserDtos.IamUserPageResponse> findAll(
            @ParameterObject @Valid IamUserCriteria criteria,
            @ParameterObject @PageableDefault(size = 20, page = 0) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {

        UserContext userContext = UserContext.fromJwt(jwt);

        log.debug("Listing IAM users with criteria: {} for user: {}", criteria, userContext.getUserId());

        Page<IamUser> page = iamUserService.findAll(criteria, pageable);
        IamUserDtos.IamUserPageResponse response = apiMapper.toPageResponse(page);

        return ResponseEntity.ok(response);
    }

    /**
     * Get IAM user by ID.
     *
     * @param userId      the user ID
     * @return IAM user details
     */
    @GetMapping("/{userId}")
    @Operation(
            summary = "Get IAM user by ID",
            description = """
                    Retrieve a specific IAM user by their ID from cached Keycloak projections.
                    This endpoint is restricted to system administrators.
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "findByIdIamUser"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "IAM user found",
                    content = @Content(schema = @Schema(implementation = IamUserDtos.Response.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - SYS_ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "IAM user not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<IamUserDtos.Response> findById(
            @Parameter(description = "User ID", example = "user1")
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {

        UserContext userContext = UserContext.fromJwt(jwt);

        log.debug("Getting IAM user by ID: {} for user: {}", userId, userContext.getUserId());

        return iamUserService.findById(IamUserId.of(userId))
                .map(apiMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List IAM users by team.
     *
     * @param teamId      the team ID
     * @return list of users in the team
     */
    @GetMapping("/by-team/{teamId}")
    @Operation(
            summary = "List users by team",
            description = """
                    Retrieve all users belonging to a specific team from cached IAM data.
                    This endpoint is restricted to system administrators.
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "findByTeamIamUser"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users in team found",
                    content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - SYS_ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<List<IamUserDtos.Response>> findByTeam(
            @Parameter(description = "Team ID to find users for", example = "team_core")
            @PathVariable String teamId,
            @AuthenticationPrincipal Jwt jwt) {

        UserContext userContext = UserContext.fromJwt(jwt);

        log.debug("Getting IAM users by team: {} for user: {}", teamId, userContext.getUserId());

        List<IamUserDtos.Response> response = iamUserService.findByTeam(teamId)
                .stream()
                .map(apiMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * List IAM users by manager.
     *
     * @param managerId   the manager's user ID
     * @param jwt current user context
     * @return list of users reporting to the manager
     */
    @GetMapping("/by-manager/{managerId}")
    @Operation(
            summary = "List users by manager",
            description = """
                    Retrieve all users reporting to a specific manager from cached IAM data.
                    This endpoint is restricted to system administrators.
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "findByManagerIamUser"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users reporting to manager found",
                    content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - SYS_ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<List<IamUserDtos.Response>> findByManager(
            @Parameter(description = "Manager's user ID", example = "manager1")
            @PathVariable String managerId,
            @AuthenticationPrincipal Jwt jwt) {

        UserContext userContext = UserContext.fromJwt(jwt);

        log.debug("Getting IAM users by manager: {} for user: {}", managerId, userContext.getUserId());

        List<IamUserDtos.Response> response = iamUserService.findByManager(managerId)
                .stream()
                .map(apiMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Get user statistics.
     *
     * @param jwt current user context
     * @return user statistics
     */
    @GetMapping("/stats")
    @Operation(
            summary = "Get user statistics",
            description = """
                    Retrieve user count statistics from cached IAM data.
                    This endpoint is restricted to system administrators.
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "getStatsIamUser"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = IamUserDtos.StatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - SYS_ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<IamUserDtos.StatsResponse> getStats(@AuthenticationPrincipal Jwt jwt) {

        UserContext userContext = UserContext.fromJwt(jwt);
        log.debug("Getting IAM user statistics for user: {}", userContext.getUserId());

        long totalUsers = iamUserService.countAll();
        long totalTeams = iamUserService.countByTeam("team_core") +
                iamUserService.countByTeam("team_analytics") +
                iamUserService.countByTeam("team_infrastructure");

        IamUserDtos.StatsResponse response = IamUserDtos.StatsResponse.builder()
                .totalUsers(totalUsers)
                .totalTeams(totalTeams)
                .build();

        return ResponseEntity.ok(response);
    }
}
