package com.example.control.application.query;

import com.example.control.domain.criteria.IamUserCriteria;
import com.example.control.domain.valueobject.id.IamUserId;
import com.example.control.domain.model.IamUser;
import com.example.control.infrastructure.adapter.external.keycloak.KeycloakAdminRestService;
import com.example.control.infrastructure.adapter.external.keycloak.dto.KeycloakGroupRepresentation;
import com.example.control.infrastructure.adapter.external.keycloak.dto.KeycloakUserRepresentation;
import com.example.control.infrastructure.adapter.external.keycloak.mapper.KeycloakUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Query service for IamUser read operations using Keycloak Admin REST API.
 * <p>
 * Handles all read operations for IamUser domain objects with caching.
 * Queries Keycloak directly instead of MongoDB.
 * Responsible for data retrieval only - no writes, no business logic, no
 * permission checks.
 * All methods are read-only with appropriate caching strategies.
 * </p>
 */
@Slf4j
@Service("iamUserQueryServiceV2")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IamUserQueryServiceV2 {

    private final KeycloakAdminRestService keycloakAdminRestService;
    private final KeycloakUserMapper keycloakUserMapper;

    /**
     * Find IAM user by ID.
     *
     * @param id the user ID
     * @return the IAM user if found
     */
    @Cacheable(value = "iam-users", key = "#id")
    public Optional<IamUser> findById(IamUserId id) {
        log.debug("Finding IAM user by ID from Keycloak: {}", id);
        Optional<KeycloakUserRepresentation> keycloakUser = keycloakAdminRestService.getUser(id.userId());

        if (keycloakUser.isEmpty()) {
            return Optional.empty();
        }

        // Get user's groups to populate teamIds
        List<String> groupPaths = keycloakAdminRestService.getUserGroups(id.userId());
        List<String> teamIds = keycloakUserMapper.extractTeamIdsFromGroups(groupPaths);

        IamUser user = keycloakUserMapper.toIamUserWithTeams(keycloakUser.get(), teamIds);
        return Optional.of(user);
    }

    @Cacheable(value = "iam-users", key = "#username")
    public Optional<IamUser> findByUsername(String username) {
        // Currently fillAll then find the user by username
        Page<IamUser> users = findAll(IamUserCriteria.builder().username(username).build(), Pageable.unpaged());
        Optional<IamUser> user = users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
        return user;
    }

    /**
     * Find all user IDs that belong to any of the specified teams.
     *
     * @param teamIds list of team IDs
     * @return list of user IDs
     */
    @Cacheable(value = "iam-users", key = "T(com.example.control.infrastructure.cache.CacheKeyGenerator).generateKey('userIds', #teamIds)")
    public List<String> findUserIdsByTeams(List<String> teamIds) {
        log.debug("Finding user IDs by teams from Keycloak: {}", teamIds);
        if (teamIds == null || teamIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> allUserIds = new ArrayList<>();
        for (String teamId : teamIds) {
            // Note: This requires finding group by name first, then getting members
            // For now, we'll need to get all groups and filter by name
            // This is not optimal but works for now
            try {
                // Get all groups and find matching one
                List<KeycloakGroupRepresentation> groups =
                        keycloakAdminRestService.getGroups(null, Pageable.unpaged());

                Optional<KeycloakGroupRepresentation> matchingGroup =
                        groups.stream()
                                .filter(g -> teamId.equals(g.getName()) || teamId.equals(g.getId()))
                                .findFirst();

                if (matchingGroup.isPresent()) {
                    String groupId = matchingGroup.get().getId();
                    List<String> memberIds = keycloakAdminRestService.getGroupMembers(groupId, Pageable.unpaged());
                    allUserIds.addAll(memberIds);
                }
            } catch (Exception e) {
                log.warn("Failed to get members for team: {}", teamId, e);
            }
        }

        return allUserIds.stream().distinct().collect(Collectors.toList());
    }

    /**
     * List IAM users with filtering and pagination.
     *
     * @param criteria the search criteria
     * @param pageable pagination information
     * @return page of IAM users
     */
    @Cacheable(value = "iam-users", key = "T(com.example.control.infrastructure.cache.CacheKeyGenerator).generateKey('list', #criteria, #pageable)")
    public Page<IamUser> findAll(IamUserCriteria criteria, Pageable pageable) {
        log.debug("Listing IAM users from Keycloak with criteria: {}", criteria);

        List<KeycloakUserRepresentation> keycloakUsers = keycloakAdminRestService.getUsers(criteria, pageable);

        // Convert to domain models
        List<IamUser> users = new ArrayList<>();
        for (KeycloakUserRepresentation keycloakUser : keycloakUsers) {
            // Get user's groups
            List<String> groupPaths = keycloakAdminRestService.getUserGroups(keycloakUser.getId());
            List<String> teamIds = keycloakUserMapper.extractTeamIdsFromGroups(groupPaths);
            IamUser user = keycloakUserMapper.toIamUserWithTeams(keycloakUser, teamIds);
            users.add(user);
        }

        // Note: Keycloak doesn't provide total count in pagination response
        // We'll fetch all to get accurate count (not ideal but works)
        long total = count(criteria);
        if (pageable != null && !pageable.isUnpaged()) {
            return new PageImpl<>(users, pageable, total);
        } else {
            return new PageImpl<>(users);
        }
    }

    /**
     * Count IAM users matching the given filter criteria.
     *
     * @param criteria the filter criteria
     * @return count of matching users
     */
    @Cacheable(value = "iam-users", key = "T(com.example.control.infrastructure.cache.CacheKeyGenerator).generateKeyFromHash('count', #criteria)")
    public long count(IamUserCriteria criteria) {
        log.debug("Counting IAM users from Keycloak with criteria: {}", criteria);
        // Keycloak doesn't provide count endpoint, so we fetch all and count
        List<KeycloakUserRepresentation> users = keycloakAdminRestService.getUsers(criteria, Pageable.unpaged());
        return users.size();
    }

    /**
     * Count all IAM users.
     *
     * @return total count of all users
     */
    @Cacheable(value = "iam-users", key = "'countAll'")
    public long countAll() {
        log.debug("Counting all IAM users from Keycloak");
        return count(IamUserCriteria.noFilter());
    }

    /**
     * Check if an IAM user exists.
     *
     * @param id the user ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(IamUserId id) {
        log.debug("Checking existence of IAM user from Keycloak: {}", id);
        return findById(id).isPresent();
    }
}

