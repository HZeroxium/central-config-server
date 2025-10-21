package com.example.control.domain.criteria;

import com.example.control.config.security.UserContext;
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
 *
 * @param ownerTeamId filter by owner team ID
 * @param lifecycle filter by service lifecycle
 * @param tags filter by tags (any match)
 * @param search search term for display name
 * @param userTeamIds team IDs for ABAC filtering (null for admin queries)
 */
@Builder(toBuilder = true)
@With
public record ApplicationServiceCriteria(
        String ownerTeamId,
        ApplicationService.ServiceLifecycle lifecycle,
        List<String> tags,
        String search,
        List<String> userTeamIds
) {

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
     * @param teamIds the team IDs for ABAC filtering
     * @return criteria for owned services
     */
    public static ApplicationServiceCriteria forOwnedByTeam(String ownerTeamId, List<String> teamIds) {
        return ApplicationServiceCriteria.builder()
                .ownerTeamId(ownerTeamId)
                .userTeamIds(teamIds)
                .build();
    }
}
