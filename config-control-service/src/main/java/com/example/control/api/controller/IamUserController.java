package com.example.control.api.controller;

import com.example.control.api.dto.IamUserDtos;
import com.example.control.api.mapper.IamUserApiMapper;
import com.example.control.application.service.IamUserService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.criteria.IamUserCriteria;
import com.example.control.domain.id.IamUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
     * @param criteria filter criteria
     * @param pageable pagination information
     * @param userContext current user context
     * @return page of IAM users
     */
    @GetMapping
    @Operation(summary = "List IAM users", description = "Get paginated list of cached user projections")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<Page<IamUserDtos.Response>> findAll(
            @Parameter(description = "Filter criteria") IamUserCriteria criteria,
            @Parameter(description = "Pagination information") Pageable pageable,
            UserContext userContext) {
        
        log.debug("Listing IAM users with criteria: {} for user: {}", criteria, userContext.getUserId());
        
        Page<IamUserDtos.Response> response = iamUserService.findAll(criteria, pageable)
                .map(apiMapper::toResponse);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get IAM user by ID.
     *
     * @param userId the user ID
     * @param userContext current user context
     * @return IAM user details
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get IAM user by ID", description = "Get cached user projection by user ID")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<IamUserDtos.Response> findById(
            @PathVariable String userId,
            UserContext userContext) {
        
        log.debug("Getting IAM user by ID: {} for user: {}", userId, userContext.getUserId());
        
        return iamUserService.findById(IamUserId.of(userId))
                .map(apiMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List IAM users by team.
     *
     * @param teamId the team ID
     * @param userContext current user context
     * @return list of users in the team
     */
    @GetMapping("/by-team/{teamId}")
    @Operation(summary = "List users by team", description = "Get all users belonging to a specific team")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<List<IamUserDtos.Response>> findByTeam(
            @PathVariable String teamId,
            UserContext userContext) {
        
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
     * @param managerId the manager's user ID
     * @param userContext current user context
     * @return list of users reporting to the manager
     */
    @GetMapping("/by-manager/{managerId}")
    @Operation(summary = "List users by manager", description = "Get all users reporting to a specific manager")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<List<IamUserDtos.Response>> findByManager(
            @PathVariable String managerId,
            UserContext userContext) {
        
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
     * @param userContext current user context
     * @return user statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get user statistics", description = "Get user count statistics")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<IamUserDtos.StatsResponse> getStats(UserContext userContext) {
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
