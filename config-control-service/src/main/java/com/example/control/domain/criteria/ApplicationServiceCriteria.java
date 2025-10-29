package com.example.control.domain.criteria;

import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.object.ApplicationService;
import lombok.Builder;
import lombok.With;

import java.util.List;

/**
 * Criteria for filtering ApplicationService entities.
 * <p>
 * Provides type-safe filtering with team-based access control enforcement.
 * All queries are automatically filtered by userTeamIds when provided.
 * </p>
 * <p>
 * New filtering logic for service visibility:
 * <ul>
 * <li>Orphaned services (ownerTeamId=null) are visible to all authenticated
 * users</li>
 * <li>Team-owned services are visible to team members</li>
 * <li>Shared services are visible to teams they're shared with</li>
 * </ul>
 * </p>
 *
 * @param ownerTeamId      filter by owner team ID
 * @param lifecycle        filter by service lifecycle
 * @param tags             filter by tags (any match)
 * @param search           search term for display name
 * @param userTeamIds      team IDs for ABAC filtering (null for admin queries)
 * @param sharedServiceIds service IDs shared to user's teams (for shared
 *                         service visibility)
 * @param includeOrphaned  whether to include orphaned services
 *                         (ownerTeamId=null)
 */
@Builder(toBuilder = true)
@With
public record ApplicationServiceCriteria(
        String ownerTeamId,
        ApplicationService.ServiceLifecycle lifecycle,
        List<String> tags,
        String search,
        List<String> userTeamIds,
        List<String> sharedServiceIds,
        Boolean includeOrphaned) {

    /**
     * Creates criteria with no filtering (admin query).
     *
     * @return criteria with no filters
     */
    public static ApplicationServiceCriteria noFilter() {
        return ApplicationServiceCriteria.builder().build();
    }

    /**
     * Creates criteria for team-based filtering.
     *
     * @param teamIds the team IDs to filter by
     * @return criteria with team filtering
     */
    public static ApplicationServiceCriteria forTeams(List<String> teamIds) {
        return ApplicationServiceCriteria.builder()
                .userTeamIds(teamIds)
                .build();
    }

    /**
     * Creates criteria for a specific user context.
     *
     * @param userContext the user context containing team IDs
     * @return criteria with user team filtering
     */
    public static ApplicationServiceCriteria forUser(UserContext userContext) {
        return ApplicationServiceCriteria.builder()
                .userTeamIds(userContext != null ? userContext.getTeamIds() : null)
                .build();
    }

    /**
     * Creates criteria for services owned by a specific team.
     *
     * @param ownerTeamId the owner team ID
     * @param teamIds     the team IDs for ABAC filtering
     * @return criteria for owned services
     */
    public static ApplicationServiceCriteria forOwnedByTeam(String ownerTeamId, List<String> teamIds) {
        return ApplicationServiceCriteria.builder()
                .ownerTeamId(ownerTeamId)
                .userTeamIds(teamIds)
                .build();
    }

    /**
     * Creates criteria for services owned by a specific team (admin query).
     *
     * @param ownerTeamId the owner team ID
     * @return criteria for owned services
     */
    public static ApplicationServiceCriteria forTeam(String ownerTeamId) {
        return ApplicationServiceCriteria.builder()
                .ownerTeamId(ownerTeamId)
                .build();
    }

    /**
     * Creates criteria for orphaned services (ownerTeamId=null).
     * These services are visible to all authenticated users for claiming.
     *
     * @return criteria for orphaned services
     */
    public static ApplicationServiceCriteria orphaned() {
        return ApplicationServiceCriteria.builder()
                .includeOrphaned(true)
                .build();
    }

    /**
     * Creates criteria for services shared to user's teams.
     *
     * @param sharedServiceIds list of service IDs shared to user's teams
     * @param userTeamIds      the user's team IDs
     * @return criteria for shared services
     */
    public static ApplicationServiceCriteria shared(List<String> sharedServiceIds, List<String> userTeamIds) {
        return ApplicationServiceCriteria.builder()
                .sharedServiceIds(sharedServiceIds)
                .userTeamIds(userTeamIds)
                .build();
    }

    /**
     * Creates criteria for searching by display name.
     *
     * @param displayName the display name to search for
     * @return criteria for display name search
     */
    public static ApplicationServiceCriteria byDisplayName(String displayName) {
        return ApplicationServiceCriteria.builder()
                .search(displayName)
                .build();
    }
}
