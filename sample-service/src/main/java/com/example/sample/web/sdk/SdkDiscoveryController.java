package com.example.sample.web.sdk;

import com.vng.zing.zcm.client.ClientApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for service discovery operations.
 * <p>
 * Provides endpoints to discover and inspect service instances from the discovery provider.
 */
@RestController
@RequestMapping("/api/sdk/discovery")
@RequiredArgsConstructor
@Tag(name = "SDK Service Discovery", description = "Endpoints for service discovery and instance inspection")
public class SdkDiscoveryController {

  private final ClientApi client;

  /**
   * Discovers all instances of a given service via Spring Discovery Client.
   *
   * @param serviceName target service to discover
   * @return details of discovered instances
   */
  @Operation(
      summary = "Discover service instances",
      description = "Lists all available service instances for a given service name from the discovery provider")
  @Parameter(name = "serviceName", description = "The name of the service to discover", required = true)
  @ApiResponse(responseCode = "200", description = "Service instances retrieved successfully")
  @GetMapping("/{serviceName}")
  public ResponseEntity<Map<String, Object>> discoverService(@PathVariable String serviceName) {
    List<ServiceInstance> instances = client.loadBalancer().instances(serviceName);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "ok");
    result.put("serviceName", serviceName);
    result.put("instanceCount", instances.size());
    result.put("instances", instances.stream().map(i -> Map.of(
        "instanceId", i.getInstanceId(),
        "host", i.getHost(),
        "port", i.getPort(),
        "uri", i.getUri().toString(),
        "metadata", i.getMetadata()
    )).toList());
    return ResponseEntity.ok(result);
  }
}

