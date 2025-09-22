package com.example.rest.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for operation status queries (V2 APIs)
 * Returns detailed operation tracking information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Operation status response for tracking async operations")
public class OperationStatusResponse {

  @Schema(description = "Unique operation identifier", example = "550e8400-e29b-41d4-a716-446655440000")
  private String operationId;

  @Schema(description = "Current operation status", example = "COMPLETED", allowableValues = { "PENDING", "IN_PROGRESS",
      "COMPLETED", "FAILED", "CANCELLED" })
  private String status;

  @Schema(description = "Operation creation timestamp", example = "2024-01-01T12:00:00")
  private LocalDateTime createdAt;

  @Schema(description = "Last update timestamp", example = "2024-01-01T12:01:00")
  private LocalDateTime updatedAt;

  @Schema(description = "Completion timestamp", example = "2024-01-01T12:01:30")
  private LocalDateTime completedAt;

  @Schema(description = "Operation result (for successful operations)", example = "{\"id\": \"user-123\", \"name\": \"John Doe\"}")
  private String result;

  @Schema(description = "Error message (for failed operations)", example = "Validation failed: name is required")
  private String errorMessage;

  @Schema(description = "Error code (for failed operations)", example = "VALIDATION_ERROR")
  private String errorCode;

  @Schema(description = "Correlation ID for tracing", example = "trace-123")
  private String correlationId;

  @Schema(description = "Operation progress percentage (0-100)", example = "75")
  private Integer progressPercentage;

  @Schema(description = "Human-readable status description", example = "User creation completed successfully")
  private String statusDescription;
}
