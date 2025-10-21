package com.example.control.api.dto.configserver;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTOs for Config Server API responses
 */
public class ConfigServerDto {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Response containing configuration environment details")
  public static class ConfigEnvironmentResponse {
    @Schema(description = "Name of the application", example = "payment-service")
    private String name;
    @Schema(description = "List of active profiles", example = "[\"dev\", \"kafka\"]")
    private List<String> profiles;
    @Schema(description = "Git label/branch used", example = "main")
    private String label;
    @Schema(description = "Version of the configuration", example = "abc123def456")
    private String version;
    @Schema(description = "State of the configuration", example = "ACTIVE")
    private String state;
    @Schema(description = "List of property sources")
    private List<PropertySource> propertySources;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Property source containing configuration properties")
  public static class PropertySource {
    @Schema(description = "Name of the property source", example = "application-dev.yml")
    private String name;
    @Schema(description = "Map of configuration properties")
    private Map<String, Object> source;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Health status response from Config Server actuator")
  public static class ActuatorHealthResponse {
    @Schema(description = "Overall health status", example = "UP", allowableValues = {"UP", "DOWN", "OUT_OF_SERVICE"})
    private String status;
    @Schema(description = "Health status of individual components")
    private Map<String, Object> components;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Environment information from Config Server actuator")
  public static class ActuatorEnvResponse {
    @Schema(description = "List of active profiles", example = "[\"dev\", \"kafka\"]")
    private List<String> activeProfiles;
    @Schema(description = "List of default profiles", example = "[\"default\"]")
    private List<String> defaultProfiles;
    @Schema(description = "List of property source information")
    private List<PropertySourceInfo> propertySources;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Property source information with detailed property values")
  public static class PropertySourceInfo {
    @Schema(description = "Name of the property source", example = "application-dev.yml")
    private String name;
    @Schema(description = "Map of properties with their values and origins")
    private Map<String, PropertyValue> properties;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Property value with its origin information")
  public static class PropertyValue {
    @Schema(description = "The property value", example = "localhost:9092")
    private Object value;
    @Schema(description = "Origin of the property value", example = "application-dev.yml:5:10")
    private String origin;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Config Server instance information")
  public static class ConfigServerInfo {
    @Schema(description = "Config Server URL", example = "http://config-server:8888")
    private String url;
    @Schema(description = "Config Server status", example = "UP", allowableValues = {"UP", "DOWN"})
    private String status;
    @Schema(description = "Config Server version", example = "1.0.0")
    private String version;
  }
}
