package com.example.control.api;

import com.example.control.api.dto.ApiResponseDto;
import com.example.control.api.dto.ConsulDto;
import com.example.control.api.mapper.ConsulMapper;
import com.example.control.application.ConsulClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
