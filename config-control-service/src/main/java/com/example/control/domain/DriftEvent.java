package com.example.control.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain model representing a configuration drift detection event.
 * <p>
 * Drift events are generated when a service instance reports a configuration hash that differs
 * from the expected value provided by the control service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriftEvent {

  /** Unique identifier of the drift event (UUID or MongoDB ObjectId). */
  private String id;

  /** Name of the service where drift occurred. */
  private String serviceName;

  /** Instance identifier within the service. */
  private String instanceId;

  /** Service ID from ApplicationService (for team-based access control). */
  private String serviceId;

  /** Team ID that owns this service (from ApplicationService.ownerTeamId). */
  private String teamId;

  /** The expected configuration hash (from control server). */
  private String expectedHash;

  /** The actual configuration hash applied on the instance. */
  private String appliedHash;

  /** Drift severity classification. */
  private DriftSeverity severity;

  /** Current lifecycle state of the drift event. */
  private DriftStatus status;

  /** Timestamp when drift was first detected. */
  private Instant detectedAt;

  /** Timestamp when drift was resolved. */
  private Instant resolvedAt;

  /** Identifier (user/system) that detected the drift. */
  private String detectedBy;

  /** Identifier (user/system) that resolved the drift. */
  private String resolvedBy;

  /** Additional notes or investigation summary. */
  private String notes;

  /**
   * Severity levels assigned to configuration drift based on impact.
   */
  public enum DriftSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
  }

  /**
   * Possible states in the drift resolution lifecycle.
   */
  public enum DriftStatus {
    DETECTED, ACKNOWLEDGED, RESOLVING, RESOLVED, IGNORED
  }

  /**
   * Checks whether this drift event has been fully resolved.
   *
   * @return {@code true} if status == {@link DriftStatus#RESOLVED}, otherwise {@code false}
   */
  public boolean isResolved() {
    return status == DriftStatus.RESOLVED;
  }

  /**
   * Marks the drift event as resolved, updating resolution time and user.
   *
   * @param resolvedBy user or system identifier that resolved the drift
   */
  public void resolve(String resolvedBy) {
    this.status = DriftStatus.RESOLVED;
    this.resolvedAt = Instant.now();
    this.resolvedBy = resolvedBy;
  }
}
