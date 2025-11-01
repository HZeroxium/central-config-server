package com.example.control.api.http.mapper.domain;

import com.example.control.api.http.dto.common.PageDtos;
import com.example.control.api.http.dto.domain.ServiceShareDtos;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.model.ServiceShare;
import com.example.control.domain.criteria.ServiceShareCriteria;
import com.example.control.domain.valueobject.id.ServiceShareId;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for ServiceShare API operations.
 * <p>
 * Provides mapping between domain objects and API DTOs with
 * proper validation and team-based filtering.
 * </p>
 */
public final class ServiceShareApiMapper {

        private ServiceShareApiMapper() {
                throw new UnsupportedOperationException("Utility class");
        }

        /**
         * Map CreateRequest DTO to domain entity.
         *
         * @param request     the create request
         * @param userContext the user context for granted by
         * @return the domain entity
         */
        public static ServiceShare toDomain(ServiceShareDtos.CreateRequest request, UserContext userContext) {
                return ServiceShare.builder()
                                .id(ServiceShareId.of(UUID.randomUUID().toString()))
                                .resourceLevel(ServiceShare.ResourceLevel.SERVICE)
                                .serviceId(request.serviceId())
                                .grantToType(ServiceShare.GranteeType.valueOf(request.grantToType()))
                                .grantToId(request.grantToId())
                                .permissions(request.permissions().stream()
                                                .map(ServiceShare.SharePermission::valueOf)
                                                .toList())
                                .environments(request.environments() != null ? request.environments() : List.of())
                                .grantedBy(userContext.getUserId())
                                .createdAt(Instant.now())
                                .expiresAt(request.expiresAt())
                                .build();
        }

        /**
         * Map domain entity to Response DTO.
         *
         * @param entity the domain entity
         * @return the response DTO
         */
        public static ServiceShareDtos.Response toResponse(ServiceShare entity) {
                return new ServiceShareDtos.Response(
                                entity.getId().id(),
                                entity.getResourceLevel().name(),
                                entity.getServiceId(),
                                entity.getGrantToType().name(),
                                entity.getGrantToId(),
                                entity.getPermissions().stream()
                                                .map(Enum::name)
                                                .toList(),
                                entity.getEnvironments(),
                                entity.getGrantedBy(),
                                entity.getCreatedAt(),
                                entity.getExpiresAt());
        }

        /**
         * Map QueryFilter to domain criteria with team filtering.
         *
         * @param filter      the query filter
         * @param userContext the user context for team filtering
         * @return the domain criteria
         */
        public static ServiceShareCriteria toCriteria(ServiceShareDtos.QueryFilter filter, UserContext userContext) {
                return ServiceShareCriteria.builder()
                                .serviceId(filter != null ? filter.serviceId() : null)
                                .grantToType(filter != null && filter.grantToType() != null
                                                ? ServiceShare.GranteeType.valueOf(filter.grantToType())
                                                : null)
                                .grantToId(filter != null ? filter.grantToId() : null)
                                .environments(filter != null ? filter.environments() : null)
                                .userTeamIds(userContext.getTeamIds())
                                .build();
        }

        /**
         * Convert Page<ServiceShare> to ServiceSharePageResponse.
         *
         * @param page the Spring Page containing ServiceShare entities
         * @return the domain-specific page response
         */
        public static ServiceShareDtos.ServiceSharePageResponse toPageResponse(Page<ServiceShare> page) {
                List<ServiceShareDtos.Response> items = page.getContent().stream()
                                .map(ServiceShareApiMapper::toResponse)
                                .toList();

                return ServiceShareDtos.ServiceSharePageResponse.builder()
                                .items(items)
                                .metadata(PageDtos.PageMetadata.from(page))
                                .build();
        }
}