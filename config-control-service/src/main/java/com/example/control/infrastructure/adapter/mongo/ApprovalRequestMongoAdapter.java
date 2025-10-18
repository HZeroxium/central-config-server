package com.example.control.infrastructure.adapter.mongo;

import com.example.control.domain.ApprovalRequest;
import com.example.control.domain.port.ApprovalRequestRepositoryPort;
import com.example.control.infrastructure.repository.ApprovalRequestMongoRepository;
import com.example.control.infrastructure.repository.documents.ApprovalRequestDocument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB adapter implementation for {@link ApprovalRequestRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for approval workflow
 * requests using Spring Data MongoDB with optimistic locking support.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalRequestMongoAdapter implements ApprovalRequestRepositoryPort {

    private final ApprovalRequestMongoRepository repository;
    private final MongoTemplate mongoTemplate;

    @Override
    public ApprovalRequest save(ApprovalRequest request) {
        log.debug("Saving approval request: {}", request.getId());
        
        ApprovalRequestDocument document = ApprovalRequestDocument.fromDomain(request);
        ApprovalRequestDocument saved = repository.save(document);
        
        log.debug("Successfully saved approval request: {} with version: {}", 
                saved.getId(), saved.getVersion());
        return saved.toDomain();
    }

    @Override
    public Optional<ApprovalRequest> findById(String id) {
        log.debug("Finding approval request by ID: {}", id);
        
        Optional<ApprovalRequestDocument> document = repository.findById(id);
        return document.map(ApprovalRequestDocument::toDomain);
    }

    @Override
    public Page<ApprovalRequest> findAll(Object filter, Pageable pageable) {
        ApprovalRequestFilter requestFilter = (ApprovalRequestFilter) filter;
        log.debug("Listing approval requests with filter: {}, pageable: {}", filter, pageable);
        
        Query query = buildQuery(requestFilter);
        
        // Get total count
        long total = mongoTemplate.count(query, ApprovalRequestDocument.class);
        
        // Apply pagination and sorting
        query.with(pageable);
        
        // Execute query
        List<ApprovalRequestDocument> documents = mongoTemplate.find(query, ApprovalRequestDocument.class);
        
        // Convert to domain objects
        List<ApprovalRequest> requests = documents.stream()
                .map(ApprovalRequestDocument::toDomain)
                .toList();
        
        return new PageImpl<>(requests, pageable, total);
    }

    @Override
    public long count(Object filter) {
        ApprovalRequestFilter requestFilter = (ApprovalRequestFilter) filter;
        Query query = buildQuery(requestFilter);
        return mongoTemplate.count(query, ApprovalRequestDocument.class);
    }

    @Override
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    public long countByStatus(ApprovalRequest.ApprovalStatus status) {
        log.debug("Counting approval requests by status: {}", status);
        
        return repository.countByStatus(status.name());
    }

    public List<ApprovalRequest> findPendingByGate(String gate) {
        log.debug("Finding pending approval requests by gate: {}", gate);
        
        List<ApprovalRequestDocument> documents = repository.findPendingByGate(gate);
        return documents.stream()
                .map(ApprovalRequestDocument::toDomain)
                .toList();
    }

    public List<ApprovalRequest> findByRequester(String requesterUserId) {
        log.debug("Finding approval requests by requester: {}", requesterUserId);
        
        List<ApprovalRequestDocument> documents = repository.findByRequesterUserId(requesterUserId);
        return documents.stream()
                .map(ApprovalRequestDocument::toDomain)
                .toList();
    }

    @Override
    public boolean updateStatusAndVersion(String id, ApprovalRequest.ApprovalStatus status, Integer version) {
        log.debug("Updating approval request status and version: id={}, status={}, version={}", 
                id, status, version);
        
        try {
            Instant now = Instant.now();
            long updatedCount = repository.updateStatusAndVersion(id, status.name(), version, now, version + 1);
            boolean updated = updatedCount > 0;
            
            if (updated) {
                log.debug("Successfully updated approval request: {} to status: {} with version: {}", 
                        id, status, version + 1);
            } else {
                log.debug("Failed to update approval request: {} - version conflict or not found", id);
            }
            
            return updated;
        } catch (Exception e) {
            log.error("Error updating approval request: {} to status: {}", id, status, e);
            return false;
        }
    }

    /**
     * Build MongoDB query from filter criteria.
     *
     * @param filter the filter criteria
     * @return MongoDB query object
     */
    private Query buildQuery(ApprovalRequestFilter filter) {
        Query query = new Query();
        
        if (filter != null) {
            if (filter.requesterUserId() != null) {
                query.addCriteria(Criteria.where("requesterUserId").is(filter.requesterUserId()));
            }
            
            if (filter.status() != null) {
                query.addCriteria(Criteria.where("status").is(filter.status().name()));
            }
            
            if (filter.requestType() != null) {
                query.addCriteria(Criteria.where("requestType").is(filter.requestType().name()));
            }
            
            if (filter.fromDate() != null) {
                query.addCriteria(Criteria.where("createdAt").gte(filter.fromDate()));
            }
            
            if (filter.toDate() != null) {
                query.addCriteria(Criteria.where("createdAt").lte(filter.toDate()));
            }
            
            if (filter.gate() != null) {
                query.addCriteria(Criteria.where("requiredGatesJson").regex(filter.gate(), "i"));
            }
        }
        
        return query;
    }
}
