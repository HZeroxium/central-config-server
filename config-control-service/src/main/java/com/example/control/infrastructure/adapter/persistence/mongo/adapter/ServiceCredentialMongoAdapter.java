package com.example.control.infrastructure.adapter.persistence.mongo.adapter;

import com.example.control.domain.model.ServiceCredential;
import com.example.control.domain.port.repository.ServiceCredentialRepositoryPort;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.valueobject.id.ServiceCredentialId;
import com.example.control.infrastructure.adapter.persistence.mongo.documents.ServiceCredentialDocument;
import com.example.control.infrastructure.adapter.persistence.mongo.repository.ServiceCredentialMongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * MongoDB adapter implementation for {@link ServiceCredentialRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for service
 * credentials using Spring Data MongoDB.
 * </p>
 */
@Slf4j
@Component
public class ServiceCredentialMongoAdapter implements ServiceCredentialRepositoryPort {

    private final ServiceCredentialMongoRepository repository;
    private final MongoTemplate mongoTemplate;

    public ServiceCredentialMongoAdapter(
            ServiceCredentialMongoRepository repository,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public ServiceCredential save(ServiceCredential credential) {
        log.debug("Saving service credential: {} for service: {}", 
                credential.getId(), credential.getServiceId());

        ServiceCredentialDocument document = ServiceCredentialDocument.fromDomain(credential);
        ServiceCredentialDocument savedDocument = repository.save(document);
        ServiceCredential result = savedDocument.toDomain();

        log.debug("Saved service credential: {} for service: {}", 
                result.getId(), result.getServiceId());
        return result;
    }

    @Override
    public Optional<ServiceCredential> findById(ServiceCredentialId id) {
        log.debug("Finding service credential by ID: {}", id);

        Optional<ServiceCredentialDocument> document = repository.findById(id.id());
        Optional<ServiceCredential> result = document.map(ServiceCredentialDocument::toDomain);

        log.debug("Found service credential by ID: {}", result.isPresent());
        return result;
    }

    @Override
    public Optional<ServiceCredential> findByServiceId(ApplicationServiceId serviceId) {
        log.debug("Finding service credential by service ID: {}", serviceId);

        Optional<ServiceCredentialDocument> document = repository.findByServiceId(serviceId.id());
        Optional<ServiceCredential> result = document.map(ServiceCredentialDocument::toDomain);

        log.debug("Found service credential by service ID: {}", result.isPresent());
        return result;
    }

    @Override
    public Optional<ServiceCredential> findByKeycloakClientId(String keycloakClientId) {
        log.debug("Finding service credential by Keycloak client ID: {}", keycloakClientId);

        Optional<ServiceCredentialDocument> document = repository.findByKeycloakClientId(keycloakClientId);
        Optional<ServiceCredential> result = document.map(ServiceCredentialDocument::toDomain);

        log.debug("Found service credential by Keycloak client ID: {}", result.isPresent());
        return result;
    }

    @Override
    public boolean existsByServiceId(ApplicationServiceId serviceId) {
        log.debug("Checking existence of service credential for service ID: {}", serviceId);

        boolean exists = repository.existsByServiceId(serviceId.id());

        log.debug("Service credential exists for service ID: {}", exists);
        return exists;
    }

    @Override
    public void deleteById(ServiceCredentialId id) {
        log.debug("Deleting service credential by ID: {}", id);

        repository.deleteById(id.id());

        log.debug("Deleted service credential by ID: {}", id);
    }

    @Override
    public long deleteAll() {
        log.debug("Deleting all service credentials");

        long count = repository.count();
        repository.deleteAll();

        log.debug("Deleted {} service credentials", count);
        return count;
    }
}

