package com.example.rest.user.controller;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.rest.user.dto.OperationStatusResponse;
import com.example.rest.user.service.OperationTrackingService;
import com.example.rest.user.constants.ApiConstants;

/**
 * Controller for tracking async operation status (V2 APIs)
 * Provides endpoints to query operation progress and results
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.OPERATIONS_V2_BASE_PATH)
@Tag(name = "Operations", description = "Async operation tracking endpoints")
public class OperationController {

  private final OperationTrackingService trackingService;

  @GetMapping(ApiConstants.OPERATION_STATUS_PATH)
  @Operation(summary = "Get operation status", description = "Query the status and progress of an async operation")
  @ApiResponse(responseCode = "200", description = "Operation status retrieved", content = @Content(schema = @Schema(implementation = OperationStatusResponse.class)))
  @ApiResponse(responseCode = "404", description = "Operation not found")
  @Timed(value = "rest.api.v2.operation.status", description = "V2 Operation status endpoint timing")
  public ResponseEntity<OperationStatusResponse> getStatus(
      @PathVariable @Parameter(description = "Operation ID") String operationId) {
    log.info("Getting status for operation ID: {}", operationId);

    return trackingService.getOperationStatus(operationId)
        .map(status -> {
          log.info("Operation status found: {} for ID: {}", status.getStatus(), operationId);
          return ResponseEntity.ok(status);
        })
        .orElseGet(() -> {
          log.warn("Operation not found with ID: {}", operationId);
          return ResponseEntity.notFound().build();
        });
  }
}
