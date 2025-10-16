package com.example.control.api;

import com.example.control.api.dto.ApiResponseDto;
import com.example.control.api.dto.PageDtos.PageResponse;
import com.example.control.api.dto.ServiceInstanceDtos;
import com.example.control.api.mapper.ServiceInstanceApiMapper;
import com.example.control.application.service.ServiceInstanceService;
import com.example.control.domain.ServiceInstance;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Optional;

@RestController
@RequestMapping("/api/service-instances")
@RequiredArgsConstructor
@Tag(name = "Service Instances", description = "CRUD and query for service instances")
public class ServiceInstanceController {

  private final ServiceInstanceService service;

  @PostMapping
  @Operation(summary = "Create instance")
  public ResponseEntity<ApiResponseDto.ApiResponse<ServiceInstanceDtos.Response>> create(
      @Valid @RequestBody ServiceInstanceDtos.CreateRequest request) {
    ServiceInstance toSave = ServiceInstanceApiMapper.toDomain(request);
    ServiceInstance saved = service.saveOrUpdate(toSave);
    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        ServiceInstanceApiMapper.toResponse(saved)));
  }

  @GetMapping("/{serviceName}/{instanceId}")
  @Operation(summary = "Get by id")
  public ResponseEntity<ApiResponseDto.ApiResponse<ServiceInstanceDtos.Response>> get(
      @PathVariable String serviceName,
      @PathVariable String instanceId) {
    Optional<ServiceInstance> opt = service.findByServiceAndInstance(serviceName, instanceId);
    return opt.map(si -> ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
            ServiceInstanceApiMapper.toResponse(si))))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{serviceName}/{instanceId}")
  @Operation(summary = "Update instance")
  public ResponseEntity<ApiResponseDto.ApiResponse<ServiceInstanceDtos.Response>> update(
      @PathVariable String serviceName,
      @PathVariable String instanceId,
      @Valid @RequestBody ServiceInstanceDtos.UpdateRequest request) {
    ServiceInstance instance = service.findByServiceAndInstance(serviceName, instanceId)
        .orElse(ServiceInstance.builder().serviceName(serviceName).instanceId(instanceId).build());
    ServiceInstanceApiMapper.apply(instance, request);
    ServiceInstance saved = service.saveOrUpdate(instance);
    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        ServiceInstanceApiMapper.toResponse(saved)));
  }

  @DeleteMapping("/{serviceName}/{instanceId}")
  @Operation(summary = "Delete instance")
  public ResponseEntity<ApiResponseDto.ApiResponse<Void>> delete(
      @PathVariable String serviceName,
      @PathVariable String instanceId) {
    // Delegate delete through service for hexagonal boundaries
    service.delete(serviceName, instanceId);
    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success("Deleted", null));
  }

  @GetMapping
  @Operation(summary = "List instances with filters and pagination")
  @Timed("api.service-instances.list")
  public ResponseEntity<ApiResponseDto.ApiResponse<PageResponse<ServiceInstanceDtos.Response>>> list(
      @RequestParam(required = false) String serviceName,
      @RequestParam(required = false) String instanceId,
      @RequestParam(required = false) ServiceInstance.InstanceStatus status,
      @RequestParam(required = false) Boolean hasDrift,
      @RequestParam(required = false) String environment,
      @RequestParam(required = false) String version,
      @PageableDefault Pageable pageable) {

    com.example.control.domain.port.ServiceInstanceRepositoryPort.ServiceInstanceFilter filter = new com.example.control.domain.port.ServiceInstanceRepositoryPort.ServiceInstanceFilter(
        serviceName, instanceId, status, hasDrift, environment, version, null, null);
    Page<ServiceInstance> page = service.list(filter, pageable);
    Page<ServiceInstanceDtos.Response> mapped = page.map(ServiceInstanceApiMapper::toResponse);
    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Instances", PageResponse.from(mapped)));
  }
}


