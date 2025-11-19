package com.example.control.domain.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Domain event published when an approval request is rejected.
 * <p>
 * This event triggers email notifications to the requester.
 * </p>
 *
 * @param requestId       the approval request ID
 * @param requesterUserId the user ID who created the request
 * @param serviceId       the service ID that was requested
 * @param targetTeamId    the team ID that was requested
 * @param rejectorUserId  the user ID who rejected the request (can be SYSTEM
 *                        for cascade rejection)
 * @param rejectedAt      timestamp when the rejection occurred
 * @param reason          reason for rejection
 */
@Data
@Builder
public class ApprovalRequestRejectedEvent {

  private final String requestId;
  private final String requesterUserId;
  private final String serviceId;
  private final String targetTeamId;
  private final String rejectorUserId;
  private final Instant rejectedAt;
  private final String reason;
}

