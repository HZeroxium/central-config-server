package com.example.control.api.controller;

import com.example.control.api.dto.common.ApiResponseDto;
import com.example.control.api.dto.domain.UserDtos;
import com.example.control.api.mapper.domain.UserApiMapper;
import com.example.control.application.service.UserPermissionsService;
import com.example.control.config.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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
    public ResponseEntity<UserDtos.MeResponse> whoami(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting current user information (whoami)");
        
        UserContext userContext = UserContext.fromJwt(jwt);
        UserDtos.MeResponse response = mapper.toMeResponse(userContext);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user information (legacy /me endpoint).
     *
     * @param jwt the JWT token
     * @return the user information
     */
    @GetMapping("/me")
    public ResponseEntity<UserDtos.MeResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting current user information (legacy /me)");
        
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
    public ResponseEntity<ApiResponseDto.ApiResponse<UserPermissionsService.UserPermissions>> getPermissions(
            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting current user permissions");
        
        UserContext userContext = UserContext.fromJwt(jwt);
        UserPermissionsService.UserPermissions permissions = userPermissionsService.getUserPermissions(userContext);
        
        return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(permissions));
    }
}