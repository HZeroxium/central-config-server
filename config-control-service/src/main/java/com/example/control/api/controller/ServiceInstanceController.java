package com.example.control.api.controller;

import com.example.control.api.dto.common.ApiResponseDto;
import com.example.control.api.dto.common.PageDtos.PageResponse;
import com.example.control.api.dto.domain.ServiceInstanceDtos;
import com.example.control.api.mapper.domain.ServiceInstanceApiMapper;
import com.example.control.application.service.ServiceInstanceService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ServiceInstance;
import com.example.control.domain.criteria.ServiceInstanceCriteria;
import com.example.control.domain.id.ServiceInstanceId;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
  @Operation(summary = "Get by id")
  public ResponseEntity<ApiResponseDto.ApiResponse<ServiceInstanceDtos.Response>> findById(
      @PathVariable String serviceName,
      @PathVariable String instanceId,
      @AuthenticationPrincipal Jwt jwt) {
    UserContext userContext = UserContext.fromJwt(jwt);
    Optional<ServiceInstance> opt = service.findById(ServiceInstanceId.of(serviceName, instanceId), userContext);
    return opt.map(si -> ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
            ServiceInstanceApiMapper.toResponse(si))))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{serviceName}/{instanceId}")
  @Operation(summary = "Update instance")
  public ResponseEntity<ApiResponseDto.ApiResponse<ServiceInstanceDtos.Response>> update(
      @PathVariable String serviceName,
      @PathVariable String instanceId,
      @Valid @RequestBody ServiceInstanceDtos.UpdateRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    UserContext userContext = UserContext.fromJwt(jwt);
    ServiceInstanceId id = ServiceInstanceId.of(serviceName, instanceId);
    ServiceInstance updates = ServiceInstance.builder().id(id).build();
    ServiceInstanceApiMapper.apply(updates, request);
    ServiceInstance saved = service.update(id, updates, userContext);
    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        ServiceInstanceApiMapper.toResponse(saved)));
  }

  @DeleteMapping("/{serviceName}/{instanceId}")
  @Operation(summary = "Delete instance")
  public ResponseEntity<ApiResponseDto.ApiResponse<Void>> delete(
      @PathVariable String serviceName,
      @PathVariable String instanceId,
      @AuthenticationPrincipal Jwt jwt) {
    UserContext userContext = UserContext.fromJwt(jwt);
    service.delete(ServiceInstanceId.of(serviceName, instanceId), userContext);
    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success("Deleted", null));
  }

  @GetMapping
  @Operation(summary = "List instances with filters and pagination")
  @Timed("api.service-instances.list")
  public ResponseEntity<ApiResponseDto.ApiResponse<PageResponse<ServiceInstanceDtos.Response>>> findAll(
      @Parameter(description = "Filter by service name") @RequestParam(required = false) String serviceName,
      @Parameter(description = "Filter by instance ID") @RequestParam(required = false) String instanceId,
      @Parameter(description = "Filter by status") @RequestParam(required = false) ServiceInstance.InstanceStatus status,
      @Parameter(description = "Filter by drift status") @RequestParam(required = false) Boolean hasDrift,
      @Parameter(description = "Filter by environment") @RequestParam(required = false) String environment,
      @Parameter(description = "Filter by version") @RequestParam(required = false) String version,
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
    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Instances", PageResponse.from(mapped)));
  }
}


