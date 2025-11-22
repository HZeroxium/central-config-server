package com.example.control.application.query;

import com.example.control.domain.model.ServiceCredential;
import com.example.control.domain.port.repository.ServiceCredentialRepositoryPort;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.valueobject.id.ServiceCredentialId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Query service for ServiceCredential read operations.
 * <p>
 * Provides read-only access to ServiceCredential data.
 * This service depends only on Repository Ports to avoid circular dependencies.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceCredentialQueryService {

    private final ServiceCredentialRepositoryPort repository;

    /**
     * Find service credential by ID.
     *
     * @param id the credential ID
     * @return optional service credential
     */
    public Optional<ServiceCredential> findById(ServiceCredentialId id) {
        log.debug("Finding service credential by ID: {}", id);
        return repository.findById(id);
    }

    /**
     * Find service credential by ApplicationService ID.
     * <p>
     * There should be at most one credential per service (unique constraint).
     *
     * @param serviceId the application service ID
     * @return optional service credential if found
     */
    public Optional<ServiceCredential> findByServiceId(ApplicationServiceId serviceId) {
        log.debug("Finding service credential by service ID: {}", serviceId);
        return repository.findByServiceId(serviceId);
    }

    /**
     * Find service credential by Keycloak client ID.
     * <p>
     * Used during heartbeat authentication to lookup credential from JWT claim.
     *
     * @param keycloakClientId the Keycloak client ID
     * @return optional service credential if found
     */
    public Optional<ServiceCredential> findByKeycloakClientId(String keycloakClientId) {
        log.debug("Finding service credential by Keycloak client ID: {}", keycloakClientId);
        return repository.findByKeycloakClientId(keycloakClientId);
    }

    /**
     * Check if credentials exist for a service.
     *
     * @param serviceId the application service ID
     * @return true if credentials exist, false otherwise
     */
    public boolean existsByServiceId(ApplicationServiceId serviceId) {
        log.debug("Checking existence of service credential for service ID: {}", serviceId);
        return repository.existsByServiceId(serviceId);
    }
}

