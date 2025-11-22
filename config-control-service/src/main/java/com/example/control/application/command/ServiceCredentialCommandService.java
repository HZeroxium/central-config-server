package com.example.control.application.command;

import com.example.control.domain.model.ServiceCredential;
import com.example.control.domain.port.repository.ServiceCredentialRepositoryPort;
import com.example.control.domain.valueobject.id.ServiceCredentialId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/**
 * Command service for ServiceCredential write operations.
 * <p>
 * Handles all write operations (save, delete) for ServiceCredential domain objects.
 * Responsible for CRUD and transaction management.
 * Does NOT handle business logic, permission checks, or cross-domain operations.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Validated
@Transactional
public class ServiceCredentialCommandService {

    private final ServiceCredentialRepositoryPort repository;

    /**
     * Saves a service credential (create or update).
     * Automatically generates ID if null.
     *
     * @param credential the service credential to save
     * @return the saved service credential
     */
    public ServiceCredential save(@Valid ServiceCredential credential) {
        log.debug("Saving service credential: {}", credential.getId());

        if (credential.getId() == null) {
            credential.setId(ServiceCredentialId.of(UUID.randomUUID().toString()));
            log.debug("Generated new ID for service credential: {}", credential.getId());
        }

        ServiceCredential saved = repository.save(credential);
        log.info("Saved service credential: {} for service: {}", 
                saved.getId(), saved.getServiceId());

        return saved;
    }

    /**
     * Deletes a service credential by ID.
     *
     * @param id the service credential ID to delete
     */
    public void deleteById(ServiceCredentialId id) {
        log.info("Deleting service credential: {}", id);
        repository.deleteById(id);
        log.info("Deleted service credential: {}", id);
    }
}

