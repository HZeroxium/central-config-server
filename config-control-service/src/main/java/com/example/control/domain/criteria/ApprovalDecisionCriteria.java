package com.example.control.domain.criteria;

import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.model.ApprovalDecision;
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
 * @param requestId      filter by approval request ID
 * @param approverUserId filter by approver user ID
 * @param gate           filter by approval gate
 * @param decision       filter by decision type
 * @param userTeamIds    team IDs for ABAC filtering (null for admin queries)
 */
@Builder(toBuilder = true)
@With
public record ApprovalDecisionCriteria(
        String requestId,
        String approverUserId,
        String gate,
        ApprovalDecision.Decision decision,
        List<String> userTeamIds) {

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
     * @param teamIds   the team IDs for ABAC filtering
     * @return criteria for request decisions
     */
    public static ApprovalDecisionCriteria forRequest(String requestId, List<String> teamIds) {
        return ApprovalDecisionCriteria.builder()
                .requestId(requestId)
                .userTeamIds(teamIds)
                .build();
    }

    /**
     * Creates criteria for decisions of a specific request (admin query).
     *
     * @param requestId the approval request ID
     * @return criteria for request decisions
     */
    public static ApprovalDecisionCriteria forRequest(String requestId) {
        return ApprovalDecisionCriteria.builder()
                .requestId(requestId)
                .build();
    }

    /**
     * Creates criteria for decisions at a specific gate.
     *
     * @param gate the approval gate
     * @return criteria for gate decisions
     */
    public static ApprovalDecisionCriteria forGate(String gate) {
        return ApprovalDecisionCriteria.builder()
                .gate(gate)
                .build();
    }

    /**
     * Creates criteria for decisions by a specific approver.
     *
     * @param approverUserId the approver user ID
     * @return criteria for approver's decisions
     */
    public static ApprovalDecisionCriteria forApprover(String approverUserId) {
        return ApprovalDecisionCriteria.builder()
                .approverUserId(approverUserId)
                .build();
    }

    /**
     * Creates criteria for a specific request, approver, and gate combination.
     * Used to check if a decision already exists.
     *
     * @param requestId      the approval request ID
     * @param approverUserId the approver user ID
     * @param gate           the approval gate
     * @return criteria for checking existing decision
     */
    public static ApprovalDecisionCriteria forRequestApproverGate(String requestId, String approverUserId,
                                                                  String gate) {
        return ApprovalDecisionCriteria.builder()
                .requestId(requestId)
                .approverUserId(approverUserId)
                .gate(gate)
                .build();
    }

    /**
     * Creates criteria for counting decisions by request, gate, and decision type.
     *
     * @param requestId the approval request ID
     * @param gate      the approval gate
     * @param decision  the decision type
     * @return criteria for counting specific decisions
     */
    public static ApprovalDecisionCriteria forRequestGateDecision(String requestId, String gate,
                                                                  ApprovalDecision.Decision decision) {
        return ApprovalDecisionCriteria.builder()
                .requestId(requestId)
                .gate(gate)
                .decision(decision)
                .build();
    }
}
