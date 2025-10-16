package com.example.control.api.mapper;

import com.example.control.api.dto.DriftEventDtos;
import com.example.control.domain.DriftEvent;

public final class DriftEventApiMapper {

  private DriftEventApiMapper() {}

  public static DriftEvent toDomain(DriftEventDtos.CreateRequest req) {
    return DriftEvent.builder()
        .serviceName(req.getServiceName())
        .instanceId(req.getInstanceId())
        .expectedHash(req.getExpectedHash())
        .appliedHash(req.getAppliedHash())
        .severity(req.getSeverity())
        .status(req.getStatus())
        .detectedBy(req.getDetectedBy())
        .notes(req.getNotes())
        .detectedAt(java.time.LocalDateTime.now())
        .build();
  }

  public static void apply(DriftEvent entity, DriftEventDtos.UpdateRequest req) {
    if (req.getStatus() != null) {
      entity.setStatus(req.getStatus());
      if (req.getStatus() == DriftEvent.DriftStatus.RESOLVED) {
        entity.setResolvedAt(java.time.LocalDateTime.now());
      }
    }
    if (req.getResolvedBy() != null) entity.setResolvedBy(req.getResolvedBy());
    if (req.getNotes() != null) entity.setNotes(req.getNotes());
  }

  public static DriftEventDtos.Response toResponse(DriftEvent ev) {
    return DriftEventDtos.Response.builder()
        .id(ev.getId())
        .serviceName(ev.getServiceName())
        .instanceId(ev.getInstanceId())
        .expectedHash(ev.getExpectedHash())
        .appliedHash(ev.getAppliedHash())
        .severity(ev.getSeverity())
        .status(ev.getStatus())
        .detectedAt(ev.getDetectedAt())
        .resolvedAt(ev.getResolvedAt())
        .detectedBy(ev.getDetectedBy())
        .resolvedBy(ev.getResolvedBy())
        .notes(ev.getNotes())
        .build();
  }
}


