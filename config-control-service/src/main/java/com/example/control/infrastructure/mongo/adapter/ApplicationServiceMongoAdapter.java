package com.example.control.infrastructure.mongo.adapter;

import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.criteria.ApplicationServiceCriteria;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.port.ApplicationServiceRepositoryPort;
import com.example.control.infrastructure.mongo.repository.ApplicationServiceMongoRepository;
import com.example.control.infrastructure.mongo.documents.ApplicationServiceDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;


/**
 * MongoDB adapter implementation for {@link ApplicationServiceRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for application services
 * using Spring Data MongoDB with caching support.
 * </p>
 */
@Slf4j
@Component
public class ApplicationServiceMongoAdapter 
    extends AbstractMongoAdapter<ApplicationService, ApplicationServiceDocument, ApplicationServiceId, ApplicationServiceCriteria>
    implements ApplicationServiceRepositoryPort {

    private final ApplicationServiceMongoRepository applicationServiceRepository;

    public ApplicationServiceMongoAdapter(ApplicationServiceMongoRepository repository, MongoTemplate mongoTemplate) {
        super(repository, mongoTemplate);
        this.applicationServiceRepository = repository;
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
        if (criteria == null) return query;
        
        // Apply filters
        if (criteria.ownerTeamId() != null) {
            query.addCriteria(Criteria.where("ownerTeamId").is(criteria.ownerTeamId()));
        }
        if (criteria.lifecycle() != null) {
            query.addCriteria(Criteria.where("lifecycle").is(criteria.lifecycle().name()));
        }
        if (criteria.tags() != null && !criteria.tags().isEmpty()) {
            query.addCriteria(Criteria.where("tags").in(criteria.tags()));
        }
        if (criteria.search() != null && !criteria.search().trim().isEmpty()) {
            String searchRegex = ".*" + criteria.search().trim() + ".*";
            query.addCriteria(Criteria.where("displayName").regex(searchRegex, "i"));
        }
        
        // ABAC: Team-based filtering
        if (criteria.userTeamIds() != null && !criteria.userTeamIds().isEmpty()) {
            query.addCriteria(Criteria.where("ownerTeamId").in(criteria.userTeamIds()));
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
    public java.util.Optional<ApplicationService> findByDisplayName(String displayName) {
        log.debug("Finding application service by display name: {}", displayName);
        
        java.util.Optional<ApplicationServiceDocument> document = applicationServiceRepository.findByDisplayName(displayName);
        java.util.Optional<ApplicationService> result = document.map(this::toDomain);
        
        log.debug("Found application service by display name: {}", result.isPresent());
        return result;
    }
}
