package com.example.control.infrastructure.config.security;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Spring Security and Keycloak integration.
 * <p>
 * Maps application.yml security configuration to strongly-typed properties.
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private final Environment environment;

    public SecurityProperties(Environment environment) {
        this.environment = environment;
        this.jwt = new Jwt();
    }

    /**
     * JWT configuration properties.
     * <p>
     * JWT properties are read from {@code spring.security.oauth2.resourceserver.jwt.*}
     * via Spring Boot's auto-configuration and populated from Environment.
     * </p>
     */
    private final Jwt jwt;

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

    /**
     * Populates JWT properties from Spring Security configuration after construction.
     */
    @PostConstruct
    public void initJwtProperties() {
        jwt.setIssuerUri(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"));
        jwt.setAudience(environment.getProperty("spring.security.oauth2.resourceserver.jwt.audience"));
        jwt.setJwkSetUri(environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri"));
    }

    /**
     * API key configuration properties for SDK client authentication.
     */
    private ApiKey apiKey = new ApiKey();

    /**
     * API key-specific configuration properties.
     */
    @Data
    public static class ApiKey {
        /**
         * Whether API key authentication is enabled.
         */
        private boolean enabled = false;

        /**
         * The hard-coded API key value.
         * <p>
         * Can be overridden by environment variable {@code ZCM_API_KEY}.
         * </p>
         */
        private String key;
    }
}
