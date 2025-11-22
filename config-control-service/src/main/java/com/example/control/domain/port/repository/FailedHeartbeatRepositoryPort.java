package com.example.control.domain.port.repository;

import com.example.control.domain.model.FailedHeartbeat;
import com.example.control.domain.criteria.FailedHeartbeatCriteria;
import com.example.control.domain.port.RepositoryPort;
import com.example.control.domain.valueobject.id.FailedHeartbeatId;

import java.util.List;
import java.util.Optional;

/**
 * Port (hexagonal architecture) for persisting and querying {@link FailedHeartbeat}.
 * <p>
 * Implementations reside in the infrastructure layer (e.g., MongoDB adapter).
 * </p>
 */
public interface FailedHeartbeatRepositoryPort
        extends RepositoryPort<FailedHeartbeat, FailedHeartbeatId, FailedHeartbeatCriteria> {

    /**
     * Find failed heartbeat by service name and instance ID.
     * <p>
     * Used for deduplication when processing DLQ messages.
     *
     * @param serviceName the service name
     * @param instanceId  the instance ID
     * @return the failed heartbeat if found, empty otherwise
     */
    Optional<FailedHeartbeat> findByServiceNameAndInstanceId(String serviceName, String instanceId);

    /**
     * Count failed heartbeats by status.
     *
     * @param status the status to count
     * @return the count of failed heartbeats with the given status
     */
    long countByStatus(FailedHeartbeat.FailedHeartbeatStatus status);

    /**
     * Bulk update status for multiple failed heartbeats.
     * <p>
     * Used for bulk operations like marking multiple items as RESOLVED or IGNORED.
     *
     * @param ids        list of failed heartbeat IDs to update
     * @param status     the new status
     * @param resolvedBy the user identifier who resolved them
     * @return number of failed heartbeats updated
     */
    long bulkUpdateStatus(List<FailedHeartbeatId> ids, FailedHeartbeat.FailedHeartbeatStatus status, String resolvedBy);
}

