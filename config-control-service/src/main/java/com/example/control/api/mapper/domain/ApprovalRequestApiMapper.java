package com.example.control.api.mapper.domain;

import com.example.control.api.dto.common.PageDtos;
import com.example.control.api.dto.domain.ApprovalRequestDtos;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.object.ApprovalRequest;
import com.example.control.domain.criteria.ApprovalRequestCriteria;
import org.springframework.data.domain.Page;

import java.util.List;


/**
 * Mapper for ApprovalRequest API operations.
 * <p>
 * Provides mapping between domain objects and API DTOs with
 * proper validation and team-based filtering.
 * </p>
 */
public final class ApprovalRequestApiMapper {

    private ApprovalRequestApiMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Map domain entity to Response DTO.
     *
     * @param entity the domain entity
     * @return the response DTO
     */
    public static ApprovalRequestDtos.Response toResponse(ApprovalRequest entity) {
        return new ApprovalRequestDtos.Response(
                entity.getId().id(),
                entity.getRequesterUserId(),
                entity.getRequestType().name(),
                new ApprovalRequestDtos.ApprovalTarget(
                        entity.getTarget().getServiceId(),
                        entity.getTarget().getTeamId()
                ),
                entity.getRequired().stream()
                        .map(gate -> new ApprovalRequestDtos.ApprovalGate(
                                gate.getGate(),
                                gate.getMinApprovals()
                        ))
                        .toList(),
                entity.getStatus().name(),
                new ApprovalRequestDtos.RequesterSnapshot(
                        entity.getSnapshot().getTeamIds(),
                        entity.getSnapshot().getManagerId(),
                        entity.getSnapshot().getRoles()
                ),
                entity.getCounts(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getNote(),
                entity.getCancelReason()
        );
    }

    /**
     * Map QueryFilter to domain criteria with team filtering.
     *
     * @param filter      the query filter
     * @param userContext the user context for team filtering
     * @return the domain criteria
     */
    public static ApprovalRequestCriteria toCriteria(ApprovalRequestDtos.QueryFilter filter, UserContext userContext) {
        return ApprovalRequestCriteria.builder()
                .requesterUserId(filter != null ? filter.requesterUserId() : null)
                .status(filter != null && filter.status() != null ?
                        ApprovalRequest.ApprovalStatus.valueOf(filter.status()) : null)
                .requestType(filter != null && filter.requestType() != null ?
                        ApprovalRequest.RequestType.valueOf(filter.requestType()) : null)
                .fromDate(filter != null ? filter.fromDate() : null)
                .toDate(filter != null ? filter.toDate() : null)
                .userTeamIds(userContext.getTeamIds())
                .build();
    }

    /**
     * Convert Page<ApprovalRequest> to ApprovalRequestPageResponse.
     *
     * @param page the Spring Page containing ApprovalRequest entities
     * @return the domain-specific page response
     */
    public static ApprovalRequestDtos.ApprovalRequestPageResponse toPageResponse(Page<ApprovalRequest> page) {
        List<ApprovalRequestDtos.Response> items = page.getContent().stream()
                .map(ApprovalRequestApiMapper::toResponse)
                .toList();

        return ApprovalRequestDtos.ApprovalRequestPageResponse.builder()
                .items(items)
                .metadata(PageDtos.PageMetadata.from(page))
                .build();
    }
}