package com.example.control.infrastructure.adapter.mongo;

import com.example.control.domain.ApprovalDecision;
import com.example.control.domain.port.ApprovalDecisionRepositoryPort;
import com.example.control.infrastructure.repository.ApprovalDecisionMongoRepository;
import com.example.control.infrastructure.repository.documents.ApprovalDecisionDocument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB adapter implementation for {@link ApprovalDecisionRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for approval decisions
 * using Spring Data MongoDB with compound unique index enforcement.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalDecisionMongoAdapter implements ApprovalDecisionRepositoryPort {

    private final ApprovalDecisionMongoRepository repository;
    private final MongoTemplate mongoTemplate;

    @Override
    public ApprovalDecision save(ApprovalDecision decision) {
        log.debug("Saving approval decision: {}", decision.getId());
        
        ApprovalDecisionDocument document = ApprovalDecisionDocument.fromDomain(decision);
        
        try {
            ApprovalDecisionDocument saved = repository.save(document);
            log.debug("Successfully saved approval decision: {}", saved.getId());
            return saved.toDomain();
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.warn("Duplicate approval decision detected for request: {}, approver: {}, gate: {}", 
                    decision.getRequestId(), decision.getApproverUserId(), decision.getGate());
            throw new IllegalStateException("A decision already exists for this request, approver, and gate", e);
        }
    }

    @Override
    public Optional<ApprovalDecision> findById(String id) {
        log.debug("Finding approval decision by ID: {}", id);
        
        Optional<ApprovalDecisionDocument> document = repository.findById(id);
        return document.map(ApprovalDecisionDocument::toDomain);
    }

    @Override
    public List<ApprovalDecision> findByRequestId(String requestId) {
        log.debug("Finding approval decisions by request ID: {}", requestId);
        
        List<ApprovalDecisionDocument> documents = repository.findByRequestId(requestId);
        return documents.stream()
                .map(ApprovalDecisionDocument::toDomain)
                .toList();
    }

    @Override
    public List<ApprovalDecision> findByApprover(String approverUserId) {
        log.debug("Finding approval decisions by approver: {}", approverUserId);
        
        List<ApprovalDecisionDocument> documents = repository.findByApproverUserId(approverUserId);
        return documents.stream()
                .map(ApprovalDecisionDocument::toDomain)
                .toList();
    }

    @Override
    public List<ApprovalDecision> findByRequestIdAndGate(String requestId, String gate) {
        log.debug("Finding approval decisions by request ID: {} and gate: {}", requestId, gate);
        
        List<ApprovalDecisionDocument> documents = repository.findByRequestIdAndGate(requestId, gate);
        return documents.stream()
                .map(ApprovalDecisionDocument::toDomain)
                .toList();
    }

    @Override
    public boolean existsByRequestAndApproverAndGate(String requestId, String approverUserId, String gate) {
        log.debug("Checking if approval decision exists: request={}, approver={}, gate={}", 
                requestId, approverUserId, gate);
        
        return repository.existsByRequestIdAndApproverUserIdAndGate(requestId, approverUserId, gate);
    }

    @Override
    public long countByRequestIdAndGate(String requestId, String gate) {
        log.debug("Counting approval decisions by request ID: {} and gate: {}", requestId, gate);
        
        return repository.countByRequestIdAndGate(requestId, gate);
    }

    @Override
    public long countByRequestIdAndGateAndDecision(String requestId, String gate, ApprovalDecision.Decision decision) {
        log.debug("Counting approval decisions by request ID: {}, gate: {}, decision: {}", 
                requestId, gate, decision);
        
        return repository.countByRequestIdAndGateAndDecision(requestId, gate, decision.name());
    }

    @Override
    public Page<ApprovalDecision> list(ApprovalDecisionFilter filter, Pageable pageable) {
        log.debug("Listing approval decisions with filter: {}, pageable: {}", filter, pageable);
        
        Query query = buildQuery(filter);
        
        // Get total count
        long total = mongoTemplate.count(query, ApprovalDecisionDocument.class);
        
        // Apply pagination and sorting
        query.with(pageable);
        
        // Execute query
        List<ApprovalDecisionDocument> documents = mongoTemplate.find(query, ApprovalDecisionDocument.class);
        
        // Convert to domain objects
        List<ApprovalDecision> decisions = documents.stream()
                .map(ApprovalDecisionDocument::toDomain)
                .toList();
        
        return new PageImpl<>(decisions, pageable, total);
    }

    /**
     * Build MongoDB query from filter criteria.
     *
     * @param filter the filter criteria
     * @return MongoDB query object
     */
    private Query buildQuery(ApprovalDecisionFilter filter) {
        Query query = new Query();
        
        if (filter != null) {
            if (filter.requestId() != null) {
                query.addCriteria(Criteria.where("requestId").is(filter.requestId()));
            }
            
            if (filter.approverUserId() != null) {
                query.addCriteria(Criteria.where("approverUserId").is(filter.approverUserId()));
            }
            
            if (filter.gate() != null) {
                query.addCriteria(Criteria.where("gate").is(filter.gate()));
            }
            
            if (filter.decision() != null) {
                query.addCriteria(Criteria.where("decision").is(filter.decision().name()));
            }
        }
        
        return query;
    }
}
