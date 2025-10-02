package com.example.control.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceInstanceRepository extends MongoRepository<ServiceInstanceDocument, String> {

  List<ServiceInstanceDocument> findByServiceName(String serviceName);

  Optional<ServiceInstanceDocument> findByServiceNameAndInstanceId(String serviceName, String instanceId);

  @Query("{ 'hasDrift': true }")
  List<ServiceInstanceDocument> findAllWithDrift();

  @Query("{ 'serviceName': ?0, 'hasDrift': true }")
  List<ServiceInstanceDocument> findByServiceNameWithDrift(String serviceName);

  @Query("{ 'lastSeenAt': { $lt: ?0 } }")
  List<ServiceInstanceDocument> findStaleInstances(LocalDateTime threshold);

  @Query("{ 'status': ?0 }")
  List<ServiceInstanceDocument> findByStatus(String status);

  long countByServiceName(String serviceName);
}
