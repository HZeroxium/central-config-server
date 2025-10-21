package com.example.control.api.controller;

import com.example.control.api.dto.common.ApiResponseDto;
import com.example.control.api.dto.domain.DriftEventDtos;
import com.example.control.api.dto.common.PageDtos.PageResponse;
import com.example.control.api.mapper.domain.DriftEventApiMapper;
import com.example.control.application.service.DriftEventService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.object.DriftEvent;
import com.example.control.domain.criteria.DriftEventCriteria;
import com.example.control.domain.id.DriftEventId;
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
@RequestMapping("/api/drift-events")
@RequiredArgsConstructor
@Tag(name = "Drift Events", description = "CRUD and query for drift events")
public class DriftEventController {

  private final DriftEventService service;

  @PostMapping
  @Operation(summary = "Create drift event")
  public ResponseEntity<ApiResponseDto.ApiResponse<DriftEventDtos.Response>> create(
      @Valid @RequestBody DriftEventDtos.CreateRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    UserContext userContext = UserContext.fromJwt(jwt);
    DriftEvent saved = service.save(DriftEventApiMapper.toDomain(request), userContext);
    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        DriftEventApiMapper.toResponse(saved)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get by id")
  public ResponseEntity<ApiResponseDto.ApiResponse<DriftEventDtos.Response>> findById(
      @PathVariable String id,
      @AuthenticationPrincipal Jwt jwt) {
    UserContext userContext = UserContext.fromJwt(jwt);
    Optional<DriftEvent> opt = service.findById(DriftEventId.of(id), userContext);
    return opt.map(ev -> ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        DriftEventApiMapper.toResponse(ev))))
        .orElse(ResponseEntity.notFound().build());
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Update drift event (status/notes)")
  public ResponseEntity<ApiResponseDto.ApiResponse<DriftEventDtos.Response>> update(
      @PathVariable String id,
      @Valid @RequestBody DriftEventDtos.UpdateRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    UserContext userContext = UserContext.fromJwt(jwt);
    DriftEvent ev = service.findById(DriftEventId.of(id), userContext).orElse(null);
    if (ev == null) return ResponseEntity.notFound().build();
    DriftEventApiMapper.apply(ev, request);
    DriftEvent saved = service.save(ev);
    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        DriftEventApiMapper.toResponse(saved)));
  }

  @GetMapping
  @Operation(summary = "List drift events with filters and pagination")
  @Timed("api.drift-events.list")
  public ResponseEntity<ApiResponseDto.ApiResponse<PageResponse<DriftEventDtos.Response>>> findAll(
      @Parameter(description = "Filter by service name") @RequestParam(required = false) String serviceName,
      @Parameter(description = "Filter by instance ID") @RequestParam(required = false) String instanceId,
      @Parameter(description = "Filter by status") @RequestParam(required = false) DriftEvent.DriftStatus status,
      @Parameter(description = "Filter by severity") @RequestParam(required = false) DriftEvent.DriftSeverity severity,
      @Parameter(description = "Filter unresolved only") @RequestParam(required = false) Boolean unresolvedOnly,
      @PageableDefault Pageable pageable,
      @AuthenticationPrincipal Jwt jwt) {

    UserContext userContext = UserContext.fromJwt(jwt);
    
    DriftEventDtos.QueryFilter queryFilter = DriftEventDtos.QueryFilter.builder()
        .serviceName(serviceName)
        .instanceId(instanceId)
        .status(status)
        .severity(severity)
        .unresolvedOnly(unresolvedOnly)
        .build();
    
    DriftEventCriteria criteria = DriftEventApiMapper.toCriteria(queryFilter, userContext);
    Page<DriftEvent> page = service.findAll(criteria, pageable, userContext);
    Page<DriftEventDtos.Response> mapped = page.map(DriftEventApiMapper::toResponse);
    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Drift events", PageResponse.from(mapped)));
  }
}


