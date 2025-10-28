package com.example.control.application.command.serviceshare;

import com.example.control.config.security.UserContext;
import com.example.control.domain.id.ServiceShareId;
import com.example.control.domain.object.ServiceShare;
import com.example.control.domain.port.ServiceShareRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command handler for revoking service share permissions.
 * <p>
 * Validates permissions before deleting the share.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RevokeShareHandler {

    private final ServiceShareRepositoryPort repository;

    /**
     * Handle revoke share command.
     *
     * @param command the revoke share command
     * @throws IllegalArgumentException if share not found
     * @throws IllegalStateException    if user lacks permission to revoke
     */
    @CacheEvict(value = "service-shares", allEntries = true)
    public void handle(RevokeShareCommand command) {
        log.info("Revoking share: {} by user: {}", command.shareId(), command.revokedBy());

        ServiceShare share = repository.findById(ServiceShareId.of(command.shareId()))
                .orElseThrow(() -> new IllegalArgumentException("Share not found: " + command.shareId()));

        // Check permission to revoke
        UserContext userContext = UserContext.builder()
                .userId(command.revokedBy())
                .build();

        if (!canRevokeShare(userContext, share)) {
            throw new IllegalStateException("User does not have permission to revoke this share");
        }

        repository.deleteById(ServiceShareId.of(command.shareId()));
        log.info("Successfully revoked share: {}", command.shareId());
    }

    /**
     * Check if user can revoke a specific share.
     * <p>
     * Users can revoke shares they granted, system admins can revoke any share.
     *
     * @param userContext the user context
     * @param share       the service share
     * @return true if user can revoke the share
     */
    private boolean canRevokeShare(UserContext userContext, ServiceShare share) {
        // System admins can revoke any share
        if (userContext.isSysAdmin()) {
            return true;
        }

        // Users can revoke shares they granted
        return userContext.getUserId().equals(share.getGrantedBy());
    }
}
