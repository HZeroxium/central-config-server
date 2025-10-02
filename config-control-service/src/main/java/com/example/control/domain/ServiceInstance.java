package com.example.control.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain model representing a service instance in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInstance {

  private String serviceName;
  private String instanceId;
  private String host;
  private Integer port;
  private String environment;
  private String version;

  private String configHash;
  private String lastAppliedHash;

  private InstanceStatus status;
  private LocalDateTime lastSeenAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private Map<String, String> metadata;

  private Boolean hasDrift;
  private LocalDateTime driftDetectedAt;

  public enum InstanceStatus {
    HEALTHY,
    UNHEALTHY,
    DRIFT,
    UNKNOWN
  }

  /**
   * Check if instance has configuration drift.
   * 
   * @return true if drift detected, false otherwise
   */
  public boolean isDrifted() {
    return Boolean.TRUE.equals(hasDrift);
  }

  /**
   * Mark instance as having configuration drift.
   * 
   * @param expectedHash the expected configuration hash from SoT
   */
  public void markDrift(String expectedHash) {
    this.hasDrift = true;
    this.driftDetectedAt = LocalDateTime.now();
    this.status = InstanceStatus.DRIFT;
    this.configHash = expectedHash;
  }

  /**
   * Clear drift flag and restore healthy status if applicable.
   */
  public void clearDrift() {
    this.hasDrift = false;
    this.driftDetectedAt = null;
    if (this.status == InstanceStatus.DRIFT) {
      this.status = InstanceStatus.HEALTHY;
    }
  }
}
