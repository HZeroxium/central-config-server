package com.example.control.application.command.driftevent;

import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.config.security.DomainPermissionEvaluator;
import com.example.control.config.security.UserContext;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.id.DriftEventId;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.object.DriftEvent;
import com.example.control.domain.port.DriftEventRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Command handler for creating drift events.
 * <p>
 * Validates service existence, sets teamId from ApplicationService, and checks
 * permissions.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateDriftEventHandler {

    private final DriftEventRepositoryPort repository;
    private final ApplicationServiceQueryService applicationServiceQueryService;
    private final DomainPermissionEvaluator permissionEvaluator;

    /**
     * Handle create drift event command.
     *
     * @param command the create command
     * @return the saved drift event
     * @throws IllegalArgumentException if service not found
     * @throws SecurityException        if user lacks permission
     */
    @CacheEvict(value = "drift-events", allEntries = true)
    public DriftEvent handle(CreateDriftEventCommand command) {
        log.debug("Creating drift event for service {} by user {}", command.serviceName(), command.detectedBy());

        // Validate serviceId exists and get ApplicationService
        if (command.serviceId() == null) {
            throw new IllegalArgumentException("ServiceId is required for drift event creation");
        }

        ApplicationService service = applicationServiceQueryService
                .findById(ApplicationServiceId.of(command.serviceId()))
                .orElseThrow(
                        () -> new IllegalArgumentException("ApplicationService not found: " + command.serviceId()));

        // Build the drift event
        DriftEvent event = DriftEvent.builder()
                .id(DriftEventId.of(UUID.randomUUID().toString()))
                .serviceName(command.serviceName())
                .instanceId(command.instanceId())
                .serviceId(command.serviceId())
                .environment(command.environment())
                .notes(command.description())
                .status(command.status())
                .teamId(service.getOwnerTeamId()) // Set teamId from ApplicationService
                .detectedAt(Instant.now())
                .detectedBy(command.detectedBy())
                .build();

        // Check if user can create drift events for this service
        UserContext userContext = UserContext.builder()
                .userId(command.detectedBy())
                .build();

        if (!permissionEvaluator.canEditDriftEvent(userContext, event)) {
            log.warn("User {} denied permission to create drift event for service {}",
                    command.detectedBy(), command.serviceName());
            throw new SecurityException(
                    "Insufficient permissions to create drift event for service: " + command.serviceName());
        }

        DriftEvent saved = repository.save(event);
        log.debug("Successfully saved drift event: {}", saved.getId());
        return saved;
    }
}
