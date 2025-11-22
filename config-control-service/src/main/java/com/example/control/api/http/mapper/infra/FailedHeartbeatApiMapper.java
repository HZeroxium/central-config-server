package com.example.control.api.http.mapper.infra;

import com.example.control.api.http.dto.common.PageDtos;
import com.example.control.api.http.dto.infra.FailedHeartbeatDtos;
import com.example.control.domain.model.FailedHeartbeat;
import com.example.control.domain.criteria.FailedHeartbeatCriteria;
import com.example.control.infrastructure.config.security.UserContext;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Mapper utility for converting between FailedHeartbeat domain objects and API DTOs.
 */
public final class FailedHeartbeatApiMapper {

    private FailedHeartbeatApiMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convert FailedHeartbeat domain model to Response DTO.
     *
     * @param failedHeartbeat the domain model
     * @return the response DTO
     */
    public static FailedHeartbeatDtos.Response toResponse(FailedHeartbeat failedHeartbeat) {
        return FailedHeartbeatDtos.Response.builder()
                .id(failedHeartbeat.getId() != null ? failedHeartbeat.getId().id() : null)
                .serviceName(failedHeartbeat.getServiceName())
                .instanceId(failedHeartbeat.getInstanceId())
                .serviceId(failedHeartbeat.getServiceId())
                .teamId(failedHeartbeat.getTeamId())
                .environment(failedHeartbeat.getEnvironment())
                .payload(failedHeartbeat.getPayload())
                .originalTopic(failedHeartbeat.getOriginalTopic())
                .originalPartition(failedHeartbeat.getOriginalPartition())
                .originalOffset(failedHeartbeat.getOriginalOffset())
                .exceptionMessage(failedHeartbeat.getExceptionMessage())
                .exceptionClass(failedHeartbeat.getExceptionClass())
                .retryCount(failedHeartbeat.getRetryCount())
                .status(failedHeartbeat.getStatus())
                .firstSeenAt(failedHeartbeat.getFirstSeenAt())
                .lastSeenAt(failedHeartbeat.getLastSeenAt())
                .resolvedAt(failedHeartbeat.getResolvedAt())
                .resolvedBy(failedHeartbeat.getResolvedBy())
                .notes(failedHeartbeat.getNotes())
                .build();
    }

    /**
     * Map QueryFilter to domain criteria with team filtering.
     *
     * @param filter      the query filter
     * @param userContext the user context for team filtering
     * @return the domain criteria
     */
    public static FailedHeartbeatCriteria toCriteria(FailedHeartbeatDtos.QueryFilter filter, UserContext userContext) {
        FailedHeartbeatCriteria.FailedHeartbeatCriteriaBuilder builder = FailedHeartbeatCriteria.builder();

        if (filter != null) {
            builder.serviceName(filter.getServiceName())
                    .instanceId(filter.getInstanceId())
                    .status(filter.getStatus())
                    .teamId(filter.getTeamId())
                    .firstSeenAtFrom(filter.getFirstSeenAtFrom())
                    .firstSeenAtTo(filter.getFirstSeenAtTo());
        }

        // System admins can see all failed heartbeats (no team filtering)
        if (userContext.isSysAdmin()) {
            return builder.build();
        }

        // Regular users: filter by their teams
        return builder.userTeamIds(userContext.getTeamIds()).build();
    }

    /**
     * Convert Page<FailedHeartbeat> to FailedHeartbeatPageResponse.
     *
     * @param page the Spring Page containing FailedHeartbeat entities
     * @return the domain-specific page response
     */
    public static FailedHeartbeatDtos.FailedHeartbeatPageResponse toPageResponse(Page<FailedHeartbeat> page) {
        List<FailedHeartbeatDtos.Response> items = page.getContent().stream()
                .map(FailedHeartbeatApiMapper::toResponse)
                .toList();

        return FailedHeartbeatDtos.FailedHeartbeatPageResponse.builder()
                .items(items)
                .metadata(PageDtos.PageMetadata.from(page))
                .build();
    }
}

