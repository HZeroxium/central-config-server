package com.example.control.infrastructure.mongo.repository;

import com.example.control.infrastructure.mongo.documents.ServiceInstanceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link ServiceInstanceDocument} entities.
 * <p>
 * Provides convenience methods and custom MongoDB queries for instance discovery,
 * drift detection, and lifecycle tracking.
 */
@Repository
public interface ServiceInstanceMongoRepository extends MongoRepository<ServiceInstanceDocument, String> {

    /**
   * Counts the total number of instances for a specific service.
   *
   * @param serviceName service name to count
   * @return number of instances belonging to the service
   */
  long countByServiceName(String serviceName);
}
