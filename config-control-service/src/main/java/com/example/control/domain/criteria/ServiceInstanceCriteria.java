package com.example.control.domain.criteria;

import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ServiceInstance;
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
 * @param serviceId filter by service ID
 * @param instanceId filter by instance ID
 * @param status filter by instance status
 * @param hasDrift filter by drift status
 * @param environment filter by environment
 * @param version filter by version
 * @param lastSeenAtFrom filter by last seen date (from)
 * @param lastSeenAtTo filter by last seen date (to)
 * @param userTeamIds team IDs for ABAC filtering (null for admin queries)
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
        List<String> userTeamIds
) {

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
     * @param teamIds the team IDs to filter by
     * @return criteria for the service
     */
    public static ServiceInstanceCriteria forService(String serviceId, List<String> teamIds) {
        return ServiceInstanceCriteria.builder()
                .serviceId(serviceId)
                .userTeamIds(teamIds)
                .build();
    }
}
