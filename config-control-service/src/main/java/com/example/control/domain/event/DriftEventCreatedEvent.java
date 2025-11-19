package com.example.control.domain.event;

import com.example.control.domain.model.DriftEvent;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Domain event published when a DriftEvent is created (after threshold is reached).
 * <p>
 * This event triggers email notifications to SYS_ADMIN users and service owner team members.
 * </p>
 *
 * @param driftEventId  the drift event ID
 * @param serviceName    the service name where drift occurred
 * @param instanceId     the instance ID where drift occurred
 * @param serviceId      the service ID (from ApplicationService)
 * @param teamId         the team ID that owns the service
 * @param environment    the environment where drift occurred
 * @param expectedHash   the expected configuration hash
 * @param appliedHash    the applied configuration hash
 * @param severity       the drift severity
 * @param detectedAt     timestamp when drift was detected
 */
@Data
@Builder
public class DriftEventCreatedEvent {

  private final String driftEventId;
  private final String serviceName;
  private final String instanceId;
  private final String serviceId;
  private final String teamId;
  private final String environment;
  private final String expectedHash;
  private final String appliedHash;
  private final DriftEvent.DriftSeverity severity;
  private final Instant detectedAt;
}

