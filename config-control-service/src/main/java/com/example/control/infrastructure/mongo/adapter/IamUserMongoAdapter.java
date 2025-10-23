package com.example.control.infrastructure.mongo.adapter;

import com.example.control.domain.object.IamUser;
import com.example.control.domain.criteria.IamUserCriteria;
import com.example.control.domain.id.IamUserId;
import com.example.control.domain.port.IamUserRepositoryPort;
import com.example.control.infrastructure.mongo.repository.IamUserMongoRepository;
import com.example.control.infrastructure.mongo.documents.IamUserDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MongoDB adapter implementation for {@link IamUserRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for cached user
 * projections from Keycloak using Spring Data MongoDB.
 * </p>
 */
@Slf4j
@Component
public class IamUserMongoAdapter 
    extends AbstractMongoAdapter<IamUser, IamUserDocument, IamUserId, IamUserCriteria, IamUserMongoRepository>
    implements IamUserRepositoryPort {

    public IamUserMongoAdapter(IamUserMongoRepository repository, MongoTemplate mongoTemplate) {
        super(repository, mongoTemplate, IamUserId::userId);
    }

    @Override
    protected IamUserDocument toDocument(IamUser domain) {
        return IamUserDocument.fromDomain(domain);
    }

    @Override
    protected IamUser toDomain(IamUserDocument document) {
        return document.toDomain();
    }

    @Override
    protected Query buildQuery(IamUserCriteria criteria) {
        Query query = new Query();
        if (criteria == null) return query;
        
        // Apply filters
        if (criteria.username() != null) {
            query.addCriteria(Criteria.where("username").is(criteria.username()));
        }
        if (criteria.email() != null) {
            query.addCriteria(Criteria.where("email").is(criteria.email()));
        }
        if (criteria.firstName() != null) {
            query.addCriteria(Criteria.where("firstName").is(criteria.firstName()));
        }
        if (criteria.lastName() != null) {
            query.addCriteria(Criteria.where("lastName").is(criteria.lastName()));
        }
        if (criteria.teamIds() != null && !criteria.teamIds().isEmpty()) {
            query.addCriteria(Criteria.where("teamIds").in(criteria.teamIds()));
        }
        if (criteria.managerId() != null) {
            query.addCriteria(Criteria.where("managerId").is(criteria.managerId()));
        }
        if (criteria.roles() != null && !criteria.roles().isEmpty()) {
            query.addCriteria(Criteria.where("roles").in(criteria.roles()));
        }
        
        // ABAC: Team-based filtering
        if (criteria.userTeamIds() != null && !criteria.userTeamIds().isEmpty()) {
            query.addCriteria(Criteria.where("teamIds").in(criteria.userTeamIds()));
        }
        
        return query;
    }

    @Override
    protected String getCollectionName() {
        return "iam_users";
    }

    @Override
    protected Class<IamUserDocument> getDocumentClass() {
        return IamUserDocument.class;
    }

    @Override
    public List<IamUser> findByTeam(String teamId) {
        log.debug("Finding IAM users by team: {}", teamId);
        
        List<IamUserDocument> documents = repository.findByTeamId(teamId);
        return documents.stream()
                .map(IamUserDocument::toDomain)
                .toList();
    }

    @Override
    public List<IamUser> findByManager(String managerId) {
        log.debug("Finding IAM users by manager: {}", managerId);
        
        List<IamUserDocument> documents = repository.findByManagerId(managerId);
        return documents.stream()
                .map(IamUserDocument::toDomain)
                .toList();
    }

    @Override
    public List<IamUser> findByRole(String role) {
        log.debug("Finding IAM users by role: {}", role);
        
        List<IamUserDocument> documents = repository.findByRole(role);
        return documents.stream()
                .map(IamUserDocument::toDomain)
                .toList();
    }

    @Override
    public List<String> findUserIdsByTeams(List<String> teamIds) {
        log.debug("Finding user IDs by teams: {}", teamIds);
        
        List<IamUserDocument> documents = repository.findUserIdsByTeamIds(teamIds);
        return documents.stream()
                .map(IamUserDocument::getUserId)
                .toList();
    }

    @Override
    public void deleteAll() {
        log.debug("Deleting all IAM users");
        
        repository.deleteAll();
        log.debug("Successfully deleted all IAM users");
    }

    @Override
    public long countByTeam(String teamId) {
        log.debug("Counting IAM users by team: {}", teamId);
        
        return repository.countByTeamId(teamId);
    }

    @Override
    public long countByRole(String role) {
        log.debug("Counting IAM users by role: {}", role);
        
        return repository.countByRole(role);
    }

    @Override
    public long countAll() {
        log.debug("Counting all IAM users");
        
        return repository.count();
    }
}
