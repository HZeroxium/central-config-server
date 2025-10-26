package com.example.control.seeding.factory;

import com.example.control.domain.id.ApprovalRequestId;
import com.example.control.domain.object.ApprovalRequest;
import com.example.control.domain.object.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Factory for generating realistic {@link ApprovalRequest} mock data.
 * <p>
 * Generates approval requests for service ownership transfer with varied
 * statuses, gates, and timestamps suitable for testing approval workflows.
 * </p>
 *
 * <p>
 * <strong>Generation Strategy:</strong>
 * </p>
 * <ul>
 * <li>Request Type: ASSIGN_SERVICE_TO_TEAM (orphan services)</li>
 * <li>Status: PENDING, APPROVED, REJECTED based on configuration</li>
 * <li>Gates: SYS_ADMIN (default), with configurable requirements</li>
 * <li>Timestamps: Realistic creation and update times</li>
 * <li>Approval Counts: Tracked per gate</li>
 * </ul>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalRequestFactory {

  private final Faker faker;

  /**
   * Mock requester user IDs.
   */
  private static final List<String> REQUESTER_USERS = List.of(
      "user1", "user2", "user3", "john.doe", "jane.smith", "team-lead");

  /**
   * Generates an {@link ApprovalRequest} for assigning an orphan service to a
   * team.
   *
   * @param service      orphan service to be assigned
   * @param targetTeamId team requesting ownership
   * @param status       desired approval status
   * @return generated approval request
   */
  public ApprovalRequest generate(ApplicationService service, String targetTeamId,
      ApprovalRequest.ApprovalStatus status) {

    String requesterId = selectRequester();

    ApprovalRequest.ApprovalTarget target = ApprovalRequest.ApprovalTarget.builder()
        .serviceId(service.getId().id())
        .teamId(targetTeamId)
        .build();

    List<ApprovalRequest.ApprovalGate> requiredGates = generateRequiredGates();

    ApprovalRequest.RequesterSnapshot snapshot = generateRequesterSnapshot(targetTeamId);

    Map<String, Integer> approvalCounts = generateApprovalCounts(requiredGates, status);

    Instant createdAt = generateCreatedAt();
    Instant updatedAt = generateUpdatedAt(createdAt, status);

    String note = generateRequestNote(service, targetTeamId);

    log.debug("Generated approval request: service={} team={} status={}",
        service.getId().id(), targetTeamId, status);

    return ApprovalRequest.builder()
        .id(ApprovalRequestId.of(UUID.randomUUID().toString()))
        .requesterUserId(requesterId)
        .requestType(ApprovalRequest.RequestType.ASSIGN_SERVICE_TO_TEAM)
        .target(target)
        .required(requiredGates)
        .status(status)
        .snapshot(snapshot)
        .counts(approvalCounts)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .version(0)
        .note(note)
        .build();
  }

  /**
   * Selects a requester user ID.
   *
   * @return requester user ID
   */
  private String selectRequester() {
    return REQUESTER_USERS.get(faker.random().nextInt(REQUESTER_USERS.size()));
  }

  /**
   * Generates required approval gates.
   * Default: SYS_ADMIN gate requiring 1 approval.
   *
   * @return list of approval gates
   */
  private List<ApprovalRequest.ApprovalGate> generateRequiredGates() {
    // For now, only SYS_ADMIN gate is required
    ApprovalRequest.ApprovalGate sysAdminGate = ApprovalRequest.ApprovalGate.builder()
        .gate("SYS_ADMIN")
        .minApprovals(1)
        .status(ApprovalRequest.ApprovalGate.GateStatus.PENDING)
        .build();

    return List.of(sysAdminGate);
  }

  /**
   * Generates requester snapshot with team and role information.
   *
   * @param targetTeamId team the requester belongs to
   * @return requester snapshot
   */
  private ApprovalRequest.RequesterSnapshot generateRequesterSnapshot(String targetTeamId) {
    return ApprovalRequest.RequesterSnapshot.builder()
        .teamIds(List.of(targetTeamId))
        .managerId("manager-" + targetTeamId)
        .roles(List.of("TEAM_MEMBER"))
        .build();
  }

  /**
   * Generates approval counts based on gates and status.
   *
   * @param requiredGates required approval gates
   * @param status        approval status
   * @return map of gate name to approval count
   */
  private Map<String, Integer> generateApprovalCounts(List<ApprovalRequest.ApprovalGate> requiredGates,
      ApprovalRequest.ApprovalStatus status) {
    Map<String, Integer> counts = new HashMap<>();

    for (ApprovalRequest.ApprovalGate gate : requiredGates) {
      int count = 0;

      // Set counts based on status
      if (status == ApprovalRequest.ApprovalStatus.APPROVED) {
        // Approved requests have met minimum approvals
        count = gate.getMinApprovals();
      } else if (status == ApprovalRequest.ApprovalStatus.REJECTED) {
        // Rejected requests may have had some approvals
        count = faker.random().nextInt(gate.getMinApprovals());
      }
      // PENDING requests have 0 approvals

      counts.put(gate.getGate(), count);
    }

    return counts;
  }

  /**
   * Generates approval request creation timestamp (1-60 days ago).
   *
   * @return creation instant
   */
  private Instant generateCreatedAt() {
    long daysAgo = faker.number().numberBetween(1, 60);
    return Instant.now().minus(daysAgo, ChronoUnit.DAYS);
  }

  /**
   * Generates update timestamp based on status.
   * PENDING: Same as creation
   * APPROVED/REJECTED: 1 hour to 3 days after creation
   *
   * @param createdAt creation timestamp
   * @param status    approval status
   * @return update instant
   */
  private Instant generateUpdatedAt(Instant createdAt, ApprovalRequest.ApprovalStatus status) {
    if (status == ApprovalRequest.ApprovalStatus.PENDING) {
      return createdAt;
    }

    // Approved/rejected 1 hour to 3 days after creation
    long hoursToDecision = faker.number().numberBetween(1, 72);
    return createdAt.plus(hoursToDecision, ChronoUnit.HOURS);
  }

  /**
   * Generates request note explaining the request.
   *
   * @param service      service being requested
   * @param targetTeamId target team ID
   * @return request note
   */
  private String generateRequestNote(ApplicationService service, String targetTeamId) {
    List<String> templates = List.of(
        "Our team %s would like to take ownership of %s as we are the primary maintainers.",
        "Team %s is requesting ownership transfer for %s to align with our service responsibilities.",
        "%s service ownership should be assigned to team %s as per organizational restructure.",
        "Requesting %s assignment to team %s for better operational alignment.",
        "Team %s is best positioned to manage %s going forward.");

    String template = templates.get(faker.random().nextInt(templates.size()));
    return String.format(template, targetTeamId, service.getDisplayName());
  }
}
