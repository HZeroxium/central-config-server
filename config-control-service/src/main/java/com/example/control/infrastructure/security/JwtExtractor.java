package com.example.control.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

/**
 * Utility class for extracting information from JWT tokens.
 * <p>
 * Provides helper methods for extracting client identifiers from JWT tokens
 * used in M2M authentication.
 * </p>
 */
public class JwtExtractor {

    /**
     * Extract client ID from JWT token.
     * <p>
     * Tries to extract client ID from JWT claims in the following order:
     * <ol>
     * <li>{@code azp} (authorized party) claim - preferred for client credentials flow</li>
     * <li>{@code aud[0]} (first audience) claim - fallback if azp is not present</li>
     * </ol>
     * </p>
     *
     * @param jwt the JWT token
     * @return the client ID if found, null otherwise
     */
    public static String extractClientId(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        // Try 'azp' claim first (authorized party - preferred for client credentials)
        if (jwt.hasClaim("azp")) {
            String azp = jwt.getClaimAsString("azp");
            if (azp != null && !azp.isEmpty()) {
                return azp;
            }
        }

        // Fallback to first audience claim
        List<String> aud = jwt.getAudience();
        if (aud != null && !aud.isEmpty()) {
            return aud.get(0);
        }

        return null;
    }

    /**
     * Extract client ID from current SecurityContext.
     * <p>
     * Extracts JWT from SecurityContext and returns the client ID.
     * </p>
     *
     * @return the client ID if found, null otherwise
     * @throws IllegalStateException if no authentication is found in SecurityContext
     */
    public static String extractClientIdFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("No authentication found in SecurityContext");
        }

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return extractClientId(jwt);
        }

        return null;
    }
}

