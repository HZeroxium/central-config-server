package com.example.control.domain.criteria;

import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ServiceShare;
import lombok.Builder;
import lombok.With;

import java.util.List;

/**
 * Criteria for filtering ServiceShare entities.
 * <p>
 * Provides type-safe filtering with team-based access control enforcement.
 * All queries are automatically filtered by userTeamIds when provided.
 * </p>
 *
 * @param serviceId filter by service ID
 * @param grantToType filter by grantee type
 * @param grantToId filter by grantee ID
 * @param environments filter by environments
 * @param grantedBy filter by who granted the share
 * @param userTeamIds team IDs for ABAC filtering (null for admin queries)
 */
@Builder(toBuilder = true)
@With
public record ServiceShareCriteria(
        String serviceId,
        ServiceShare.GranteeType grantToType,
        String grantToId,
        List<String> environments,
        String grantedBy,
        List<String> userTeamIds
) {

    /**
     * Creates criteria with no filtering (admin query).
     *
     * @return criteria with no filters
     */
    public static ServiceShareCriteria noFilter() {
        return ServiceShareCriteria.builder().build();
    }

    /**
     * Creates criteria for team-based filtering.
     *
     * @param teamIds the team IDs to filter by
     * @return criteria with team filtering
     */
    public static ServiceShareCriteria forTeams(List<String> teamIds) {
        return ServiceShareCriteria.builder()
                .userTeamIds(teamIds)
                .build();
    }

    /**
     * Creates criteria for a specific user context.
     *
     * @param userContext the user context containing team IDs
     * @return criteria with user team filtering
     */
    public static ServiceShareCriteria forUser(UserContext userContext) {
        return ServiceShareCriteria.builder()
                .userTeamIds(userContext != null ? userContext.getTeamIds() : null)
                .build();
    }

    /**
     * Creates criteria for shares of a specific service.
     *
     * @param serviceId the service ID
     * @param teamIds the team IDs for ABAC filtering
     * @return criteria for service shares
     */
    public static ServiceShareCriteria forService(String serviceId, List<String> teamIds) {
        return ServiceShareCriteria.builder()
                .serviceId(serviceId)
                .userTeamIds(teamIds)
                .build();
    }
}
