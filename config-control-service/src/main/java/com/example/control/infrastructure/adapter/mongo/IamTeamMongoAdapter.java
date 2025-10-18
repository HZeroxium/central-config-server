package com.example.control.infrastructure.adapter.mongo;

import com.example.control.domain.IamTeam;
import com.example.control.domain.port.IamTeamRepositoryPort;
import com.example.control.infrastructure.repository.IamTeamMongoRepository;
import com.example.control.infrastructure.repository.documents.IamTeamDocument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB adapter implementation for {@link IamTeamRepositoryPort}.
 * <p>
 * This adapter provides the persistence layer implementation for cached team
 * projections from Keycloak using Spring Data MongoDB.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IamTeamMongoAdapter implements IamTeamRepositoryPort {

    private final IamTeamMongoRepository repository;

    @Override
    public IamTeam save(IamTeam team) {
        log.debug("Saving IAM team: {}", team.getTeamId());
        
        IamTeamDocument document = IamTeamDocument.fromDomain(team);
        IamTeamDocument saved = repository.save(document);
        
        log.debug("Successfully saved IAM team: {}", saved.getTeamId());
        return saved.toDomain();
    }

    @Override
    public Optional<IamTeam> findById(String teamId) {
        log.debug("Finding IAM team by ID: {}", teamId);
        
        Optional<IamTeamDocument> document = repository.findById(teamId);
        return document.map(IamTeamDocument::toDomain);
    }

    @Override
    public List<IamTeam> findAll() {
        log.debug("Finding all IAM teams");
        
        List<IamTeamDocument> documents = repository.findAll();
        return documents.stream()
                .map(IamTeamDocument::toDomain)
                .toList();
    }

    @Override
    public Page<IamTeam> findAll(Object filter, Pageable pageable) {
        log.debug("Listing IAM teams with pageable: {}", pageable);
        
        org.springframework.data.domain.Page<IamTeamDocument> documentPage = repository.findAll(pageable);
        
        List<IamTeam> teams = documentPage.getContent().stream()
                .map(IamTeamDocument::toDomain)
                .toList();
        
        return new PageImpl<>(teams, pageable, documentPage.getTotalElements());
    }

    @Override
    public Page<IamTeam> list(Pageable pageable) {
        return findAll(null, pageable);
    }

    @Override
    public long count(Object filter) {
        return repository.count();
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
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
    public boolean existsById(String teamId) {
        log.debug("Checking if IAM team exists: {}", teamId);
        
        return repository.existsById(teamId);
    }

    @Override
    public void delete(String teamId) {
        log.debug("Deleting IAM team: {}", teamId);
        
        repository.deleteById(teamId);
        log.debug("Successfully deleted IAM team: {}", teamId);
    }

    @Override
    public void deleteAll() {
        log.debug("Deleting all IAM teams");
        
        repository.deleteAll();
        log.debug("Successfully deleted all IAM teams");
    }

    @Override
    public long count() {
        log.debug("Counting IAM teams");
        
        return repository.count();
    }
}
