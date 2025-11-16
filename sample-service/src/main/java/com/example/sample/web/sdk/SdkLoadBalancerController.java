package com.example.sample.web.sdk;

import com.vng.zing.zcm.client.ClientApi;
import com.vng.zing.zcm.loadbalancer.LbRequest;
import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for load balancing operations.
 * <p>
 * Provides endpoints to:
 * <ul>
 *   <li>Choose service instances using different load balancing policies</li>
 *   <li>Test session affinity and consistent hashing</li>
 *   <li>Select instances with request context (userId, sessionId, etc.)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/sdk/loadbalancer")
@RequiredArgsConstructor
@Tag(name = "SDK Load Balancer", description = "Endpoints for load balancing and instance selection")
public class SdkLoadBalancerController {

  private final ClientApi client;

  /**
   * Chooses one instance using the default load balancing strategy.
   *
   * @param serviceName target service name
   * @return chosen instance details
   */
  @Operation(
      summary = "Choose service instance (default policy)",
      description = "Selects a service instance using the default load balancing policy (e.g., ROUND_ROBIN)")
  @Parameter(name = "serviceName", description = "Service name to choose from", required = true)
  @ApiResponse(responseCode = "200", description = "Instance chosen successfully")
  @ApiResponse(responseCode = "404", description = "No instances available")
  @GetMapping("/choose/{serviceName}")
  public ResponseEntity<Map<String, Object>> chooseInstance(@PathVariable String serviceName) {
    ServiceInstance chosen = client.loadBalancer().choose(serviceName);
    if (chosen == null) {
      return ResponseEntity.notFound().build();
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "ok");
    result.put("serviceName", serviceName);
    result.put("strategy", client.loadBalancer().strategy());
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
   * @param policy      the policy string (e.g., ROUND_ROBIN, RANDOM, WEIGHTED_RANDOM, RENDEZVOUS, CONSISTENT_HASHING)
   * @return selected instance or error if invalid policy
   */
  @Operation(
      summary = "Choose service instance (custom policy)",
      description = "Selects an instance based on a specific load balancer policy")
  @Parameter(name = "serviceName", description = "Target service name", required = true, example = "sample-service")
  @Parameter(name = "policy", description = "Load balancing policy", required = true, example = "ROUND_ROBIN")
  @ApiResponse(responseCode = "200", description = "Instance chosen successfully")
  @ApiResponse(responseCode = "400", description = "Invalid policy")
  @ApiResponse(responseCode = "404", description = "No instances available")
  @GetMapping("/choose/{serviceName}/{policy}")
  public ResponseEntity<Map<String, Object>> chooseInstanceWithPolicy(
      @PathVariable String serviceName,
      @PathVariable String policy) {
    try {
      LoadBalancerStrategy.Policy policyEnum = LoadBalancerStrategy.Policy.fromString(policy);
      ServiceInstance chosen = client.loadBalancer().choose(serviceName, policyEnum);
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
      error.put("validPolicies", new String[]{"ROUND_ROBIN", "RANDOM", "WEIGHTED_RANDOM", "RENDEZVOUS", "CONSISTENT_HASHING"});
      return ResponseEntity.badRequest().body(error);
    }
  }

  /**
   * Chooses one instance using specified policy with request context.
   *
   * @param serviceName the service name
   * @param policy      the load balancing policy
   * @param requestId   optional request ID
   * @param userId      optional user ID for session affinity
   * @param sessionId   optional session ID for session affinity
   * @param clientId    optional client ID
   * @return selected instance with request context details
   */
  @Operation(
      summary = "Choose instance with request context",
      description = "Selects a service instance using LbRequest for advanced load balancing scenarios like session affinity")
  @Parameter(name = "serviceName", description = "Service name to choose from", required = true)
  @Parameter(name = "policy", description = "Load balancing policy", required = true)
  @Parameter(name = "requestId", description = "Request ID for tracking", required = false)
  @Parameter(name = "userId", description = "User ID for session affinity", required = false)
  @Parameter(name = "sessionId", description = "Session ID for session affinity", required = false)
  @Parameter(name = "clientId", description = "Client ID", required = false)
  @ApiResponse(responseCode = "200", description = "Instance chosen successfully")
  @ApiResponse(responseCode = "400", description = "Invalid policy or request")
  @ApiResponse(responseCode = "404", description = "No instances available")
  @GetMapping("/choose-with-context/{serviceName}/{policy}")
  public ResponseEntity<Map<String, Object>> chooseWithContext(
      @PathVariable String serviceName,
      @PathVariable String policy,
      @RequestParam(required = false) String requestId,
      @RequestParam(required = false) String userId,
      @RequestParam(required = false) String sessionId,
      @RequestParam(required = false) String clientId) {
    
    try {
      LoadBalancerStrategy.Policy lbPolicy = LoadBalancerStrategy.Policy.fromString(policy);
      
      // Create LbRequest with provided context
      LbRequest.Builder builder = LbRequest.builder();
      if (requestId != null) builder.requestId(requestId);
      if (userId != null) builder.userId(userId);
      if (sessionId != null) builder.sessionId(sessionId);
      if (clientId != null) builder.clientId(clientId);
      
      LbRequest request = builder.build();
      ServiceInstance chosen = client.loadBalancer().choose(serviceName, lbPolicy, request);
      
      if (chosen == null) {
        return ResponseEntity.notFound().build();
      }
      
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("status", "ok");
      result.put("serviceName", serviceName);
      result.put("strategy", lbPolicy.getValue());
      result.put("requestContext", Map.of(
          "requestId", request.getRequestId(),
          "userId", request.getUserId() != null ? request.getUserId() : "null",
          "sessionId", request.getSessionId() != null ? request.getSessionId() : "null",
          "clientId", request.getClientId() != null ? request.getClientId() : "null",
          "hashKey", request.getHashKey(),
          "timestamp", request.getTimestamp()
      ));
      result.put("chosen", Map.of(
          "instanceId", chosen.getInstanceId(),
          "host", chosen.getHost(),
          "port", chosen.getPort(),
          "metadata", chosen.getMetadata(),
          "uri", chosen.getUri().toString()
      ));
      
      return ResponseEntity.ok(result);
      
    } catch (Exception e) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("status", "error");
      error.put("message", e.getMessage());
      return ResponseEntity.badRequest().body(error);
    }
  }

