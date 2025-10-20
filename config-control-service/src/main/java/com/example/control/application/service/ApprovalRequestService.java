package com.example.control.application.service;

import com.example.control.config.security.DomainPermissionEvaluator;
import com.example.control.config.security.UserContext;
import com.example.control.domain.ApprovalRequest;
import com.example.control.domain.criteria.ApprovalRequestCriteria;
import com.example.control.domain.id.ApprovalRequestId;
import com.example.control.domain.port.ApprovalRequestRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing approval requests with team-based access control.
 * <p>
 * Provides CRUD operations for approval requests with permission checks
 * and caching for performance optimization.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalRequestService {

    private final ApprovalRequestRepositoryPort repository;
    private final DomainPermissionEvaluator permissionEvaluator;

    /**
     * Save or update an approval request.
     * <p>
     * Evicts cache entries to ensure consistency.
     *
     * @param request the approval request to save
     * @return the saved request
     */
    @Transactional
    @CacheEvict(value = "approval-requests", allEntries = true)
    public ApprovalRequest save(ApprovalRequest request) {
        log.debug("Saving approval request: {}", request.getId());
        ApprovalRequest saved = repository.save(request);
        log.debug("Saved approval request: {}", saved.getId());
        return saved;
    }

    /**
     * Find approval request by ID with permission check.
     * <p>
     * Users can only view their own requests unless they are SYS_ADMIN.
     *
     * @param id the request ID
     * @param userContext the user context for permission check
     * @return the approval request if found and accessible
     */
    @Cacheable(value = "approval-requests", key = "#id + ':' + #userContext.userId")
    public Optional<ApprovalRequest> findById(ApprovalRequestId id, UserContext userContext) {
        log.debug("Finding approval request by ID: {} for user: {}", id, userContext.getUserId());
        
        Optional<ApprovalRequest> request = repository.findById(id);
        if (request.isEmpty()) {
            return Optional.empty();
        }
        
        ApprovalRequest approvalRequest = request.get();
        
        // Check permissions: user can view their own requests or be SYS_ADMIN
        if (!approvalRequest.getRequesterUserId().equals(userContext.getUserId()) && 
            !userContext.isSysAdmin()) {
            log.warn("User {} attempted to access request {} without permission", 
                    userContext.getUserId(), id);
            return Optional.empty();
        }
        
        log.debug("Found approval request: {}", approvalRequest.getId());
        return Optional.of(approvalRequest);
    }

    /**
     * List approval requests with filtering and pagination.
     * <p>
     * Regular users see only their own requests, SYS_ADMIN sees all.
     *
     * @param criteria the search criteria
     * @param pageable pagination information
     * @param userContext the user context for filtering
     * @return page of approval requests
     */
    @Cacheable(value = "approval-requests", key = "'list:' + #criteria.hashCode() + ':' + #pageable + ':' + #userContext.userId")
    public Page<ApprovalRequest> findAll(ApprovalRequestCriteria criteria, 
                                     Pageable pageable, 
                                     UserContext userContext) {
        log.debug("Listing approval requests for user: {}", userContext.getUserId());
        
        // Apply user-based filtering
        ApprovalRequestCriteria userCriteria = criteria.toBuilder()
                .requesterUserId(userContext.isSysAdmin() ? null : userContext.getUserId())
                .build();
        
        Page<ApprovalRequest> result = repository.findAll(userCriteria, pageable);
        log.debug("Found {} approval requests for user: {}", result.getTotalElements(), userContext.getUserId());
        return result;
    }

    /**
     * Count approval requests by status.
     *
     * @param status the approval status
     * @return count of requests with the given status
     */
    @Cacheable(value = "approval-requests", key = "'count:' + #status")
    public long countByStatus(ApprovalRequest.ApprovalStatus status) {
        log.debug("Counting approval requests by status: {}", status);
        long count = repository.countByStatus(status);
        log.debug("Found {} requests with status: {}", count, status);
        return count;
    }

    /**
     * Cancel an approval request with permission check.
     * <p>
     * Only the requester or SYS_ADMIN can cancel requests.
     *
     * @param id the request ID
     * @param userContext the user context for permission check
     */
    @Transactional
    @CacheEvict(value = "approval-requests", allEntries = true)
    public void cancelRequest(ApprovalRequestId id, UserContext userContext) {
        log.debug("Cancelling approval request: {} by user: {}", id, userContext.getUserId());
        
        Optional<ApprovalRequest> requestOpt = repository.findById(id);
        if (requestOpt.isEmpty()) {
            throw new IllegalArgumentException("Approval request not found: " + id);
        }
        
        ApprovalRequest request = requestOpt.get();
        
        // Check permissions
        if (!permissionEvaluator.canCancelRequest(userContext, request)) {
            throw new SecurityException("User " + userContext.getUserId() + 
                    " is not authorized to cancel request " + id);
        }
        
        // Cancel the request
        ApprovalRequest cancelledRequest = request.toBuilder()
                .status(ApprovalRequest.ApprovalStatus.CANCELLED)
                .cancelReason("Cancelled by " + userContext.getUserId())
                .build();
        
        repository.save(cancelledRequest);
        log.debug("Cancelled approval request: {}", id);
    }

    /**
     * Update request status and version for optimistic locking.
     * <p>
     * This method provides atomic update for high-contention operations.
     *
     * @param id the request ID
     * @param status the new status
     * @param version the expected version for optimistic locking
     * @return true if update successful, false if version conflict
     */
    @Transactional
    @CacheEvict(value = "approval-requests", allEntries = true)
    public boolean updateStatus(ApprovalRequestId id, 
                               ApprovalRequest.ApprovalStatus status, 
                               Integer version) {
        log.debug("Updating approval request status: {} to {} with version: {}", id, status, version);
        
        boolean updated = repository.updateStatusAndVersion(id, status, version);
        if (updated) {
            log.debug("Successfully updated approval request status: {}", id);
        } else {
            log.warn("Failed to update approval request status due to version conflict: {}", id);
        }
        
        return updated;
    }

    /**
     * Find approval request by ID without permission check.
     * <p>
     * Used internally by other services that need to access requests.
     *
     * @param id the request ID
     * @return the approval request if found
     */
    public Optional<ApprovalRequest> findById(ApprovalRequestId id) {
        log.debug("Finding approval request by ID: {}", id);
        Optional<ApprovalRequest> result = repository.findById(id);
        log.debug("Found approval request: {}", result.isPresent());
        return result;
    }

    /**
     * Check if an approval request exists.
     *
     * @param id the request ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(ApprovalRequestId id) {
        log.debug("Checking existence of approval request: {}", id);
        boolean exists = repository.existsById(id);
        log.debug("Approval request exists: {}", exists);
        return exists;
    }
}
