package com.example.sample.web;

import com.vng.zing.zcm.client.ClientApi;
import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing endpoints for testing and interacting with the ZCM SDK.
 * <p>
 * This controller provides utilities to:
 * <ul>
 *   <li>Inspect SDK configuration snapshot and hash</li>
 *   <li>Check available service instances and load balancing policies</li>
 *   <li>Trigger health pings to control services</li>
 *   <li>Perform HTTP calls to discovered services</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/sdk")
@RequiredArgsConstructor
@Tag(name = "ZCM SDK Test Controller", description = "Endpoints for inspecting SDK configuration, discovery, and load balancing behavior.")
public class SdkTestController {

  private final ClientApi client;

  // =====================================================================
  //  Snapshot and SDK Info
  // =====================================================================

  /**
   * Retrieves the current configuration snapshot and its hash for debugging or drift detection.
   *
   * @return a map containing application, profile, version, and config hash
   */
  @Operation(summary = "Get configuration snapshot", description = "Retrieve the full configuration snapshot and computed hash for diagnostic purposes.")
  @ApiResponse(responseCode = "200", description = "Configuration snapshot retrieved successfully")
  @GetMapping("/snapshot")
  public ResponseEntity<Map<String, Object>> getSnapshot() {
    String hash = client.configHash();
    Map<String, Object> snap = client.configSnapshotMap();

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
  @Operation(summary = "Get SDK info", description = "Return general SDK metadata including config hash and load balancing strategy.")
  @GetMapping("/info")
  public ResponseEntity<Map<String, Object>> getSdkInfo() {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("status", "ok");
    info.put("configHash", client.configHash());
    info.put("loadBalancerStrategy", client.loadBalancerStrategy());
    return ResponseEntity.ok(info);
  }

  // =====================================================================
  //  Discovery and Load Balancing
  // =====================================================================

  /**
   * Discovers all instances of a given service via Spring Discovery Client.
   *
   * @param serviceName target service to discover
   * @return details of discovered instances
   */
  @Operation(summary = "Discover service instances", description = "Lists all available service instances for a given service name from the discovery provider.")
  @Parameter(name = "serviceName", description = "The name of the service to discover", required = true)
  @GetMapping("/discovery/{serviceName}")
  public ResponseEntity<Map<String, Object>> discoverService(@PathVariable String serviceName) {
    List<ServiceInstance> instances = client.instances(serviceName);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "ok");
    result.put("serviceName", serviceName);
    result.put("instanceCount", instances.size());
    result.put("instances", instances.stream().map(i -> Map.of(
        "instanceId", i.getInstanceId(),
        "host", i.getHost(),
        "port", i.getPort(),
        "metadata", i.getMetadata()
    )).toList());
    return ResponseEntity.ok(result);
  }

  /**
   * Chooses one instance using the default load balancing strategy.
   *
   * @param serviceName target service name
   * @return chosen instance details
   */
  @Operation(summary = "Choose service instance (default policy)", description = "Selects a service instance using the default load balancing policy (e.g., ROUND_ROBIN).")
  @Parameter(name = "serviceName", description = "Service name to choose from", required = true)
  @GetMapping("/choose/{serviceName}")
  public ResponseEntity<Map<String, Object>> chooseInstance(@PathVariable String serviceName) {
    ServiceInstance chosen = client.choose(serviceName);
    if (chosen == null) {
      return ResponseEntity.notFound().build();
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "ok");
    result.put("serviceName", serviceName);
    result.put("strategy", client.loadBalancerStrategy());
    result.put("chosen", Map.of(
        "instanceId", chosen.getInstanceId(),
        "host", chosen.getHost(),
        "port", chosen.getPort(),
        "uri", chosen.getUri().toString(),
        "metadata", chosen.getMetadata()
    ));
    return ResponseEntity.ok(result);
  }

