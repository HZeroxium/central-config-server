package com.example.control.domain.criteria;

import com.example.control.infrastructure.config.security.UserContext;
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
 * @param status          filter by approval status
 * @param requestType     filter by request type
 * @param fromDate        filter by creation date (from)
 * @param toDate          filter by creation date (to)
 * @param gate            filter by approval gate
 * @param userTeamIds     team IDs for ABAC filtering (null for admin queries)
 * @param targetServiceId filter by target service ID (for cascade operations)
 * @param targetTeamId    filter by target team ID (for cascade operations)
 * @param excludeTeamId   exclude requests with this target team ID (for cascade
 *                        operations)
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
        List<String> userTeamIds,
        String targetServiceId,
        String targetTeamId,
        String excludeTeamId) {

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
     * @param gate    the approval gate
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

    /**
     * Creates criteria for requests by a specific requester.
     *
     * @param requesterUserId the requester user ID
     * @return criteria for requester's requests
     */
    public static ApprovalRequestCriteria forRequester(String requesterUserId) {
        return ApprovalRequestCriteria.builder()
                .requesterUserId(requesterUserId)
                .build();
    }

    /**
     * Creates criteria for requests by status.
     *
     * @param status the approval status
     * @return criteria for requests with the given status
     */
    public static ApprovalRequestCriteria byStatus(ApprovalRequest.ApprovalStatus status) {
        return ApprovalRequestCriteria.builder()
                .status(status)
                .build();
    }

    /**
     * Creates criteria for pending requests by requester for a service.
     * Used to check for duplicate pending requests.
     *
     * @param requesterUserId the requester user ID
     * @return criteria for pending requests by requester
     */
    public static ApprovalRequestCriteria pendingByRequester(String requesterUserId) {
        return ApprovalRequestCriteria.builder()
                .requesterUserId(requesterUserId)
                .status(ApprovalRequest.ApprovalStatus.PENDING)
                .build();
    }

    /**
     * Creates criteria for all pending requests.
     *
     * @return criteria for pending requests
     */
    public static ApprovalRequestCriteria allPending() {
        return ApprovalRequestCriteria.builder()
                .status(ApprovalRequest.ApprovalStatus.PENDING)
                .build();
    }

    /**
     * Creates criteria for requests by service ID, status, and team ID.
     * Used for cascade operations to find approved requests for the same team.
     *
     * @param serviceId the service ID
     * @param status    the approval status
     * @param teamId    the target team ID
     * @return criteria for requests matching service, status, and team
     */
    public static ApprovalRequestCriteria forServiceIdAndStatusAndTeamId(String serviceId,
            ApprovalRequest.ApprovalStatus status,
            String teamId) {
        return ApprovalRequestCriteria.builder()
                .status(status)
                .targetServiceId(serviceId)
                .targetTeamId(teamId)
                .build();
    }

    /**
     * Creates criteria for requests by service ID and status, excluding a specific
     * team ID.
     * Used for cascade operations to find rejected requests for other teams.
     *
     * @param serviceId     the service ID
     * @param status        the approval status
     * @param excludeTeamId the team ID to exclude
     * @return criteria for requests matching service and status, but not team
     */
    public static ApprovalRequestCriteria forServiceIdAndStatusExcludingTeamId(String serviceId,
            ApprovalRequest.ApprovalStatus status,
            String excludeTeamId) {
        return ApprovalRequestCriteria.builder()
                .status(status)
                .targetServiceId(serviceId)
                .excludeTeamId(excludeTeamId)
                .build();
    }
}
