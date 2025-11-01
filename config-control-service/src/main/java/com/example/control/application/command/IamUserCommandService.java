package com.example.control.application.command;

import com.example.control.domain.valueobject.id.IamUserId;
import com.example.control.domain.model.IamUser;
import com.example.control.domain.port.repository.IamUserRepositoryPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Command service for IamUser write operations.
 * <p>
 * Handles all write operations (save, update, delete) for IamUser domain
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
public class IamUserCommandService {

    private final IamUserRepositoryPort repository;

    /**
     * Saves an IAM user (create or update).
     * ID must be provided (Keycloak user ID).
     * Evicts specific iam-users cache entry by ID.
     *
     * @param user the IAM user to save
     * @return the saved IAM user
     */
    @CacheEvict(value = "iam-users", key = "#user.userId")
    public IamUser save(@Valid IamUser user) {
        log.debug("Saving IAM user: {}", user.getUserId());

        IamUser saved = repository.save(user);
        log.info("Saved IAM user: {} (username: {})", saved.getUserId(), saved.getUsername());
        return saved;
    }

    /**
     * Deletes an IAM user by ID.
     * Evicts specific iam-users cache entry by ID.
     *
     * @param id the IAM user ID to delete
     */
    @CacheEvict(value = "iam-users", key = "#id")
    public void deleteById(IamUserId id) {
        log.info("Deleting IAM user: {}", id);
        repository.deleteById(id);
    }

    /**
     * Deletes all IAM users.
     * WARNING: This operation removes ALL entities. Use with caution.
     * Typically used for testing or full sync scenarios.
     * Evicts all iam-users cache entries.
     *
     * @return the count of deleted entities
     */
    @CacheEvict(value = "iam-users", allEntries = true)
    public long deleteAll() {
        log.warn("Deleting all IAM users - this is a destructive operation");
        long count = repository.deleteAll();
        log.info("Deleted {} IAM users", count);
        return count;
    }
}
