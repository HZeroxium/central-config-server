package com.example.control.application.service;

import com.example.control.config.security.UserContext;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.criteria.ApplicationServiceCriteria;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.port.ApplicationServiceRepositoryPort;
import com.example.control.domain.port.DriftEventRepositoryPort;
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
    private final ServiceInstanceService serviceInstanceService;
    private final DriftEventRepositoryPort driftEventRepository;

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
    public Optional<ApplicationService> findById(ApplicationServiceId id) {
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
     * Find application service by display name.
     * <p>
     * This method provides O(1) lookup for service name resolution during heartbeat processing.
     * Used for auto-linking service instances to their corresponding application services.
     *
     * @param displayName the exact display name to search for
     * @return the application service if found, empty otherwise
     */
    @Cacheable(value = "application-services", key = "#displayName")
    public Optional<ApplicationService> findByDisplayName(String displayName) {
        log.debug("Finding application service by display name: {}", displayName);
        return repository.findByDisplayName(displayName);
    }

    /**
     * Find or create application service by display name.
     * <p>
     * If service exists, returns it. If not, creates an orphaned service with ownerTeamId=null.
     * This is used during heartbeat processing when a service instance registers but no
     * ApplicationService exists yet.
     *
     * @param displayName the exact display name to search for
     * @return the application service (existing or newly created orphaned)
     */
    @Transactional
    @CacheEvict(value = "application-services", allEntries = true)
    public ApplicationService findOrCreateByDisplayName(String displayName) {
        log.debug("Finding or creating application service by display name: {}", displayName);
        
        Optional<ApplicationService> existing = repository.findByDisplayName(displayName);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Create orphaned service (no owner team)
        ApplicationService orphanedService = ApplicationService.builder()
            .id(ApplicationServiceId.of(displayName.toLowerCase().replaceAll("[^a-z0-9-]", "-")))
            .displayName(displayName)
            .ownerTeamId(null) // Orphaned - requires approval workflow
            .environments(List.of("dev", "staging", "prod")) // Default environments
            .lifecycle(ApplicationService.ServiceLifecycle.ACTIVE)
            .createdAt(Instant.now())
            .createdBy("system") // System-created
            .build();
        
        ApplicationService saved = repository.save(orphanedService);
        log.warn("Auto-created orphaned ApplicationService: {} (displayName: {}) - requires approval workflow for team assignment", 
                 saved.getId(), displayName);
        
        return saved;
    }

    /**
     * Find application services owned by a specific team.
     *
     * @param ownerTeamId the team ID
     * @return list of services owned by the team
     */
    public List<ApplicationService> findByOwnerTeam(String ownerTeamId) {
        log.debug("Finding application services by owner team: {}", ownerTeamId);
        ApplicationServiceCriteria criteria = ApplicationServiceCriteria.builder()
            .ownerTeamId(ownerTeamId)
            .build();
        return repository.findAll(criteria, Pageable.unpaged()).getContent();
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
    public Page<ApplicationService> findAll(ApplicationServiceCriteria criteria, 
                                        Pageable pageable, 
                                        UserContext userContext) {
        log.debug("Listing application services with criteria: {}, pageable: {}", criteria, pageable);
        return repository.findAll(criteria, pageable);
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
    public void delete(ApplicationServiceId id, UserContext userContext) {
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
        ApplicationServiceCriteria criteria = ApplicationServiceCriteria.builder()
            .ownerTeamId(ownerTeamId)
            .build();
        return repository.count(criteria);
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

    /**
     * Get list of serviceIds that the user has access to through team ownership or sharing.
     * <p>
     * This method pre-computes accessible services to optimize database queries
     * by avoiding the need to check permissions for each individual entity.
     *
     * @param userContext the user context
     * @return list of accessible service IDs
     */
    public List<String> findAccessibleServiceIds(UserContext userContext) {
        log.debug("Getting accessible serviceIds for user: {}", userContext.getUserId());
        
        List<String> accessibleServiceIds = new java.util.ArrayList<>();
        
        // Get team-owned services
        if (userContext.getTeamIds() != null && !userContext.getTeamIds().isEmpty()) {
            for (String teamId : userContext.getTeamIds()) {
                List<ApplicationService> teamServices = findByOwnerTeam(teamId);
                List<String> teamServiceIds = teamServices.stream()
                        .map(service -> service.getId().id())
                        .collect(java.util.stream.Collectors.toList());
                accessibleServiceIds.addAll(teamServiceIds);
            }
        }
        
        // TODO: Get shared services (services shared with user's teams or directly with user)
        // This would require integration with ServiceShareService to find services
        // that have been shared with the user's teams or directly with the user
        
        log.debug("Found {} accessible serviceIds for user: {}", accessibleServiceIds.size(), userContext.getUserId());
        return accessibleServiceIds;
    }

    /**
     * Transfer ownership of an application service and cascade the change to all related entities.
     * <p>
     * This method updates the ApplicationService ownerTeamId and then synchronizes
     * the teamId field across all related ServiceInstances and DriftEvents.
     * This ensures data consistency and proper ABAC filtering.
     *
     * @param serviceId the service ID to transfer
     * @param newTeamId the new team ID to assign
     * @param userContext the user context for audit purposes
     * @return the updated application service
     * @throws IllegalArgumentException if service not found
     * @throws IllegalStateException if user lacks permission
     */
    @Transactional
    @CacheEvict(value = "application-services", allEntries = true)
    public ApplicationService transferOwnershipWithCascade(String serviceId, String newTeamId, UserContext userContext) {
        log.info("Transferring ownership of service {} to team {} by user {}", 
                serviceId, newTeamId, userContext.getUserId());

        // Get the service
        ApplicationService service = repository.findById(ApplicationServiceId.of(serviceId))
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        // Check permissions
        if (!canEditService(userContext, service)) {
            throw new IllegalStateException("User does not have permission to transfer ownership of this service");
        }

        // Update the service
        String oldTeamId = service.getOwnerTeamId();
        service.setOwnerTeamId(newTeamId);
        service.setUpdatedAt(Instant.now());
        
        ApplicationService updatedService = repository.save(service);
        log.info("Updated ApplicationService {} ownerTeamId from {} to {}", 
                serviceId, oldTeamId, newTeamId);

        // Cascade to ServiceInstances
        long instanceCount = serviceInstanceService.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);
        log.info("Updated {} service instances for service {}", instanceCount, serviceId);

        // Cascade to DriftEvents
        long driftEventCount = driftEventRepository.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);
        log.info("Updated {} drift events for service {}", driftEventCount, serviceId);

        log.info("Successfully transferred ownership of service {} to team {} (instances: {}, drift events: {})", 
                serviceId, newTeamId, instanceCount, driftEventCount);

        return updatedService;
    }
}
