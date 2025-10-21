package com.example.control.infrastructure.mongo.repository;

import com.example.control.infrastructure.mongo.documents.DriftEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing {@link DriftEventDocument} entities.
 * <p>
 * Provides data access operations for drift detection, tracking, and reporting.
 */
@Repository
public interface DriftEventMongoRepository extends MongoRepository<DriftEventDocument, String> {

  /**
   * Retrieves all drift events associated with a specific service.
   *
   * @param serviceName the service name
   * @return list of drift events
   */
  List<DriftEventDocument> findByServiceName(String serviceName);

  /**
   * Retrieves all drift events for a specific service instance.
   *
   * @param serviceName the service name
   * @param instanceId  the instance ID
   * @return list of drift events
   */
  List<DriftEventDocument> findByServiceNameAndInstanceId(String serviceName, String instanceId);

  /**
   * Finds all drift events that are not yet resolved.
   *
   * @return list of unresolved drift events
   */
  @Query("{ 'status': { $in: ['DETECTED', 'ACKNOWLEDGED', 'RESOLVING'] } }")
  List<DriftEventDocument> findUnresolvedEvents();

  /**
   * Finds unresolved drift events for a specific service.
   *
   * @param serviceName target service name
   * @return list of unresolved drift events for the given service
   */
  @Query("{ 'serviceName': ?0, 'status': { $in: ['DETECTED', 'ACKNOWLEDGED', 'RESOLVING'] } }")
  List<DriftEventDocument> findUnresolvedEventsByService(String serviceName);

  /**
   * Finds all drift events detected between two timestamps.
   *
   * @param start start timestamp
   * @param end   end timestamp
   * @return list of drift events within the time range
   */
  @Query("{ 'detectedAt': { $gte: ?0, $lte: ?1 } }")
  List<DriftEventDocument> findByDetectedAtBetween(java.time.Instant start, java.time.Instant end);

  /**
   * Finds all drift events by severity level.
   *
   * @param severity severity level (LOW, MEDIUM, HIGH, CRITICAL)
   * @return list of drift events matching the severity
   */
  @Query("{ 'severity': ?0 }")
  List<DriftEventDocument> findBySeverity(String severity);

  /**
   * Counts all drift events belonging to a service.
   *
   * @param serviceName service name
   * @return total drift event count for that service
   */
  long countByServiceName(String serviceName);

  /**
   * Counts all drift events matching a specific status.
   *
   * @param status drift event status (DETECTED, RESOLVED, etc.)
   * @return total count
   */
  long countByStatus(String status);
}
