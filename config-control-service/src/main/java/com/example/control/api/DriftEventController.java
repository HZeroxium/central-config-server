package com.example.control.api;

import com.example.control.api.dto.ApiResponseDto;
import com.example.control.api.dto.PageDtos.PageResponse;
import com.example.control.api.dto.DriftEventDtos;
import com.example.control.api.mapper.DriftEventApiMapper;
import com.example.control.application.service.DriftEventService;
import com.example.control.domain.DriftEvent;
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
@RequestMapping("/api/drift-events")
@RequiredArgsConstructor
@Tag(name = "Drift Events", description = "CRUD and query for drift events")
public class DriftEventController {

  private final DriftEventService service;

  @PostMapping
  @Operation(summary = "Create drift event")
  public ResponseEntity<ApiResponseDto.ApiResponse<DriftEventDtos.Response>> create(
      @Valid @RequestBody DriftEventDtos.CreateRequest request) {
    DriftEvent saved = service.save(DriftEventApiMapper.toDomain(request));
    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        DriftEventApiMapper.toResponse(saved)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get by id")
  public ResponseEntity<ApiResponseDto.ApiResponse<DriftEventDtos.Response>> get(@PathVariable String id) {
    Optional<DriftEvent> opt = service.findById(id);
    return opt.map(ev -> ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        DriftEventApiMapper.toResponse(ev))))
        .orElse(ResponseEntity.notFound().build());
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Update drift event (status/notes)")
  public ResponseEntity<ApiResponseDto.ApiResponse<DriftEventDtos.Response>> update(
      @PathVariable String id,
      @Valid @RequestBody DriftEventDtos.UpdateRequest request) {
    DriftEvent ev = service.findById(id).orElse(null);
    if (ev == null) return ResponseEntity.notFound().build();
    DriftEventApiMapper.apply(ev, request);
    DriftEvent saved = service.save(ev);
    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        DriftEventApiMapper.toResponse(saved)));
  }

  @GetMapping
  @Operation(summary = "List drift events with filters and pagination")
  @Timed("api.drift-events.list")
  public ResponseEntity<ApiResponseDto.ApiResponse<PageResponse<DriftEventDtos.Response>>> list(
      @RequestParam(required = false) String serviceName,
      @RequestParam(required = false) String instanceId,
      @RequestParam(required = false) DriftEvent.DriftStatus status,
      @RequestParam(required = false) DriftEvent.DriftSeverity severity,
      @RequestParam(required = false) Boolean unresolvedOnly,
      @PageableDefault Pageable pageable) {

    com.example.control.domain.port.DriftEventRepositoryPort.DriftEventFilter filter = new com.example.control.domain.port.DriftEventRepositoryPort.DriftEventFilter(
        serviceName, instanceId, status, severity, null, null, unresolvedOnly, null);
    Page<DriftEvent> page = service.list(filter, pageable);
    Page<DriftEventDtos.Response> mapped = page.map(DriftEventApiMapper::toResponse);
    return ResponseEntity.ok(ApiResponseDto.ApiResponse.success(
        "Drift events", PageResponse.from(mapped)));
  }
}


