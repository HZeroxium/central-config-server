package com.example.control.api.dto.domain;

import com.example.control.domain.object.DriftEvent;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
public class DriftEventDtos {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {
    @NotBlank
    private String serviceName;
    @NotBlank
    private String instanceId;
    private String expectedHash;
    private String appliedHash;
    private DriftEvent.DriftSeverity severity;
    private DriftEvent.DriftStatus status;
    private String detectedBy;
    private String notes;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    private DriftEvent.DriftStatus status;
    private String resolvedBy;
    private String notes;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class QueryFilter {
    private String serviceName;
    private String instanceId;
    private DriftEvent.DriftStatus status;
    private DriftEvent.DriftSeverity severity;
    private Instant detectedAtFrom;
    private Instant detectedAtTo;
    private Boolean unresolvedOnly;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private String id;
    private String serviceName;
    private String instanceId;
    private String expectedHash;
    private String appliedHash;
    private DriftEvent.DriftSeverity severity;
    private DriftEvent.DriftStatus status;
    private Instant detectedAt;
    private Instant resolvedAt;
    private String detectedBy;
    private String resolvedBy;
    private String notes;
  }
}


