package com.example.control.infrastructure.adapter.persistence.mongo.adapter;

import com.example.control.domain.model.ServiceShare;
import com.example.control.domain.criteria.ServiceShareCriteria;
import com.example.control.domain.valueobject.id.ServiceShareId;
import com.example.control.domain.port.repository.ServiceShareRepositoryPort;
import com.example.control.infrastructure.adapter.persistence.mongo.repository.ServiceShareMongoRepository;
import com.example.control.infrastructure.adapter.persistence.mongo.documents.ServiceShareDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

/**
 * MongoDB adapter implementation for {@link ServiceShareRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for service
 * sharing
 * ACL using Spring Data MongoDB.
 * </p>
 */
@Slf4j
@Component
public class ServiceShareMongoAdapter
        extends
        AbstractMongoAdapter<ServiceShare, ServiceShareDocument, ServiceShareId, ServiceShareCriteria, ServiceShareMongoRepository>
        implements ServiceShareRepositoryPort {

    public ServiceShareMongoAdapter(ServiceShareMongoRepository repository, MongoTemplate mongoTemplate) {
        super(repository, mongoTemplate, ServiceShareId::id);
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
        if (criteria == null)
            return query;

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

        // Filter out expired shares: expiresAt == null OR expiresAt > now
        Instant now = Instant.now();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("expiresAt").is(null),
                        Criteria.where("expiresAt").gt(now)));

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
        log.debug("Checking if service share exists with overlap: service={}, grantee={}-{}, environments={}",
                serviceId, grantToType, grantToId, environments);

        // Find all existing shares for this (serviceId, grantToType, grantToId) combination
        // that are not expired
        List<Criteria> baseCriteria = new ArrayList<>();
        baseCriteria.add(Criteria.where("serviceId").is(serviceId));
        baseCriteria.add(Criteria.where("grantToType").is(grantToType.name()));
        baseCriteria.add(Criteria.where("grantToId").is(grantToId));
        
        // Filter out expired shares: expiresAt == null OR expiresAt > now
        Instant now = Instant.now();
        baseCriteria.add(
            new Criteria().orOperator(
                Criteria.where("expiresAt").is(null),
                Criteria.where("expiresAt").gt(now)
            )
        );
        
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(baseCriteria.toArray(new Criteria[0])));
        
        // Fetch all matching shares to check for environment overlaps
        List<ServiceShareDocument> existingShares = mongoTemplate.find(query, ServiceShareDocument.class, getCollectionName());
        
        if (existingShares.isEmpty()) {
            log.debug("No existing shares found, no overlap");
            return false;
        }
        
        // Normalize environments: null or empty means "all environments"
        boolean newShareCoversAll = (environments == null || environments.isEmpty());
        
        // Check each existing share for overlap
        for (ServiceShareDocument existingShare : existingShares) {
            List<String> existingEnvs = existingShare.getEnvironments();
            boolean existingCoversAll = (existingEnvs == null || existingEnvs.isEmpty());
            
            // Case 1: If existing share covers all environments, any new share overlaps
            if (existingCoversAll) {
                log.debug("Overlap detected: existing share covers all environments");
                return true;
            }
            
            // Case 2: If new share covers all environments, it overlaps with any existing share
            if (newShareCoversAll) {
                log.debug("Overlap detected: new share covers all environments, existing share exists");
                return true;
            }
            
            // Case 3: Both have specific environments - check for intersection
            if (existingEnvs != null && !existingEnvs.isEmpty() && environments != null && !environments.isEmpty()) {
                // Check if there's any common environment
                boolean hasIntersection = existingEnvs.stream().anyMatch(environments::contains);
                if (hasIntersection) {
                    log.debug("Overlap detected: environments intersect - existing={}, new={}", existingEnvs, environments);
                    return true;
                }
            }
        }
        
        log.debug("No overlap detected with {} existing shares", existingShares.size());
        return false;
    }

    @Override
    public List<ServiceShare.SharePermission> findEffectivePermissions(String userId,
            List<String> userTeamIds,
            String serviceId,
            List<String> environments) {
        log.debug("Finding effective permissions for user: {} on service: {} in environments: {}",
                userId, serviceId, environments);

        List<ServiceShareDocument> documents = repository.findEffectivePermissions(
                userId, userTeamIds, serviceId, environments, Instant.now());

        // Collect all permissions from matching shares (expiration already filtered in
        // query)
        return documents.stream()
                .flatMap(doc -> doc.getPermissions().stream())
                .map(ServiceShare.SharePermission::valueOf)
                .distinct()
                .toList();
    }

    @Override
    public List<String> findServiceIdsByGranteeTeams(List<String> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) {
            log.debug("No team IDs provided, returning empty list");
            return List.of();
        }

        log.debug("Finding service IDs shared to teams: {}", teamIds);

        // Build query: grantToType = TEAM AND grantToId IN teamIds
        Query query = new Query();
        query.addCriteria(Criteria.where("grantToType").is(ServiceShare.GranteeType.TEAM.name()));
        query.addCriteria(Criteria.where("grantToId").in(teamIds));

        // Filter out expired shares: expiresAt == null OR expiresAt > now
        Instant now = Instant.now();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("expiresAt").is(null),
                        Criteria.where("expiresAt").gt(now)));

        // Project only serviceId field for efficiency
        query.fields().include("serviceId");

        // Execute query
        List<ServiceShareDocument> documents = mongoTemplate.find(query, ServiceShareDocument.class,
                getCollectionName());

        // Extract unique service IDs
        List<String> serviceIds = documents.stream()
                .map(ServiceShareDocument::getServiceId)
                .distinct()
                .toList();

        log.debug("Found {} unique service IDs shared to teams: {}", serviceIds.size(), teamIds);
        return serviceIds;
    }
}
