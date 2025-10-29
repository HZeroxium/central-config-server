package com.example.control.application.command;

import com.example.control.domain.id.ApprovalDecisionId;
import com.example.control.domain.object.ApprovalDecision;
import com.example.control.domain.port.ApprovalDecisionRepositoryPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/**
 * Command service for ApprovalDecision write operations.
 * <p>
 * Handles all write operations (save, update, delete) for ApprovalDecision
 * domain objects.
 * Responsible for CRUD, cache eviction, and transaction management.
 * Does NOT handle business logic, permission checks, or cross-domain
 * operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Validated
@Transactional
public class ApprovalDecisionCommandService {

    private final ApprovalDecisionRepositoryPort repository;

    /**
     * Saves an approval decision (create or update).
     * Automatically generates ID if null.
     * Evicts all approval-decisions cache entries.
     *
     * @param decision the approval decision to save
     * @return the saved approval decision
     */
    @CacheEvict(value = "approval-decisions", allEntries = true)
    public ApprovalDecision save(@Valid ApprovalDecision decision) {
        log.debug("Saving approval decision: {}", decision.getId());

        if (decision.getId() == null) {
            decision.setId(ApprovalDecisionId.of(UUID.randomUUID().toString()));
            log.debug("Generated new ID for approval decision: {}", decision.getId());
        }

        ApprovalDecision saved = repository.save(decision);
        log.info("Saved approval decision: {} for request: {} by user: {}",
                saved.getId(), saved.getRequestId(), saved.getApproverUserId());
        return saved;
    }

    /**
     * Deletes an approval decision by ID.
     * Evicts all approval-decisions cache entries.
     *
     * @param id the approval decision ID to delete
     */
    @CacheEvict(value = "approval-decisions", allEntries = true)
    public void deleteById(ApprovalDecisionId id) {
        log.info("Deleting approval decision: {}", id);
        repository.deleteById(id);
    }
}
