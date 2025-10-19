package com.example.control.domain.criteria;

import lombok.Builder;
import lombok.With;

import java.util.List;

/**
 * Criteria for filtering IamUser entities.
 * <p>
 * Provides type-safe filtering with team-based access control enforcement.
 * All queries are automatically filtered by userTeamIds when provided.
 * </p>
 *
 * @param username filter by username
 * @param email filter by email
 * @param firstName filter by first name
 * @param lastName filter by last name
 * @param teamIds filter by team IDs
 * @param managerId filter by manager ID
 * @param roles filter by roles
 * @param userTeamIds team IDs for ABAC filtering (null for admin queries)
 */
@Builder(toBuilder = true)
@With
public record IamUserCriteria(
        String username,
        String email,
        String firstName,
        String lastName,
        List<String> teamIds,
        String managerId,
        List<String> roles,
        List<String> userTeamIds
) {

    /**
     * Creates criteria with no filtering (admin query).
     *
     * @return criteria with no filters
     */
    public static IamUserCriteria noFilter() {
        return IamUserCriteria.builder().build();
    }

    /**
     * Creates criteria for team-based filtering.
     *
     * @param teamIds the team IDs to filter by
     * @return criteria with team filtering
     */
    public static IamUserCriteria forTeams(List<String> teamIds) {
        return IamUserCriteria.builder()
                .userTeamIds(teamIds)
                .build();
    }

    /**
     * Creates criteria for a specific user context.
     *
     * @param userContext the user context containing team IDs
     * @return criteria with user team filtering
     */
    public static IamUserCriteria forUser(com.example.control.config.security.UserContext userContext) {
        return IamUserCriteria.builder()
                .userTeamIds(userContext != null ? userContext.getTeamIds() : null)
                .build();
    }

    /**
     * Creates criteria for users in a specific team.
     *
     * @param teamId the team ID
     * @param teamIds the team IDs for ABAC filtering
     * @return criteria for team members
     */
    public static IamUserCriteria forTeam(String teamId, List<String> teamIds) {
        return IamUserCriteria.builder()
                .teamIds(List.of(teamId))
                .userTeamIds(teamIds)
                .build();
    }
}
