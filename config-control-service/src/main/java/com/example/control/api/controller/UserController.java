package com.example.control.api.controller;

import com.example.control.api.dto.UserDtos;
import com.example.control.api.mapper.UserApiMapper;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserApiMapper mapper;

    /**
     * Get current user information.
     *
     * @param jwt the JWT token
     * @return the user information
     */
    @GetMapping("/me")
    public ResponseEntity<UserDtos.MeResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Getting current user information");
        
        UserContext userContext = UserContext.fromJwt(jwt);
        UserDtos.MeResponse response = mapper.toMeResponse(userContext);
        
        return ResponseEntity.ok(response);
    }
}