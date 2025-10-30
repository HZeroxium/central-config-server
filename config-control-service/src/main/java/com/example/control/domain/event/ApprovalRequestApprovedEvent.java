package com.example.control.domain.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Domain event published when an approval request is approved.
 * <p>
 * This event triggers email notifications to the requester.
 * </p>
 *
 * @param requestId       the approval request ID
 * @param requesterUserId the user ID who created the request
 * @param serviceId       the service ID that was requested
 * @param targetTeamId    the team ID that will own the service
 * @param approverUserId  the user ID who approved the request (can be SYSTEM
 *                        for cascade)
 * @param approvedAt      timestamp when the approval occurred
 */
@Data
@Builder
public class ApprovalRequestApprovedEvent {

  private final String requestId;
  private final String requesterUserId;
  private final String serviceId;
  private final String targetTeamId;
  private final String approverUserId;
  private final Instant approvedAt;
}
