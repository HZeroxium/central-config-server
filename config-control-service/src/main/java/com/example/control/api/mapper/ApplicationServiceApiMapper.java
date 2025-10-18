package com.example.control.api.mapper;

import com.example.control.api.dto.ApplicationServiceDtos;
import com.example.control.domain.ApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for converting between ApplicationService domain objects and API DTOs.
 * <p>
 * This mapper provides static methods for converting between the domain model
 * and the API layer, ensuring clean separation of concerns.
 * </p>
 */
public class ApplicationServiceApiMapper {

    /**
     * Convert CreateRequest DTO to domain object.
     *
     * @param request the create request DTO
     * @param userContext the current user context
     * @return the domain object
     */
    public static ApplicationService toDomain(ApplicationServiceDtos.CreateRequest request, 
                                            com.example.control.config.security.UserContext userContext) {
        return ApplicationService.builder()
                .id(request.id())
                .displayName(request.displayName())
                .ownerTeamId(request.ownerTeamId())
                .environments(request.environments())
                .tags(request.tags() != null ? request.tags() : List.of())
                .repoUrl(request.repoUrl())
                .lifecycle(ApplicationService.ServiceLifecycle.ACTIVE)
                .createdBy(userContext.getUserId())
                .attributes(request.attributes() != null ? request.attributes() : Map.of())
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
    public static ApplicationService apply(ApplicationService existing, 
                                         ApplicationServiceDtos.UpdateRequest request,
                                         com.example.control.config.security.UserContext userContext) {
        return existing.toBuilder()
                .displayName(request.displayName())
                .lifecycle(request.lifecycle() != null ? request.lifecycle() : existing.getLifecycle())
                .tags(request.tags() != null ? request.tags() : existing.getTags())
                .repoUrl(request.repoUrl())
                .attributes(request.attributes() != null ? request.attributes() : existing.getAttributes())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Convert domain object to Response DTO.
     *
     * @param service the domain object
     * @return the response DTO
     */
    public static ApplicationServiceDtos.Response toResponse(ApplicationService service) {
        return new ApplicationServiceDtos.Response(
                service.getId(),
                service.getDisplayName(),
                service.getOwnerTeamId(),
                service.getEnvironments(),
                service.getTags(),
                service.getRepoUrl(),
                service.getLifecycle(),
                service.getCreatedAt(),
                service.getUpdatedAt(),
                service.getCreatedBy(),
                service.getAttributes()
        );
    }

    /**
     * Convert list of domain objects to Response DTOs.
     *
     * @param services the list of domain objects
     * @return the list of response DTOs
     */
    public static List<ApplicationServiceDtos.Response> toResponseList(List<ApplicationService> services) {
        return services.stream()
                .map(ApplicationServiceApiMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert paginated domain objects to ListResponse DTO.
     *
     * @param page the paginated domain objects
     * @return the list response DTO
     */
    public static ApplicationServiceDtos.ListResponse toListResponse(Page<ApplicationService> page) {
        List<ApplicationServiceDtos.Response> content = toResponseList(page.getContent());
        
        return new ApplicationServiceDtos.ListResponse(
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
    public static com.example.control.domain.port.ApplicationServiceRepositoryPort.ApplicationServiceFilter toFilter(
            ApplicationServiceDtos.ListRequest request, List<String> userTeamIds) {
        return new com.example.control.domain.port.ApplicationServiceRepositoryPort.ApplicationServiceFilter(
                request.ownerTeamId(),
                request.lifecycle(),
                request.tags(),
                request.search(),
                userTeamIds
        );
    }

    /**
     * Convert ListRequest DTO to Pageable.
     *
     * @param request the list request DTO
     * @return the pageable object
     */
    public static Pageable toPageable(ApplicationServiceDtos.ListRequest request) {
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;
        String sort = request.sort() != null ? request.sort() : "displayName,asc";
        
        return org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(sort.split(",")));
    }
}
