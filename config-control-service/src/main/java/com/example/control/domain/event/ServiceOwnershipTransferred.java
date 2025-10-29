package com.example.control.domain.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Domain event published when service ownership is transferred to a new team.
 * <p>
 * This event triggers cascading updates to related entities (ServiceInstance,
 * DriftEvent)
 * to maintain data consistency across aggregates.
 * </p>
 *
 * @param serviceId     the service ID that was transferred
 * @param oldTeamId     the previous owner team ID (null for orphaned services)
 * @param newTeamId     the new owner team ID
 * @param transferredBy the user who initiated the transfer
 * @param transferredAt timestamp when the transfer occurred
 */
@Data
@Builder
public class ServiceOwnershipTransferred {

    private final String serviceId;
    private final String oldTeamId;
    private final String newTeamId;
    private final String transferredBy;
    private final Instant transferredAt;
}
