package com.example.control.infrastructure.adapter.persistence.mongo.repository;

import com.example.control.infrastructure.adapter.persistence.mongo.documents.ServiceCredentialDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link ServiceCredentialDocument}.
 * <p>
 * Provides basic CRUD operations and custom queries for service credentials.
 * </p>
 */
@Repository
public interface ServiceCredentialMongoRepository extends MongoRepository<ServiceCredentialDocument, String> {

    /**
     * Find service credential by ApplicationService ID.
     * <p>
     * There should be at most one credential per service (unique constraint).
     *
     * @param serviceId the application service ID
     * @return the credential if found, empty otherwise
     */
    Optional<ServiceCredentialDocument> findByServiceId(String serviceId);

    /**
     * Find service credential by Keycloak client ID.
     * <p>
     * Used during heartbeat authentication to lookup credential from JWT claim.
     * Should be indexed for optimal performance.
     *
     * @param keycloakClientId the Keycloak client ID
     * @return the credential if found, empty otherwise
     */
    Optional<ServiceCredentialDocument> findByKeycloakClientId(String keycloakClientId);

    /**
     * Check if credentials exist for a service.
     *
     * @param serviceId the application service ID
     * @return true if credentials exist, false otherwise
     */
    boolean existsByServiceId(String serviceId);
}

