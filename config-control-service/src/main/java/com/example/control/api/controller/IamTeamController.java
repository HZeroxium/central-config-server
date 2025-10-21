package com.example.control.api.controller;

import com.example.control.api.dto.IamTeamDtos;
import com.example.control.api.mapper.IamTeamApiMapper;
import com.example.control.application.service.IamTeamService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.criteria.IamTeamCriteria;
import com.example.control.domain.id.IamTeamId;
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
     * @param criteria filter criteria
     * @param pageable pagination information
     * @param userContext current user context
     * @return page of IAM teams
     */
    @GetMapping
    @Operation(summary = "List IAM teams", description = "Get paginated list of cached team projections")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<Page<IamTeamDtos.Response>> findAll(
            @Parameter(description = "Filter criteria") IamTeamCriteria criteria,
            @Parameter(description = "Pagination information") Pageable pageable,
            UserContext userContext) {
        
        log.debug("Listing IAM teams with criteria: {} for user: {}", criteria, userContext.getUserId());
        
        Page<IamTeamDtos.Response> response = iamTeamService.findAll(criteria, pageable)
                .map(apiMapper::toResponse);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get IAM team by ID.
     *
     * @param teamId the team ID
     * @param userContext current user context
     * @return IAM team details
     */
    @GetMapping("/{teamId}")
    @Operation(summary = "Get IAM team by ID", description = "Get cached team projection by team ID")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<IamTeamDtos.Response> findById(
            @PathVariable String teamId,
            UserContext userContext) {
        
        log.debug("Getting IAM team by ID: {} for user: {}", teamId, userContext.getUserId());
        
        return iamTeamService.findById(IamTeamId.of(teamId))
                .map(apiMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List IAM teams by member.
     *
     * @param userId the user ID
     * @param userContext current user context
     * @return list of teams containing the user
     */
    @GetMapping("/by-member/{userId}")
    @Operation(summary = "List teams by member", description = "Get all teams containing a specific user")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<List<IamTeamDtos.Response>> findByMember(
            @PathVariable String userId,
            UserContext userContext) {
        
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
     * @param userContext current user context
     * @return team statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get team statistics", description = "Get team count statistics")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ResponseEntity<IamTeamDtos.StatsResponse> getStats(UserContext userContext) {
        log.debug("Getting IAM team statistics for user: {}", userContext.getUserId());
        
        long totalTeams = iamTeamService.countAll();
        
        IamTeamDtos.StatsResponse response = IamTeamDtos.StatsResponse.builder()
                .totalTeams(totalTeams)
                .build();
        
        return ResponseEntity.ok(response);
    }
}
