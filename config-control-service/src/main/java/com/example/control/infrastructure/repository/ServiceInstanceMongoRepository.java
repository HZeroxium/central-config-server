package com.example.control.infrastructure.repository;

import com.example.control.infrastructure.repository.documents.ServiceInstanceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link ServiceInstanceDocument} entities.
 * <p>
 * Provides convenience methods and custom MongoDB queries for instance discovery,
 * drift detection, and lifecycle tracking.
 */
@Repository
public interface ServiceInstanceMongoRepository extends MongoRepository<ServiceInstanceDocument, String> {

  /**
   * Finds all service instances by service name.
   *
   * @param serviceName name of the service
   * @return list of instances belonging to the given service
   */
  List<ServiceInstanceDocument> findByServiceName(String serviceName);

  /**
   * Finds a specific service instance by service name and instance ID.
   *
   * @param serviceName the service name
   * @param instanceId  the instance identifier
   * @return an {@link Optional} containing the instance if found
   */
  Optional<ServiceInstanceDocument> findByServiceNameAndInstanceId(String serviceName, String instanceId);

  /**
   * Finds all instances currently marked as drifted (hasDrift = true).
   *
   * @return list of drifted instances
   */
  @Query("{ 'hasDrift': true }")
  List<ServiceInstanceDocument> findAllWithDrift();

  /**
   * Finds all drifted instances for a specific service.
   *
   * @param serviceName service name to filter
   * @return list of drifted instances for the service
   */
  @Query("{ 'serviceName': ?0, 'hasDrift': true }")
  List<ServiceInstanceDocument> findByServiceNameWithDrift(String serviceName);

  /**
   * Finds instances that have not sent heartbeat updates after the given threshold.
   *
   * @param threshold timestamp defining staleness
   * @return list of stale (inactive) instances
   */
  @Query("{ 'lastSeenAt': { $lt: ?0 } }")
  List<ServiceInstanceDocument> findStaleInstances(java.time.Instant threshold);

  /**
   * Finds all instances with the given status.
   *
   * @param status instance status (e.g., HEALTHY, UNHEALTHY, DRIFT)
   * @return list of instances matching the given status
   */
  @Query("{ 'status': ?0 }")
  List<ServiceInstanceDocument> findByStatus(String status);

  /**
   * Counts the total number of instances for a specific service.
   *
   * @param serviceName service name to count
   * @return number of instances belonging to the service
   */
  long countByServiceName(String serviceName);
}
