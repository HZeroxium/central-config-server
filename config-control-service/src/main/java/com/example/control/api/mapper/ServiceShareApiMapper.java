package com.example.control.api.mapper;

import com.example.control.api.dto.ServiceShareDtos;
import com.example.control.domain.ServiceShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between ServiceShare domain objects and API DTOs.
 * <pされます>
 * This mapper provides static methods for converting between the domain model
 * and the API layer, ensuring clean separation of concerns.
 * </p>
 */
public class ServiceShareApiMapper {

    /**
     * Convert GrantRequest DTO to domain object.
     *
     * @param request the grant request DTO
     * @param userContext the current user context
     * @return the domain object
     */
    public static ServiceShare toDomain(ServiceShareDtos.GrantRequest request, 
                                      com.example.control.config.security.UserContext userContext) {
        return ServiceShare.builder()
                .serviceId(request.serviceId())
                .grantToType(request.grantToType())
                .grantToId(request.grantToId())
                .permissions(request.permissions())
                .environments(request.environments())
                .expiresAt(request.expiresAt())
                .grantedBy(userContext.getUserId())
                .build();
    }

    /**
     * Apply UpdateRequest DTO to existing domain object.
     *
     * @param existing the existing domain object
     * @param request the update request DTO
     * @param userContext the current user context
     * @return the updated domain object
     */
    public static ServiceShare apply(ServiceShare existing, 
                                   ServiceShareDtos.UpdateRequest request,
                                   com.example.control.config.security.UserContext userContext) {
        return existing.toBuilder()
                .permissions(request.permissions() != null ? request.permissions() : existing.getPermissions())
                .environments(request.environments() != null ? request.environments() : existing.getEnvironments())
                .expiresAt(request.expiresAt())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Convert domain object to Response DTO.
     *
     * @param share the domain object
     * @return the response DTO
     */
    public static ServiceShareDtos.Response toResponse(ServiceShare share) {
        return new ServiceShareDtos.Response(
                share.getId(),
                share.getServiceId(),
                share.getGrantToType(),
                share.getGrantToId(),
                share.getPermissions(),
                share.getEnvironments(),
                share.getExpiresAt(),
                share.getCreatedAt(),
                share.getUpdatedAt(),
                share.getGrantedBy()
        );
    }

    /**
     * Convert list of domain objects to Response DTOs.
     *
     * @param shares the list of domain objects
     * @return the list of response DTOs
     */
    public static List<ServiceShareDtos.Response> toResponseList(List<ServiceShare> shares) {
        return shares.stream()
                .map(ServiceShareApiMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert paginated domain objects to ListResponse DTO.
     *
     * @param page the paginated domain objects
     * @return the list response DTO
     */
    public static ServiceShareDtos.ListResponse toListResponse(Page<ServiceShare> page) {
        List<ServiceShareDtos.Response> content = toResponseList(page.getContent());
        
        return new ServiceShareDtos.ListResponse(
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
    public static com.example.control.domain.port.ServiceShareRepositoryPort.ServiceShareFilter toFilter(
            ServiceShareDtos.ListRequest request, List<String> userTeamIds) {
        return new com.example.control.domain.port.ServiceShareRepositoryPort.ServiceShareFilter(
                request.serviceId(),
                request.grantToType(),
                request.grantToId(),
                request.environments(),
                request.grantedBy(),
                userTeamIds
        );
    }

    /**
     * Convert ListRequest DTO to Pageable.
     *
     * @param request the list request DTO
     * @return the pageable object
     */
    public static Pageable toPageable(ServiceShareDtos.ListRequest request) {
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;
        String sort = request.sort() != null ? request.sort() : "createdAt,desc";
        
        return org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(sort.split(",")));
    }
}
