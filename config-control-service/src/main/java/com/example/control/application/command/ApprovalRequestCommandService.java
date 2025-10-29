package com.example.control.application.command;

import com.example.control.domain.id.ApprovalRequestId;
import com.example.control.domain.object.ApprovalRequest;
import com.example.control.domain.port.ApprovalRequestRepositoryPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/**
 * Command service for ApprovalRequest write operations.
 * <p>
 * Handles all write operations (save, update, delete) for ApprovalRequest
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
public class ApprovalRequestCommandService {

  private final ApprovalRequestRepositoryPort repository;

  /**
   * Saves an approval request (create or update).
   * Automatically generates ID if null.
   * Evicts all approval-requests cache entries.
   *
   * @param request the approval request to save
   * @return the saved approval request
   */
  @CacheEvict(value = "approval-requests", allEntries = true)
  public ApprovalRequest save(@Valid ApprovalRequest request) {
    log.debug("Saving approval request: {}", request.getId());

    if (request.getId() == null) {
      request.setId(ApprovalRequestId.of(UUID.randomUUID().toString()));
      log.debug("Generated new ID for approval request: {}", request.getId());
    }

    ApprovalRequest saved = repository.save(request);
    log.info("Saved approval request: {} by user: {}",
        saved.getId(), saved.getRequesterUserId());
    return saved;
  }

  /**
   * Deletes an approval request by ID.
   * Evicts all approval-requests cache entries.
   *
   * @param id the approval request ID to delete
   */
  @CacheEvict(value = "approval-requests", allEntries = true)
  public void deleteById(ApprovalRequestId id) {
    log.info("Deleting approval request: {}", id);
    repository.deleteById(id);
  }

  /**
   * Updates status and version for optimistic locking.
   * <p>
   * Used to atomically transition request status while preventing lost updates.
   *
   * @param id      the approval request ID
   * @param status  the new status to set
   * @param version the expected current version for optimistic locking
   * @return true if update succeeded (version matched), false otherwise
   */
  @CacheEvict(value = "approval-requests", allEntries = true)
  public boolean updateStatusAndVersion(ApprovalRequestId id, ApprovalRequest.ApprovalStatus status, Integer version) {
    log.info("Updating approval request: {} to status: {} with version: {}", id, status, version);
    return repository.updateStatusAndVersion(id, status, version);
  }

  /**
   * Cascades approval for all PENDING requests from the same team for the same
   * service.
   * <p>
   * When one team's request is approved, other requests from the same team for
   * the same service
   * should also be auto-approved to avoid duplicate ownership requests.
   *
   * @param serviceId      target service ID
   * @param approvedTeamId the team whose requests should be cascaded
   * @return number of requests auto-approved
   */
  @CacheEvict(value = "approval-requests", allEntries = true)
  public long cascadeApproveSameTeamPending(String serviceId, String approvedTeamId) {
    log.info("Cascading approval for service: {} and team: {}", serviceId, approvedTeamId);
    long count = repository.cascadeApproveSameTeamPending(serviceId, approvedTeamId);
    log.info("Cascaded {} approval requests", count);
    return count;
  }

  /**
   * Cascades rejection for all PENDING requests from other teams for the same
   * service.
   * <p>
   * When a team is granted ownership, all competing requests from other teams
   * should be rejected.
   *
   * @param serviceId       target service ID
   * @param approvedTeamId  the team that was approved
   * @param rejectionReason reason for rejection
   * @return number of requests auto-rejected
   */
  @CacheEvict(value = "approval-requests", allEntries = true)
  public long cascadeRejectOtherTeamsPending(String serviceId, String approvedTeamId, String rejectionReason) {
    log.info("Cascading rejection for service: {} excluding team: {}", serviceId, approvedTeamId);
    long count = repository.cascadeRejectOtherTeamsPending(serviceId, approvedTeamId, rejectionReason);
    log.info("Cascaded {} rejection requests", count);
    return count;
  }
}
