package com.example.control.api.controller;

import com.example.control.api.dto.domain.UserDtos;
import com.example.control.api.mapper.domain.UserApiMapper;
import com.example.control.application.service.UserPermissionsService;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.api.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * REST controller for User operations.
 * <p>
 * Provides endpoints for user information with
 * JWT authentication.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User information and permissions endpoints")
public class UserController {

    private final UserApiMapper mapper;
    private final UserPermissionsService userPermissionsService;

    /**
     * Get current user information (whoami endpoint).
     *
     * @param jwt the JWT token
     * @return the user information
     */
    @GetMapping("/whoami")
    @Operation(
            summary = "Get current user information",
            description = """
                    Retrieve detailed information about the currently authenticated user.
                    This includes user ID, username, email, names, team memberships, roles, and manager ID.
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "findCurrentUserInformation"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserDtos.MeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserDtos.MeResponse> whoami(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting current user information (whoami)");

        UserContext userContext = UserContext.fromJwt(jwt);
        UserDtos.MeResponse response = mapper.toMeResponse(userContext);

        return ResponseEntity.ok(response);
    }

    /**
     * Get current user permissions and allowed routes.
     *
     * @param jwt the JWT token
     * @return the user permissions
     */
    @GetMapping("/me/permissions")
    @Operation(
            summary = "Get user permissions and routes",
            description = """
                    Retrieve comprehensive permission information for the current user.
                    This includes allowed API routes, UI routes, roles, teams, actions, and service access.
                    
                    **Permission Matrix:**
                    - API routes: Endpoints the user can access
                    - UI routes: Frontend pages the user can navigate to
                    - Actions: Specific operations the user can perform
                    - Services: Owned and shared services the user can access
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "findCurrentUserPermissions"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User permissions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserDtos.PermissionsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserDtos.PermissionsResponse> getPermissions(
            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting current user permissions");

        UserContext userContext = UserContext.fromJwt(jwt);
        UserPermissionsService.UserPermissions permissions = userPermissionsService.getUserPermissions(userContext);

        // Convert to API response format
        UserDtos.PermissionsResponse response = UserDtos.PermissionsResponse.builder()
                .allowedApiRoutes(permissions.getAccessibleApiRoutes() != null ?
                        permissions.getAccessibleApiRoutes().stream().toList() : List.of())
                .allowedUiRoutes(permissions.getAccessibleUiRoutes() != null ?
                        permissions.getAccessibleUiRoutes().stream().toList() : List.of())
                .roles(permissions.getRoles())
                .teams(permissions.getTeamIds())
                .features(Map.of()) // Placeholder for future features
                .actions(permissions.getActions() != null ?
                        permissions.getActions().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().stream().toList()
                                )) : Map.of())
                .ownedServiceIds(permissions.getOwnedServiceIds())
                .sharedServiceIds(permissions.getSharedServiceIds())
                .build();

        return ResponseEntity.ok(response);
    }
}