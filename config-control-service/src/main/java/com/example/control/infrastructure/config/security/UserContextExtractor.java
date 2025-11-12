package com.example.control.infrastructure.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

/**
 * Utility class for extracting UserContext from Spring Security context.
 * <p>
 * This utility handles multiple authentication types:
 * <ul>
 *   <li>JWT authentication (JwtAuthenticationToken)</li>
 *   <li>API key authentication (UsernamePasswordAuthenticationToken with authorities)</li>
 *   <li>Other authentication types (falls back to authorities)</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
@Slf4j
public class UserContextExtractor {

    /**
     * Extracts UserContext from the current SecurityContext.
     * <p>
     * This method handles both JWT and API key authentication:
     * <ul>
     *   <li>If authentication is a JwtAuthenticationToken, extracts UserContext from JWT</li>
     *   <li>If authentication has authorities (e.g., API key), extracts UserContext from authorities</li>
     *   <li>Falls back to fromAuthorities if principal is not a JWT</li>
     * </ul>
     * </p>
     *
     * @return UserContext extracted from SecurityContext
     * @throws IllegalStateException if no authentication is found in SecurityContext
     */
    public static UserContext extract() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("No authentication found in SecurityContext");
        }

        // Handle JWT authentication
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return UserContext.fromJwt(jwt);
        }

        // Handle API key authentication or other authentication with authorities
        // API key authentication creates UsernamePasswordAuthenticationToken with ROLE_SYS_ADMIN
        if (auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            List<GrantedAuthority> authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority.class::cast)
                    .toList();
            return UserContext.fromAuthorities(authorities);
        }

        // Fallback: create minimal context
        log.warn("Authentication found but no JWT or authorities available. Principal type: {}", 
                auth.getPrincipal() != null ? auth.getPrincipal().getClass().getName() : "null");
        return UserContext.builder()
                .roles(List.of())
                .teamIds(List.of())
                .build();
    }

    /**
     * Extracts UserContext from an Authentication object.
     * <p>
     * This is a convenience method that can be used when you already have an Authentication object.
     * </p>
     *
     * @param auth the Authentication object
     * @return UserContext extracted from the authentication
     * @throws IllegalArgumentException if auth is null
     */
    public static UserContext extract(Authentication auth) {
        if (auth == null) {
            throw new IllegalArgumentException("Authentication cannot be null");
        }

        // Handle JWT authentication
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return UserContext.fromJwt(jwt);
        }

        // Handle API key authentication or other authentication with authorities
        if (auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            List<GrantedAuthority> authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority.class::cast)
                    .toList();
            return UserContext.fromAuthorities(authorities);
        }

        // Fallback: create minimal context
        log.warn("Authentication found but no JWT or authorities available. Principal type: {}", 
                auth.getPrincipal() != null ? auth.getPrincipal().getClass().getName() : "null");
        return UserContext.builder()
                .roles(List.of())
                .teamIds(List.of())
                .build();
    }
}

