package com.example.control.infrastructure.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Spring Security and Keycloak integration.
 * <p>
 * Maps application.yml security configuration to strongly-typed properties.
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.security.oauth2.resourceserver")
public class SecurityProperties {

    /**
     * JWT configuration properties.
     */
    private Jwt jwt = new Jwt();

    /**
     * JWT-specific configuration properties.
     */
    @Data
    public static class Jwt {
        /**
         * Keycloak issuer URI for JWT validation.
         */
        private String issuerUri;

        /**
         * Expected audience claim in JWT tokens.
         */
        private String audience;

        /**
         * JWK Set URI for token validation (derived from issuer URI).
         */
        private String jwkSetUri;
    }
}
