package com.example.control.api.mapper.domain;

import com.example.control.api.dto.common.PageDtos;
import com.example.control.api.dto.domain.ServiceInstanceDtos;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.object.ServiceInstance;
import com.example.control.domain.criteria.ServiceInstanceCriteria;
import com.example.control.domain.id.ServiceInstanceId;
import org.springframework.data.domain.Page;

import java.util.List;

public final class ServiceInstanceApiMapper {

    private ServiceInstanceApiMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ServiceInstance toDomain(ServiceInstanceDtos.CreateRequest req) {
        return ServiceInstance.builder()
                .id(ServiceInstanceId.of(req.getInstanceId()))
                .serviceId(req.getServiceId())
                .host(req.getHost())
                .port(req.getPort())
                .environment(req.getEnvironment())
                .version(req.getVersion())
                .configHash(req.getConfigHash())
                .lastAppliedHash(req.getLastAppliedHash())
                .expectedHash(req.getExpectedHash())
                .metadata(req.getMetadata())
                .build();
    }

    public static void apply(ServiceInstance entity, ServiceInstanceDtos.UpdateRequest req) {
        if (req.getHost() != null)
            entity.setHost(req.getHost());
        if (req.getPort() != null)
            entity.setPort(req.getPort());
        if (req.getEnvironment() != null)
            entity.setEnvironment(req.getEnvironment());
        if (req.getVersion() != null)
            entity.setVersion(req.getVersion());
        if (req.getConfigHash() != null)
            entity.setConfigHash(req.getConfigHash());
        if (req.getLastAppliedHash() != null)
            entity.setLastAppliedHash(req.getLastAppliedHash());
        if (req.getExpectedHash() != null)
            entity.setExpectedHash(req.getExpectedHash());
        if (req.getHasDrift() != null)
            entity.setHasDrift(req.getHasDrift());
        if (req.getStatus() != null)
            entity.setStatus(req.getStatus());
        if (req.getMetadata() != null)
            entity.setMetadata(req.getMetadata());
    }

    public static ServiceInstanceDtos.Response toResponse(ServiceInstance si) {
        return ServiceInstanceDtos.Response.builder()
                .serviceId(si.getServiceId())
                .instanceId(si.getInstanceId())
                .host(si.getHost())
                .port(si.getPort())
                .environment(si.getEnvironment())
                .version(si.getVersion())
                .configHash(si.getConfigHash())
                .lastAppliedHash(si.getLastAppliedHash())
                .expectedHash(si.getExpectedHash())
                .status(si.getStatus())
                .lastSeenAt(si.getLastSeenAt())
                .createdAt(si.getCreatedAt())
                .updatedAt(si.getUpdatedAt())
                .metadata(si.getMetadata())
                .hasDrift(si.getHasDrift())
                .driftDetectedAt(si.getDriftDetectedAt())
                .build();
    }

    /**
     * Map QueryFilter to domain criteria with team filtering.
     *
     * @param filter      the query filter
     * @param userContext the user context for team filtering
     * @return the domain criteria
     */
    public static ServiceInstanceCriteria toCriteria(ServiceInstanceDtos.QueryFilter filter, UserContext userContext) {
        return ServiceInstanceCriteria.builder()
                .serviceId(filter != null ? filter.getServiceId() : null)
                .instanceId(filter != null ? filter.getInstanceId() : null)
                .status(filter != null ? filter.getStatus() : null)
                .hasDrift(filter != null ? filter.getHasDrift() : null)
                .environment(filter != null ? filter.getEnvironment() : null)
                .version(filter != null ? filter.getVersion() : null)
                .lastSeenAtFrom(filter != null ? filter.getLastSeenAtFrom() : null)
                .lastSeenAtTo(filter != null ? filter.getLastSeenAtTo() : null)
                .userTeamIds(userContext.getTeamIds())
                .build();
    }

    /**
     * Convert Page<ServiceInstance> to ServiceInstancePageResponse.
     *
     * @param page the Spring Page containing ServiceInstance entities
     * @return the domain-specific page response
     */
    public static ServiceInstanceDtos.ServiceInstancePageResponse toPageResponse(Page<ServiceInstance> page) {
        List<ServiceInstanceDtos.Response> items = page.getContent().stream()
                .map(ServiceInstanceApiMapper::toResponse)
                .toList();

        return ServiceInstanceDtos.ServiceInstancePageResponse.builder()
                .items(items)
                .metadata(PageDtos.PageMetadata.from(page))
                .build();
    }
}
