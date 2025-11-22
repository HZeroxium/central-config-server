package com.example.control.application.command;

import com.example.control.domain.model.FailedHeartbeat;
import com.example.control.domain.valueobject.id.FailedHeartbeatId;
import com.example.control.domain.port.repository.FailedHeartbeatRepositoryPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Command service for FailedHeartbeat write operations.
 * <p>
 * Handles all write operations (save, update, delete) for FailedHeartbeat domain
 * objects. This service is responsible for:
 * <ul>
 * <li>CRUD operations with validation</li>
 * <li>Transaction management</li>
 * </ul>
 * <p>
 * Does NOT handle:
 * <ul>
 * <li>Business logic or permission checks (delegated to orchestrator services)</li>
 * <li>Cross-domain operations (use orchestrator services)</li>
 * <li>Read operations (use {@link com.example.control.application.query.FailedHeartbeatQueryService})</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Validated
@Transactional
public class FailedHeartbeatCommandService {

    private final FailedHeartbeatRepositoryPort repository;

    /**
     * Saves a failed heartbeat (create or update).
     * <p>
     * Automatically generates ID if null (new entity).
     *
     * @param failedHeartbeat the failed heartbeat to save (must be valid)
     * @return the saved failed heartbeat with generated/updated fields
     */
    public FailedHeartbeat save(@Valid FailedHeartbeat failedHeartbeat) {
        log.debug("Saving failed heartbeat: {}", failedHeartbeat.getId());

        // Check if this is a new failed heartbeat (ID is null)
        boolean isNew = failedHeartbeat.getId() == null;

        // Generate UUID if ID is null (new failed heartbeat)
        if (isNew) {
            failedHeartbeat.setId(FailedHeartbeatId.of(UUID.randomUUID().toString()));
            log.debug("Generated new ID for failed heartbeat: {}", failedHeartbeat.getId());
        }

        FailedHeartbeat saved = repository.save(failedHeartbeat);
        log.info("Saved failed heartbeat: {} for service: {}:{}", 
                saved.getId(), saved.getServiceName(), saved.getInstanceId());

        return saved;
    }

    /**
     * Updates the status of a failed heartbeat.
     * <p>
     * Sets resolvedAt and resolvedBy when status is RESOLVED or IGNORED.
     *
     * @param id         the failed heartbeat ID
     * @param status     the new status
     * @param resolvedBy the user identifier who resolved it
     * @param notes      optional notes
     * @return the updated failed heartbeat
     */
    public FailedHeartbeat updateStatus(
            FailedHeartbeatId id,
            FailedHeartbeat.FailedHeartbeatStatus status,
            String resolvedBy,
            String notes) {
        log.debug("Updating failed heartbeat {} status to {}", id, status);

        FailedHeartbeat failedHeartbeat = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Failed heartbeat not found: " + id));

        failedHeartbeat.setStatus(status);
        failedHeartbeat.setNotes(notes);

        // Set resolvedAt and resolvedBy when status is RESOLVED or IGNORED
        if (status == FailedHeartbeat.FailedHeartbeatStatus.RESOLVED 
                || status == FailedHeartbeat.FailedHeartbeatStatus.IGNORED) {
            failedHeartbeat.setResolvedAt(Instant.now());
            failedHeartbeat.setResolvedBy(resolvedBy);
        } else {
            // Clear resolvedAt and resolvedBy for other statuses
            failedHeartbeat.setResolvedAt(null);
            failedHeartbeat.setResolvedBy(null);
        }

        FailedHeartbeat updated = repository.save(failedHeartbeat);
        log.info("Updated failed heartbeat {} status to {}", id, status);

        return updated;
    }

    /**
     * Bulk update status for multiple failed heartbeats.
     *
     * @param ids        list of failed heartbeat IDs to update
     * @param status     the new status
     * @param resolvedBy the user identifier who resolved them
     * @return number of failed heartbeats updated
     */
    public long bulkUpdateStatus(
            List<FailedHeartbeatId> ids,
            FailedHeartbeat.FailedHeartbeatStatus status,
            String resolvedBy) {
        log.debug("Bulk updating {} failed heartbeats to status {}", ids.size(), status);

        long updated = repository.bulkUpdateStatus(ids, status, resolvedBy);
        log.info("Bulk updated {} failed heartbeats to status {}", updated, status);

        return updated;
    }
}

