package com.example.control.application.command;

import com.example.control.domain.valueobject.id.IamTeamId;
import com.example.control.domain.model.IamTeam;
import com.example.control.domain.port.repository.IamTeamRepositoryPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Command service for IamTeam write operations.
 * <p>
 * Handles all write operations (save, update, delete) for IamTeam domain
 * objects.
 * Responsible for CRUD, cache eviction, and transaction management.
 * Does NOT handle business logic, permission checks, or cross-domain
 * operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Validated
@Transactional
public class IamTeamCommandService {

    private final IamTeamRepositoryPort repository;

    /**
     * Saves an IAM team (create or update).
     * ID must be provided (Keycloak group ID).
     * Evicts specific iam-teams cache entry by ID.
     *
     * @param team the IAM team to save
     * @return the saved IAM team
     */
    @CacheEvict(value = "iam-teams", key = "#team.teamId")
    public IamTeam save(@Valid IamTeam team) {
        log.debug("Saving IAM team: {}", team.getTeamId());

        IamTeam saved = repository.save(team);
        log.info("Saved IAM team: {} (name: {})", saved.getTeamId(), saved.getDisplayName());
        return saved;
    }

    /**
     * Deletes an IAM team by ID.
     * Evicts specific iam-teams cache entry by ID.
     *
     * @param id the IAM team ID to delete
     */
    @CacheEvict(value = "iam-teams", key = "#id")
    public void deleteById(IamTeamId id) {
        log.info("Deleting IAM team: {}", id);
        repository.deleteById(id);
    }

    /**
     * Deletes all IAM teams.
     * WARNING: This operation removes ALL entities. Use with caution.
     * Typically used for testing or full sync scenarios.
     * Evicts all iam-teams cache entries.
     *
     * @return the count of deleted entities
     */
    @CacheEvict(value = "iam-teams", allEntries = true)
    public long deleteAll() {
        log.warn("Deleting all IAM teams - this is a destructive operation");
        long count = repository.deleteAll();
        log.info("Deleted {} IAM teams", count);
        return count;
    }
}
