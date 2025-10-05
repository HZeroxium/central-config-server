package com.example.control.api.dto;

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
  public static class ConfigEnvironmentResponse {
    private String name;
    private List<String> profiles;
    private String label;
    private String version;
    private String state;
    private List<PropertySource> propertySources;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PropertySource {
    private String name;
    private Map<String, Object> source;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ActuatorHealthResponse {
    private String status;
    private Map<String, Object> components;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ActuatorEnvResponse {
    private List<String> activeProfiles;
    private List<String> defaultProfiles;
    private List<PropertySourceInfo> propertySources;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PropertySourceInfo {
    private String name;
    private Map<String, PropertyValue> properties;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PropertyValue {
    private Object value;
    private String origin;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ConfigServerInfo {
    private String url;
    private String status;
    private String version;
  }
}
