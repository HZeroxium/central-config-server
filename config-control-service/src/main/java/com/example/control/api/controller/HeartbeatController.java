package com.example.control.api.controller;

import com.example.control.domain.object.HeartbeatPayload;
import com.example.control.application.service.HeartbeatService;
import com.example.control.domain.object.ServiceInstance;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller responsible for receiving heartbeat signals from running service instances.
 * <p>
 * Each heartbeat allows the control plane to:
 * <ul>
 *   <li>Track instance liveness and metadata</li>
 *   <li>Detect configuration drift via config hash comparison</li>
 *   <li>Trigger automatic config refresh on persistent drift</li>
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
  @Operation(summary = "Process heartbeat",
      description = "Receive heartbeat from a service instance with config hash for drift detection")
  @Timed(value = "api.heartbeat.process", description = "Time taken to process heartbeat")
  public ResponseEntity<Map<String, Object>> processHeartbeat(@Valid @RequestBody HeartbeatPayload payload) {
    log.debug("Received heartbeat from {}:{}", payload.getServiceName(), payload.getInstanceId());

    ServiceInstance instance = heartbeatService.processHeartbeat(payload);

    return ResponseEntity.ok(Map.of(
        "status", "ok",
        "message", "Heartbeat processed",
        "instance", Map.of(
            "serviceName", instance.getServiceName(),
            "instanceId", instance.getInstanceId(),
            "status", instance.getStatus().name(),
            "hasDrift", instance.isDrifted(),
            "configHash", instance.getConfigHash() != null ? instance.getConfigHash() : "unknown",
            "lastAppliedHash", instance.getLastAppliedHash() != null ? instance.getLastAppliedHash() : "unknown")));
  }

  /**
   * Simple health check endpoint to verify that the heartbeat controller is operational.
   *
   * @return status JSON with "UP" if controller is healthy
   */
  @GetMapping("/health")
  @Operation(summary = "Health check", description = "Check if heartbeat endpoint is operational")
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of(
        "status", "UP",
        "service", "heartbeat-controller"));
  }
}
