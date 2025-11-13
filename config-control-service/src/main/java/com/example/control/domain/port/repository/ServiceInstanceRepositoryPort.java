package com.example.control.domain.port.repository;

import com.example.control.domain.model.ServiceInstance;
import com.example.control.domain.criteria.ServiceInstanceCriteria;
import com.example.control.domain.port.RepositoryPort;
import com.example.control.domain.valueobject.id.ServiceInstanceId;
import com.mongodb.bulk.BulkWriteResult;

import java.util.List;
import java.util.Set;

/**
 * Port (hexagonal architecture) for persisting and querying
 * {@link ServiceInstance}.
 * Implementations reside in the infrastructure layer (e.g., MongoDB adapter).
 */
public interface ServiceInstanceRepositoryPort
        extends RepositoryPort<ServiceInstance, ServiceInstanceId, ServiceInstanceCriteria> {

    /**
     * Count instances by service ID.
     */
    long countByServiceId(String serviceId);

    /**
     * Bulk update teamId for all service instances with the given serviceId.
     * <p>
     * Used during ownership transfer to ensure all instances are updated
     * to reflect the new team ownership.
     *
     * @param serviceId the service ID to match
     * @param newTeamId the new team ID to set
     * @return number of instances updated
     */
    long bulkUpdateTeamIdByServiceId(String serviceId, String newTeamId);

    /**
     * Bulk upsert service instances.
     * <p>
     * Efficiently upserts multiple service instances in a single MongoDB bulk operation.
     * Used for batch heartbeat processing to reduce write overhead.
     *
     * @param instances list of service instances to upsert
     * @return bulk write result with counts of inserted/updated documents
     */
    BulkWriteResult bulkUpsert(List<ServiceInstance> instances);

    /**
     * Find all service instances by their IDs.
     * <p>
     * Efficiently loads multiple instances in a single query for batch processing.
     *
     * @param ids set of service instance IDs to load
     * @return list of service instances (may be smaller than input if some don't exist)
     */
    List<ServiceInstance> findAllByIds(Set<ServiceInstanceId> ids);
}
