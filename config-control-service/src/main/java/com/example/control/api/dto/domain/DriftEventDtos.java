package com.example.control.api.dto.domain;

import com.example.control.domain.object.DriftEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTOs for DriftEvent API operations.
 * <p>
 * Provides request/response DTOs for managing configuration drift events
 * with severity tracking and resolution workflow.
 * </p>
 */
@Data
@Schema(name = "DriftEventDtos", description = "DTOs for DriftEvent API operations")
public class DriftEventDtos {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "DriftEventCreateRequest", description = "Request to create a new drift event")
  public static class CreateRequest {
    @NotBlank
    @Schema(description = "Name of the service", example = "payment-service")
    private String serviceName;
    
    @NotBlank
    @Schema(description = "Instance identifier", example = "payment-dev-1")
    private String instanceId;
    
    @Schema(description = "Expected configuration hash", example = "abc123def456")
    private String expectedHash;
    
    @Schema(description = "Applied configuration hash", example = "def456ghi789")
    private String appliedHash;
    
    @Schema(description = "Drift severity level", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
    private DriftEvent.DriftSeverity severity;
    
    @Schema(description = "Drift status", example = "DETECTED", allowableValues = {"DETECTED", "RESOLVED", "IGNORED"})
    private DriftEvent.DriftStatus status;
    
    @Schema(description = "Who detected the drift", example = "system")
    private String detectedBy;
    
    @Schema(description = "Additional notes about the drift", example = "Configuration mismatch in database connection settings")
    private String notes;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "DriftEventUpdateRequest", description = "Request to update an existing drift event")
  public static class UpdateRequest {
    @Schema(description = "New drift status", example = "RESOLVED", allowableValues = {"DETECTED", "RESOLVED", "IGNORED"})
    private DriftEvent.DriftStatus status;
    
    @Schema(description = "Who resolved the drift", example = "user1")
    private String resolvedBy;
    
    @Schema(description = "Resolution notes", example = "Applied configuration update to resolve drift")
    private String notes;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "DriftEventQueryFilter", description = "Query filter for searching drift events")
  public static class QueryFilter {
    @Schema(description = "Filter by service name", example = "payment-service")
    private String serviceName;
    
    @Schema(description = "Filter by instance ID", example = "payment-dev-1")
    private String instanceId;
    
    @Schema(description = "Filter by drift status", example = "DETECTED", allowableValues = {"DETECTED", "RESOLVED", "IGNORED"})
    private DriftEvent.DriftStatus status;
    
    @Schema(description = "Filter by severity level", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
    private DriftEvent.DriftSeverity severity;
    
    @Schema(description = "Filter events detected after this timestamp", example = "2024-01-15T00:00:00Z")
    private Instant detectedAtFrom;
    
    @Schema(description = "Filter events detected before this timestamp", example = "2024-01-15T23:59:59Z")
    private Instant detectedAtTo;
    
    @Schema(description = "Show only unresolved events", example = "true")
    private Boolean unresolvedOnly;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "DriftEventResponse", description = "Drift event details response")
  public static class Response {
    @Schema(description = "Unique drift event identifier", example = "drift-12345")
    private String id;
    
    @Schema(description = "Name of the service", example = "payment-service")
    private String serviceName;
    
    @Schema(description = "Instance identifier", example = "payment-dev-1")
    private String instanceId;
    
    @Schema(description = "Expected configuration hash", example = "abc123def456")
    private String expectedHash;
    
    @Schema(description = "Applied configuration hash", example = "def456ghi789")
    private String appliedHash;
    
    @Schema(description = "Drift severity level", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
    private DriftEvent.DriftSeverity severity;
    
    @Schema(description = "Drift status", example = "DETECTED", allowableValues = {"DETECTED", "RESOLVED", "IGNORED"})
    private DriftEvent.DriftStatus status;
    
    @Schema(description = "When drift was detected", example = "2024-01-15T14:30:45.123Z")
    private Instant detectedAt;
    
    @Schema(description = "When drift was resolved", example = "2024-01-15T15:45:30.456Z")
    private Instant resolvedAt;
    
    @Schema(description = "Who detected the drift", example = "system")
    private String detectedBy;
    
    @Schema(description = "Who resolved the drift", example = "user1")
    private String resolvedBy;
    
    @Schema(description = "Event notes", example = "Configuration mismatch in database connection settings")
    private String notes;
  }
}


