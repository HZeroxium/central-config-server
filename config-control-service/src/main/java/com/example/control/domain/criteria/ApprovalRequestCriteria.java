package com.example.control.domain.criteria;

import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ApprovalRequest;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.List;

/**
 * Criteria for filtering ApprovalRequest entities.
 * <p>
 * Provides type-safe filtering with team-based access control enforcement.
 * All queries are automatically filtered by userTeamIds when provided.
 * </p>
 *
 * @param requesterUserId filter by requester user ID
 * @param status filter by approval status
 * @param requestType filter by request type
 * @param fromDate filter by creation date (from)
 * @param toDate filter by creation date (to)
 * @param gate filter by approval gate
 * @param userTeamIds team IDs for ABAC filtering (null for admin queries)
 */
@Builder(toBuilder = true)
@With
public record ApprovalRequestCriteria(
        String requesterUserId,
        ApprovalRequest.ApprovalStatus status,
        ApprovalRequest.RequestType requestType,
        Instant fromDate,
        Instant toDate,
        String gate,
        List<String> userTeamIds
) {

    /**
     * Creates criteria with no filtering (admin query).
     *
     * @return criteria with no filters
     */
    public static ApprovalRequestCriteria noFilter() {
        return ApprovalRequestCriteria.builder().build();
    }

    /**
     * Creates criteria for team-based filtering.
     *
     * @param teamIds the team IDs to filter by
     * @return criteria with team filtering
     */
    public static ApprovalRequestCriteria forTeams(List<String> teamIds) {
        return ApprovalRequestCriteria.builder()
                .userTeamIds(teamIds)
                .build();
    }

    /**
     * Creates criteria for a specific user context.
     *
     * @param userContext the user context containing team IDs
     * @return criteria with user team filtering
     */
    public static ApprovalRequestCriteria forUser(UserContext userContext) {
        return ApprovalRequestCriteria.builder()
                .userTeamIds(userContext != null ? userContext.getTeamIds() : null)
                .build();
    }

    /**
     * Creates criteria for pending requests requiring a specific gate.
     *
     * @param gate the approval gate
     * @param teamIds the team IDs for ABAC filtering
     * @return criteria for pending requests
     */
    public static ApprovalRequestCriteria pendingForGate(String gate, List<String> teamIds) {
        return ApprovalRequestCriteria.builder()
                .status(ApprovalRequest.ApprovalStatus.PENDING)
                .gate(gate)
                .userTeamIds(teamIds)
                .build();
    }
}
