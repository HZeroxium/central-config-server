package com.example.control.infrastructure.adapter.persistence.mongo.repository;

import com.example.control.infrastructure.adapter.persistence.mongo.documents.FailedHeartbeatDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link FailedHeartbeatDocument}.
 * <p>
 * Provides basic CRUD operations and custom queries for failed heartbeat documents.
 * </p>
 */
public interface FailedHeartbeatMongoRepository extends MongoRepository<FailedHeartbeatDocument, String> {

    /**
     * Find failed heartbeat by service name and instance ID.
     * <p>
     * Used for deduplication when processing DLQ messages.
     *
     * @param serviceName the service name
     * @param instanceId  the instance ID
     * @return the failed heartbeat document if found, empty otherwise
     */
    @Query("{'serviceName': ?0, 'instanceId': ?1}")
    Optional<FailedHeartbeatDocument> findByServiceNameAndInstanceId(String serviceName, String instanceId);

    /**
     * Count failed heartbeats by status.
     *
     * @param status the status to count (as string)
     * @return the count of failed heartbeats with the given status
     */
    @Query(value = "{'status': ?0}", count = true)
    long countByStatus(String status);
}

