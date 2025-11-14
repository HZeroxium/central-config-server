package com.example.control.application.service;

import com.example.control.application.command.DriftEventCommandService;
import com.example.control.application.query.DriftEventQueryService;
import com.example.control.infrastructure.config.security.DomainPermissionEvaluator;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.infrastructure.observability.MetricsNames;
import com.example.control.domain.model.DriftEvent;
import com.example.control.domain.criteria.DriftEventCriteria;
import com.example.control.domain.valueobject.id.DriftEventId;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Orchestrator service for DriftEvent business operations.
 * <p>
 * This service acts as an orchestrator/aggregator that:
 * <ul>
 * <li>Handles business logic and orchestration</li>
 * <li>Enforces permission checks and team-based filtering</li>
 * <li>Enriches criteria with user context (team IDs)</li>
 * <li>Coordinates between CommandService and QueryService</li>
 * <li>Manages transactions for complex operations</li>
 * </ul>
 * <p>
 * Uses:
 * <ul>
 * <li>{@link DriftEventCommandService} for write operations</li>
 * <li>{@link DriftEventQueryService} for read operations</li>
 * <li>{@link DomainPermissionEvaluator} for permission checks</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriftEventService {

    private final DriftEventCommandService commandService;
    private final DriftEventQueryService queryService;
    private final DomainPermissionEvaluator permissionEvaluator;

    /**
     * Saves a new drift event to the database.
     * <p>
     * No permission checking - use {@link #save(DriftEvent, UserContext)} for
     * permission-aware save.
     *
     * @param event domain drift event
     * @return persisted {@link DriftEvent}
     */
    public DriftEvent save(DriftEvent event) {
        log.debug("Saving drift event (no permission check): {}", event.getId());
        return commandService.save(event);
    }

    /**
     * Saves a new drift event with permission validation.
     * <p>
     * Validates that the user can create drift events for the specified service.
     *
     * @param event       the drift event to save
     * @param userContext the user context for permission checking
     * @return the saved drift event
     * @throws SecurityException if user lacks permission to create drift event
     */
    @Transactional
    @Observed(name = MetricsNames.DriftEvent.SAVE, contextualName = "drift-event-save", lowCardinalityKeyValues = {
            "operation", "save" })
    public DriftEvent save(DriftEvent event, UserContext userContext) {
        log.debug("Saving drift event {} for user {}", event.getId(), userContext.getUserId());

        // Check if user can create drift events for this service
        if (!permissionEvaluator.canEditDriftEvent(userContext, event)) {
            log.warn("User {} denied permission to create drift event for service {}",
                    userContext.getUserId(), event.getServiceName());
            throw new SecurityException(
                    "Insufficient permissions to create drift event for service: " + event.getServiceName());
        }

        return commandService.save(event);
    }

    /**
     * Retrieves a page of drift events using flexible filters and pagination.
     * <p>
     * Sorting is applied via {@link Pageable#getSort()} and delegated to the
     * Mongo adapter using {@code query.with(pageable)}.
     * <p>
     * Results are filtered by user permissions - users can only see drift events
     * for services they own or have been granted access to via service shares.
     *
     * @param criteria    optional filter parameters encapsulated in a record
     * @param pageable    pagination and sorting information
     * @param userContext the user context for permission filtering
     * @return a page of {@link DriftEvent}
     */
    @Cacheable(value = "drift-events", key = "'list:' + #criteria.hashCode() + ':' + #pageable + ':' + #userContext.userId")
    public Page<DriftEvent> findAll(DriftEventCriteria criteria, Pageable pageable, UserContext userContext) {
        log.debug("Listing drift events with criteria: {} for user: {}", criteria, userContext.getUserId());

        // System admins can see all events
        if (userContext.isSysAdmin()) {
            return queryService.findAll(criteria, pageable);
        }

        // Build enriched criteria with team filtering
        DriftEventCriteria enrichedCriteria = criteria.toBuilder()
                .userTeamIds(userContext.getTeamIds())
                .build();

        // Query with team-based filtering (repository handles team filtering)
        Page<DriftEvent> events = queryService.findAll(enrichedCriteria, pageable);

        log.debug("Found {} drift events for user: {} (team-owned)",
                events.getContent().size(), userContext.getUserId());

        return events;
    }

    /**
     * Finds a drift event by its identifier.
     * <p>
     * Returns the drift event only if the user has permission to view it.
     *
     * @param id          event identifier
     * @param userContext the user context for permission checking
     * @return optional {@link DriftEvent} if found and user has permission
     */
    public Optional<DriftEvent> findById(DriftEventId id, UserContext userContext) {
        log.debug("Finding drift event by ID: {} for user: {}", id, userContext.getUserId());

        Optional<DriftEvent> event = queryService.findById(id);

        if (event.isPresent() && !permissionEvaluator.canViewDriftEvent(userContext, event.get())) {
            log.warn("User {} does not have permission to view drift event {}", userContext.getUserId(), id);
            return Optional.empty();
        }

        return event;
    }

    /**
     * Retrieves all drift events that are not resolved.
     *
     * @return list of unresolved drift events
     */
    public List<DriftEvent> findUnresolved() {
        DriftEventCriteria criteria = DriftEventCriteria.unresolved();
        return queryService.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * Retrieves unresolved drift events for a specific service.
     *
     * @param serviceName service name
     * @return list of unresolved events
     */
    public List<DriftEvent> findUnresolvedByService(String serviceName) {
        DriftEventCriteria criteria = DriftEventCriteria.unresolvedForService(serviceName);
        return queryService.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * Finds all drift events by service name.
     *
     * @param serviceName service name
     * @return list of drift events
     */
    public List<DriftEvent> findByService(String serviceName) {
        DriftEventCriteria criteria = DriftEventCriteria.forService(serviceName);
        return queryService.findAll(criteria, Pageable.unpaged()).getContent();
    }

    /**
     * Counts all drift events.
     *
     * @return total drift event count
     */
    public long countAll() {
        return queryService.countAll();
    }

    /**
     * Counts drift events by their current status.
     *
     * @param status drift status
     * @return count of events with the given status
     */
    public long countByStatus(DriftEvent.DriftStatus status) {
        DriftEventCriteria criteria = DriftEventCriteria.withStatus(status);
        return queryService.count(criteria);
    }

    /**
     * Resolves all unresolved drift events for a specific instance.
     * <p>
     * This method is typically triggered by the heartbeat service after verifying
     * that the instance configuration hash is up-to-date.
     *
     * @param serviceName service name
     * @param instanceId  instance identifier
     * @param resolvedBy  identifier of who/what resolved the drift
     */
    @Observed(name = MetricsNames.DriftEvent.RESOLVE, contextualName = "drift-event-resolve", lowCardinalityKeyValues = {
            "operation", "resolve" })
    public void resolveForInstance(String serviceName, String instanceId, String resolvedBy) {
        commandService.resolveForInstance(serviceName, instanceId, resolvedBy);
    }

    /**
     * Bulk update teamId for all drift events with the given serviceId.
     * <p>
     * Used during ownership transfer to ensure all drift events are updated
     * to reflect the new team ownership.
     *
     * @param serviceId the service ID to match
     * @param newTeamId the new team ID to set
     * @return number of drift events updated
     */
    public long bulkUpdateTeamIdByServiceId(String serviceId, String newTeamId) {
        log.info("Orchestrating bulk teamId update for drift events of service: {}", serviceId);
        return commandService.bulkUpdateTeamIdByServiceId(serviceId, newTeamId);
    }

    /**
     * Bulk save drift events.
     * <p>
     * Efficiently saves multiple drift events in a single MongoDB bulk operation.
     * Used for batch heartbeat processing to reduce write overhead.
     * <p>
     * No permission checking - this is used internally by heartbeat processing.
     *
     * @param events list of drift events to save
     * @return bulk write result with counts of inserted/updated documents
     */
    public com.mongodb.bulk.BulkWriteResult bulkSave(List<DriftEvent> events) {
        log.debug("Bulk saving {} drift events (no permission check)", events.size());
        return commandService.bulkSave(events);
    }

}
