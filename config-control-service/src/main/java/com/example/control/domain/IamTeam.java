package com.example.control.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
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

    /** Keycloak group name (team ID). */
    @NotBlank(message = "Team ID is required")
    private String teamId;

    /** Display name of the team. */
    private String displayName;

    /** List of member user IDs in this team. */
    private List<String> members;

    /** Timestamp when this projection was last synced from Keycloak. */
    private Instant syncedAt;
}
