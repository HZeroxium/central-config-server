package com.example.control.infrastructure.adapter.external.keycloak.mapper;

import com.example.control.domain.model.IamTeam;
import com.example.control.domain.valueobject.id.IamTeamId;
import com.example.control.infrastructure.adapter.external.keycloak.dto.KeycloakGroupRepresentation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mapper for converting Keycloak Group Representation to IamTeam domain model.
 * <p>
 * Handles extraction of team information from Keycloak groups.
 * </p>
 */
@Slf4j
@Component
public class KeycloakTeamMapper {

    /**
     * Convert KeycloakGroupRepresentation to IamTeam domain model.
     * <p>
     * Extracts:
     * - Team ID from group name
     * - Display name from group name
     * - Members are not extracted here (require separate API call to get group members)
     * </p>
     *
     * @param keycloakGroup Keycloak group representation
     * @return IamTeam domain model
     */
    public IamTeam toIamTeam(KeycloakGroupRepresentation keycloakGroup) {
        if (keycloakGroup == null) {
            return null;
        }

        Instant now = Instant.now();

        // Extract team ID from name (group name is the team identifier)
        String teamId = extractTeamId(keycloakGroup);

        return IamTeam.builder()
                .teamId(IamTeamId.of(teamId))
                .displayName(keycloakGroup.getName())
                .members(Collections.emptyList()) // Will be populated separately
                .createdAt(now)
                .updatedAt(now)
                .syncedAt(now)
                .build();
    }

    /**
     * Convert KeycloakGroupRepresentation to IamTeam with members.
     * <p>
     * Includes member user IDs extracted from separate API call.
     * </p>
     *
     * @param keycloakGroup Keycloak group representation
     * @param members       List of member user IDs
     * @return IamTeam domain model with members
     */
    public IamTeam toIamTeamWithMembers(KeycloakGroupRepresentation keycloakGroup, List<String> members) {
        IamTeam team = toIamTeam(keycloakGroup);
        if (team != null) {
            team.setMembers(members != null ? new ArrayList<>(members) : Collections.emptyList());
        }
        return team;
    }

    /**
     * Extract team ID from Keycloak group.
     * <p>
     * Uses group name as team ID. If name contains path separators,
     * extracts the last segment.
     * </p>
     *
     * @param keycloakGroup Keycloak group representation
     * @return Team ID extracted from group
     */
    private String extractTeamId(KeycloakGroupRepresentation keycloakGroup) {
        String name = keycloakGroup.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Keycloak group name cannot be null or blank");
        }

        // If name contains path separators (e.g., "/teams/team_core"), extract last segment
        if (name.contains("/")) {
            String[] parts = name.split("/");
            return parts[parts.length - 1];
        }

        return name;
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
    public String extractTeamIdFromPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        // Remove leading slash
        String normalized = path.startsWith("/") ? path.substring(1) : path;

        // Remove "teams/" prefix if present
        if (normalized.startsWith("teams/")) {
            normalized = normalized.substring("teams/".length());
        }

        // Extract last segment if path contains separators
        if (normalized.contains("/")) {
            String[] parts = normalized.split("/");
            return parts[parts.length - 1];
        }

        return normalized;
    }
}

