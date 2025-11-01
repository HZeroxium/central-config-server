package com.example.control.infrastructure.adapter.persistence.mongo.adapter;

import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.criteria.ApplicationServiceCriteria;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.port.repository.ApplicationServiceRepositoryPort;
import com.example.control.infrastructure.adapter.persistence.mongo.repository.ApplicationServiceMongoRepository;
import com.example.control.infrastructure.adapter.persistence.mongo.documents.ApplicationServiceDocument;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

/**
 * MongoDB adapter implementation for {@link ApplicationServiceRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for application
 * services
 * using Spring Data MongoDB with caching support.
 * </p>
 */
@Slf4j
@Component
public class ApplicationServiceMongoAdapter
        extends
        AbstractMongoAdapter<ApplicationService, ApplicationServiceDocument, ApplicationServiceId, ApplicationServiceCriteria, ApplicationServiceMongoRepository>
        implements ApplicationServiceRepositoryPort {

    public ApplicationServiceMongoAdapter(ApplicationServiceMongoRepository repository, MongoTemplate mongoTemplate) {
        super(repository, mongoTemplate, ApplicationServiceId::id);
    }

    @Override
    protected ApplicationServiceDocument toDocument(ApplicationService domain) {
        return ApplicationServiceDocument.fromDomain(domain);
    }

    @Override
    protected ApplicationService toDomain(ApplicationServiceDocument document) {
        return document.toDomain();
    }

    @Override
    protected Query buildQuery(ApplicationServiceCriteria criteria) {
        Query query = new Query();
        if (criteria == null)
            return query;

        // Apply basic filters
        if (criteria.lifecycle() != null) {
            query.addCriteria(Criteria.where("lifecycle").is(criteria.lifecycle().name()));
        }
        if (criteria.tags() != null && !criteria.tags().isEmpty()) {
            query.addCriteria(Criteria.where("tags").in(criteria.tags()));
        }
        // Text search: use MongoDB text index for efficient full-text search
        if (criteria.search() != null && !criteria.search().trim().isEmpty()) {
            String searchTerm = criteria.search().trim();
            // Use $text search for full-text matching (requires text index)
            // This is more efficient than regex for search queries
            query.addCriteria(new org.springframework.data.mongodb.core.query.Criteria()
                    .orOperator(
                            org.springframework.data.mongodb.core.query.Criteria.where("displayName")
                                    .regex(".*" + searchTerm + ".*", "i"), // Fallback regex for partial matching
                            org.springframework.data.mongodb.core.query.Criteria.where("displayName")
                                    .is(searchTerm) // Exact match
                    ));
            // Note: MongoDB text search with $text operator requires special syntax
            // For now, using regex with case-insensitive flag as text index supports it
            // Full $text search can be enabled later if needed for more advanced queries
        }

        // Build visibility filtering with $or operator
        // Users can see: (1) orphaned services, (2) team-owned services, (3) shared
        // services
        boolean hasVisibilityFilters = (criteria.includeOrphaned() != null && criteria.includeOrphaned()) ||
                (criteria.userTeamIds() != null && !criteria.userTeamIds().isEmpty()) ||
                (criteria.sharedServiceIds() != null && !criteria.sharedServiceIds().isEmpty());

        if (hasVisibilityFilters) {
            java.util.List<Criteria> orCriteria = new java.util.ArrayList<>();

            // Include orphaned services (ownerTeamId = null)
            if (criteria.includeOrphaned() != null && criteria.includeOrphaned()) {
                orCriteria.add(Criteria.where("ownerTeamId").is(null));
            }

            // Include team-owned services (ownerTeamId in userTeamIds)
            if (criteria.userTeamIds() != null && !criteria.userTeamIds().isEmpty()) {
                orCriteria.add(Criteria.where("ownerTeamId").in(criteria.userTeamIds()));
            }

            // Include shared services (id in sharedServiceIds)
            if (criteria.sharedServiceIds() != null && !criteria.sharedServiceIds().isEmpty()) {
                orCriteria.add(Criteria.where("id").in(criteria.sharedServiceIds()));
            }

            // Apply $or criteria if we have any visibility filters
            if (!orCriteria.isEmpty()) {
                query.addCriteria(new Criteria().orOperator(orCriteria.toArray(new Criteria[0])));
            }
        }

        // Specific ownerTeamId filter (takes precedence over visibility filtering)
        // This allows admin to filter by specific team even when visibility filters are
        // applied
        if (criteria.ownerTeamId() != null) {
            query.addCriteria(Criteria.where("ownerTeamId").is(criteria.ownerTeamId()));
        }

        return query;
    }

    @Override
    protected String getCollectionName() {
        return "application_services";
    }

    @Override
    protected Class<ApplicationServiceDocument> getDocumentClass() {
        return ApplicationServiceDocument.class;
    }

    @Override
    public Optional<ApplicationService> findByDisplayName(String displayName) {
        log.debug("Finding application service by display name: {}", displayName);

        Optional<ApplicationServiceDocument> document = repository.findByDisplayName(displayName);
        Optional<ApplicationService> result = document.map(this::toDomain);

        log.debug("Found application service by display name: {}", result.isPresent());
        return result;
    }
}
