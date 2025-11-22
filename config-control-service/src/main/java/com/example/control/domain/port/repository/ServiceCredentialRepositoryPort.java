package com.example.control.domain.port.repository;

import com.example.control.domain.model.ServiceCredential;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.valueobject.id.ServiceCredentialId;

import java.util.Optional;

/**
 * Port (hexagonal architecture) for persisting and querying
 * {@link ServiceCredential}.
 * <p>
 * Provides CRUD operations for service credentials used in M2M authentication.
 * </p>
 */
public interface ServiceCredentialRepositoryPort {

    /**
     * Save a service credential (create or update).
     *
     * @param credential the credential to save
     * @return the saved credential
     */
    ServiceCredential save(ServiceCredential credential);

    /**
     * Find service credential by ID.
     *
     * @param id the credential ID
     * @return optional credential if found
     */
    Optional<ServiceCredential> findById(ServiceCredentialId id);

    /**
     * Find service credential by ApplicationService ID.
     * <p>
     * There should be at most one credential per service (unique constraint).
     *
     * @param serviceId the application service ID
     * @return optional credential if found
     */
    Optional<ServiceCredential> findByServiceId(ApplicationServiceId serviceId);

    /**
     * Find service credential by Keycloak client ID.
     * <p>
     * Used during heartbeat authentication to lookup credential from JWT claim.
     *
     * @param keycloakClientId the Keycloak client ID
     * @return optional credential if found
     */
    Optional<ServiceCredential> findByKeycloakClientId(String keycloakClientId);

    /**
     * Check if credentials exist for a service.
     *
     * @param serviceId the application service ID
     * @return true if credentials exist, false otherwise
     */
    boolean existsByServiceId(ApplicationServiceId serviceId);

    /**
     * Delete service credential by ID.
     *
     * @param id the credential ID to delete
     */
    void deleteById(ServiceCredentialId id);
}

