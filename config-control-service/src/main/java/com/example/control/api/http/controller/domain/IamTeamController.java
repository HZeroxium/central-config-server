package com.example.control.api.http.controller.domain;

import com.example.control.api.http.dto.domain.IamTeamDtos;
import com.example.control.api.http.mapper.domain.IamTeamApiMapper;
import com.example.control.application.service.IamTeamService;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.criteria.IamTeamCriteria;
import com.example.control.domain.valueobject.id.IamTeamId;
import com.example.control.domain.model.IamTeam;
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
 * REST controller for IAM team management.
 * <p>
 * Provides endpoints for managing cached team projections from Keycloak.
 * All endpoints require SYS_ADMIN role.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/iam/teams")
@RequiredArgsConstructor
@Tag(name = "IAM Teams", description = "Team management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class IamTeamController {

    private final IamTeamService iamTeamService;
    private final IamTeamApiMapper apiMapper;

    /**
     * List IAM teams with pagination and filtering.
     *
     * @param criteria    filter criteria
     * @param pageable    pagination information
     * @param jwt current user context
     * @return page of IAM teams
     */
    @GetMapping
    @Operation(
            summary = "List IAM teams",
            description = """
                    Retrieve a paginated list of cached IAM team projections from Keycloak.
                    This endpoint is restricted to system administrators.
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "findAllIamTeams"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved IAM teams",
                    content = @Content(schema = @Schema(implementation = IamTeamDtos.IamTeamPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - SYS_ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<IamTeamDtos.IamTeamPageResponse> findAll(
            @ParameterObject @Valid IamTeamCriteria criteria,
            @ParameterObject @PageableDefault(size = 20, page = 0) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {

        UserContext userContext = UserContext.fromJwt(jwt);

        log.debug("Listing IAM teams with criteria: {} for user: {}", criteria, userContext.getUserId());

        Page<IamTeam> page = iamTeamService.findAll(criteria, pageable);
        IamTeamDtos.IamTeamPageResponse response = apiMapper.toPageResponse(page);

        return ResponseEntity.ok(response);
    }

    /**
     * Get IAM team by ID.
     *
     * @param teamId      the team ID
     * @return IAM team details
     */
    @GetMapping("/{teamId}")
    @Operation(
            summary = "Get IAM team by ID",
            description = """
                    Retrieve a specific IAM team by its ID from cached Keycloak projections.
                    This endpoint is restricted to system administrators.
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "findByIdIamTeam"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "IAM team found",
                    content = @Content(schema = @Schema(implementation = IamTeamDtos.Response.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - SYS_ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "IAM team not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<IamTeamDtos.Response> findById(
            @Parameter(description = "Team ID", example = "team_core")
            @PathVariable String teamId,
            @AuthenticationPrincipal Jwt jwt) {

        UserContext userContext = UserContext.fromJwt(jwt);

        log.debug("Getting IAM team by ID: {} for user: {}", teamId, userContext.getUserId());

        return iamTeamService.findById(IamTeamId.of(teamId))
                .map(apiMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List IAM teams by member.
     *
     * @param userId      the user ID
     * @return list of teams containing the user
     */
    @GetMapping("/by-member/{userId}")
    @Operation(
            summary = "List teams by member",
            description = """
                    Retrieve all teams that contain a specific user as a member.
                    This endpoint is restricted to system administrators.
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "findByMemberIamTeam"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Teams containing the user found",
                    content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - SYS_ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<List<IamTeamDtos.Response>> findByMember(
            @Parameter(description = "User ID to find teams for", example = "user1")
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {

        UserContext userContext = UserContext.fromJwt(jwt);

        log.debug("Getting IAM teams by member: {} for user: {}", userId, userContext.getUserId());

        List<IamTeamDtos.Response> response = iamTeamService.findByMember(userId)
                .stream()
                .map(apiMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Get team statistics.
     *
     * @return team statistics
     */
    @GetMapping("/stats")
    @Operation(
            summary = "Get team statistics",
            description = """
                    Retrieve team count statistics from cached IAM data.
                    This endpoint is restricted to system administrators.
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "getStatsIamTeam"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Team statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = IamTeamDtos.StatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - SYS_ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<IamTeamDtos.StatsResponse> getStats(@AuthenticationPrincipal Jwt jwt) {

        UserContext userContext = UserContext.fromJwt(jwt);
        log.debug("Getting IAM team statistics for user: {}", userContext.getUserId());

        long totalTeams = iamTeamService.countAll();

        IamTeamDtos.StatsResponse response = IamTeamDtos.StatsResponse.builder()
                .totalTeams(totalTeams)
                .build();

        return ResponseEntity.ok(response);
    }
}
