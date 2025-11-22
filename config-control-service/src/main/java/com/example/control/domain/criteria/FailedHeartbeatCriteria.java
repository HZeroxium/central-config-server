package com.example.control.domain.criteria;

import com.example.control.domain.model.FailedHeartbeat;
import com.example.control.infrastructure.config.security.UserContext;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.List;

/**
 * Criteria for filtering FailedHeartbeat entities.
 * <p>
 * Provides type-safe filtering with team-based access control enforcement.
 * All queries are automatically filtered by userTeamIds when provided.
 * </p>
 *
 * @param serviceName     filter by service name
 * @param instanceId      filter by instance ID
 * @param status          filter by failed heartbeat status
 * @param teamId          filter by team ID (for team-based access)
 * @param firstSeenAtFrom filter by first seen date (from)
 * @param firstSeenAtTo   filter by first seen date (to)
 * @param userTeamIds     team IDs for ABAC filtering (null for admin queries)
 */
@Builder(toBuilder = true)
@With
public record FailedHeartbeatCriteria(
        String serviceName,
        String instanceId,
        FailedHeartbeat.FailedHeartbeatStatus status,
        String teamId,
        Instant firstSeenAtFrom,
        Instant firstSeenAtTo,
        List<String> userTeamIds) {

    /**
     * Creates criteria with no filtering (admin query).
     *
     * @return criteria with no filters
     */
    public static FailedHeartbeatCriteria noFilter() {
        return FailedHeartbeatCriteria.builder().build();
    }

    /**
     * Creates criteria for team-based filtering.
     *
     * @param teamIds the team IDs to filter by
     * @return criteria with team filtering
     */
    public static FailedHeartbeatCriteria forTeams(List<String> teamIds) {
        return FailedHeartbeatCriteria.builder()
                .userTeamIds(teamIds)
                .build();
    }

    /**
     * Creates criteria for a specific user context.
     *
     * @param userContext the user context containing team IDs
     * @return criteria with user team filtering
     */
    public static FailedHeartbeatCriteria forUser(UserContext userContext) {
        return FailedHeartbeatCriteria.builder()
                .userTeamIds(userContext != null ? userContext.getTeamIds() : null)
                .build();
    }

    /**
     * Creates criteria for unresolved failed heartbeats only.
     *
     * @param teamIds the team IDs to filter by
     * @return criteria for unresolved failed heartbeats
     */
    public static FailedHeartbeatCriteria unresolvedForTeams(List<String> teamIds) {
        return FailedHeartbeatCriteria.builder()
                .status(FailedHeartbeat.FailedHeartbeatStatus.NEW)
                .userTeamIds(teamIds)
                .build();
    }
}

