package com.example.control.api.dto;

import com.example.control.domain.ServiceInstance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ServiceInstanceDtos {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {
    @NotBlank
    private String serviceName;
    @NotBlank
    private String instanceId;
    private String host;
    @Positive
    private Integer port;
    private String environment;
    private String version;
    private String configHash;
    private String lastAppliedHash;
    private Map<String, String> metadata;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    private String host;
    @Positive
    private Integer port;
    private String environment;
    private String version;
    private String configHash;
    private String lastAppliedHash;
    private Boolean hasDrift;
    private ServiceInstance.InstanceStatus status;
    private Map<String, String> metadata;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class QueryFilter {
    private String serviceName;
    private String instanceId;
    private ServiceInstance.InstanceStatus status;
    private Boolean hasDrift;
    private String environment;
    private String version;
    private LocalDateTime lastSeenAtFrom;
    private LocalDateTime lastSeenAtTo;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private String serviceName;
    private String instanceId;
    private String host;
    private Integer port;
    private String environment;
    private String version;
    private String configHash;
    private String lastAppliedHash;
    private ServiceInstance.InstanceStatus status;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, String> metadata;
    private Boolean hasDrift;
    private LocalDateTime driftDetectedAt;
  }
}


