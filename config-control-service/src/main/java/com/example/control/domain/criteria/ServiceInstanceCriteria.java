package com.example.control.domain.criteria;

import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.model.ServiceInstance;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.List;

/**
 * Criteria for filtering ServiceInstance entities.
 * <p>
 * Provides type-safe filtering with team-based access control enforcement.
 * All queries are automatically filtered by userTeamIds when provided.
 * </p>
 *
 * @param serviceId      filter by service ID
 * @param instanceId     filter by instance ID
 * @param status         filter by instance status
 * @param hasDrift       filter by drift status
 * @param environment    filter by environment
 * @param version        filter by version
 * @param lastSeenAtFrom filter by last seen date (from)
 * @param lastSeenAtTo   filter by last seen date (to)
 * @param userTeamIds    team IDs for ABAC filtering (null for admin queries)
 */
@Builder(toBuilder = true)
@With
public record ServiceInstanceCriteria(
        String serviceId,
        String instanceId,
        ServiceInstance.InstanceStatus status,
        Boolean hasDrift,
        String environment,
        String version,
        Instant lastSeenAtFrom,
        Instant lastSeenAtTo,
        List<String> userTeamIds) {

    /**
     * Creates criteria with no filtering (admin query).
     *
     * @return criteria with no filters
     */
    public static ServiceInstanceCriteria noFilter() {
        return ServiceInstanceCriteria.builder().build();
    }

    /**
     * Creates criteria for team-based filtering.
     *
     * @param teamIds the team IDs to filter by
     * @return criteria with team filtering
     */
    public static ServiceInstanceCriteria forTeams(List<String> teamIds) {
        return ServiceInstanceCriteria.builder()
                .userTeamIds(teamIds)
                .build();
    }

    /**
     * Creates criteria for a specific user context.
     *
     * @param userContext the user context containing team IDs
     * @return criteria with user team filtering
     */
    public static ServiceInstanceCriteria forUser(UserContext userContext) {
        return ServiceInstanceCriteria.builder()
                .userTeamIds(userContext != null ? userContext.getTeamIds() : null)
                .build();
    }

    /**
     * Creates criteria for a specific service.
     *
     * @param serviceId the service ID
     * @param teamIds   the team IDs to filter by
     * @return criteria for the service
     */
    public static ServiceInstanceCriteria forService(String serviceId, List<String> teamIds) {
        return ServiceInstanceCriteria.builder()
                .serviceId(serviceId)
                .userTeamIds(teamIds)
                .build();
    }

    /**
     * Creates criteria for a specific service (admin query, no team filtering).
     *
     * @param serviceId the service ID
     * @return criteria for the service
     */
    public static ServiceInstanceCriteria forService(String serviceId) {
        return ServiceInstanceCriteria.builder()
                .serviceId(serviceId)
                .build();
    }

    /**
     * Creates criteria for instances with drift (admin query).
     *
     * @return criteria for drifted instances
     */
    public static ServiceInstanceCriteria withDrift() {
        return ServiceInstanceCriteria.builder()
                .hasDrift(true)
                .build();
    }

    /**
     * Creates criteria for instances with specific status (admin query).
     *
     * @param status the instance status
     * @return criteria for status
     */
    public static ServiceInstanceCriteria withStatus(ServiceInstance.InstanceStatus status) {
        return ServiceInstanceCriteria.builder()
                .status(status)
                .build();
    }

    /**
     * Creates criteria for a specific instance (admin query).
     *
     * @param instanceId the instance ID
     * @return criteria for instance
     */
    public static ServiceInstanceCriteria forInstance(String instanceId) {
        return ServiceInstanceCriteria.builder()
                .instanceId(instanceId)
                .build();
    }

    /**
     * Creates criteria for a specific environment (admin query).
     *
     * @param environment the environment
     * @return criteria for environment
     */
    public static ServiceInstanceCriteria forEnvironment(String environment) {
        return ServiceInstanceCriteria.builder()
                .environment(environment)
                .build();
    }

    /**
     * Creates criteria for drifted instances of a specific service.
     *
     * @param serviceId the service ID
     * @return criteria for service with drift
     */
    public static ServiceInstanceCriteria forServiceWithDrift(String serviceId) {
        return ServiceInstanceCriteria.builder()
                .serviceId(serviceId)
                .hasDrift(true)
                .build();
    }

    /**
     * Creates criteria for stale instances (not seen since threshold).
     *
     * @param threshold the timestamp threshold
     * @return criteria for stale instances
     */
    public static ServiceInstanceCriteria staleInstances(Instant threshold) {
        return ServiceInstanceCriteria.builder()
                .lastSeenAtTo(threshold)
                .build();
    }
}
