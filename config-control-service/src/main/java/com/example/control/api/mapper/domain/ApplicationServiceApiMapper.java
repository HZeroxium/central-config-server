package com.example.control.api.mapper.domain;

import com.example.control.api.dto.common.PageDtos;
import com.example.control.api.dto.domain.ApplicationServiceDtos;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.criteria.ApplicationServiceCriteria;
import com.example.control.domain.id.ApplicationServiceId;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Mapper for ApplicationService API operations.
 * <p>
 * Provides mapping between domain objects and API DTOs with
 * proper validation and team-based filtering.
 * </p>
 */
@Component
public final class ApplicationServiceApiMapper {

    private ApplicationServiceApiMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Map CreateRequest DTO to domain entity.
     *
     * @param request the create request
     * @return the domain entity
     */
    public static ApplicationService toDomain(ApplicationServiceDtos.CreateRequest request) {
        return ApplicationService.builder()
                .id(ApplicationServiceId.of(request.id()))
                .displayName(request.displayName())
                .ownerTeamId(request.ownerTeamId())
                .environments(request.environments())
                .tags(request.tags() != null ? request.tags() : List.of())
                .repoUrl(request.repoUrl())
                .lifecycle(ApplicationService.ServiceLifecycle.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy("system") // Will be set by service layer
                .attributes(request.attributes() != null ? request.attributes() : Map.of())
                .build();
    }

    /**
     * Apply UpdateRequest to existing domain entity.
     *
     * @param entity  the existing entity
     * @param request the update request
     * @return the updated entity
     */
    public static ApplicationService apply(ApplicationService entity, ApplicationServiceDtos.UpdateRequest request) {
        return entity.toBuilder()
                .displayName(request.displayName() != null ? request.displayName() : entity.getDisplayName())
                .lifecycle(
                        request.lifecycle() != null ? ApplicationService.ServiceLifecycle.valueOf(request.lifecycle())
                                : entity.getLifecycle())
                .tags(request.tags() != null ? request.tags() : entity.getTags())
                .repoUrl(request.repoUrl() != null ? request.repoUrl() : entity.getRepoUrl())
                .attributes(request.attributes() != null ? request.attributes() : entity.getAttributes())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Map domain entity to Response DTO.
     *
     * @param entity the domain entity
     * @return the response DTO
     */
    public static ApplicationServiceDtos.Response toResponse(ApplicationService entity) {
        return new ApplicationServiceDtos.Response(
                entity.getId().id(),
                entity.getDisplayName(),
                entity.getOwnerTeamId(),
                entity.getEnvironments(),
                entity.getTags(),
                entity.getRepoUrl(),
                entity.getLifecycle().name(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getAttributes());
    }

    /**
     * Map QueryFilter to domain criteria with team filtering.
     *
     * @param filter      the query filter
     * @param userContext the user context for team filtering
     * @return the domain criteria
     */
    public static ApplicationServiceCriteria toCriteria(ApplicationServiceDtos.QueryFilter filter,
                                                        UserContext userContext) {
        return ApplicationServiceCriteria.builder()
                .ownerTeamId(filter != null ? filter.ownerTeamId() : null)
                .lifecycle(filter != null && filter.lifecycle() != null
                        ? ApplicationService.ServiceLifecycle.valueOf(filter.lifecycle())
                        : null)
                .tags(filter != null ? filter.tags() : null)
                .search(filter != null ? filter.search() : null)
                .userTeamIds(userContext.getTeamIds())
                .build();
    }

    /**
     * Convert Page<ApplicationService> to ApplicationServicePageResponse.
     *
     * @param page the Spring Page containing ApplicationService entities
     * @return the domain-specific page response
     */
    public static ApplicationServiceDtos.ApplicationServicePageResponse toPageResponse(Page<ApplicationService> page) {
        List<ApplicationServiceDtos.Response> items = page.getContent().stream()
                .map(ApplicationServiceApiMapper::toResponse)
                .toList();

        return ApplicationServiceDtos.ApplicationServicePageResponse.builder()
                .items(items)
                .metadata(PageDtos.PageMetadata.from(page))
                .build();
    }
}