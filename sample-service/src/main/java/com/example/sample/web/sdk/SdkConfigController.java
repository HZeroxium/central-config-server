package com.example.sample.web.sdk;

import com.vng.zing.zcm.client.ClientApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for SDK configuration management API.
 * <p>
 * Provides endpoints to:
 * <ul>
 *   <li>Retrieve configuration snapshot and hash</li>
 *   <li>Get SDK runtime information</li>
 *   <li>Access individual configuration values</li>
 *   <li>List available load balancing policies</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/sdk/config")
@RequiredArgsConstructor
@Tag(name = "SDK Configuration", description = "Endpoints for SDK configuration management and inspection")
public class SdkConfigController {

  private final ClientApi client;

  /**
   * Retrieves the current configuration snapshot and its hash for debugging or drift detection.
   *
   * @return a map containing application, profile, version, and config hash
   */
  @Operation(
      summary = "Get configuration snapshot",
      description = "Retrieve the full configuration snapshot and computed hash for diagnostic purposes")
  @ApiResponse(responseCode = "200", description = "Configuration snapshot retrieved successfully")
  @GetMapping("/snapshot")
  public ResponseEntity<Map<String, Object>> getSnapshot() {
    String hash = client.config().hash();
    Map<String, Object> snap = client.config().snapshot();

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", "ok");
    body.put("hash", hash);
    body.putAll(snap);
    body.put("keyCount", ((Map<?, ?>) snap.get("properties")).size());
    return ResponseEntity.ok(body);
  }

  /**
   * Retrieves SDK metadata such as current load balancer strategy and config hash.
   *
   * @return a summary of SDK runtime info
   */
  @Operation(
      summary = "Get SDK info",
      description = "Return general SDK metadata including config hash and load balancing strategy")
  @ApiResponse(responseCode = "200", description = "SDK info retrieved successfully")
  @GetMapping("/info")
  public ResponseEntity<Map<String, Object>> getSdkInfo() {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("status", "ok");
    info.put("configHash", client.config().hash());
    info.put("loadBalancerStrategy", client.loadBalancer().strategy());
    return ResponseEntity.ok(info);
  }

  /**
   * Retrieves a specific configuration value from the environment.
   *
   * @param key the configuration property key
   * @return key-value pair result
   */
  @Operation(
      summary = "Get configuration value",
      description = "Retrieve a specific configuration key from the environment")
  @Parameter(name = "key", description = "The configuration key to retrieve", required = true)
  @ApiResponse(responseCode = "200", description = "Configuration value retrieved successfully")
  @GetMapping("/value/{key}")
  public ResponseEntity<Map<String, Object>> getConfigValue(@PathVariable String key) {
    String value = client.config().get(key);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "ok");
    result.put("key", key);
    result.put("value", value != null ? value : "null");
    return ResponseEntity.ok(result);
  }

  /**
   * Lists available load balancing policies and the current active strategy.
   *
   * @return policy overview
   */
  @Operation(
      summary = "List available load balancing policies",
      description = "Retrieve all supported load balancing strategies and the currently active one")
  @ApiResponse(responseCode = "200", description = "Policies retrieved successfully")
  @GetMapping("/policies")
  public ResponseEntity<Map<String, Object>> getAvailablePolicies() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "ok");
    result.put("currentStrategy", client.loadBalancer().strategy());
    result.put("availablePolicies", Map.of(
        "ROUND_ROBIN", "Round-robin load balancing (default)",
        "RANDOM", "Random instance selection",
        "WEIGHTED_RANDOM", "Weighted random selection based on instance metadata",
        "RENDEZVOUS", "Rendezvous hashing for session affinity",
        "CONSISTENT_HASHING", "Consistent hashing with virtual nodes"
    ));
    return ResponseEntity.ok(result);
  }
}

