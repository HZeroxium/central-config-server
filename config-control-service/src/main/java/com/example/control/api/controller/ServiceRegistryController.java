package com.example.control.api.controller;

import com.example.control.api.dto.common.ApiResponseDto;
import com.example.control.api.dto.consul.ConsulDto;
import com.example.control.api.mapper.consul.ConsulMapper;
import com.example.control.application.ConsulClient;
import com.example.control.api.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
  @Operation(
      summary = "List all registered services",
      description = """
          Retrieves a list of all services registered with Consul.
          This provides an overview of available services in the ecosystem.
          """,
      security = {
        @SecurityRequirement(name = "oauth2_auth_code"),
        @SecurityRequirement(name = "oauth2_password")
      },
      operationId = "listServiceRegistryServices"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved service list",
          content = @Content(schema = @Schema(implementation = ConsulDto.ConsulServicesMap.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error or Consul unreachable",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @Timed(value = "api.registry.services.list")
  public ResponseEntity<ConsulDto.ConsulServicesMap> listServiceRegistryServices() {
    log.debug("Listing all services from Consul");

    String response = consulClient.getServices();
    ConsulDto.ConsulServicesMap services = ConsulMapper.parseServicesResponse(response, objectMapper);

    return ResponseEntity.ok(services);
  }

  @GetMapping("/services/{serviceName}")
  @Operation(
      summary = "Get service details",
      description = """
          Retrieves detailed information about a specific service from Consul.
          This includes all registered instances and their metadata.
          """,
      security = {
        @SecurityRequirement(name = "oauth2_auth_code"),
        @SecurityRequirement(name = "oauth2_password")
      },
      operationId = "getServiceRegistryService"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Service details retrieved successfully",
          content = @Content(schema = @Schema(implementation = List.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Service not found",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error or Consul unreachable",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @Timed(value = "api.registry.services.get")
  public ResponseEntity<List<ConsulDto.ConsulServiceResponse>> getServiceRegistryService(
      @Parameter(description = "Name of the service", example = "payment-service")
      @PathVariable String serviceName) {

    log.debug("Getting service details for: {}", serviceName);

    String response = consulClient.getService(serviceName);
    List<ConsulDto.ConsulServiceResponse> serviceDetails = ConsulMapper.parseServiceResponse(response, objectMapper);

    return ResponseEntity.ok(serviceDetails);
  }

  @GetMapping("/services/{serviceName}/instances")
  @Operation(
      summary = "Get service instances",
      description = """
          Retrieves instances of a specific service from Consul.
          Can filter to show only healthy instances or all instances.
          """,
      security = {
        @SecurityRequirement(name = "oauth2_auth_code"),
        @SecurityRequirement(name = "oauth2_password")
      },
      operationId = "getServiceRegistryServiceInstances"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Service instances retrieved successfully",
          content = @Content(schema = @Schema(implementation = List.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Service not found",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error or Consul unreachable",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @Timed(value = "api.registry.services.instances")
  public ResponseEntity<List<ApiResponseDto.ServiceInstanceSummary>> getServiceRegistryServiceInstances(
      @Parameter(description = "Name of the service", example = "payment-service")
      @PathVariable String serviceName,
      @Parameter(description = "Filter to only healthy instances", example = "true")
      @RequestParam(defaultValue = "true") boolean passing) {

    log.debug("Getting instances for service: {}, passing only: {}", serviceName, passing);

    String response = passing
        ? consulClient.getHealthyServiceInstances(serviceName)
        : consulClient.getServiceInstances(serviceName);
    List<ApiResponseDto.ServiceInstanceSummary> summaries = ConsulMapper.toSummariesFromHealthJson(response, objectMapper);

    return ResponseEntity.ok(summaries);
  }

  @GetMapping("/health/{serviceName}")
  @Operation(
      summary = "Get service health status",
      description = """
          Retrieves the health status of a specific service from Consul.
          This includes health checks for all instances of the service.
          """,
      security = {
        @SecurityRequirement(name = "oauth2_auth_code"),
        @SecurityRequirement(name = "oauth2_password")
      },
      operationId = "getServiceRegistryServiceHealth"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Service health status retrieved successfully",
          content = @Content(schema = @Schema(implementation = List.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Service not found",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error or Consul unreachable",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @Timed(value = "api.registry.health.get")
  public ResponseEntity<List<ConsulDto.ConsulHealthResponse>> getServiceHealth(
      @Parameter(description = "Name of the service", example = "payment-service")
      @PathVariable String serviceName) {

    log.debug("Getting health status for service: {}", serviceName);

    String response = consulClient.getServiceHealth(serviceName);
    List<ConsulDto.ConsulHealthResponse> healthStatus = ConsulMapper.parseHealthResponse(response, objectMapper);

    return ResponseEntity.ok(healthStatus);
  }
}
