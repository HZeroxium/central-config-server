package com.example.control.api.controller;

import com.example.control.api.dto.domain.ServiceInstanceDtos;
import com.example.control.api.mapper.domain.ServiceInstanceApiMapper;
import com.example.control.application.service.ServiceInstanceService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ServiceInstance;
import com.example.control.domain.criteria.ServiceInstanceCriteria;
import com.example.control.domain.id.ServiceInstanceId;
import com.example.control.api.exception.ErrorResponse;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/service-instances")
@RequiredArgsConstructor
@Tag(name = "Service Instances", description = "CRUD and query for service instances")
public class ServiceInstanceController {

  private final ServiceInstanceService service;

  // @PostMapping
  // @Operation(summary = "Create instance")
  // public ResponseEntity<ApiResponseDto.ApiResponse<ServiceInstanceDtos.Response>> create(
  //     @Valid @RequestBody ServiceInstanceDtos.CreateRequest request,
  //     @AuthenticationPrincipal Jwt jwt) {
  //   UserContext userContext = UserContext.fromJwt(jwt);
  //   ServiceInstance toSave = ServiceInstanceApiMapper.toDomain(request);
  //   ServiceInstance saved = service.create(toSave, userContext);
  //   return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
  //       ServiceInstanceApiMapper.toResponse(saved)));
  // }

  @GetMapping("/{serviceName}/{instanceId}")
  @Operation(
      summary = "Get service instance by ID",
      description = """
          Retrieve a specific service instance by service name and instance ID.
          
          **Access Control:**
          - Team members: Can view instances of services owned by their team
          - Shared access: Can view instances of services shared with their team
          - SYS_ADMIN: Can view all instances
          """,
      security = {
        @SecurityRequirement(name = "oauth2_auth_code"),
        @SecurityRequirement(name = "oauth2_password")
      },
      operationId = "findByIdServiceInstance"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Service instance found",
          content = @Content(schema = @Schema(implementation = ServiceInstanceDtos.Response.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Service instance not found",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ServiceInstanceDtos.Response> findById(
      @Parameter(description = "Service name", example = "payment-service")
      @PathVariable String serviceName,
      @Parameter(description = "Instance ID", example = "payment-dev-1")
      @PathVariable String instanceId,
      @AuthenticationPrincipal Jwt jwt) {
    UserContext userContext = UserContext.fromJwt(jwt);
    Optional<ServiceInstance> opt = service.findById(ServiceInstanceId.of(serviceName, instanceId), userContext);
    return opt.map(si -> ResponseEntity.ok(ServiceInstanceApiMapper.toResponse(si)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{serviceName}/{instanceId}")
  @Operation(
      summary = "Update service instance",
      description = """
          Update an existing service instance.
          
          **Required Permissions:**
          - Team members: Can update instances of services owned by their team
          - SYS_ADMIN: Can update any instance
          - Updates include configuration hash, status, and drift information
          """,
      security = {
        @SecurityRequirement(name = "oauth2_auth_code"),
        @SecurityRequirement(name = "oauth2_password")
      },
      operationId = "updateServiceInstance"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Service instance updated successfully",
          content = @Content(schema = @Schema(implementation = ServiceInstanceDtos.Response.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request data",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Service instance not found",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ServiceInstanceDtos.Response> update(
      @Parameter(description = "Service name", example = "payment-service")
      @PathVariable String serviceName,
      @Parameter(description = "Instance ID", example = "payment-dev-1")
      @PathVariable String instanceId,
      @Parameter(description = "Service instance update request", 
                schema = @Schema(implementation = ServiceInstanceDtos.UpdateRequest.class))
      @Valid @RequestBody ServiceInstanceDtos.UpdateRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    UserContext userContext = UserContext.fromJwt(jwt);
    ServiceInstanceId id = ServiceInstanceId.of(serviceName, instanceId);
    ServiceInstance updates = ServiceInstance.builder().id(id).build();
    ServiceInstanceApiMapper.apply(updates, request);
    ServiceInstance saved = service.update(id, updates, userContext);
    return ResponseEntity.ok(ServiceInstanceApiMapper.toResponse(saved));
  }

  @DeleteMapping("/{serviceName}/{instanceId}")
  @Operation(
      summary = "Delete service instance",
      description = """
          Delete a service instance permanently.
          
          **Required Permissions:**
          - Team members: Can delete instances of services owned by their team
          - SYS_ADMIN: Can delete any instance
          - This action is irreversible
          """,
      security = {
        @SecurityRequirement(name = "oauth2_auth_code"),
        @SecurityRequirement(name = "oauth2_password")
      },
      operationId = "deleteServiceInstance"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Service instance deleted successfully",
          content = @Content(schema = @Schema(implementation = Void.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Service instance not found",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<Void> delete(
      @Parameter(description = "Service name", example = "payment-service")
      @PathVariable String serviceName,
      @Parameter(description = "Instance ID", example = "payment-dev-1")
      @PathVariable String instanceId,
      @AuthenticationPrincipal Jwt jwt) {
    UserContext userContext = UserContext.fromJwt(jwt);
    service.delete(ServiceInstanceId.of(serviceName, instanceId), userContext);
    return ResponseEntity.ok().build();
  }

  @GetMapping
  @Operation(
      summary = "List service instances with filters and pagination",
      description = """
          Retrieve a paginated list of service instances with optional filtering.
          
          **Access Control:**
          - Team members: Can view instances of services owned by their team
          - Shared access: Can view instances of services shared with their team
          - SYS_ADMIN: Can view all instances
          - Results are automatically filtered based on user permissions
          """,
      security = {
        @SecurityRequirement(name = "oauth2_auth_code"),
        @SecurityRequirement(name = "oauth2_password")
      },
      operationId = "findAllServiceInstances"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Service instances retrieved successfully",
          content = @Content(schema = @Schema(implementation = Page.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request parameters",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @Timed("api.service-instances.list")
  public ResponseEntity<Page<ServiceInstanceDtos.Response>> findAll(
      @Parameter(description = "Filter by service name", example = "payment-service") 
      @RequestParam(required = false) String serviceName,
      @Parameter(description = "Filter by instance ID", example = "payment-dev-1") 
      @RequestParam(required = false) String instanceId,
      @Parameter(description = "Filter by instance status", example = "HEALTHY") 
      @RequestParam(required = false) ServiceInstance.InstanceStatus status,
      @Parameter(description = "Filter by drift status", example = "true") 
      @RequestParam(required = false) Boolean hasDrift,
      @Parameter(description = "Filter by environment", example = "dev") 
      @RequestParam(required = false) String environment,
      @Parameter(description = "Filter by service version", example = "1.2.0") 
      @RequestParam(required = false) String version,
      @Parameter(description = "Pagination parameters (page, size, sort)") 
      @PageableDefault Pageable pageable,
      @AuthenticationPrincipal Jwt jwt) {

    UserContext userContext = UserContext.fromJwt(jwt);
    
    ServiceInstanceDtos.QueryFilter queryFilter = ServiceInstanceDtos.QueryFilter.builder()
        .serviceName(serviceName)
        .instanceId(instanceId)
        .status(status)
        .hasDrift(hasDrift)
        .environment(environment)
        .version(version)
        .build();
    
    ServiceInstanceCriteria criteria = ServiceInstanceApiMapper.toCriteria(queryFilter, userContext);
    Page<ServiceInstance> page = service.findAll(criteria, pageable, userContext);
    Page<ServiceInstanceDtos.Response> mapped = page.map(ServiceInstanceApiMapper::toResponse);
    return ResponseEntity.ok(mapped);
  }
}


