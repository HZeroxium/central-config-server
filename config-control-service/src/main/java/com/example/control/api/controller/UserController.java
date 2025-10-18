package com.example.control.api.controller;

import com.example.control.api.dto.UserDtos;
import com.example.control.api.mapper.UserApiMapper;
import com.example.control.config.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for User operations.
 * <p>
 * This controller provides operations for managing user information,
 * including the current user's profile and preferences.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Operations for managing user information")
public class UserController {

    /**
     * Get current user information.
     *
     * @param jwt the JWT token
     * @return the current user information
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get information about the current authenticated user")
    public ResponseEntity<UserDtos.MeResponse> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Getting current user information");
        
        UserContext userContext = UserContext.fromJwt(jwt);
        UserDtos.MeResponse response = UserApiMapper.toMeResponse(userContext);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user profile.
     *
     * @param jwt the JWT token
     * @return the current user profile
     */
    @GetMapping("/me/profile")
    @Operation(summary = "Get current user profile", description = "Get detailed profile information for the current user")
    public ResponseEntity<UserDtos.ProfileResponse> getCurrentUserProfile(
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Getting current user profile");
        
        UserContext userContext = UserContext.fromJwt(jwt);
        UserDtos.ProfileResponse response = UserApiMapper.toProfileResponse(userContext);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update current user profile.
     *
     * @param request the update request
     * @param jwt the JWT token
     * @return the updated user profile
     */
    @PutMapping("/me/profile")
    @Operation(summary = "Update current user profile", description = "Update profile information for the current user")
    public ResponseEntity<UserDtos.ProfileResponse> updateCurrentUserProfile(
            @Valid @RequestBody UserDtos.UpdateProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Updating current user profile");
        
        UserContext userContext = UserContext.fromJwt(jwt);
        
        // Note: In a real implementation, this would update the user's profile in the identity provider
        // For now, we'll just return the current user context with the updated fields
        UserContext updatedContext = UserContext.builder()
                .userId(userContext.getUserId())
                .username(userContext.getUsername())
                .email(request.email() != null ? request.email() : userContext.getEmail())
                .firstName(request.firstName() != null ? request.firstName() : userContext.getFirstName())
                .lastName(request.lastName() != null ? request.lastName() : userContext.getLastName())
                .managerId(userContext.getManagerId())
                .teamIds(userContext.getTeamIds())
                .roles(userContext.getRoles())
                .lastLoginAt(userContext.getLastLoginAt())
                .createdAt(userContext.getCreatedAt())
                .updatedAt(java.time.Instant.now())
                .build();
        
        UserDtos.ProfileResponse response = UserApiMapper.toProfileResponse(updatedContext);
        
        return ResponseEntity.ok(response);
    }
}
