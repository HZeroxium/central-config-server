package com.example.control.api.mapper;

import com.example.control.api.dto.ApprovalRequestDtos;
import com.example.control.domain.ApprovalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between ApprovalRequest domain objects and API DTOs.
 * <p>
 * This mapper provides static methods for converting between the domain model
 * and the API layer, ensuring clean separation of concerns.
 * </p>
 */
public class ApprovalRequestApiMapper {

    /**
     * Convert CreateRequest DTO to domain object.
     *
     * @param request the create request DTO
     * @param userContext the current user context
     * @return the domain object
     */
    public static ApprovalRequest toDomain(ApprovalRequestDtos.CreateRequest request, 
                                         com.example.control.config.security.UserContext userContext) {
        return ApprovalRequest.builder()
                .requestType(ApprovalRequest.RequestType.SERVICE_OWNERSHIP_TRANSFER)
                .requesterUserId(userContext.getUserId())
                .target(ApprovalRequest.ApprovalTarget.builder()
                        .serviceId(request.serviceId())
                        .teamId(request.targetTeamId())
                        .build())
                .status(ApprovalRequest.ApprovalStatus.PENDING)
                .gates(List.of(
                        ApprovalRequest.ApprovalGate.builder()
                                .gate("SYS_ADMIN")
                                .status(ApprovalRequest.ApprovalGate.GateStatus.PENDING)
                                .build()
                ))
                .note(request.note())
                .build();
    }

    /**
     * Convert domain object to Response DTO.
     *
     * @param approvalRequest the domain object
     * @return the response DTO
     */
    public static ApprovalRequestDtos.Response toResponse(ApprovalRequest approvalRequest) {
        return new ApprovalRequestDtos.Response(
                approvalRequest.getId(),
                approvalRequest.getRequestType(),
                approvalRequest.getRequesterUserId(),
                approvalRequest.getTarget(),
                approvalRequest.getStatus(),
                approvalRequest.getGates(),
                approvalRequest.getNote(),
                approvalRequest.getCancelReason(),
                approvalRequest.getCreatedAt(),
                approvalRequest.getUpdatedAt(),
                approvalRequest.getVersion()
        );
    }

    /**
     * Convert list of domain objects to Response DTOs.
     *
     * @param requests the list of domain objects
     * @return the list of response DTOs
     */
    public static List<ApprovalRequestDtos.Response> toResponseList(List<ApprovalRequest> requests) {
        return requests.stream()
                .map(ApprovalRequestApiMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert paginated domain objects to ListResponse DTO.
     *
     * @param page the paginated domain objects
     * @return the list response DTO
     */
    public static ApprovalRequestDtos.ListResponse toListResponse(Page<ApprovalRequest> page) {
        List<ApprovalRequestDtos.Response> content = toResponseList(page.getContent());
        
        return new ApprovalRequestDtos.ListResponse(
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
    public static com.example.control.domain.port.ApprovalRequestRepositoryPort.ApprovalRequestFilter toFilter(
            ApprovalRequestDtos.ListRequest request, List<String> userTeamIds) {
        return new com.example.control.domain.port.ApprovalRequestRepositoryPort.ApprovalRequestFilter(
                request.requesterUserId(),
                request.status(),
                request.requestType(),
                request.fromDate(),
                request.toDate(),
                request.gate(),
                userTeamIds
        );
    }

    /**
     * Convert ListRequest DTO to Pageable.
     *
     * @param request the list request DTO
     * @return the pageable object
     */
    public static Pageable toPageable(ApprovalRequestDtos.ListRequest request) {
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;
        String sort = request.sort() != null ? request.sort() : "createdAt,desc";
        
        return org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(sort.split(",")));
    }
}
