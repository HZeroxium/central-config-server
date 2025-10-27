package com.example.control.infrastructure.mongo.adapter;

import com.example.control.domain.object.ApprovalDecision;
import com.example.control.domain.criteria.ApprovalDecisionCriteria;
import com.example.control.domain.id.ApprovalDecisionId;
import com.example.control.domain.id.ApprovalRequestId;
import com.example.control.domain.port.ApprovalDecisionRepositoryPort;
import com.example.control.infrastructure.mongo.repository.ApprovalDecisionMongoRepository;
import com.example.control.infrastructure.mongo.documents.ApprovalDecisionDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

/**
 * MongoDB adapter implementation for {@link ApprovalDecisionRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for approval
 * decisions
 * using Spring Data MongoDB with compound unique index enforcement.
 * </p>
 */
@Slf4j
@Component
public class ApprovalDecisionMongoAdapter
        extends
        AbstractMongoAdapter<ApprovalDecision, ApprovalDecisionDocument, ApprovalDecisionId, ApprovalDecisionCriteria, ApprovalDecisionMongoRepository>
        implements ApprovalDecisionRepositoryPort {

    public ApprovalDecisionMongoAdapter(ApprovalDecisionMongoRepository repository, MongoTemplate mongoTemplate) {
        super(repository, mongoTemplate, ApprovalDecisionId::id);
    }

    @Override
    protected ApprovalDecisionDocument toDocument(ApprovalDecision domain) {
        return ApprovalDecisionDocument.fromDomain(domain);
    }

    @Override
    protected ApprovalDecision toDomain(ApprovalDecisionDocument document) {
        return document.toDomain();
    }

    @Override
    protected Query buildQuery(ApprovalDecisionCriteria criteria) {
        Query query = new Query();
        if (criteria == null)
            return query;

        // Apply filters
        if (criteria.requestId() != null) {
            query.addCriteria(Criteria.where("requestId").is(criteria.requestId()));
        }
        if (criteria.approverUserId() != null) {
            query.addCriteria(Criteria.where("approverUserId").is(criteria.approverUserId()));
        }
        if (criteria.gate() != null) {
            query.addCriteria(Criteria.where("gate").is(criteria.gate()));
        }
        if (criteria.decision() != null) {
            query.addCriteria(Criteria.where("decision").is(criteria.decision().name()));
        }

        // ABAC: Team-based filtering
        if (criteria.userTeamIds() != null && !criteria.userTeamIds().isEmpty()) {
            query.addCriteria(Criteria.where("approverUserId").in(criteria.userTeamIds()));
        }

        return query;
    }

    @Override
    protected String getCollectionName() {
        return "approval_decisions";
    }

    @Override
    protected Class<ApprovalDecisionDocument> getDocumentClass() {
        return ApprovalDecisionDocument.class;
    }

    @Override
    public boolean existsByRequestAndApproverAndGate(ApprovalRequestId requestId, String approverUserId, String gate) {
        log.debug("Checking if approval decision exists: request={}, approver={}, gate={}",
                requestId, approverUserId, gate);

        return repository.existsByRequestIdAndApproverUserIdAndGate(requestId.id(), approverUserId, gate);
    }

    @Override
    public long countByRequestIdAndGate(ApprovalRequestId requestId, String gate) {
        log.debug("Counting approval decisions by request ID: {} and gate: {}", requestId, gate);

        return repository.countByRequestIdAndGate(requestId.id(), gate);
    }

    @Override
    public long countByRequestIdAndGateAndDecision(ApprovalRequestId requestId, String gate,
            ApprovalDecision.Decision decision) {
        log.debug("Counting approval decisions by request ID: {}, gate: {}, decision: {}",
                requestId, gate, decision);

        return repository.countByRequestIdAndGateAndDecision(requestId.id(), gate, decision.name());
    }
}
