package com.example.control.domain.criteria;

import lombok.Builder;
import lombok.With;

import java.util.List;

/**
 * Criteria for filtering IamTeam entities.
 * <p>
 * Provides type-safe filtering with team-based access control enforcement.
 * All queries are automatically filtered by userTeamIds when provided.
 * </p>
 *
 * @param displayName exact display name match
 * @param members list of member user IDs to filter by
 * @param userTeamIds team IDs for ABAC filtering (null for admin queries)
 */
@Builder(toBuilder = true)
@With
public record IamTeamCriteria(
        String displayName,
        List<String> members,
        List<String> userTeamIds
) {

    /**
     * Creates criteria with no filtering (admin query).
     *
     * @return criteria with no filters
     */
    public static IamTeamCriteria noFilter() {
        return IamTeamCriteria.builder().build();
    }

    /**
     * Creates criteria for team-based filtering.
     *
     * @param teamIds the team IDs to filter by
     * @return criteria with team filtering
     */
    public static IamTeamCriteria forTeams(List<String> teamIds) {
        return IamTeamCriteria.builder()
                .userTeamIds(teamIds)
                .build();
    }

    /**
     * Creates criteria for a specific user context.
     *
     * @param userContext the user context containing team IDs
     * @return criteria with user team filtering
     */
    public static IamTeamCriteria forUser(com.example.control.config.security.UserContext userContext) {
        return IamTeamCriteria.builder()
                .userTeamIds(userContext != null ? userContext.getTeamIds() : null)
                .build();
    }
}
