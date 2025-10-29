package com.example.control.api.dto.configserver;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTOs for Config Server API responses.
 * <p>
 * Provides comprehensive data transfer objects for Spring Cloud Config Server
 * operations including configuration retrieval, health checks, and metadata.
 * </p>
 */
@Schema(name = "ConfigServerDto", description = "DTOs for Config Server API responses")
public final class ConfigServerDto {

    private ConfigServerDto() {
        throw new UnsupportedOperationException("Utility class");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConfigServerEnvironmentResponse", description = "Response containing configuration environment details from Config Server")
    public static class ConfigEnvironmentResponse {
        @Schema(description = "Name of the application requesting configuration",
                example = "payment-service",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @Schema(description = "List of active Spring profiles for this configuration",
                example = "[\"dev\", \"kafka\", \"mysql\"]")
        private List<String> profiles;

        @Schema(description = "Git label, branch, or tag used for this configuration",
                example = "main")
        private String label;

        @Schema(description = "Git commit SHA or version identifier of the configuration",
                example = "abc123def456789")
        private String version;

        @Schema(description = "Current state of the configuration",
                example = "ACTIVE",
                allowableValues = {"ACTIVE", "INACTIVE"})
        private String state;

        @Schema(description = "Ordered list of property sources, with higher priority sources first")
        private List<PropertySource> propertySources;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConfigServerPropertySource", description = "A single property source containing configuration key-value pairs")
    public static class PropertySource {
        @Schema(description = "Name identifying this property source (typically filename)",
                example = "classpath:/config/payment-service-dev.yml")
        private String name;

        @Schema(description = "Map of configuration properties with their values",
                example = "{\"spring.datasource.url\": \"jdbc:mysql://localhost:3306/payment\", \"server.port\": \"8080\"}")
        private Map<String, Object> source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConfigServerActuatorHealthResponse", description = "Health status response from Config Server actuator endpoint")
    public static class ActuatorHealthResponse {
        @Schema(description = "Overall health status of the Config Server",
                example = "UP",
                allowableValues = {"UP", "DOWN", "OUT_OF_SERVICE", "UNKNOWN"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String status;

        @Schema(description = "Health status breakdown by individual components (database, disk space, etc.)",
                example = "{\"diskSpace\": {\"status\": \"UP\", \"details\": {...}}, \"db\": {\"status\": \"UP\"}}")
        private Map<String, Object> components;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConfigServerActuatorEnvResponse", description = "Environment information from Config Server actuator with all property sources")
    public static class ActuatorEnvResponse {
        @Schema(description = "List of currently active Spring profiles",
                example = "[\"dev\", \"kafka\"]")
        private List<String> activeProfiles;

        @Schema(description = "List of default Spring profiles when no profile is specified",
                example = "[\"default\"]")
        private List<String> defaultProfiles;

        @Schema(description = "Complete list of property sources with detailed origin information")
        private List<PropertySourceInfo> propertySources;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConfigServerPropertySourceInfo", description = "Detailed property source information including property origins and values")
    public static class PropertySourceInfo {
        @Schema(description = "Name of the property source",
                example = "classpath:/config/application-dev.yml")
        private String name;

        @Schema(description = "Map of properties with their values and origin metadata",
                example = "{\"server.port\": {\"value\": \"8080\", \"origin\": \"application-dev.yml:3:14\"}}")
        private Map<String, PropertyValue> properties;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConfigServerPropertyValue", description = "A single property value with its source origin for traceability")
    public static class PropertyValue {
        @Schema(description = "The actual property value (can be any type: string, number, boolean, etc.)",
                example = "localhost:9092")
        private Object value;

        @Schema(description = "Source location where this property was defined (file:line:column)",
                example = "classpath:/config/application-dev.yml:15:22")
        private String origin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConfigServerInfo", description = "Basic information about the Config Server instance")
    public static class ConfigServerInfo {
        @Schema(description = "Base URL of the Config Server",
                example = "http://config-server:8888",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String url;

        @Schema(description = "Current operational status of the Config Server",
                example = "UP",
                allowableValues = {"UP", "DOWN", "UNKNOWN"})
        private String status;

        @Schema(description = "Version of the Config Server application",
                example = "1.0.0")
        private String version;
    }
}
