package com.example.control.infrastructure.config.keycloak;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Keycloak Admin REST API client.
 * <p>
 * Binds to {@code keycloak.admin.*} properties in application.yml and provides
 * type-safe access to Keycloak admin client configuration with validation.
 * </p>
 *
 * <p>
 * Example configuration:
 *
 * <pre>
 * keycloak:
 *   admin:
 *     url: http://keycloak:8080
 *     realm: config-control
 *     client-id: config-control-service
 *     client-secret: secret-value
 * </pre>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakAdminProperties {

    /**
     * Keycloak server base URL (without /admin path).
     * Example: http://keycloak:8080
     */
    @NotBlank(message = "Keycloak admin URL is required")
    private String url;

    /**
     * Keycloak realm name.
     * Default: config-control
     */
    @NotBlank(message = "Keycloak realm is required")
    private String realm = "config-control";

    /**
     * Client ID for admin API access (client credentials flow).
     */
    @NotBlank(message = "Keycloak client ID is required")
    private String clientId;

    /**
     * Client secret for admin API access.
     */
    @NotBlank(message = "Keycloak client secret is required")
    private String clientSecret;
}

