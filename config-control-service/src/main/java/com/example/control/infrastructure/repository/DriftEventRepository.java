package com.example.control.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DriftEventRepository extends MongoRepository<DriftEventDocument, String> {

  List<DriftEventDocument> findByServiceName(String serviceName);

  List<DriftEventDocument> findByServiceNameAndInstanceId(String serviceName, String instanceId);

  @Query("{ 'status': { $in: ['DETECTED', 'ACKNOWLEDGED', 'RESOLVING'] } }")
  List<DriftEventDocument> findUnresolvedEvents();

  @Query("{ 'serviceName': ?0, 'status': { $in: ['DETECTED', 'ACKNOWLEDGED', 'RESOLVING'] } }")
  List<DriftEventDocument> findUnresolvedEventsByService(String serviceName);

  @Query("{ 'detectedAt': { $gte: ?0, $lte: ?1 } }")
  List<DriftEventDocument> findByDetectedAtBetween(LocalDateTime start, LocalDateTime end);

  @Query("{ 'severity': ?0 }")
  List<DriftEventDocument> findBySeverity(String severity);

  long countByServiceName(String serviceName);

  long countByStatus(String status);
}
