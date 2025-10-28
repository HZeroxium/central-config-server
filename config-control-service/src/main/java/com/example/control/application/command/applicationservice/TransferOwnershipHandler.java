package com.example.control.application.command.applicationservice;

import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.event.ServiceOwnershipTransferred;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.port.ApplicationServiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Command handler for transferring service ownership.
 * <p>
 * Updates the service ownerTeamId and publishes a domain event for cascading
 * updates.
 * This decouples the ownership transfer from related entity updates.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransferOwnershipHandler {

    private final ApplicationServiceRepositoryPort repository;
    private final ApplicationServiceQueryService queryService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Handle transfer ownership command.
     *
     * @param command the transfer ownership command
     * @return the updated application service
     * @throws IllegalArgumentException if service not found
     * @throws IllegalStateException    if user lacks permission
     */
    @CacheEvict(value = "application-services", allEntries = true)
    public ApplicationService handle(TransferOwnershipCommand command) {
        log.info("Transferring ownership of service {} to team {} by user {}",
                command.serviceId(), command.newTeamId(), command.transferredBy());

        // Get the service
        ApplicationService service = queryService.findById(ApplicationServiceId.of(command.serviceId()))
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + command.serviceId()));

        // Check permissions
        UserContext userContext = UserContext.builder()
                .userId(command.transferredBy())
                .build();

        if (!canEditService(userContext, service)) {
            throw new IllegalStateException("User does not have permission to transfer ownership of this service");
        }

        // Update the service
        String oldTeamId = service.getOwnerTeamId();
        service.setOwnerTeamId(command.newTeamId());
        service.setUpdatedAt(Instant.now());

        ApplicationService updatedService = repository.save(service);
        log.info("Updated ApplicationService {} ownerTeamId from {} to {}",
                command.serviceId(), oldTeamId, command.newTeamId());

        // Publish domain event for cascading updates
        ServiceOwnershipTransferred event = ServiceOwnershipTransferred.builder()
                .serviceId(command.serviceId())
                .oldTeamId(oldTeamId)
                .newTeamId(command.newTeamId())
                .transferredBy(command.transferredBy())
                .transferredAt(Instant.now())
                .build();

        eventPublisher.publishEvent(event);
        log.info("Published ServiceOwnershipTransferred event for service: {}", command.serviceId());

        return updatedService;
    }

    /**
     * Check if user can edit a service.
     * <p>
     * System admins can edit any service, team members can edit services owned by
     * their team.
     *
     * @param userContext the user context
     * @param service     the application service
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
