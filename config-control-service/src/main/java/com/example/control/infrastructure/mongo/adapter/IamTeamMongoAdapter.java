package com.example.control.infrastructure.mongo.adapter;

import com.example.control.domain.object.IamTeam;
import com.example.control.domain.criteria.IamTeamCriteria;
import com.example.control.domain.id.IamTeamId;
import com.example.control.domain.port.IamTeamRepositoryPort;
import com.example.control.infrastructure.mongo.repository.IamTeamMongoRepository;
import com.example.control.infrastructure.mongo.documents.IamTeamDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MongoDB adapter implementation for {@link IamTeamRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for cached team
 * projections from Keycloak using Spring Data MongoDB.
 * </p>
 */
@Slf4j
@Component
public class IamTeamMongoAdapter 
    extends AbstractMongoAdapter<IamTeam, IamTeamDocument, IamTeamId, IamTeamCriteria>
    implements IamTeamRepositoryPort {

    private final IamTeamMongoRepository repository;

    public IamTeamMongoAdapter(IamTeamMongoRepository repository, MongoTemplate mongoTemplate) {
        super(repository, mongoTemplate);
        this.repository = repository;
    }

    @Override
    protected IamTeamDocument toDocument(IamTeam domain) {
        return IamTeamDocument.fromDomain(domain);
    }

    @Override
    protected IamTeam toDomain(IamTeamDocument document) {
        return document.toDomain();
    }

    @Override
    protected Query buildQuery(IamTeamCriteria criteria) {
        Query query = new Query();
        if (criteria == null) return query;
        
        // Apply filters
        if (criteria.displayName() != null) {
            query.addCriteria(Criteria.where("displayName").is(criteria.displayName()));
        }
        if (criteria.members() != null && !criteria.members().isEmpty()) {
            query.addCriteria(Criteria.where("members").in(criteria.members()));
        }
        
        // ABAC: Team-based filtering
        if (criteria.userTeamIds() != null && !criteria.userTeamIds().isEmpty()) {
            query.addCriteria(Criteria.where("teamId").in(criteria.userTeamIds()));
        }
        
        return query;
    }

    @Override
    protected String getCollectionName() {
        return "iam_teams";
    }

    @Override
    protected Class<IamTeamDocument> getDocumentClass() {
        return IamTeamDocument.class;
    }

    @Override
    public List<IamTeam> findByMember(String userId) {
        log.debug("Finding IAM teams by member: {}", userId);
        
        List<IamTeamDocument> documents = repository.findByMember(userId);
        return documents.stream()
                .map(IamTeamDocument::toDomain)
                .toList();
    }

    @Override
    public void deleteAll() {
        log.debug("Deleting all IAM teams");
        
        repository.deleteAll();
        log.debug("Successfully deleted all IAM teams");
    }

    @Override
    public long countAll() {
        log.debug("Counting all IAM teams");
        
        return repository.count();
    }
}
