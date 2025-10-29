package com.example.control.application.event;

import com.example.control.domain.event.ServiceOwnershipTransferred;
import com.example.control.domain.port.DriftEventRepositoryPort;
import com.example.control.domain.port.ServiceInstanceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for ServiceOwnershipTransferred domain events.
 * <p>
 * Handles cascading updates to related entities when service ownership is transferred.
 * Uses @TransactionalEventListener with AFTER_COMMIT to ensure the main transaction
 * completes before performing cascading updates.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceOwnershipEventListener {

    private final ServiceInstanceRepositoryPort serviceInstanceRepository;
    private final DriftEventRepositoryPort driftEventRepository;

    /**
     * Handle service ownership transferred event.
     * <p>
     * Updates teamId for all related ServiceInstances and DriftEvents to maintain
     * data consistency across aggregates.
     * </p>
     *
     * @param event the service ownership transferred event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    @CacheEvict(value = {"service-instances", "drift-events"}, allEntries = true)
    public void handleServiceOwnershipTransferred(ServiceOwnershipTransferred event) {
        log.info("Processing ServiceOwnershipTransferred event for service: {} from team: {} to team: {}",
                event.getServiceId(), event.getOldTeamId(), event.getNewTeamId());

        try {
            // Cascade to ServiceInstances
            long instanceCount = serviceInstanceRepository.bulkUpdateTeamIdByServiceId(
                    event.getServiceId(), event.getNewTeamId());
            log.info("Updated {} service instances for service: {}", instanceCount, event.getServiceId());

            // Cascade to DriftEvents
            long driftEventCount = driftEventRepository.bulkUpdateTeamIdByServiceId(
                    event.getServiceId(), event.getNewTeamId());
            log.info("Updated {} drift events for service: {}", driftEventCount, event.getServiceId());

            log.info("Successfully processed ownership transfer for service: {} (instances: {}, drift events: {})",
                    event.getServiceId(), instanceCount, driftEventCount);

        } catch (Exception e) {
            log.error("Failed to process ServiceOwnershipTransferred event for service: {}",
                    event.getServiceId(), e);
            // Note: We don't rethrow the exception to avoid rolling back the main transaction
            // The event processing failure should be handled separately (e.g., retry mechanism)
        }
    }
}
