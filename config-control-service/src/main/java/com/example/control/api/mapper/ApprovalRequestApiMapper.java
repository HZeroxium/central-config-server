package com.example.control.api.mapper;

import com.example.control.api.dto.ApprovalRequestDtos;
import com.example.control.config.security.UserContext;
import com.example.control.domain.ApprovalRequest;
import com.example.control.domain.criteria.ApprovalRequestCriteria;


/**
 * Mapper for ApprovalRequest API operations.
 * <p>
 * Provides mapping between domain objects and API DTOs with
 * proper validation and team-based filtering.
 * </p>
 */
public final class ApprovalRequestApiMapper {

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
     * @param filter the query filter
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
}