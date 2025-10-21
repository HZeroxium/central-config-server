package com.example.control.domain.criteria;

import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ApprovalDecision;
import lombok.Builder;
import lombok.With;

import java.util.List;

/**
 * Criteria for filtering ApprovalDecision entities.
 * <p>
 * Provides type-safe filtering with team-based access control enforcement.
 * All queries are automatically filtered by userTeamIds when provided.
 * </p>
 *
 * @param requestId filter by approval request ID
 * @param approverUserId filter by approver user ID
 * @param gate filter by approval gate
 * @param decision filter by decision type
 * @param userTeamIds team IDs for ABAC filtering (null for admin queries)
 */
@Builder(toBuilder = true)
@With
public record ApprovalDecisionCriteria(
        String requestId,
        String approverUserId,
        String gate,
        ApprovalDecision.Decision decision,
        List<String> userTeamIds
) {

    /**
     * Creates criteria with no filtering (admin query).
     *
     * @return criteria with no filters
     */
    public static ApprovalDecisionCriteria noFilter() {
        return ApprovalDecisionCriteria.builder().build();
    }

    /**
     * Creates criteria for team-based filtering.
     *
     * @param teamIds the team IDs to filter by
     * @return criteria with team filtering
     */
    public static ApprovalDecisionCriteria forTeams(List<String> teamIds) {
        return ApprovalDecisionCriteria.builder()
                .userTeamIds(teamIds)
                .build();
    }

    /**
     * Creates criteria for a specific user context.
     *
     * @param userContext the user context containing team IDs
     * @return criteria with user team filtering
     */
    public static ApprovalDecisionCriteria forUser(UserContext userContext) {
        return ApprovalDecisionCriteria.builder()
                .userTeamIds(userContext != null ? userContext.getTeamIds() : null)
                .build();
    }

    /**
     * Creates criteria for decisions of a specific request.
     *
     * @param requestId the approval request ID
     * @param teamIds the team IDs for ABAC filtering
     * @return criteria for request decisions
     */
    public static ApprovalDecisionCriteria forRequest(String requestId, List<String> teamIds) {
        return ApprovalDecisionCriteria.builder()
                .requestId(requestId)
                .userTeamIds(teamIds)
                .build();
    }
}
