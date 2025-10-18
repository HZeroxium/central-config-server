package com.example.control.application.service;

import com.example.control.config.security.UserContext;
import com.example.control.domain.ApplicationService;
import com.example.control.domain.port.ApplicationServiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Application service for managing application services.
 * <p>
 * Provides business logic for CRUD operations on application services with
 * team-based access control and caching support.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationServiceService {

    private final ApplicationServiceRepositoryPort repository;

    /**
     * Save or update an application service.
     * <p>
     * Validates ownership and updates timestamps automatically.
     *
     * @param service the application service to save
     * @param userContext the current user context
     * @return the saved application service
     */
    @Transactional
    @CacheEvict(value = "application-services", allEntries = true)
    public ApplicationService save(ApplicationService service, UserContext userContext) {
        log.info("Saving application service: {} by user: {}", service.getId(), userContext.getUserId());

        // Set audit fields
        if (service.getCreatedAt() == null) {
            service.setCreatedAt(Instant.now());
            service.setCreatedBy(userContext.getUserId());
        }
        service.setUpdatedAt(Instant.now());

        // Validate ownership for updates
        if (service.getId() != null) {
            Optional<ApplicationService> existing = repository.findById(service.getId());
            if (existing.isPresent() && !canEditService(userContext, existing.get())) {
                throw new IllegalStateException("User does not have permission to edit this service");
            }
        }

        ApplicationService saved = repository.save(service);
        log.info("Successfully saved application service: {}", saved.getId());
        return saved;
    }

    /**
     * Find an application service by ID.
     * <p>
     * Application services are public - anyone can view them.
     *
     * @param id the service ID
     * @return optional application service
     */
    @Cacheable(value = "application-services", key = "#id")
    public Optional<ApplicationService> findById(String id) {
        log.debug("Finding application service by ID: {}", id);
        return repository.findById(id);
    }

    /**
     * Find all application services.
     * <p>
     * Application services are public - anyone can view them.
     *
     * @return list of all application services
     */
    @Cacheable(value = "application-services", key = "'all'")
    public List<ApplicationService> findAll() {
        log.debug("Finding all application services");
        return repository.findAll(null, Pageable.unpaged()).getContent();
    }

    /**
     * Find application services owned by a specific team.
     *
     * @param ownerTeamId the team ID
     * @return list of services owned by the team
     */
    public List<ApplicationService> findByOwnerTeam(String ownerTeamId) {
        log.debug("Finding application services by owner team: {}", ownerTeamId);
        ApplicationServiceRepositoryPort.ApplicationServiceFilter filter = 
            new ApplicationServiceRepositoryPort.ApplicationServiceFilter(ownerTeamId, null, null, null, null);
        return repository.findAll(filter, Pageable.unpaged()).getContent();
    }

    /**
     * List application services with filtering and pagination.
     * <p>
     * Application services are public - no team filtering required.
     *
     * @param filter the filter criteria
     * @param pageable pagination information
     * @param userContext the current user context (for future use)
     * @return page of application services
     */
    public Page<ApplicationService> list(ApplicationServiceRepositoryPort.ApplicationServiceFilter filter, 
                                        Pageable pageable, 
                                        UserContext userContext) {
        log.debug("Listing application services with filter: {}, pageable: {}", filter, pageable);
        return repository.findAll(filter, pageable);
    }

    /**
     * Delete an application service.
     * <p>
     * Only system admins can delete services.
     *
     * @param id the service ID to delete
     * @param userContext the current user context
     */
    @Transactional
    @CacheEvict(value = "application-services", allEntries = true)
    public void delete(String id, UserContext userContext) {
        log.info("Deleting application service: {} by user: {}", id, userContext.getUserId());

        Optional<ApplicationService> service = repository.findById(id);
        if (service.isEmpty()) {
            throw new IllegalArgumentException("Application service not found: " + id);
        }

        // Only system admins can delete services
        if (!userContext.isSysAdmin()) {
            throw new IllegalStateException("Only system administrators can delete services");
        }

        repository.deleteById(id);
        log.info("Successfully deleted application service: {}", id);
    }

    /**
     * Count application services by owner team.
     *
     * @param ownerTeamId the team ID
     * @return number of services owned by the team
     */
    public long countByOwnerTeam(String ownerTeamId) {
        log.debug("Counting application services by owner team: {}", ownerTeamId);
        ApplicationServiceRepositoryPort.ApplicationServiceFilter filter = 
            new ApplicationServiceRepositoryPort.ApplicationServiceFilter(ownerTeamId, null, null, null, null);
        return repository.count(filter);
    }

    /**
     * Check if user can edit a service.
     * <p>
     * System admins can edit any service, team members can edit services owned by their team.
     *
     * @param userContext the user context
     * @param service the application service
     * @return true if user can edit the service
     */
    private boolean canEditService(UserContext userContext, ApplicationService service) {
        // System admins can edit any service
        if (userContext.isSysAdmin()) {
            return true;
        }

        // Team members can edit services owned by their team
        return userContext.isMemberOfTeam(service.getOwnerTeamId());
    }
}
