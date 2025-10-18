package com.example.control.infrastructure.adapter.mongo;

import com.example.control.domain.IamUser;
import com.example.control.domain.port.IamUserRepositoryPort;
import com.example.control.infrastructure.repository.IamUserMongoRepository;
import com.example.control.infrastructure.repository.documents.IamUserDocument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB adapter implementation for {@link IamUserRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for cached user
 * projections from Keycloak using Spring Data MongoDB.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IamUserMongoAdapter implements IamUserRepositoryPort {

    private final IamUserMongoRepository repository;

    @Override
    public IamUser save(IamUser user) {
        log.debug("Saving IAM user: {}", user.getUserId());
        
        IamUserDocument document = IamUserDocument.fromDomain(user);
        IamUserDocument saved = repository.save(document);
        
        log.debug("Successfully saved IAM user: {}", saved.getUserId());
        return saved.toDomain();
    }

    @Override
    public Optional<IamUser> findById(String userId) {
        log.debug("Finding IAM user by ID: {}", userId);
        
        Optional<IamUserDocument> document = repository.findById(userId);
        return document.map(IamUserDocument::toDomain);
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
    public Page<IamUser> list(Pageable pageable) {
        log.debug("Listing IAM users with pageable: {}", pageable);
        
        org.springframework.data.domain.Page<IamUserDocument> documentPage = repository.findAll(pageable);
        
        List<IamUser> users = documentPage.getContent().stream()
                .map(IamUserDocument::toDomain)
                .toList();
        
        return new PageImpl<>(users, pageable, documentPage.getTotalElements());
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
    public void delete(String userId) {
        log.debug("Deleting IAM user: {}", userId);
        
        repository.deleteById(userId);
        log.debug("Successfully deleted IAM user: {}", userId);
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
}
