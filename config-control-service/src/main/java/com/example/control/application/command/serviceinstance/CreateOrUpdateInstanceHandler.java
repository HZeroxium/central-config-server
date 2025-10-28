package com.example.control.application.command.serviceinstance;

import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.config.security.DomainPermissionEvaluator;
import com.example.control.config.security.UserContext;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.id.ServiceInstanceId;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.object.ServiceInstance;
import com.example.control.domain.port.ServiceInstanceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Command handler for creating or updating service instances.
 * <p>
 * Validates service existence, sets teamId from ApplicationService, and checks
 * permissions.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateOrUpdateInstanceHandler {

    private final ServiceInstanceRepositoryPort repository;
    private final ApplicationServiceQueryService applicationServiceQueryService;
    private final DomainPermissionEvaluator permissionEvaluator;

    /**
     * Handle create or update instance command.
     *
     * @param command the create/update command
     * @return the saved service instance
     * @throws IllegalArgumentException if service not found
     * @throws SecurityException        if user lacks permission
     */
    @CacheEvict(value = "service-instances", allEntries = true)
    public ServiceInstance handle(CreateOrUpdateInstanceCommand command) {
        log.debug("Creating/updating service instance {} for user {}", command.instanceId(), command.createdBy());

        // Validate serviceId exists and get ApplicationService
        if (command.serviceId() == null) {
            throw new IllegalArgumentException("ServiceId is required for instance creation");
        }

        ApplicationService service = applicationServiceQueryService
                .findById(ApplicationServiceId.of(command.serviceId()))
                .orElseThrow(
                        () -> new IllegalArgumentException("ApplicationService not found: " + command.serviceId()));

        // Build the instance
        ServiceInstance instance = ServiceInstance.builder()
                .id(ServiceInstanceId.of(command.instanceId()))
                .serviceId(command.serviceId())
                .host(command.host())
                .port(command.port())
                .environment(command.environment())
                .version(command.version())
                .status(command.status())
                .hasDrift(Boolean.TRUE.equals(command.hasDrift()))
                .expectedHash(command.expectedHash())
                .lastAppliedHash(command.lastAppliedHash())
                .teamId(service.getOwnerTeamId()) // Set teamId from ApplicationService
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Check if user can edit instances for this service
        UserContext userContext = UserContext.builder()
                .userId(command.createdBy())
                .build();

        if (!permissionEvaluator.canEditInstance(userContext, instance)) {
            log.warn("User {} denied permission to create instance for service {}",
                    command.createdBy(), command.serviceId());
            throw new SecurityException(
                    "Insufficient permissions to create instance for service: " + command.serviceId());
        }

        ServiceInstance saved = repository.save(instance);
        log.debug("Successfully saved service instance: {}", saved.getId());
        return saved;
    }
}
