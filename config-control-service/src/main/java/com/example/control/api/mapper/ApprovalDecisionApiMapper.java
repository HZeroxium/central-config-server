package com.example.control.api.mapper;

import com.example.control.api.dto.ApprovalDecisionDtos;
import com.example.control.domain.ApprovalDecision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between ApprovalDecision domain objects and API DTOs.
 * <p>
 * This mapper provides static methods for converting between the domain model
 * and the API layer, ensuring clean separation of concerns.
 * </p>
 */
public class ApprovalDecisionApiMapper {

    /**
     * Convert CreateRequest DTO to domain object.
     *
     * @param request the create request DTO
     * @param requestId the approval request ID
     * @param userContext the current user context
     * @return the domain object
     */
    public static ApprovalDecision toDomain(ApprovalDecisionDtos.CreateRequest request, 
                                          String requestId,
                                          com.example.control.config.security.UserContext userContext) {
        return ApprovalDecision.builder()
                .requestId(requestId)
                .approverUserId(userContext.getUserId())
                .decision(request.decision())
                .gate(request.gate())
                .note(request.note())
                .build();
    }

    /**
     * Convert domain object to Response DTO.
     *
     * @param decision the domain object
     * @return the response DTO
     */
    public static ApprovalDecisionDtos.Response toResponse(ApprovalDecision decision) {
        return new ApprovalDecisionDtos.Response(
                decision.getId(),
                decision.getRequestId(),
                decision.getApproverUserId(),
                decision.getDecision(),
                decision.getGate(),
                decision.getNote(),
                decision.getCreatedAt()
        );
    }

    /**
     * Convert list of domain objects to Response DTOs.
     *
     * @param decisions the list of domain objects
     * @return the list of response DTOs
     */
    public static List<ApprovalDecisionDtos.Response> toResponseList(List<ApprovalDecision> decisions) {
        return decisions.stream()
                .map(ApprovalDecisionApiMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert paginated domain objects to ListResponse DTO.
     *
     * @param page the paginated domain objects
     * @return the list response DTO
     */
    public static ApprovalDecisionDtos.ListResponse toListResponse(Page<ApprovalDecision> page) {
        List<ApprovalDecisionDtos.Response> content = toResponseList(page.getContent());
        
        return new ApprovalDecisionDtos.ListResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    /**
     * Convert ListRequest DTO to repository filter.
     *
     * @param request the list request DTO
     * @param userTeamIds the user's team IDs for access control
     * @return the repository filter
     */
    public static com.example.control.domain.port.ApprovalDecisionRepositoryPort.ApprovalDecisionFilter toFilter(
            ApprovalDecisionDtos.ListRequest request, List<String> userTeamIds) {
        return new com.example.control.domain.port.ApprovalDecisionRepositoryPort.ApprovalDecisionFilter(
                request.requestId(),
                request.approverUserId(),
                request.gate(),
                request.decision(),
                userTeamIds
        );
    }

    /**
     * Convert ListRequest DTO to Pageable.
     *
     * @param request the list request DTO
     * @return the pageable object
     */
    public static Pageable toPageable(ApprovalDecisionDtos.ListRequest request) {
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;
        String sort = request.sort() != null ? request.sort() : "createdAt,desc";
        
        return org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(sort.split(",")));
    }
}