  /**
   * Chooses one instance using a specific load balancing policy.
   *
   * @param serviceName the service name
   * @param policy      the policy string (e.g., ROUND_ROBIN, RANDOM, WEIGHTED_RANDOM)
   * @return selected instance or error if invalid policy
   */
  @Operation(summary = "Choose service instance (custom policy)",
      description = "Selects an instance based on a specific load balancer policy (ROUND_ROBIN, RANDOM, or WEIGHTED_RANDOM).")
  @Parameter(name = "serviceName", description = "Target service name", required = true)
  @Parameter(name = "policy", description = "Load balancing policy", required = true, example = "ROUND_ROBIN")
  @GetMapping("/choose/{serviceName}/{policy}")
  public ResponseEntity<Map<String, Object>> chooseInstanceWithPolicy(
      @PathVariable String serviceName,
      @PathVariable String policy) {
    try {
      LoadBalancerStrategy.Policy policyEnum = LoadBalancerStrategy.Policy.fromString(policy);
      ServiceInstance chosen = client.choose(serviceName, policyEnum);
      if (chosen == null) {
        return ResponseEntity.notFound().build();
      }
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("status", "ok");
      result.put("serviceName", serviceName);
      result.put("strategy", policyEnum.getValue());
      result.put("chosen", Map.of(
          "instanceId", chosen.getInstanceId(),
          "host", chosen.getHost(),
          "port", chosen.getPort(),
          "uri", chosen.getUri().toString(),
          "metadata", chosen.getMetadata()
      ));
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("status", "error");
      error.put("message", "Invalid policy: " + policy);
      error.put("validPolicies", new String[]{"ROUND_ROBIN", "RANDOM", "WEIGHTED_RANDOM"});
      return ResponseEntity.badRequest().body(error);
    }
  }

  // =====================================================================
  //  Ping and Config
  // =====================================================================

  /**
   * Triggers an immediate ping to the control service.
   *
   * @return success confirmation
   */
  @Operation(summary = "Trigger SDK ping", description = "Manually triggers a ping to the control service to update heartbeat and configuration hash.")
  @PostMapping("/ping")
  public ResponseEntity<Map<String, String>> triggerPing() {
    client.pingNow();
    return ResponseEntity.ok(Map.of("status", "ok", "message", "Ping triggered"));
  }

  /**
   * Retrieves a specific configuration value from the environment.
   *
   * @param key the configuration property key
   * @return key-value pair result
   */
  @Operation(summary = "Get configuration value", description = "Retrieve a specific configuration key from the environment.")
  @Parameter(name = "key", description = "The configuration key to retrieve", required = true)
  @GetMapping("/config/{key}")
  public ResponseEntity<Map<String, Object>> getConfigValue(@PathVariable String key) {
    String value = client.get(key);
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
  @Operation(summary = "List available load balancing policies", description = "Retrieve all supported load balancing strategies and the currently active one.")
  @GetMapping("/policies")
  public ResponseEntity<Map<String, Object>> getAvailablePolicies() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "ok");
    result.put("currentStrategy", client.loadBalancerStrategy());
    result.put("availablePolicies", Map.of(
        "ROUND_ROBIN", "Round-robin load balancing (default)",
        "RANDOM", "Random instance selection",
        "WEIGHTED_RANDOM", "Weighted random selection based on instance metadata"
    ));
    return ResponseEntity.ok(result);
  }

  // =====================================================================
  //  Service Call
  // =====================================================================

  /**
   * Calls a target service via the load-balanced {@link ClientApi#http()} RestClient.
   *
   * @param serviceName logical service name (registered in discovery)
   * @param endpoint    relative endpoint path (e.g., /api/health)
   * @return response body from the target service
   */
  @Operation(summary = "Call another HTTP service via SDK",
      description = "Perform a GET request to another service using the SDK's load-balanced RestClient.")
  @ApiResponse(responseCode = "200", description = "Call succeeded",
      content = @Content(schema = @Schema(implementation = Map.class)))
  @ApiResponse(responseCode = "400", description = "Service call failed")
  @PostMapping("/call-http-service")
  public ResponseEntity<Map<String, Object>> callHttpService(
      @Parameter(description = "Service name to call", required = true, example = "user-service")
      @RequestParam String serviceName,
      @Parameter(description = "Endpoint path to call within the service", required = true, example = "/api/health")
      @RequestParam String endpoint) {

    try {
      // Use RestClient to make the call
      RestClient restClient = client.http();
      ResponseEntity<String> response = restClient.get()
          .uri("http://" + serviceName + endpoint)
          .retrieve()
          .toEntity(String.class);
      
      // Parse JSON response manually or use ObjectMapper
      Map<String, Object> body = Map.of("rawResponse", response.getBody());
      
      return ResponseEntity.ok(Map.of("status", "ok", "response", body));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of(
          "status", "error", 
          "message", "Failed to call HTTP service: " + e.getMessage()));
    }
  }
}
