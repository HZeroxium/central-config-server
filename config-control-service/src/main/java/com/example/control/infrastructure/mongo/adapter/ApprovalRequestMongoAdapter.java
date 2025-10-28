package com.example.control.infrastructure.mongo.adapter;

import com.example.control.domain.object.ApprovalRequest;
import com.example.control.domain.criteria.ApprovalRequestCriteria;
import com.example.control.domain.id.ApprovalRequestId;
import com.example.control.domain.port.ApprovalRequestRepositoryPort;
import com.example.control.infrastructure.mongo.repository.ApprovalRequestMongoRepository;
import com.example.control.infrastructure.mongo.documents.ApprovalRequestDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB adapter implementation for {@link ApprovalRequestRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for approval
 * workflow
 * requests using Spring Data MongoDB with optimistic locking support.
 * </p>
 */
@Slf4j
@Component
public class ApprovalRequestMongoAdapter
        extends
        AbstractMongoAdapter<ApprovalRequest, ApprovalRequestDocument, ApprovalRequestId, ApprovalRequestCriteria, ApprovalRequestMongoRepository>
        implements ApprovalRequestRepositoryPort {

    public ApprovalRequestMongoAdapter(ApprovalRequestMongoRepository repository, MongoTemplate mongoTemplate) {
        super(repository, mongoTemplate, ApprovalRequestId::id);
    }

    @Override
    protected ApprovalRequestDocument toDocument(ApprovalRequest domain) {
        return ApprovalRequestDocument.fromDomain(domain);
    }

    @Override
    protected ApprovalRequest toDomain(ApprovalRequestDocument document) {
        return document.toDomain();
    }

    @Override
    protected Query buildQuery(ApprovalRequestCriteria criteria) {
        Query query = new Query();
        if (criteria == null)
            return query;

        // Apply filters
        if (criteria.requesterUserId() != null) {
            query.addCriteria(Criteria.where("requesterUserId").is(criteria.requesterUserId()));
        }
        if (criteria.status() != null) {
            query.addCriteria(Criteria.where("status").is(criteria.status().name()));
        }
        if (criteria.requestType() != null) {
            query.addCriteria(Criteria.where("requestType").is(criteria.requestType().name()));
        }
        if (criteria.fromDate() != null) {
            query.addCriteria(Criteria.where("createdAt").gte(criteria.fromDate()));
        }
        if (criteria.toDate() != null) {
            query.addCriteria(Criteria.where("createdAt").lte(criteria.toDate()));
        }
        if (criteria.gate() != null) {
            query.addCriteria(Criteria.where("requiredGatesJson").regex(criteria.gate(), "i"));
        }

        // ABAC: Team-based filtering
        if (criteria.userTeamIds() != null && !criteria.userTeamIds().isEmpty()) {
            query.addCriteria(Criteria.where("requesterUserId").in(criteria.userTeamIds()));
        }

        return query;
    }

    @Override
    protected String getCollectionName() {
        return "approval_requests";
    }

    @Override
    protected Class<ApprovalRequestDocument> getDocumentClass() {
        return ApprovalRequestDocument.class;
    }

    @Override
    public long countByStatus(ApprovalRequest.ApprovalStatus status) {
        log.debug("Counting approval requests by status: {}", status);

        return repository.countByStatus(status.name());
    }

    @Override
    public boolean updateStatusAndVersion(ApprovalRequestId id, ApprovalRequest.ApprovalStatus status,
            Integer version) {
        log.debug("Updating approval request status and version: id={}, status={}, version={}",
                id, status, version);

        try {
            Instant now = Instant.now();
            long updatedCount = repository.updateStatusAndVersion(id.id(), status.name(), version, now, version + 1);
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

    @Override
    public boolean existsPendingByRequesterAndService(String requesterUserId, String serviceId) {
        return repository.existsByRequesterUserIdAndTargetServiceIdAndStatus(requesterUserId, serviceId, "PENDING");
    }

    @Override
    public long cascadeApproveSameTeamPending(String serviceId, String teamId) {
        return repository.cascadeApproveSameTeamPending(serviceId, teamId, Instant.now());
    }

    @Override
    public long cascadeRejectOtherTeamsPending(String serviceId, String approvedTeamId, String reason) {
        return repository.cascadeRejectOtherTeamsPending(serviceId, approvedTeamId, Instant.now(), reason);
    }

    @Override
    public List<ApprovalRequest> findAllPendingByServiceId(String serviceId) {
        log.debug("Finding all PENDING requests for service: {}", serviceId);

        Query query = new Query();
        query.addCriteria(Criteria.where("targetServiceId").is(serviceId));
        query.addCriteria(Criteria.where("status").is("PENDING"));

        List<ApprovalRequestDocument> documents = mongoTemplate.find(query, ApprovalRequestDocument.class);
        List<ApprovalRequest> requests = documents.stream()
                .map(this::toDomain)
                .toList();

        log.debug("Found {} PENDING requests for service: {}", requests.size(), serviceId);
        return requests;
    }
}
