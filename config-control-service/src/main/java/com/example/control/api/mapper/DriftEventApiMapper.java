package com.example.control.api.mapper;

import com.example.control.api.dto.DriftEventDtos;
import com.example.control.config.security.UserContext;
import com.example.control.domain.DriftEvent;
import com.example.control.domain.criteria.DriftEventCriteria;

import java.time.Instant;

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
        .detectedAt(Instant.now())
        .build();
  }

  public static void apply(DriftEvent entity, DriftEventDtos.UpdateRequest req) {
    if (req.getStatus() != null) {
      entity.setStatus(req.getStatus());
      if (req.getStatus() == DriftEvent.DriftStatus.RESOLVED) {
        entity.setResolvedAt(Instant.now());
      }
    }
    if (req.getResolvedBy() != null) entity.setResolvedBy(req.getResolvedBy());
    if (req.getNotes() != null) entity.setNotes(req.getNotes());
  }

  public static DriftEventDtos.Response toResponse(DriftEvent ev) {
    return DriftEventDtos.Response.builder()
        .id(ev.getId().id())
        .serviceName(ev.getServiceName())
        .instanceId(ev.getInstanceId())
        .expectedHash(ev.getExpectedHash())
        .appliedHash(ev.getAppliedHash())
        .severity(ev.getSeverity())
        .status(ev.getStatus())
        .detectedAt(Instant.now())
        .resolvedAt(ev.getResolvedAt())
        .detectedBy(ev.getDetectedBy())
        .resolvedBy(ev.getResolvedBy())
        .notes(ev.getNotes())
        .build();
  }

  /**
   * Map QueryFilter to domain criteria with team filtering.
   *
   * @param filter the query filter
   * @param userContext the user context for team filtering
   * @return the domain criteria
   */
  public static DriftEventCriteria toCriteria(DriftEventDtos.QueryFilter filter, UserContext userContext) {
    return DriftEventCriteria.builder()
        .serviceName(filter != null ? filter.getServiceName() : null)
        .instanceId(filter != null ? filter.getInstanceId() : null)
        .severity(filter != null ? filter.getSeverity() : null)
        .status(filter != null ? filter.getStatus() : null)
        .detectedAtFrom(filter != null ? filter.getDetectedAtFrom() : null)
        .detectedAtTo(filter != null ? filter.getDetectedAtTo() : null)
        .userTeamIds(userContext.getTeamIds())
        .build();
  }
}


