package com.example.control.infrastructure.config.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenAPI/Swagger documentation.
 * <p>
 * Maps properties from {@code app.*} in application.yml.
 * Reuses standard Spring Boot properties where applicable.
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "app")
public class OpenApiProperties {

    /**
     * Application name.
     */
    @NotBlank
    private String name = "config-control-service";

    /**
     * Application version.
     */
    @NotBlank
    private String version = "1.0.0";

    /**
     * Application environment.
     */
    @NotBlank
    private String environment = "development";

    /**
     * Server port (reuses server.port from Spring Boot).
     * This is read separately via @Value or ServerProperties.
     */
    @Positive
    private int serverPort = 8080;
}

