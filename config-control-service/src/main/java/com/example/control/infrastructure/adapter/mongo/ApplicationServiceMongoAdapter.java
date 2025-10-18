package com.example.control.infrastructure.adapter.mongo;

import com.example.control.domain.ApplicationService;
import com.example.control.domain.port.ApplicationServiceRepositoryPort;
import com.example.control.infrastructure.repository.ApplicationServiceMongoRepository;
import com.example.control.infrastructure.repository.documents.ApplicationServiceDocument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB adapter implementation for {@link ApplicationServiceRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for application services
 * using Spring Data MongoDB with caching support.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationServiceMongoAdapter implements ApplicationServiceRepositoryPort {

    private final ApplicationServiceMongoRepository repository;
    private final MongoTemplate mongoTemplate;

    @Override
    public ApplicationService save(ApplicationService service) {
        log.debug("Saving application service: {}", service.getId());
        
        ApplicationServiceDocument document = ApplicationServiceDocument.fromDomain(service);
        ApplicationServiceDocument saved = repository.save(document);
        
        log.debug("Successfully saved application service: {}", saved.getId());
        return saved.toDomain();
    }

    @Override
    @Cacheable(value = "application-services", key = "#id")
    public Optional<ApplicationService> findById(String id) {
        log.debug("Finding application service by ID: {}", id);
        
        Optional<ApplicationServiceDocument> document = repository.findById(id);
        return document.map(ApplicationServiceDocument::toDomain);
    }


    @Override
    public Page<ApplicationService> findAll(Object filter, Pageable pageable) {
        ApplicationServiceFilter serviceFilter = (ApplicationServiceFilter) filter;
        log.debug("Finding application services with filter: {}, pageable: {}", serviceFilter, pageable);
        
        Query query = buildQuery(serviceFilter);
        
        // Get total count
        long total = mongoTemplate.count(query, ApplicationServiceDocument.class);
        
        // Apply pagination and sorting
        query.with(pageable);
        
        // Execute query
        List<ApplicationServiceDocument> documents = mongoTemplate.find(query, ApplicationServiceDocument.class);
        
        // Convert to domain objects
        List<ApplicationService> services = documents.stream()
                .map(ApplicationServiceDocument::toDomain)
                .collect(Collectors.toList());
        
        return new PageImpl<>(services, pageable, total);
    }

    @Override
    public long count(Object filter) {
        ApplicationServiceFilter serviceFilter = (ApplicationServiceFilter) filter;
        log.debug("Counting application services with filter: {}", serviceFilter);
        
        Query query = buildQuery(serviceFilter);
        return mongoTemplate.count(query, ApplicationServiceDocument.class);
    }

    @Override
    public void deleteById(String id) {
        log.debug("Deleting application service: {}", id);
        
        repository.deleteById(id);
        log.debug("Successfully deleted application service: {}", id);
    }

    @Override
    public boolean existsById(String id) {
        return repository.existsById(id);
    }


    /**
     * Build MongoDB query from filter criteria.
     *
     * @param filter the filter criteria
     * @return MongoDB query object
     */
    private Query buildQuery(ApplicationServiceFilter filter) {
        Query query = new Query();
        
        if (filter != null) {
            if (filter.ownerTeamId() != null) {
                query.addCriteria(Criteria.where("ownerTeamId").is(filter.ownerTeamId()));
            }
            
            if (filter.lifecycle() != null) {
                query.addCriteria(Criteria.where("lifecycle").is(filter.lifecycle().name()));
            }
            
            if (filter.tags() != null && !filter.tags().isEmpty()) {
                query.addCriteria(Criteria.where("tags").in(filter.tags()));
            }
            
            if (filter.search() != null && !filter.search().trim().isEmpty()) {
                String searchRegex = ".*" + filter.search().trim() + ".*";
                query.addCriteria(Criteria.where("displayName").regex(searchRegex, "i"));
            }
            
            // Team-based access control: filter by userTeamIds if provided
            if (filter.userTeamIds() != null && !filter.userTeamIds().isEmpty()) {
                query.addCriteria(Criteria.where("ownerTeamId").in(filter.userTeamIds()));
            }
        }
        
        return query;
    }
}
