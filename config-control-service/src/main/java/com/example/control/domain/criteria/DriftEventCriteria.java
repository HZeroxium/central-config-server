package com.example.control.domain.criteria;

import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.object.DriftEvent;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.List;

/**
 * Criteria for filtering DriftEvent entities.
 * <p>
 * Provides type-safe filtering with team-based access control enforcement.
 * All queries are automatically filtered by userTeamIds when provided.
 * </p>
 *
 * @param serviceName    filter by service name
 * @param instanceId     filter by instance ID
 * @param status         filter by drift status
 * @param severity       filter by drift severity
 * @param detectedAtFrom filter by detection date (from)
 * @param detectedAtTo   filter by detection date (to)
 * @param unresolvedOnly filter for unresolved events only
 * @param userTeamIds    team IDs for ABAC filtering (null for admin queries)
 */
@Builder(toBuilder = true)
@With
public record DriftEventCriteria(
        String serviceName,
        String instanceId,
        DriftEvent.DriftStatus status,
        DriftEvent.DriftSeverity severity,
        Instant detectedAtFrom,
        Instant detectedAtTo,
        Boolean unresolvedOnly,
        List<String> userTeamIds) {

    /**
     * Creates criteria with no filtering (admin query).
     *
     * @return criteria with no filters
     */
    public static DriftEventCriteria noFilter() {
        return DriftEventCriteria.builder().build();
    }

    /**
     * Creates criteria for team-based filtering.
     *
     * @param teamIds the team IDs to filter by
     * @return criteria with team filtering
     */
    public static DriftEventCriteria forTeams(List<String> teamIds) {
        return DriftEventCriteria.builder()
                .userTeamIds(teamIds)
                .build();
    }

    /**
     * Creates criteria for a specific user context.
     *
     * @param userContext the user context containing team IDs
     * @return criteria with user team filtering
     */
    public static DriftEventCriteria forUser(UserContext userContext) {
        return DriftEventCriteria.builder()
                .userTeamIds(userContext != null ? userContext.getTeamIds() : null)
                .build();
    }

    /**
     * Creates criteria for unresolved events only.
     *
     * @param teamIds the team IDs to filter by
     * @return criteria for unresolved events
     */
    public static DriftEventCriteria unresolvedForTeams(List<String> teamIds) {
        return DriftEventCriteria.builder()
                .unresolvedOnly(true)
                .userTeamIds(teamIds)
                .build();
    }

    /**
     * Creates criteria for a specific service (admin query, no team filtering).
     *
     * @param serviceName the service name to filter by
     * @return criteria for service
     */
    public static DriftEventCriteria forService(String serviceName) {
        return DriftEventCriteria.builder()
                .serviceName(serviceName)
                .build();
    }

    /**
     * Creates criteria for unresolved events of a specific service (admin query).
     *
     * @param serviceName the service name to filter by
     * @return criteria for unresolved events of service
     */
    public static DriftEventCriteria unresolvedForService(String serviceName) {
        return DriftEventCriteria.builder()
                .serviceName(serviceName)
                .status(DriftEvent.DriftStatus.DETECTED)
                .build();
    }

    /**
     * Creates criteria for a specific drift status (admin query).
     *
     * @param status the drift status to filter by
     * @return criteria for status
     */
    public static DriftEventCriteria withStatus(DriftEvent.DriftStatus status) {
        return DriftEventCriteria.builder()
                .status(status)
                .build();
    }

    /**
     * Creates criteria for unresolved events only (admin query).
     *
     * @return criteria for unresolved events
     */
    public static DriftEventCriteria unresolved() {
        return DriftEventCriteria.builder()
                .status(DriftEvent.DriftStatus.DETECTED)
                .build();
    }

    /**
     * Creates criteria for a specific instance (admin query).
     *
     * @param serviceName the service name
     * @param instanceId  the instance ID
     * @return criteria for instance
     */
    public static DriftEventCriteria forInstance(String serviceName, String instanceId) {
        return DriftEventCriteria.builder()
                .serviceName(serviceName)
                .instanceId(instanceId)
                .build();
    }
}
