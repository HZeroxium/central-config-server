package com.example.control.application.query;

import com.example.control.domain.criteria.ApprovalRequestCriteria;
import com.example.control.domain.id.ApprovalRequestId;
import com.example.control.domain.object.ApprovalRequest;
import com.example.control.domain.port.ApprovalRequestRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Query service for ApprovalRequest read operations.
 * <p>
 * Handles all read operations for ApprovalRequest domain objects with caching.
 * Responsible for data retrieval only - no writes, no business logic, no
 * permission checks.
 * All methods are read-only with appropriate caching strategies.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalRequestQueryService {

  private final ApprovalRequestRepositoryPort repository;

  /**
   * Find approval request by ID.
   *
   * @param id the approval request ID
   * @return the approval request if found
   */
  @Cacheable(value = "approval-requests", key = "#id")
  public Optional<ApprovalRequest> findById(ApprovalRequestId id) {
    log.debug("Finding approval request by ID: {}", id);
    return repository.findById(id);
  }

  /**
   * List approval requests with filtering and pagination.
   *
   * @param criteria the search criteria
   * @param pageable pagination information
   * @return page of approval requests
   */
  @Cacheable(value = "approval-requests", key = "'list:' + #criteria.hashCode() + ':' + #pageable")
  public Page<ApprovalRequest> findAll(ApprovalRequestCriteria criteria, Pageable pageable) {
    log.debug("Listing approval requests with criteria: {}", criteria);
    return repository.findAll(criteria, pageable);
  }

  /**
   * Count approval requests matching the given filter criteria.
   *
   * @param criteria the filter criteria
   * @return count of matching requests
   */
  @Cacheable(value = "approval-requests", key = "'count:' + #criteria.hashCode()")
  public long count(ApprovalRequestCriteria criteria) {
    log.debug("Counting approval requests with criteria: {}", criteria);
    return repository.count(criteria);
  }

  /**
   * Check if an approval request exists.
   *
   * @param id the approval request ID
   * @return true if exists, false otherwise
   */
  public boolean existsById(ApprovalRequestId id) {
    log.debug("Checking existence of approval request: {}", id);
    return repository.existsById(id);
  }

  /**
   * Count approval requests by status.
   *
   * @param status the approval status
   * @return number of requests with the given status
   */
  @Cacheable(value = "approval-requests", key = "'countByStatus:' + #status")
  public long countByStatus(ApprovalRequest.ApprovalStatus status) {
    log.debug("Counting approval requests by status: {}", status);
    return repository.countByStatus(status);
  }

  /**
   * Check if there is an existing PENDING request by requester for a service.
   *
   * @param requesterUserId requester user ID
   * @param serviceId       target service ID
   * @return true if such a PENDING request exists
   */
  public boolean existsPendingByRequesterAndService(String requesterUserId, String serviceId) {
    log.debug("Checking if pending request exists for requester: {} and service: {}", requesterUserId, serviceId);
    return repository.existsPendingByRequesterAndService(requesterUserId, serviceId);
  }

  /**
   * Find all PENDING approval requests for a specific service.
   * Used for cascade decision generation.
   *
   * @param serviceId target service ID
   * @return list of PENDING requests for the service
   */
  public List<ApprovalRequest> findAllPendingByServiceId(String serviceId) {
    log.debug("Finding all pending requests for service: {}", serviceId);
    return repository.findAllPendingByServiceId(serviceId);
  }
}
