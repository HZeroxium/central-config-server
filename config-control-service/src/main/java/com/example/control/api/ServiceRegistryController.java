package com.example.control.api;

import com.example.control.api.dto.ApiResponseDto;
import com.example.control.api.dto.ConsulDto;
import com.example.control.api.mapper.ConsulMapper;                                                                                                        
import com.example.control.application.ConsulClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Qualifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Consul service registry operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/registry")
@RequiredArgsConstructor
@Tag(name = "Service Registry", description = "Consul service discovery and registry operations")
public class ServiceRegistryController {

  @Qualifier("customConsulClient")
  private final ConsulClient consulClient;
  private final ObjectMapper objectMapper;

  @GetMapping("/services")
  @Operation(summary = "List all services", description = "Get list of all registered services from Consul")
  @Timed(value = "api.registry.services.list")
  public ResponseEntity<ApiResponseDto.ApiResponse<ConsulDto.ConsulServicesMap>> listServices() {
    log.debug("Listing all services from Consul");

    String response = consulClient.getServices();
    ConsulDto.ConsulServicesMap services = ConsulMapper.parseServicesResponse(response, objectMapper);

    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Services retrieved successfully", services));
  }

  @GetMapping("/services/{serviceName}")
  @Operation(summary = "Get service details", description = "Get detailed information about a specific service")
  @Timed(value = "api.registry.services.get")
  public ResponseEntity<ApiResponseDto.ApiResponse<List<ConsulDto.ConsulServiceResponse>>> getService(
      @PathVariable @Parameter(description = "Service name") String serviceName) {

    log.debug("Getting service details for: {}", serviceName);

    String response = consulClient.getService(serviceName);
    List<ConsulDto.ConsulServiceResponse> serviceDetails = ConsulMapper.parseServiceResponse(response, objectMapper);

    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Service details retrieved successfully", serviceDetails));
  }

  @GetMapping("/services/{serviceName}/instances")
  @Operation(summary = "Get service instances", description = "Get healthy instances of a service")
  @Timed(value = "api.registry.services.instances")
  public ResponseEntity<ApiResponseDto.ApiResponse<List<ApiResponseDto.ServiceInstanceSummary>>> getServiceInstances(
      @PathVariable @Parameter(description = "Service name") String serviceName,
      @RequestParam(defaultValue = "true") @Parameter(description = "Only passing instances") boolean passing) {

    log.debug("Getting instances for service: {}, passing only: {}", serviceName, passing);

    String response = passing
        ? consulClient.getHealthyServiceInstances(serviceName)
        : consulClient.getServiceInstances(serviceName);
    List<ApiResponseDto.ServiceInstanceSummary> summaries = ConsulMapper.toSummariesFromHealthJson(response, objectMapper);

    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Service instances retrieved successfully", summaries));
  }

  @GetMapping("/health/{serviceName}")
  @Operation(summary = "Get service health", description = "Get health status of a service")
  @Timed(value = "api.registry.health.get")
  public ResponseEntity<ApiResponseDto.ApiResponse<List<ConsulDto.ConsulHealthResponse>>> getServiceHealth(
      @PathVariable @Parameter(description = "Service name") String serviceName) {

    log.debug("Getting health status for service: {}", serviceName);

    String response = consulClient.getServiceHealth(serviceName);
    List<ConsulDto.ConsulHealthResponse> healthStatus = ConsulMapper.parseHealthResponse(response, objectMapper);

    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Service health retrieved successfully", healthStatus));
  }

  // @GetMapping("/nodes")
  // @Operation(summary = "List cluster nodes", description = "Get information about all nodes in the Consul cluster")
  // @Timed(value = "api.registry.nodes.list")
  // public ResponseEntity<ApiResponseDto.ApiResponse<List<ConsulDto.ConsulNodeInfo>>> listNodes() {
  //   log.debug("Listing all nodes in Consul cluster");

  //   String response = consulClient.getNodes();
  //   List<ConsulDto.ConsulNodeInfo> nodes = ConsulMapper.parseNodesResponse(response, objectMapper);

  //   return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
  //       "Nodes retrieved successfully", nodes));
  // }

  // @GetMapping("/kv/{key}")
  // @Operation(summary = "Get KV value", description = "Get value from Consul key-value store")
  // @Timed(value = "api.registry.kv.get")
  // public ResponseEntity<ApiResponseDto.ApiResponse<List<ConsulDto.ConsulKVResponse>>> getKVValue(
  //     @PathVariable @Parameter(description = "Key path") String key,
  //     @RequestParam(required = false) @Parameter(description = "Recursive listing") boolean recurse) {

  //   log.debug("Getting KV value for key: {}, recurse: {}", key, recurse);

  //   String response = consulClient.getKVValue(key, recurse);
  //   List<ConsulDto.ConsulKVResponse> kvResponse = ConsulMapper.parseKVResponse(response, objectMapper);

  //   return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
  //       "KV value retrieved successfully", kvResponse));
  // }

  // @PutMapping("/kv/{key}")
  // @Operation(summary = "Set KV value", description = "Set value in Consul key-value store")
  // @Timed(value = "api.registry.kv.set")
  // public ResponseEntity<ApiResponseDto.ApiResponse<Boolean>> setKVValue(
  //     @PathVariable @Parameter(description = "Key path") String key,
  //     @RequestBody @Parameter(description = "Value to set") String value) {

  //   log.debug("Setting KV value for key: {}", key);

  //   boolean success = consulClient.setKVValue(key, value);

  //   return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
  //       "KV value set successfully", success));
  // }

  // @DeleteMapping("/kv/{key}")
  // @Operation(summary = "Delete KV value", description = "Delete value from Consul key-value store")
  // @Timed(value = "api.registry.kv.delete")
  // public ResponseEntity<ApiResponseDto.ApiResponse<Boolean>> deleteKVValue(
  //     @PathVariable @Parameter(description = "Key path") String key) {

  //   log.debug("Deleting KV value for key: {}", key);

  //   boolean success = consulClient.deleteKVValue(key);

  //   return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
  //       "KV value deleted successfully", success));
  // }

  // @GetMapping("/agent/services")
  // @Operation(summary = "List agent services", description = "Get services registered on local Consul agent")
  // @Timed(value = "api.registry.agent.services")
  // public ResponseEntity<ApiResponseDto.ApiResponse<Map<String, ConsulDto.ConsulAgentService>>> getAgentServices() {
  //   log.debug("Getting agent services");

  //   String response = consulClient.getAgentServices();
  //   Map<String, ConsulDto.ConsulAgentService> services = ConsulMapper.parseAgentServicesResponse(response, objectMapper);

  //   return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
  //       "Agent services retrieved successfully", services));
  // }

  // @GetMapping("/agent/members")
  // @Operation(summary = "List agent members", description = "Get cluster members from local agent")
  // @Timed(value = "api.registry.agent.members")
  // public ResponseEntity<ApiResponseDto.ApiResponse<List<ConsulDto.ConsulMemberInfo>>> getAgentMembers() {
  //   log.debug("Getting agent members");

  //   String response = consulClient.getAgentMembers();
  //   List<ConsulDto.ConsulMemberInfo> members = ConsulMapper.parseMembersResponse(response, objectMapper);

  //   return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
  //       "Agent members retrieved successfully", members));
  // }

}
