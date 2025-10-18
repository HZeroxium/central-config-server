package com.example.control.infrastructure.adapter.mongo;

import com.example.control.domain.ServiceShare;
import com.example.control.domain.port.ServiceShareRepositoryPort;
import com.example.control.infrastructure.repository.ServiceShareMongoRepository;
import com.example.control.infrastructure.repository.documents.ServiceShareDocument;

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
 * MongoDB adapter implementation for {@link ServiceShareRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for service sharing
 * ACL using Spring Data MongoDB.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceShareMongoAdapter implements ServiceShareRepositoryPort {

    private final ServiceShareMongoRepository repository;
    private final MongoTemplate mongoTemplate;

    @Override
    public ServiceShare save(ServiceShare share) {
        log.debug("Saving service share: {}", share.getId());
        
        ServiceShareDocument document = ServiceShareDocument.fromDomain(share);
        ServiceShareDocument saved = repository.save(document);
        
        log.debug("Successfully saved service share: {}", saved.getId());
        return saved.toDomain();
    }

    @Override
    public Optional<ServiceShare> findById(String id) {
        log.debug("Finding service share by ID: {}", id);
        
        Optional<ServiceShareDocument> document = repository.findById(id);
        return document.map(ServiceShareDocument::toDomain);
    }

    @Override
    public List<ServiceShare> findByService(String serviceId) {
        log.debug("Finding service shares by service ID: {}", serviceId);
        
        List<ServiceShareDocument> documents = repository.findByServiceId(serviceId);
        return documents.stream()
                .map(ServiceShareDocument::toDomain)
                .toList();
    }

    @Override
    public List<ServiceShare> findByGrantee(ServiceShare.GranteeType grantToType, String grantToId) {
        log.debug("Finding service shares by grantee: {} - {}", grantToType, grantToId);
        
        List<ServiceShareDocument> documents = repository.findByGrantToTypeAndGrantToId(
                grantToType.name(), grantToId);
        return documents.stream()
                .map(ServiceShareDocument::toDomain)
                .toList();
    }

    @Override
    public Page<ServiceShare> list(ServiceShareFilter filter, Pageable pageable) {
        log.debug("Listing service shares with filter: {}, pageable: {}", filter, pageable);
        
        Query query = buildQuery(filter);
        
        // Get total count
        long total = mongoTemplate.count(query, ServiceShareDocument.class);
        
        // Apply pagination and sorting
        query.with(pageable);
        
        // Execute query
        List<ServiceShareDocument> documents = mongoTemplate.find(query, ServiceShareDocument.class);
        
        // Convert to domain objects
        List<ServiceShare> shares = documents.stream()
                .map(ServiceShareDocument::toDomain)
                .toList();
        
        return new PageImpl<>(shares, pageable, total);
    }

    @Override
    public void delete(String id) {
        log.debug("Deleting service share: {}", id);
        
        repository.deleteById(id);
        log.debug("Successfully deleted service share: {}", id);
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

    /**
     * Build MongoDB query from filter criteria.
     *
     * @param filter the filter criteria
     * @return MongoDB query object
     */
    private Query buildQuery(ServiceShareFilter filter) {
        Query query = new Query();
        
        if (filter != null) {
            if (filter.serviceId() != null) {
                query.addCriteria(Criteria.where("serviceId").is(filter.serviceId()));
            }
            
            if (filter.grantToType() != null) {
                query.addCriteria(Criteria.where("grantToType").is(filter.grantToType().name()));
            }
            
            if (filter.grantToId() != null) {
                query.addCriteria(Criteria.where("grantToId").is(filter.grantToId()));
            }
            
            if (filter.environments() != null && !filter.environments().isEmpty()) {
                query.addCriteria(Criteria.where("environments").in(filter.environments()));
            }
            
            if (filter.grantedBy() != null) {
                query.addCriteria(Criteria.where("grantedBy").is(filter.grantedBy()));
            }
        }
        
        return query;
    }
}
