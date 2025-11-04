package com.example.control.infrastructure.adapter.external.keycloak.mapper;

import com.example.control.domain.model.IamUser;
import com.example.control.domain.valueobject.id.IamUserId;
import com.example.control.infrastructure.adapter.external.keycloak.dto.KeycloakUserRepresentation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for converting Keycloak User Representation to IamUser domain model.
 * <p>
 * Handles extraction of user attributes, roles, and team memberships from Keycloak format.
 * </p>
 */
@Slf4j
@Component
public class KeycloakUserMapper {

    /**
     * Convert KeycloakUserRepresentation to IamUser domain model.
     * <p>
     * Extracts:
     * - Basic user info (username, email, firstName, lastName)
     * - Manager ID from attributes
     * - Roles from realmAccess
     * - Team IDs are not extracted here (require separate API call to get user's groups)
     * </p>
     *
     * @param keycloakUser Keycloak user representation
     * @return IamUser domain model
     */
    public IamUser toIamUser(KeycloakUserRepresentation keycloakUser) {
        if (keycloakUser == null) {
            return null;
        }

        Instant now = Instant.now();

        // Extract manager ID from attributes
        String managerId = extractManagerId(keycloakUser.getAttributes());

        // Extract roles from realmAccess
        List<String> roles = extractRoles(keycloakUser);

        // Note: teamIds will be populated separately via getUserGroups() call
        // This mapper focuses on user-level attributes only

        return IamUser.builder()
                .userId(IamUserId.of(keycloakUser.getId()))
                .username(keycloakUser.getUsername())
                .email(keycloakUser.getEmail())
                .firstName(keycloakUser.getFirstName())
                .lastName(keycloakUser.getLastName())
                .teamIds(Collections.emptyList()) // Will be populated separately
                .managerId(managerId)
                .roles(roles)
                .createdAt(now)
                .updatedAt(now)
                .syncedAt(now)
                .build();
    }

    /**
     * Convert KeycloakUserRepresentation to IamUser with team IDs.
     * <p>
     * Includes team IDs extracted from groups list.
     * </p>
     *
     * @param keycloakUser Keycloak user representation
     * @param teamIds      List of team IDs the user belongs to
     * @return IamUser domain model with team IDs
     */
    public IamUser toIamUserWithTeams(KeycloakUserRepresentation keycloakUser, List<String> teamIds) {
        IamUser user = toIamUser(keycloakUser);
        if (user != null) {
            user.setTeamIds(teamIds != null ? new ArrayList<>(teamIds) : Collections.emptyList());
        }
        return user;
    }

    /**
     * Extract manager ID from Keycloak user attributes.
     * <p>
     * Looks for "manager_id" attribute and returns the first value if present.
     * </p>
     *
     * @param attributes Keycloak user attributes map
     * @return Manager ID or null if not found
     */
    private String extractManagerId(Map<String, List<String>> attributes) {
        if (attributes == null) {
            return null;
        }

        List<String> managerIds = attributes.get("manager_id");
        if (managerIds != null && !managerIds.isEmpty()) {
            return managerIds.get(0);
        }

        return null;
    }

    /**
     * Extract roles from Keycloak user representation.
     * <p>
     * Gets roles from realmAccess.roles field.
     * </p>
     *
     * @param keycloakUser Keycloak user representation
     * @return List of role names, empty list if none found
     */
    private List<String> extractRoles(KeycloakUserRepresentation keycloakUser) {
        if (keycloakUser.getRealmAccess() == null) {
            return Collections.emptyList();
        }

        List<String> roles = keycloakUser.getRealmAccess().getRoles();
        return roles != null ? new ArrayList<>(roles) : Collections.emptyList();
    }

    /**
     * Extract team IDs from Keycloak group paths.
     * <p>
     * Converts group paths like "/teams/team_core" to team IDs like "team_core".
     * Handles various path formats.
     * </p>
     *
     * @param groupPaths List of Keycloak group paths
     * @return List of team IDs
     */
    public List<String> extractTeamIdsFromGroups(List<String> groupPaths) {
        if (groupPaths == null || groupPaths.isEmpty()) {
            return Collections.emptyList();
        }

        return groupPaths.stream()
                .map(this::extractTeamIdFromPath)
                .filter(teamId -> teamId != null && !teamId.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Extract team ID from Keycloak group path.
     * <p>
     * Examples:
     * - "/teams/team_core" -> "team_core"
     * - "/team_core" -> "team_core"
     * - "team_core" -> "team_core"
     * </p>
     *
     * @param path Group path from Keycloak
     * @return Team ID extracted from path
     */
    private String extractTeamIdFromPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        // Remove leading slash
        String normalized = path.startsWith("/") ? path.substring(1) : path;

        // Remove "teams/" prefix if present
        if (normalized.startsWith("teams/")) {
            normalized = normalized.substring("teams/".length());
        }

        return normalized;
    }
}

