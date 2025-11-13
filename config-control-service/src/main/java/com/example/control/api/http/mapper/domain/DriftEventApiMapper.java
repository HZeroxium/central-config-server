package com.example.control.api.http.mapper.domain;

import com.example.control.api.http.dto.common.PageDtos;
import com.example.control.api.http.dto.domain.DriftEventDtos;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.model.DriftEvent;
import com.example.control.domain.criteria.DriftEventCriteria;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

public final class DriftEventApiMapper {

    private DriftEventApiMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

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
        if (req.getResolvedBy() != null)
            entity.setResolvedBy(req.getResolvedBy());
        if (req.getNotes() != null)
            entity.setNotes(req.getNotes());
    }

    public static DriftEventDtos.Response toResponse(DriftEvent ev) {
        return DriftEventDtos.Response.builder()
                .id(ev.getId().id())
                .serviceName(ev.getServiceName())
                .instanceId(ev.getInstanceId())
                .expectedHash(ev.getExpectedHash())
                .appliedHash(ev.getAppliedHash())
                .environment(ev.getEnvironment())
                .severity(ev.getSeverity())
                .status(ev.getStatus())
                .detectedAt(ev.getDetectedAt())
                .resolvedAt(ev.getResolvedAt())
                .detectedBy(ev.getDetectedBy())
                .resolvedBy(ev.getResolvedBy())
                .notes(ev.getNotes())
                .build();
    }

    /**
     * Map QueryFilter to domain criteria with team filtering.
     *
     * @param filter      the query filter
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

    /**
     * Convert Page<DriftEvent> to DriftEventPageResponse.
     *
     * @param page the Spring Page containing DriftEvent entities
     * @return the domain-specific page response
     */
    public static DriftEventDtos.DriftEventPageResponse toPageResponse(Page<DriftEvent> page) {
        List<DriftEventDtos.Response> items = page.getContent().stream()
                .map(DriftEventApiMapper::toResponse)
                .toList();

        return DriftEventDtos.DriftEventPageResponse.builder()
                .items(items)
                .metadata(PageDtos.PageMetadata.from(page))
                .build();
    }
}
