package com.example.control.application.query;

import com.example.control.domain.criteria.IamTeamCriteria;
import com.example.control.domain.valueobject.id.IamTeamId;
import com.example.control.domain.model.IamTeam;
import com.example.control.infrastructure.adapter.external.keycloak.KeycloakAdminRestService;
import com.example.control.infrastructure.adapter.external.keycloak.dto.KeycloakGroupRepresentation;
import com.example.control.infrastructure.adapter.external.keycloak.mapper.KeycloakTeamMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Query service for IamTeam read operations using Keycloak Admin REST API.
 * <p>
 * Handles all read operations for IamTeam domain objects with caching.
 * Queries Keycloak directly instead of MongoDB.
 * Responsible for data retrieval only - no writes, no business logic, no
 * permission checks.
 * All methods are read-only with appropriate caching strategies.
 * </p>
 */
@Slf4j
@Service("iamTeamQueryServiceV2")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IamTeamQueryServiceV2 {

    private final KeycloakAdminRestService keycloakAdminRestService;
    private final KeycloakTeamMapper keycloakTeamMapper;

    /**
     * Find IAM team by ID.
     *
     * @param id the team ID
     * @return the IAM team if found
     */
    @Cacheable(value = "iam-teams", key = "#id")
    public Optional<IamTeam> findById(IamTeamId id) {
        log.debug("Finding IAM team by ID from Keycloak: {}", id);
        // Keycloak groups can be identified by ID or name
        // First try to find by ID
        Optional<KeycloakGroupRepresentation> group = keycloakAdminRestService.getGroup(id.teamId());

        // If not found by ID, try to find by name
        if (group.isEmpty()) {
            List<KeycloakGroupRepresentation> allGroups = keycloakAdminRestService.getGroups(null, Pageable.unpaged());
            group = allGroups.stream()
                    .filter(g -> id.teamId().equals(g.getName()) || id.teamId().equals(g.getId()))
                    .findFirst();
        }

        if (group.isEmpty()) {
            return Optional.empty();
        }

        // Get group members
        String groupId = group.get().getId();
        List<String> memberIds = keycloakAdminRestService.getGroupMembers(groupId, Pageable.unpaged());

        IamTeam team = keycloakTeamMapper.toIamTeamWithMembers(group.get(), memberIds);
        return Optional.of(team);
    }

    /**
     * List IAM teams with filtering and pagination.
     *
     * @param criteria the search criteria
     * @param pageable pagination information
     * @return page of IAM teams
     */
    @Cacheable(value = "iam-teams", key = "T(com.example.control.infrastructure.cache.CacheKeyGenerator).generateKey('list', #criteria, #pageable)")
    public Page<IamTeam> findAll(IamTeamCriteria criteria, Pageable pageable) {
        log.debug("Listing IAM teams from Keycloak with criteria: {}", criteria);

        List<KeycloakGroupRepresentation> keycloakGroups = keycloakAdminRestService.getGroups(criteria, pageable);

        // Apply client-side filtering for criteria not supported by Keycloak API
        List<KeycloakGroupRepresentation> filteredGroups = keycloakGroups;
        if (criteria != null) {
            if (criteria.displayName() != null) {
                filteredGroups = keycloakGroups.stream()
                        .filter(g -> criteria.displayName().equals(g.getName()))
                        .collect(Collectors.toList());
            }

            if (criteria.members() != null && !criteria.members().isEmpty()) {
                // Filter groups that contain the specified members
                filteredGroups = filteredGroups.stream()
                        .filter(g -> {
                            List<String> memberIds = keycloakAdminRestService.getGroupMembers(g.getId(), Pageable.unpaged());
                            return criteria.members().stream().anyMatch(memberIds::contains);
                        })
                        .collect(Collectors.toList());
            }

            if (criteria.userTeamIds() != null && !criteria.userTeamIds().isEmpty()) {
                // Filter to only teams the user belongs to
                filteredGroups = filteredGroups.stream()
                        .filter(g -> {
                            String teamId = keycloakTeamMapper.extractTeamIdFromPath(g.getPath());
                            return teamId != null && criteria.userTeamIds().contains(teamId);
                        })
                        .collect(Collectors.toList());
            }
        }

        // Convert to domain models
        List<IamTeam> teams = new ArrayList<>();
        for (KeycloakGroupRepresentation keycloakGroup : filteredGroups) {
            // Get group members
            List<String> memberIds = keycloakAdminRestService.getGroupMembers(keycloakGroup.getId(), Pageable.unpaged());
            IamTeam team = keycloakTeamMapper.toIamTeamWithMembers(keycloakGroup, memberIds);
            teams.add(team);
        }

        // Note: Keycloak doesn't provide total count in pagination response
        long total = count(criteria);
        if (pageable != null && !pageable.isUnpaged()) {
            return new PageImpl<>(teams, pageable, total);
        } else {
            return new PageImpl<>(teams);
        }
    }

    /**
     * Count IAM teams matching the given filter criteria.
     *
     * @param criteria the filter criteria
     * @return count of matching teams
     */
    @Cacheable(value = "iam-teams", key = "T(com.example.control.infrastructure.cache.CacheKeyGenerator).generateKeyFromHash('count', #criteria)")
    public long count(IamTeamCriteria criteria) {
        log.debug("Counting IAM teams from Keycloak with criteria: {}", criteria);
        // Keycloak doesn't provide count endpoint, so we fetch all and count
        List<KeycloakGroupRepresentation> groups = keycloakAdminRestService.getGroups(criteria, Pageable.unpaged());

        // Apply same filtering as findAll
        if (criteria != null) {
            if (criteria.displayName() != null) {
                groups = groups.stream()
                        .filter(g -> criteria.displayName().equals(g.getName()))
                        .collect(Collectors.toList());
            }

            if (criteria.members() != null && !criteria.members().isEmpty()) {
                groups = groups.stream()
                        .filter(g -> {
                            List<String> memberIds = keycloakAdminRestService.getGroupMembers(g.getId(), Pageable.unpaged());
                            return criteria.members().stream().anyMatch(memberIds::contains);
                        })
                        .collect(Collectors.toList());
            }

            if (criteria.userTeamIds() != null && !criteria.userTeamIds().isEmpty()) {
                groups = groups.stream()
                        .filter(g -> {
                            String teamId = keycloakTeamMapper.extractTeamIdFromPath(g.getPath());
                            return teamId != null && criteria.userTeamIds().contains(teamId);
                        })
                        .collect(Collectors.toList());
            }
        }

        return groups.size();
    }

    /**
     * Count all IAM teams.
     *
     * @return total count of all teams
     */
    @Cacheable(value = "iam-teams", key = "'countAll'")
    public long countAll() {
        log.debug("Counting all IAM teams from Keycloak");
        return count(IamTeamCriteria.noFilter());
    }

    /**
     * Check if an IAM team exists.
     *
     * @param id the team ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(IamTeamId id) {
        log.debug("Checking existence of IAM team from Keycloak: {}", id);
        return findById(id).isPresent();
    }
}

