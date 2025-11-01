package com.example.control.domain.model;

import com.example.control.domain.valueobject.id.IamTeamId;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Domain model representing a cached team projection from Keycloak.
 * <p>
 * This is an optional projection for audit, reporting, and workflow support.
 * The source of truth remains Keycloak groups, but this provides faster access
 * to team information without requiring Keycloak Admin API calls.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IamTeam {

    /**
     * Keycloak group name (team ID).
     */
    @NotNull(message = "Team ID is required")
    private IamTeamId teamId;

    /**
     * Display name of the team.
     */
    private String displayName;

    /**
     * List of member user IDs in this team.
     */
    private List<String> members;

    /**
     * Timestamp when this projection was created.
     */
    private Instant createdAt;

    /**
     * Timestamp when this projection was last updated.
     */
    private Instant updatedAt;

    /**
     * Timestamp when this projection was last synced from Keycloak.
     */
    private Instant syncedAt;
}
