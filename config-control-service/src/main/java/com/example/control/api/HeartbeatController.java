package com.example.control.api;

import com.example.control.application.HeartbeatPayload;
import com.example.control.application.HeartbeatService;
import com.example.control.domain.ServiceInstance;
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
 * REST controller for receiving heartbeat from service instances.
 */
@Slf4j
@RestController
@RequestMapping("/api/heartbeat")
@RequiredArgsConstructor
@Tag(name = "Heartbeat", description = "Service instance heartbeat and health tracking")
public class HeartbeatController {

  private final HeartbeatService heartbeatService;

  @PostMapping
  @Operation(summary = "Process heartbeat", description = "Receive heartbeat from a service instance with config hash for drift detection")
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

  @GetMapping("/health")
  @Operation(summary = "Health check", description = "Check if heartbeat endpoint is operational")
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of(
        "status", "UP",
        "service", "heartbeat-controller"));
  }
}
