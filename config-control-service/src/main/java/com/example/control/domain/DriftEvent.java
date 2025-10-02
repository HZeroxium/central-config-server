package com.example.control.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain model representing a configuration drift detection event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriftEvent {

  private String id;
  private String serviceName;
  private String instanceId;

  private String expectedHash;
  private String appliedHash;

  private DriftSeverity severity;
  private DriftStatus status;

  private LocalDateTime detectedAt;
  private LocalDateTime resolvedAt;

  private String detectedBy;
  private String resolvedBy;

  private String notes;

  public enum DriftSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  public enum DriftStatus {
    DETECTED,
    ACKNOWLEDGED,
    RESOLVING,
    RESOLVED,
    IGNORED
  }

  /**
   * Check if drift event is resolved.
   * 
   * @return true if resolved, false otherwise
   */
  public boolean isResolved() {
    return status == DriftStatus.RESOLVED;
  }

  /**
   * Mark drift event as resolved.
   * 
   * @param resolvedBy identifier of who/what resolved the drift
   */
  public void resolve(String resolvedBy) {
    this.status = DriftStatus.RESOLVED;
    this.resolvedAt = LocalDateTime.now();
    this.resolvedBy = resolvedBy;
  }
}
