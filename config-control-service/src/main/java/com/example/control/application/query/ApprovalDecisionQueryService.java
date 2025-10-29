package com.example.control.application.query;

import com.example.control.domain.criteria.ApprovalDecisionCriteria;
import com.example.control.domain.id.ApprovalDecisionId;
import com.example.control.domain.id.ApprovalRequestId;
import com.example.control.domain.object.ApprovalDecision;
import com.example.control.domain.port.ApprovalDecisionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Query service for ApprovalDecision read operations.
 * <p>
 * Handles all read operations for ApprovalDecision domain objects with caching.
 * Responsible for data retrieval only - no writes, no business logic, no
 * permission checks.
 * All methods are read-only with appropriate caching strategies.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalDecisionQueryService {

  private final ApprovalDecisionRepositoryPort repository;

  /**
   * Find approval decision by ID.
   *
   * @param id the approval decision ID
   * @return the approval decision if found
   */
  @Cacheable(value = "approval-decisions", key = "#id")
  public Optional<ApprovalDecision> findById(ApprovalDecisionId id) {
    log.debug("Finding approval decision by ID: {}", id);
    return repository.findById(id);
  }

  /**
   * List approval decisions with filtering and pagination.
   *
   * @param criteria the search criteria
   * @param pageable pagination information
   * @return page of approval decisions
   */
  @Cacheable(value = "approval-decisions", key = "'list:' + #criteria.hashCode() + ':' + #pageable")
  public Page<ApprovalDecision> findAll(ApprovalDecisionCriteria criteria, Pageable pageable) {
    log.debug("Listing approval decisions with criteria: {}", criteria);
    return repository.findAll(criteria, pageable);
  }

  /**
   * Count approval decisions matching the given filter criteria.
   *
   * @param criteria the filter criteria
   * @return count of matching decisions
   */
  @Cacheable(value = "approval-decisions", key = "'count:' + #criteria.hashCode()")
  public long count(ApprovalDecisionCriteria criteria) {
    log.debug("Counting approval decisions with criteria: {}", criteria);
    return repository.count(criteria);
  }

  /**
   * Check if an approval decision exists.
   *
   * @param id the approval decision ID
   * @return true if exists, false otherwise
   */
  public boolean existsById(ApprovalDecisionId id) {
    log.debug("Checking existence of approval decision: {}", id);
    return repository.existsById(id);
  }

  /**
   * Check if a decision exists for a specific request, approver, and gate.
   * <p>
   * This enforces the constraint that each user can only make one decision
   * per request per gate.
   *
   * @param requestId      the approval request ID
   * @param approverUserId the user ID of the approver
   * @param gate           the gate name
   * @return true if decision exists, false otherwise
   */
  @Cacheable(value = "approval-decisions", key = "'exists:' + #requestId + ':' + #approverUserId + ':' + #gate")
  public boolean existsByRequestAndApproverAndGate(ApprovalRequestId requestId, String approverUserId, String gate) {
    log.debug("Checking if decision exists for request: {}, approver: {}, gate: {}", requestId, approverUserId, gate);
    return repository.existsByRequestAndApproverAndGate(requestId, approverUserId, gate);
  }

  /**
   * Count decisions by decision type for a specific request and gate.
   *
   * @param requestId the approval request ID
   * @param gate      the gate name
   * @param decision  the decision type (APPROVE or REJECT)
   * @return number of decisions of the specified type
   */
  @Cacheable(value = "approval-decisions", key = "'count-by-request-gate-decision:' + #requestId + ':' + #gate + ':' + #decision")
  public long countByRequestIdAndGateAndDecision(ApprovalRequestId requestId, String gate,
      ApprovalDecision.Decision decision) {
    log.debug("Counting decisions for request: {}, gate: {}, decision: {}", requestId, gate, decision);
    return repository.countByRequestIdAndGateAndDecision(requestId, gate, decision);
  }
}
