package com.example.control.infrastructure.adapter.mongo;

import com.example.control.domain.ServiceShare;
import com.example.control.domain.id.ServiceShareId;
import com.example.control.domain.criteria.ServiceShareCriteria;
import com.example.control.domain.port.ServiceShareRepositoryPort;
import com.example.control.infrastructure.repository.ServiceShareMongoRepository;
import com.example.control.infrastructure.repository.documents.ServiceShareDocument;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MongoDB adapter implementation for {@link ServiceShareRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for service sharing
 * ACL using Spring Data MongoDB.
 * </p>
 */
@Slf4j
@Component
public class ServiceShareMongoAdapter 
    extends AbstractMongoAdapter<ServiceShare, ServiceShareDocument, ServiceShareId, ServiceShareCriteria> 
    implements ServiceShareRepositoryPort {

    private final ServiceShareMongoRepository repository;

    public ServiceShareMongoAdapter(ServiceShareMongoRepository repository, MongoTemplate mongoTemplate) {
        super(repository, mongoTemplate);
        this.repository = repository;
    }

    @Override
    protected ServiceShareDocument toDocument(ServiceShare domain) {
        return ServiceShareDocument.fromDomain(domain);
    }

    @Override
    protected ServiceShare toDomain(ServiceShareDocument document) {
        return document.toDomain();
    }

    @Override
    protected Query buildQuery(ServiceShareCriteria criteria) {
        Query query = new Query();
        if (criteria == null) return query;
        
        // Apply filters
        if (criteria.serviceId() != null) {
            query.addCriteria(Criteria.where("serviceId").is(criteria.serviceId()));
        }
        if (criteria.grantToType() != null) {
            query.addCriteria(Criteria.where("grantToType").is(criteria.grantToType().name()));
        }
        if (criteria.grantToId() != null) {
            query.addCriteria(Criteria.where("grantToId").is(criteria.grantToId()));
        }
        if (criteria.environments() != null && !criteria.environments().isEmpty()) {
            query.addCriteria(Criteria.where("environments").in(criteria.environments()));
        }
        if (criteria.grantedBy() != null) {
            query.addCriteria(Criteria.where("grantedBy").is(criteria.grantedBy()));
        }
        
        // ABAC: Team-based filtering
        if (criteria.userTeamIds() != null && !criteria.userTeamIds().isEmpty()) {
            query.addCriteria(Criteria.where("grantToId").in(criteria.userTeamIds()));
        }
        
        return query;
    }

    @Override
    protected String getCollectionName() {
        return "service_shares";
    }

    @Override
    protected Class<ServiceShareDocument> getDocumentClass() {
        return ServiceShareDocument.class;
    }

    @Override
    public boolean existsByServiceAndGranteeAndEnvironments(String serviceId, 
                                                            ServiceShare.GranteeType grantToType, 
                                                            String grantToId, 
                                                            List<String> environments) {
        log.debug("Checking if service share exists: service={}, grantee={}-{}, environments={}", 
                serviceId, grantToType, grantToId, environments);
        
        return repository.existsByServiceAndGranteeAndEnvironments(
                serviceId, grantToType.name(), grantToId, environments);
    }

    @Override
    public List<ServiceShare.SharePermission> findEffectivePermissions(String userId, 
                                                                      List<String> userTeamIds, 
                                                                      String serviceId, 
                                                                      List<String> environments) {
        log.debug("Finding effective permissions for user: {} on service: {} in environments: {}", 
                userId, serviceId, environments);
        
        List<ServiceShareDocument> documents = repository.findEffectivePermissions(
                userId, userTeamIds, serviceId, environments);
        
        // Collect all permissions from matching shares
        return documents.stream()
                .flatMap(doc -> doc.getPermissions().stream())
                .map(ServiceShare.SharePermission::valueOf)
                .distinct()
                .toList();
    }
}
