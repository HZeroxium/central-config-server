package com.example.control.api.mapper.domain;

import com.example.control.api.dto.domain.ApprovalDecisionDtos;
import com.example.control.domain.criteria.ApprovalDecisionCriteria;
import com.example.control.domain.object.ApprovalDecision;
import org.springframework.stereotype.Component;

/**
 * Mapper for ApprovalDecision API operations.
 * <p>
 * Provides mapping between domain objects and API DTOs with
 * proper JSON serialization.
 * </p>
 */
@Component
public class ApprovalDecisionApiMapper {

    /**
     * Map domain entity to Response DTO.
     *
     * @param entity the domain entity
     * @return the response DTO
     */
    public ApprovalDecisionDtos.Response toResponse(ApprovalDecision entity) {
        return new ApprovalDecisionDtos.Response(
                entity.getId().id(),
                entity.getRequestId().id(),
                entity.getApproverUserId(),
                entity.getGate(),
                entity.getDecision().name(),
                entity.getDecidedAt(),
                entity.getNote());
    }

    /**
     * Map QueryFilter to domain criteria.
     *
     * @param filter the query filter
     * @return the domain criteria
     */
    public ApprovalDecisionCriteria toCriteria(ApprovalDecisionDtos.QueryFilter filter) {
        return ApprovalDecisionCriteria.builder()
                .requestId(filter != null ? filter.requestId() : null)
                .approverUserId(filter != null ? filter.approverUserId() : null)
                .gate(filter != null ? filter.gate() : null)
                .decision(filter != null && filter.decision() != null
                        ? ApprovalDecision.Decision.valueOf(filter.decision())
                        : null)
                .build();
    }
}