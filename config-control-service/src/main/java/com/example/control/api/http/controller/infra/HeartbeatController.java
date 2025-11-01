package com.example.control.api.http.controller.infra;

import com.example.control.domain.model.HeartbeatPayload;
import com.example.control.application.service.infra.HeartbeatService;
import com.example.control.domain.model.ServiceInstance;
import com.example.control.api.http.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller responsible for receiving heartbeat signals from running
 * service instances.
 * <p>
 * Each heartbeat allows the control plane to:
 * <ul>
 * <li>Track instance liveness and metadata</li>
 * <li>Detect configuration drift via config hash comparison</li>
 * <li>Trigger automatic config refresh on persistent drift</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/heartbeat")
@RequiredArgsConstructor
@Tag(name = "Heartbeat", description = "Service instance heartbeat and health tracking")
public class HeartbeatController {

  private final HeartbeatService heartbeatService;

  /**
   * Processes a heartbeat payload sent from a service instance.
   * <p>
   * Delegates to {@link HeartbeatService#processHeartbeat(HeartbeatPayload)}
   * to perform validation, drift detection, and persistence.
   *
   * @param payload validated heartbeat data including config hash
   * @return standardized JSON response with drift and status info
   */
  @PostMapping
  @Operation(summary = "Process heartbeat from service instance", description = """
      Receives periodic heartbeat signals from running service instances.
      This endpoint tracks instance liveness, detects configuration drift,
      and can trigger automatic configuration refreshes.

      **Public Endpoint:** No authentication required for SDK integration
      **Drift Detection:** Compares current config hash with last applied hash
      **Auto-Refresh:** Can trigger config refresh if drift is detected
      """, security = {
      @SecurityRequirement(name = "oauth2_auth_code"),
      @SecurityRequirement(name = "oauth2_password")
  }, operationId = "processHeartbeat")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Heartbeat processed successfully", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "Successful Heartbeat", value = """
              {
                "status": "ok",
                "message": "Heartbeat processed",
                "instance": {
                  "serviceName": "payment-service",
                  "instanceId": "payment-dev-1",
                  "status": "HEALTHY",
                  "hasDrift": false,
                  "configHash": "abc123def456",
                  "lastAppliedHash": "abc123def456"
                }
              }
              """)
      })),
      @ApiResponse(responseCode = "400", description = "Invalid heartbeat payload", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error during heartbeat processing", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<Map<String, Object>> processHeartbeat(
      @Parameter(description = "Heartbeat payload with service instance information and config hash", schema = @Schema(implementation = HeartbeatPayload.class)) @Valid @RequestBody HeartbeatPayload payload) {
    log.debug("Received heartbeat from {}:{}", payload.getServiceName(), payload.getInstanceId());

    ServiceInstance instance = heartbeatService.processHeartbeat(payload);

    // Build response with null-safe values
    // Use HashMap instead of Map.of() to handle null values
    Map<String, Object> instanceMap = new HashMap<>();
    instanceMap.put("serviceName", instance.getServiceId() != null ? instance.getServiceId() : "unknown");
    instanceMap.put("instanceId", instance.getInstanceId() != null ? instance.getInstanceId() : "unknown");
    instanceMap.put("status", instance.getStatus() != null ? instance.getStatus().name() : "UNKNOWN");
    instanceMap.put("hasDrift", instance.isDrifted());
    instanceMap.put("configHash", instance.getConfigHash() != null ? instance.getConfigHash() : "unknown");
    instanceMap.put("lastAppliedHash",
        instance.getLastAppliedHash() != null ? instance.getLastAppliedHash() : "unknown");

    Map<String, Object> response = new HashMap<>();
    response.put("status", "ok");
    response.put("message", "Heartbeat processed");
    response.put("instance", instanceMap);

    return ResponseEntity.ok(response);
  }

  /**
   * Simple health check endpoint to verify that the heartbeat controller is
   * operational.
   *
   * @return status JSON with "UP" if controller is healthy
   */
  @GetMapping("/health")
  @Operation(summary = "Health check endpoint", description = """
      Simple health check to verify that the heartbeat controller is operational.
      This endpoint is used for monitoring and load balancer health checks.
      """, security = {
      @SecurityRequirement(name = "oauth2_auth_code"),
      @SecurityRequirement(name = "oauth2_password")
  }, operationId = "heartbeatHealth")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Heartbeat controller is healthy", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "Healthy Status", value = """
              {
                "status": "UP",
                "service": "heartbeat-controller"
              }
              """)
      })),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of(
        "status", "UP",
        "service", "heartbeat-controller"));
  }
}
