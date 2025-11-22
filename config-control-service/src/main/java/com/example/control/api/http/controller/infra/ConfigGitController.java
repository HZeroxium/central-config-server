package com.example.control.api.http.controller.infra;

import com.example.control.api.http.dto.infra.ConfigGitDtos;
import com.example.control.api.http.exception.ErrorResponse;
import com.example.control.api.http.mapper.infra.ConfigGitApiMapper;
import com.example.control.application.service.ConfigGitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for GitHub config file operations.
 * <p>
 * Provides endpoints to read, update, and view commit history for config files
 * stored in GitHub repository with permission-based access control.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/config-git")
@RequiredArgsConstructor
@Tag(name = "Config Git", description = "GitHub config file operations with commit history")
public class ConfigGitController {

    private final ConfigGitService configGitService;
    private final ConfigGitApiMapper apiMapper;

    /**
     * Get config file content from GitHub.
     *
     * @param serviceId the service identifier
     * @param profile   the environment profile
     * @param jwt       the JWT token
     * @return config file response with content and SHA
     */
    @GetMapping("/{serviceId}/files/{profile}")
    @Operation(
            summary = "Get config file content",
            description = """
                    Retrieves config file content from GitHub repository for a specific service and profile.
                    
                    **Permission Required:** VIEW_SERVICE
                    - Team members can view config files for services owned by their team
                    - SYS_ADMIN can view all config files
                    - Users can view config files for services shared to their teams
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "getConfigFile"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Config file retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ConfigGitDtos.ConfigFileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Config file not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ConfigGitDtos.ConfigFileResponse> getConfigFile(
            @Parameter(description = "Service identifier", required = true, example = "sample-service")
            @PathVariable String serviceId,
            @Parameter(description = "Environment profile", required = true, example = "dev")
            @PathVariable String profile,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Getting config file for serviceId={}, profile={}", serviceId, profile);

        ConfigGitService.ConfigFileResponse response = configGitService.getConfigFile(serviceId, profile);
        return ResponseEntity.ok(apiMapper.toConfigFileResponse(response));
    }

    /**
     * Update config file in GitHub repository.
     *
     * @param serviceId the service identifier
     * @param profile   the environment profile
     * @param request   update request with content, optional custom message, and optional expected SHA
     * @param jwt       the JWT token
     * @return commit response with SHA and metadata
     */
    @PutMapping("/{serviceId}/files/{profile}")
    @Operation(
            summary = "Update config file",
            description = """
                    Updates config file in GitHub repository with YAML validation and optimistic locking.
                    
                    **Permission Required:** EDIT_SERVICE
                    - Team members can edit config files for services owned by their team
                    - SYS_ADMIN can edit all config files
                    
                    **Features:**
                    - YAML syntax validation before commit
                    - Optimistic locking via expected SHA (prevents concurrent modification conflicts)
                    - Automatic commit message generation with optional custom message
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "updateConfigFile"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Config file updated successfully",
                    content = @Content(schema = @Schema(implementation = ConfigGitDtos.CommitResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid YAML syntax or request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - file was modified by another user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ConfigGitDtos.CommitResponse> updateConfigFile(
            @Parameter(description = "Service identifier", required = true, example = "sample-service")
            @PathVariable String serviceId,
            @Parameter(description = "Environment profile", required = true, example = "dev")
            @PathVariable String profile,
            @Valid @RequestBody ConfigGitDtos.UpdateConfigFileRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Updating config file for serviceId={}, profile={}", serviceId, profile);

        ConfigGitService.CommitResponse response = configGitService.updateConfigFile(
                serviceId, profile, request.getContent(), request.getCustomMessage(), request.getExpectedSha());

        return ResponseEntity.ok(apiMapper.toCommitResponse(response));
    }

    /**
     * Get commit history for a config file.
     *
     * @param serviceId the service identifier
     * @param profile  the environment profile
     * @param jwt      the JWT token
     * @return list of commits (most recent first)
     */
    @GetMapping("/{serviceId}/files/{profile}/commits")
    @Operation(
            summary = "Get commit history",
            description = """
                    Retrieves commit history for a config file from GitHub repository.
                    
                    **Permission Required:** VIEW_SERVICE
                    - Team members can view commit history for services owned by their team
                    - SYS_ADMIN can view commit history for all services
                    - Users can view commit history for services shared to their teams
                    
                    Returns up to 50 most recent commits.
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "getCommitHistory"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Commit history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ConfigGitDtos.CommitResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Config file not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ConfigGitDtos.CommitResponse>> getCommitHistory(
            @Parameter(description = "Service identifier", required = true, example = "sample-service")
            @PathVariable String serviceId,
            @Parameter(description = "Environment profile", required = true, example = "dev")
            @PathVariable String profile,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Getting commit history for serviceId={}, profile={}", serviceId, profile);

        List<ConfigGitService.CommitResponse> responses = configGitService.getCommitHistory(serviceId, profile);
        return ResponseEntity.ok(apiMapper.toCommitResponseList(responses));
    }
}

