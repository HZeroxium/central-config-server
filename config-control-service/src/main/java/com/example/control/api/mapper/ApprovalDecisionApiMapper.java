package com.example.control.api.mapper;

import com.example.control.api.dto.ApprovalDecisionDtos;
import com.example.control.domain.ApprovalDecision;
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
                entity.getNote()
        );
    }
}