package com.example.control.application.command;

import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.port.repository.ApplicationServiceRepositoryPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/**
 * Command service for ApplicationService write operations.
 * <p>
 * Handles all write operations (save, update, delete) for ApplicationService
 * domain objects.
 * Responsible for CRUD, cache eviction, and transaction management.
 * Does NOT handle business logic, permission checks, or cross-domain
 * operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Validated
@Transactional
public class ApplicationServiceCommandService {

    private final ApplicationServiceRepositoryPort repository;

    /**
     * Saves an application service (create or update).
     * Automatically generates ID if null.
     * Evicts specific application-services cache entry by ID.
     *
     * @param service the application service to save
     * @return the saved application service
     */
    @CacheEvict(value = "application-services", key = "#service.id")
    public ApplicationService save(@Valid ApplicationService service) {
        log.debug("Saving application service: {}", service.getId());

        if (service.getId() == null) {
            service.setId(ApplicationServiceId.of(UUID.randomUUID().toString()));
            log.debug("Generated new ID for application service: {}", service.getId());
        }

        ApplicationService saved = repository.save(service);
        log.info("Saved application service: {} (displayName: {})", saved.getId(), saved.getDisplayName());
        return saved;
    }

    /**
     * Deletes an application service by ID.
     * Evicts specific application-services cache entry by ID.
     *
     * @param id the application service ID to delete
     */
    @CacheEvict(value = "application-services", key = "#id")
    public void deleteById(ApplicationServiceId id) {
        log.info("Deleting application service: {}", id);
        repository.deleteById(id);
    }
}
