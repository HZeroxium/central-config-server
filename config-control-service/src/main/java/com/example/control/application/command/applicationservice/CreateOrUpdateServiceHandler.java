package com.example.control.application.command.applicationservice;

import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.config.security.UserContext;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.port.ApplicationServiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Command handler for creating or updating application services.
 * <p>
 * Validates ownership and permissions before saving the service.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateOrUpdateServiceHandler {

    private final ApplicationServiceRepositoryPort repository;
    private final ApplicationServiceQueryService queryService;

    /**
     * Handle create or update service command.
     *
     * @param command the create/update command
     * @return the saved application service
     * @throws IllegalStateException if user lacks permission to edit
     */
    @CacheEvict(value = "application-services", allEntries = true)
    public ApplicationService handle(CreateOrUpdateServiceCommand command) {
        log.info("Creating/updating application service: {} by user: {}", command.id(), command.createdBy());

        ApplicationServiceId serviceId = command.id() != null ? 
                ApplicationServiceId.of(command.id()) : 
                ApplicationServiceId.of(UUID.randomUUID().toString());

        // Validate ownership for updates
        if (command.id() != null && !command.id().isBlank()) {
            Optional<ApplicationService> existing = queryService.findById(serviceId);
            if (existing.isPresent()) {
                UserContext userContext = UserContext.builder()
                        .userId(command.createdBy())
                        .build();
                
                if (!canEditService(userContext, existing.get())) {
                    throw new IllegalStateException("User does not have permission to edit this service");
                }
            }
        }

        // Build the service
        ApplicationService service = ApplicationService.builder()
                .id(serviceId)
                .displayName(command.displayName())
                .ownerTeamId(command.ownerTeamId())
                .environments(command.environments())
                .tags(command.tags())
                .repoUrl(command.repoUrl())
                .lifecycle(command.lifecycle())
                .attributes(command.attributes())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(command.createdBy())
                .build();

        ApplicationService saved = repository.save(service);
        log.info("Successfully saved application service: {}", saved.getId());
        return saved;
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
