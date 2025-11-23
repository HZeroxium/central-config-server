package com.example.control.application.service;

import com.example.control.application.command.ApplicationServiceCommandService;
import com.example.control.application.command.DriftEventCommandService;
import com.example.control.application.command.ServiceInstanceCommandService;
import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.ServiceShareQueryService;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.infrastructure.observability.MetricsNames;
import com.example.control.domain.criteria.ApplicationServiceCriteria;
import com.example.control.domain.criteria.ServiceShareCriteria;
import com.example.control.domain.event.ServiceOwnershipTransferred;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.ServiceShare;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrator service for managing ApplicationService entities.
 * <p>
 * Coordinates between Command/QueryServices for complex business operations.
 * Handles ownership transfer with cascading updates across related entities.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationServiceService {

    private final ApplicationServiceCommandService commandService;
    private final ApplicationServiceQueryService queryService;
    private final ServiceShareQueryService serviceShareQueryService;
    private final ServiceInstanceCommandService serviceInstanceCommandService;
    private final DriftEventCommandService driftEventCommandService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Save or update an application service.
     * <p>
     * Business logic: Initializes timestamps and ID if needed, validates
     * permissions,
     * applies default environments if needed, then delegates to CommandService.
     *
     * @param service     the application service to save
     * @param userContext the current user context
     * @return the saved application service
     * @throws IllegalStateException if user lacks permission to create orphaned
     *                               service
     */
    @Transactional
    @Observed(name = MetricsNames.ApplicationService.SAVE, contextualName = "application-service-save", lowCardinalityKeyValues = {
            "operation", "save" })
    public ApplicationService save(ApplicationService service, UserContext userContext) {
        log.info("Orchestrating save for application service: {} by user: {}", service.getId(),
                userContext.getUserId());

        // Business logic: Permission check for orphaned services
        // Any authenticated user can create orphaned services (ownerTeamId=null)
        // Orphaned services require approval workflow to assign ownership
        if (service.getOwnerTeamId() == null) {
            // Validate user is authenticated
            if (userContext.getUserId() == null || userContext.getUserId().isBlank()) {
                throw new IllegalStateException(
                        "User must be authenticated to create services");
            }
            log.debug("Creating orphaned service by user: {} (will require approval workflow)", 
                    userContext.getUserId());
        } else {
            // Business logic: Team members can only create services for their own team
            if (!userContext.isSysAdmin()) {
                List<String> userTeamIds = userContext.getTeamIds();
                if (userTeamIds == null || userTeamIds.isEmpty()) {
                    throw new IllegalStateException(
                            String.format(
                                    "User %s has no team membership and cannot create services for a team. " +
                                    "Create an orphaned service (ownerTeamId=null) and request ownership via approval workflow.",
                                    userContext.getUserId()));
                }
                if (!userTeamIds.contains(service.getOwnerTeamId())) {
                    throw new IllegalStateException(
                            String.format(
                                    "User %s cannot create service for team %s. Users can only create services for their own teams. " +
                                    "Create an orphaned service (ownerTeamId=null) and request ownership via approval workflow.",
                                    userContext.getUserId(), service.getOwnerTeamId()));
                }
            }
        }

        // Business logic: Generate ID if null
        if (service.getId() == null) {
            service.setId(ApplicationServiceId.of(UUID.randomUUID().toString()));
        }

        // Business logic: Initialize environments to empty list if null (user must explicitly choose)
        if (service.getEnvironments() == null) {
            service.setEnvironments(List.of());
            log.debug("Initialized empty environments list for service: {} (user must explicitly choose environments)", service.getId());
        }

        // Business logic: Initialize timestamps
        if (service.getCreatedAt() == null) {
            service.setCreatedAt(Instant.now());
            service.setCreatedBy(userContext.getUserId());
        }
        service.setUpdatedAt(Instant.now());

        ApplicationService saved = commandService.save(service);
        log.info("Successfully saved application service: {}", saved.getId());
        return saved;
    }

    /**
     * Find an application service by ID.
     * <p>
     * Delegates to ApplicationServiceQueryService for read operations.
     *
     * @param id the service ID
     * @return optional application service
     */
    public Optional<ApplicationService> findById(ApplicationServiceId id) {
        log.debug("Finding application service by ID: {}", id);
        return queryService.findById(id);
    }

    /**
     * Find all application services.
     * <p>
     * Delegates to ApplicationServiceQueryService for read operations.
     *
     * @return list of all application services
     */
    public List<ApplicationService> findAll() {
        log.debug("Finding all application services");
        return queryService.findAll();
    }

    /**
     * Find or create application service by display name.
     * <p>
     * <strong>DEPRECATED:</strong> This method auto-creates orphan services, which is no longer allowed.
     * Services must be registered via Admin Dashboard first.
     * </p>
     * <p>
     * If service exists, returns it. If not, creates an orphaned service with
     * ownerTeamId=null.
     * This method is deprecated because it violates the new workflow where services
     * must be explicitly registered and approved before heartbeats can be processed.
     * </p>
     *
     * @param displayName the exact display name to search for
     * @return the application service (existing or newly created orphaned)
     * @deprecated Use {@link #findByDisplayName(String)} instead. Services must be registered via Admin Dashboard.
     *             Auto-creation will be removed in a future version.
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    @Transactional
    public ApplicationService findOrCreateByDisplayName(String displayName) {
        log.warn(
                "DEPRECATED: findOrCreateByDisplayName() called for displayName: {}. " +
                        "This method will be removed. Services must be registered via Admin Dashboard first.",
                displayName);

        ApplicationServiceCriteria criteria = ApplicationServiceCriteria.byDisplayName(displayName);
        Page<ApplicationService> results = queryService.findAll(criteria, Pageable.unpaged());
        Optional<ApplicationService> existing = results.getContent().stream().findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }

        // Business logic: Create orphaned service (no owner team) - generate UUID
        // NOTE: This is deprecated behavior - will be removed
        ApplicationService orphanedService = ApplicationService.builder()
                .id(ApplicationServiceId.of(UUID.randomUUID().toString()))
                .displayName(displayName)
                .ownerTeamId(null) // Orphaned - requires approval workflow
                .environments(List.of()) // Empty environments - user must explicitly choose
                .lifecycle(ApplicationService.ServiceLifecycle.ACTIVE)
                .createdAt(Instant.now())
                .createdBy("system") // System-created
                .build();

        ApplicationService saved = commandService.save(orphanedService);
        log.warn(
                "DEPRECATED: Auto-created orphaned ApplicationService: {} (displayName: {}) - requires approval workflow for team assignment. " +
                        "This behavior will be removed. Please register services via Admin Dashboard.",
                saved.getId(), displayName);

        return saved;
    }

    /**
     * Find application services owned by a specific team.
     * <p>
     * Delegates to ApplicationServiceQueryService for read operations.
     *
     * @param ownerTeamId the team ID
     * @return list of services owned by the team
     */
    public List<ApplicationService> findByOwnerTeam(String ownerTeamId) {
        log.debug("Finding application services by owner team: {}", ownerTeamId);
        ApplicationServiceCriteria criteria = ApplicationServiceCriteria.forTeam(ownerTeamId);
        return queryService.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * List application services with filtering and pagination.
     * <p>
     * <strong>Visibility Rules:</strong>
     * <ul>
     * <li>System admins see all services</li>
     * <li>Regular users see:
     * <ul>
     * <li>Orphaned services (ownerTeamId=null) - for ownership requests</li>
     * <li>Services owned by their teams</li>
     * <li>Services shared to their teams via ServiceShare</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param criteria    the filter criteria (may be enriched with visibility
     *                    filters)
     * @param pageable    pagination information
     * @param userContext the current user context for permission filtering
     * @return page of application services visible to the user
     */
    public Page<ApplicationService> findAll(ApplicationServiceCriteria criteria,
            Pageable pageable,
            UserContext userContext) {
        log.debug("Listing application services with criteria: {} for user: {}", criteria, userContext.getUserId());

        // System admins can see all services - no filtering
        if (userContext.isSysAdmin()) {
            log.debug("User {} is SYS_ADMIN, returning all services", userContext.getUserId());
            return queryService.findAll(criteria, pageable);
        }

        // For regular users: apply visibility filtering
        // Users can see: (1) orphaned services, (2) team-owned services, (3) shared
        // services

        // Get services shared to user's teams
        // Get shared service IDs via criteria + mapping
        List<String> sharedServiceIds;
        if (userContext.getTeamIds() == null || userContext.getTeamIds().isEmpty()) {
            sharedServiceIds = List.of();
        } else {
            ServiceShareCriteria shareCriteria = ServiceShareCriteria.forTeams(userContext.getTeamIds());
            List<ServiceShare> shares = serviceShareQueryService.findAll(shareCriteria, Pageable.unpaged())
                    .getContent();
            sharedServiceIds = shares.stream()
                    .map(ServiceShare::getServiceId)
                    .distinct()
                    .toList();
        }
        log.debug("Found {} services shared to user {} teams: {}",
                sharedServiceIds.size(), userContext.getUserId(), userContext.getTeamIds());

        // Build enriched criteria with visibility filters
        ApplicationServiceCriteria enrichedCriteria = (criteria != null ? criteria.toBuilder()
                : ApplicationServiceCriteria.builder())
                .includeOrphaned(true) // Always include orphaned services for ownership requests
                .userTeamIds(userContext.getTeamIds()) // Include team-owned services
                .sharedServiceIds(sharedServiceIds.isEmpty() ? null : sharedServiceIds) // Include shared services
                .build();

        log.debug("Enriched criteria for user {}: includeOrphaned=true, userTeamIds={}, sharedServiceIds={}",
                userContext.getUserId(), userContext.getTeamIds(), sharedServiceIds.size());

        Page<ApplicationService> result = queryService.findAll(enrichedCriteria, pageable);
        log.debug("Found {} application services for user {}", result.getTotalElements(), userContext.getUserId());

        return result;
    }

    /**
     * Delete an application service.
     * <p>
     * Business logic: Only system admins can delete services.
     *
     * @param id          the service ID to delete
     * @param userContext the current user context
     */
    @Transactional
    public void delete(ApplicationServiceId id, UserContext userContext) {
        log.info("Orchestrating delete for application service: {} by user: {}", id, userContext.getUserId());

        // Business logic: Verify service exists
        Optional<ApplicationService> service = queryService.findById(id);
        if (service.isEmpty()) {
            throw new IllegalArgumentException("Application service not found: " + id);
        }

        // Business logic: Only system admins can delete services
        if (!userContext.isSysAdmin()) {
            throw new IllegalStateException("Only system administrators can delete services");
        }

        commandService.deleteById(id);
        log.info("Successfully deleted application service: {}", id);
    }

    /**
     * Transfer ownership of an application service and cascade the change to all
     * related entities.
     * <p>
     * Complex orchestration: Updates ApplicationService, then cascades to
     * ServiceInstances and DriftEvents.
     * Publishes ServiceOwnershipTransferred event for other async listeners.
     *
     * @param serviceId   the service ID to transfer
     * @param newTeamId   the new team ID to assign
     * @param userContext the user context for audit purposes
     * @return the updated application service
     * @throws IllegalArgumentException if service not found
     * @throws IllegalStateException    if user lacks permission
     */
    @Transactional
    @Observed(name = MetricsNames.ApplicationService.TRANSFER_OWNERSHIP, contextualName = "application-service-transfer-ownership", lowCardinalityKeyValues = {
            "operation", "transfer_ownership" })
    public ApplicationService transferOwnershipWithCascade(String serviceId, String newTeamId,
            UserContext userContext) {
        log.info("Orchestrating ownership transfer of service {} to team {} by user {}",
                serviceId, newTeamId, userContext.getUserId());

        // Business logic: Validate service exists
        ApplicationService service = queryService.findById(ApplicationServiceId.of(serviceId))
                .orElseThrow(() -> new IllegalArgumentException("Application service not found: " + serviceId));

        String oldTeamId = service.getOwnerTeamId();

        // Business logic: Update service ownership
        service.setOwnerTeamId(newTeamId);
        service.setUpdatedAt(Instant.now());

        ApplicationService updatedService = commandService.save(service);

        // Cascade updates to related entities
        log.debug("Cascading teamId update to service instances and drift events for service: {}", serviceId);
        serviceInstanceCommandService.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);
        driftEventCommandService.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);

        // Publish domain event for any other async listeners
        ServiceOwnershipTransferred event = ServiceOwnershipTransferred.builder()
                .serviceId(serviceId)
                .oldTeamId(oldTeamId)
                .newTeamId(newTeamId)
                .transferredAt(Instant.now())
                .transferredBy(userContext.getUserId())
                .build();
        eventPublisher.publishEvent(event);

        log.info("Successfully transferred ownership of service {} to team {}", serviceId, newTeamId);
        return updatedService;
    }
}
