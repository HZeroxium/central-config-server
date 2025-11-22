package com.example.control.application.service;

import com.example.control.application.command.FailedHeartbeatCommandService;
import com.example.control.application.query.FailedHeartbeatQueryService;
import com.example.control.domain.model.FailedHeartbeat;
import com.example.control.domain.model.HeartbeatPayload;
import com.example.control.domain.criteria.FailedHeartbeatCriteria;
import com.example.control.domain.valueobject.id.FailedHeartbeatId;
import com.example.control.infrastructure.config.messaging.HeartbeatProperties;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.infrastructure.observability.heartbeat.HeartbeatMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrator service for FailedHeartbeat business operations.
 * <p>
 * This service acts as an orchestrator that:
 * <ul>
 * <li>Handles business logic and orchestration</li>
 * <li>Enforces permission checks and team-based filtering</li>
 * <li>Enriches criteria with user context (team IDs)</li>
 * <li>Coordinates between CommandService and QueryService</li>
 * <li>Manages re-drive operations</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FailedHeartbeatService {

    private final FailedHeartbeatCommandService commandService;
    private final FailedHeartbeatQueryService queryService;
    private final HeartbeatMetrics heartbeatMetrics;
    private final HeartbeatProperties heartbeatProperties;
    @Qualifier("heartbeatKafkaTemplate")
    private final KafkaTemplate<String, HeartbeatPayload> heartbeatKafkaTemplate;

    /**
     * Retrieves a page of failed heartbeats using flexible filters and pagination.
     * <p>
     * Results are filtered by user permissions - users can only see failed heartbeats
     * for services they own or have been granted access to via service shares.
     *
     * @param criteria    optional filter parameters
     * @param pageable    pagination and sorting information
     * @param userContext the user context for permission filtering
     * @return a page of failed heartbeats
     */
    public Page<FailedHeartbeat> findAll(FailedHeartbeatCriteria criteria, Pageable pageable, UserContext userContext) {
        log.debug("Listing failed heartbeats with criteria: {} for user: {}", criteria, userContext.getUserId());

        // System admins can see all failed heartbeats
        if (userContext.isSysAdmin()) {
            return queryService.findAll(criteria, pageable);
        }

        // Build enriched criteria with team filtering
        FailedHeartbeatCriteria enrichedCriteria = criteria != null
                ? criteria.toBuilder()
                        .userTeamIds(userContext.getTeamIds())
                        .build()
                : FailedHeartbeatCriteria.forUser(userContext);

        // Query with team-based filtering (repository handles team filtering)
        Page<FailedHeartbeat> failedHeartbeats = queryService.findAll(enrichedCriteria, pageable);

        log.debug("Found {} failed heartbeats for user: {} (team-owned)",
                failedHeartbeats.getContent().size(), userContext.getUserId());

        return failedHeartbeats;
    }

    /**
     * Finds a failed heartbeat by its identifier.
     * <p>
     * Returns the failed heartbeat only if the user has permission to view it.
     *
     * @param id          failed heartbeat identifier
     * @param userContext the user context for permission checking
     * @return optional failed heartbeat if found and user has permission
     */
    public Optional<FailedHeartbeat> findById(FailedHeartbeatId id, UserContext userContext) {
        log.debug("Finding failed heartbeat by ID: {} for user: {}", id, userContext.getUserId());

        Optional<FailedHeartbeat> failedHeartbeat = queryService.findById(id);

        if (failedHeartbeat.isEmpty()) {
            return Optional.empty();
        }

        FailedHeartbeat fh = failedHeartbeat.get();

        // System admins can view all failed heartbeats
        if (userContext.isSysAdmin()) {
            return Optional.of(fh);
        }

        // Check team-based access: user must be member of owning team
        if (fh.getTeamId() != null && userContext.isMemberOfTeam(fh.getTeamId())) {
            return Optional.of(fh);
        }

        // Orphaned services (teamId=null) are visible to all authenticated users
        if (fh.getTeamId() == null) {
            return Optional.of(fh);
        }

        log.warn("User {} does not have permission to view failed heartbeat {}", userContext.getUserId(), id);
        return Optional.empty();
    }

    /**
     * Updates the status of a failed heartbeat.
     * <p>
     * Validates that the user has permission to update the failed heartbeat
     * (must be owner team member or SYS_ADMIN).
     *
     * @param id          the failed heartbeat ID
     * @param status      the new status
     * @param notes       optional notes
     * @param userContext the user context for permission checking
     * @return the updated failed heartbeat
     * @throws SecurityException if user lacks permission
     */
    @Transactional
    public FailedHeartbeat updateStatus(
            FailedHeartbeatId id,
            FailedHeartbeat.FailedHeartbeatStatus status,
            String notes,
            UserContext userContext) {
        log.debug("Updating failed heartbeat {} status to {} by user {}", id, status, userContext.getUserId());

        FailedHeartbeat failedHeartbeat = findById(id, userContext)
                .orElseThrow(() -> new IllegalArgumentException("Failed heartbeat not found: " + id));

        // Permission check: must be owner team member or SYS_ADMIN
        if (!userContext.isSysAdmin() && failedHeartbeat.getTeamId() != null
                && !userContext.isMemberOfTeam(failedHeartbeat.getTeamId())) {
            throw new SecurityException("Insufficient permissions to update failed heartbeat: " + id);
        }

        return commandService.updateStatus(id, status, userContext.getUserId(), notes);
    }

    /**
     * Bulk update status for multiple failed heartbeats.
     * <p>
     * Validates that the user has permission to update all failed heartbeats.
     *
     * @param ids         list of failed heartbeat IDs
     * @param status      the new status
     * @param userContext the user context for permission checking
     * @return number of failed heartbeats updated
     * @throws SecurityException if user lacks permission for any failed heartbeat
     */
    @Transactional
    public long bulkUpdateStatus(
            List<FailedHeartbeatId> ids,
            FailedHeartbeat.FailedHeartbeatStatus status,
            UserContext userContext) {
        log.debug("Bulk updating {} failed heartbeats to status {} by user {}", 
                ids.size(), status, userContext.getUserId());

        // System admins can bulk update any failed heartbeats
        if (userContext.isSysAdmin()) {
            return commandService.bulkUpdateStatus(ids, status, userContext.getUserId());
        }

        // For non-admin users, verify permission for each failed heartbeat
        for (FailedHeartbeatId id : ids) {
            Optional<FailedHeartbeat> fh = queryService.findById(id);
            if (fh.isPresent() && fh.get().getTeamId() != null
                    && !userContext.isMemberOfTeam(fh.get().getTeamId())) {
                throw new SecurityException("Insufficient permissions to update failed heartbeat: " + id);
            }
        }

        return commandService.bulkUpdateStatus(ids, status, userContext.getUserId());
    }

    /**
     * Re-drives a failed heartbeat back to the main topic for reprocessing.
     * <p>
     * Validates that the user has permission and that the failed heartbeat is not too old
     * (default: >5 minutes requires SYS_ADMIN). Publishes the original payload back to
     * the main topic with x-redrive=true header.
     *
     * @param id          the failed heartbeat ID
     * @param userContext the user context for permission checking
     * @throws SecurityException if user lacks permission
     * @throws IllegalArgumentException if failed heartbeat is too old for non-admin users
     */
    @Transactional
    public void redrive(FailedHeartbeatId id, UserContext userContext) {
        log.info("Re-driving failed heartbeat {} to main topic by user {}", id, userContext.getUserId());

        FailedHeartbeat failedHeartbeat = findById(id, userContext)
                .orElseThrow(() -> new IllegalArgumentException("Failed heartbeat not found: " + id));

        // Permission check: must be owner team member or SYS_ADMIN
        if (!userContext.isSysAdmin() && failedHeartbeat.getTeamId() != null
                && !userContext.isMemberOfTeam(failedHeartbeat.getTeamId())) {
            throw new SecurityException("Insufficient permissions to re-drive failed heartbeat: " + id);
        }

        // Age check: non-admin users can only re-drive recent failures (<5 minutes)
        if (!userContext.isSysAdmin() && failedHeartbeat.getFirstSeenAt() != null) {
            Duration age = Duration.between(failedHeartbeat.getFirstSeenAt(), Instant.now());
            if (age.toMinutes() > 5) {
                throw new IllegalArgumentException(
                        "Failed heartbeat is too old (" + age.toMinutes() + " minutes). " +
                        "Only SYS_ADMIN can re-drive failed heartbeats older than 5 minutes.");
            }
        }

        // Extract original payload
        HeartbeatPayload payload = failedHeartbeat.getPayload();
        if (payload == null) {
            throw new IllegalStateException("Failed heartbeat has no payload: " + id);
        }

        // Publish back to main topic with x-redrive header
        String partitionKey = payload.getServiceName();
        RecordHeaders headers = new RecordHeaders();
        headers.add("x-redrive", "true".getBytes(StandardCharsets.UTF_8));
        headers.add("x-redrive-by", userContext.getUserId().getBytes(StandardCharsets.UTF_8));
        headers.add("x-redrive-at", Instant.now().toString().getBytes(StandardCharsets.UTF_8));
        headers.add("x-original-failed-heartbeat-id", id.id().getBytes(StandardCharsets.UTF_8));

        // Create ProducerRecord with headers
        ProducerRecord<String, HeartbeatPayload> record = new ProducerRecord<>(
                heartbeatProperties.getKafka().getTopic(),
                null, // partition (let Kafka decide based on key)
                partitionKey,
                payload,
                headers);

        // Use KafkaTemplate directly (not ResilientKafkaProducer) for re-drive
        // Re-drive is a manual operation, so we want immediate feedback on failures
        CompletableFuture<SendResult<String, HeartbeatPayload>> future = heartbeatKafkaTemplate.send(record);

        // Record metrics
        heartbeatMetrics.recordRedrive();

        // Handle send result
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to re-drive failed heartbeat {} to main topic", id, ex);
            } else {
                log.info("Successfully re-drove failed heartbeat {} to main topic", id);
            }
        });

        // Update status to RESOLVED after successful re-drive
        // Note: We don't wait for Kafka send completion, but update status optimistically
        commandService.updateStatus(id, FailedHeartbeat.FailedHeartbeatStatus.RESOLVED,
                userContext.getUserId(), "Re-driven to main topic");
    }
}

