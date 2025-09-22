package com.example.rest.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for async operations (V2 APIs)
 * Returns operation ID and tracking information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Async operation response for V2 endpoints")
public class AsyncOperationResponse {

  @Schema(description = "Unique operation identifier for tracking", example = "550e8400-e29b-41d4-a716-446655440000")
  private String operationId;

  @Schema(description = "Current operation status", example = "PENDING", allowableValues = { "PENDING", "IN_PROGRESS",
      "COMPLETED", "FAILED", "CANCELLED" })
  private String status;

  @Schema(description = "Human-readable message", example = "User creation request submitted")
  private String message;

  @Schema(description = "URL to track operation status", example = "/v2/operations/550e8400-e29b-41d4-a716-446655440000/status")
  private String trackingUrl;

  @Schema(description = "Operation timestamp", example = "2024-01-01T12:00:00")
  private LocalDateTime timestamp;

  @Schema(description = "Correlation ID for tracing", example = "trace-123")
  private String correlationId;
}
