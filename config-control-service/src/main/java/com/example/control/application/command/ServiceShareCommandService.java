package com.example.control.application.command;

import com.example.control.domain.id.ServiceShareId;
import com.example.control.domain.object.ServiceShare;
import com.example.control.domain.port.ServiceShareRepositoryPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/**
 * Command service for ServiceShare write operations.
 * <p>
 * Handles all write operations (save, update, delete) for ServiceShare domain
 * objects.
 * Responsible for CRUD, cache eviction, and transaction management.
 * Does NOT handle business logic, permission checks, or cross-domain
 * operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Validated
@Transactional
public class ServiceShareCommandService {

    private final ServiceShareRepositoryPort repository;

    /**
     * Saves a service share (create or update).
     * Automatically generates ID if null.
     * Evicts all service-shares cache entries.
     *
     * @param share the service share to save
     * @return the saved service share
     */
    @CacheEvict(value = "service-shares", allEntries = true)
    public ServiceShare save(@Valid ServiceShare share) {
        log.debug("Saving service share: {}", share.getId());

        if (share.getId() == null) {
            share.setId(ServiceShareId.of(UUID.randomUUID().toString()));
            log.debug("Generated new ID for service share: {}", share.getId());
        }

        ServiceShare saved = repository.save(share);
        log.info("Saved service share: {} for service: {}", saved.getId(), saved.getServiceId());
        return saved;
    }

    /**
     * Deletes a service share by ID.
     * Evicts all service-shares cache entries.
     *
     * @param id the service share ID to delete
     */
    @CacheEvict(value = "service-shares", allEntries = true)
    public void deleteById(ServiceShareId id) {
        log.info("Deleting service share: {}", id);
        repository.deleteById(id);
    }
}