  /**
   * Tests session affinity by making multiple requests with the same session ID.
   *
   * @param serviceName the service name to test
   * @param sessionId   the session ID for affinity testing
   * @param count       number of requests to make (default: 5)
   * @return test results showing which instance was chosen for each request
   */
  @Operation(
      summary = "Test session affinity",
      description = "Demonstrates session affinity by making multiple requests with the same session ID")
  @Parameter(name = "serviceName", description = "Service name to test", required = true)
  @Parameter(name = "sessionId", description = "Session ID for affinity testing", required = true)
  @Parameter(name = "count", description = "Number of requests to make", required = false)
  @ApiResponse(responseCode = "200", description = "Session affinity test completed")
  @GetMapping("/test-session-affinity/{serviceName}/{sessionId}")
  public ResponseEntity<Map<String, Object>> testSessionAffinity(
      @PathVariable String serviceName,
      @PathVariable String sessionId,
      @RequestParam(defaultValue = "5") int count) {
    
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "ok");
    result.put("serviceName", serviceName);
    result.put("sessionId", sessionId);
    result.put("testCount", count);
    
    List<Map<String, Object>> results = new ArrayList<>();
    
    for (int i = 0; i < count; i++) {
      LbRequest request = LbRequest.builder()
          .requestId("req-" + i)
          .sessionId(sessionId)
          .build();
      
      ServiceInstance chosen = client.loadBalancer().choose(serviceName, LoadBalancerStrategy.Policy.RENDEZVOUS, request);
      
      if (chosen != null) {
        results.add(Map.of(
            "request", i,
            "instanceId", chosen.getInstanceId(),
            "host", chosen.getHost(),
            "port", chosen.getPort()
        ));
      }
    }
    
    result.put("results", results);
    
    // Check if all requests went to the same instance (session affinity working)
    String firstInstanceId = results.isEmpty() ? null : (String) results.get(0).get("instanceId");
    boolean sessionAffinityWorking = results.stream()
        .allMatch(r -> firstInstanceId != null && firstInstanceId.equals(r.get("instanceId")));
    
    result.put("sessionAffinityWorking", sessionAffinityWorking);
    
    return ResponseEntity.ok(result);
  }
}

