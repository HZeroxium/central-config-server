package com.example.control.application.command.serviceshare;

import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.config.security.DomainPermissionEvaluator;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.id.ServiceShareId;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.object.ServiceShare;
import com.example.control.domain.port.ServiceShareRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command handler for granting service share permissions.
 * <p>
 * Validates service existence, permissions, and duplicate shares before creating a new share.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GrantShareHandler {

    private final ServiceShareRepositoryPort repository;
    private final ApplicationServiceQueryService applicationServiceQueryService;
    private final DomainPermissionEvaluator permissionEvaluator;

    /**
     * Handle grant share command.
     *
     * @param command the grant share command
     * @return the created service share
     * @throws IllegalArgumentException if service not found
     * @throws IllegalStateException if user lacks permission or share already exists
     */
    @CacheEvict(value = "service-shares", allEntries = true)
    public ServiceShare handle(GrantShareCommand command) {
        log.info("Granting share for service: {} to {}:{} by user: {}", 
                command.serviceId(), command.grantToType(), command.grantToId(), command.grantedBy());

        // Validate service exists
        ApplicationService service = applicationServiceQueryService.findById(ApplicationServiceId.of(command.serviceId()))
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + command.serviceId()));

        // Check if user can manage shares for this service
        if (!permissionEvaluator.canManageShares(
                com.example.control.config.security.UserContext.builder()
                        .userId(command.grantedBy())
                        .build(), 
                service)) {
            throw new IllegalStateException("User does not have permission to manage shares for this service");
        }

        // Check if share already exists
        if (repository.existsByServiceAndGranteeAndEnvironments(
                command.serviceId(), command.grantToType(), command.grantToId(), command.environments())) {
            throw new IllegalStateException("Share already exists for the specified criteria");
        }

        // Create share
        ServiceShare share = ServiceShare.builder()
                .id(ServiceShareId.of(generateShareId()))
                .resourceLevel(ServiceShare.ResourceLevel.SERVICE)
                .serviceId(command.serviceId())
                .grantToType(command.grantToType())
                .grantToId(command.grantToId())
                .permissions(command.permissions())
                .environments(command.environments())
                .grantedBy(command.grantedBy())
                .createdAt(java.time.Instant.now())
                .expiresAt(command.expiresAt())
                .build();

        ServiceShare saved = repository.save(share);
        log.info("Successfully granted share: {}", saved.getId());
        return saved;
    }

    /**
     * Generate a unique share ID.
     *
     * @return unique share ID
     */
    private String generateShareId() {
        return "share_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }
}
