package com.example.control.domain.object;

import com.example.control.domain.id.IamUserId;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Domain model representing a cached user projection from Keycloak.
 * <p>
 * This is an optional projection for audit, reporting, and workflow support.
 * The source of truth remains Keycloak, but this provides faster access
 * to user information without requiring Keycloak Admin API calls.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IamUser {

    /**
     * Keycloak user ID (sub claim).
     */
    @NotNull(message = "User ID is required")
    private IamUserId userId;

    /**
     * Username.
     */
    private String username;

    /**
     * Email address.
     */
    private String email;

    /**
     * First name.
     */
    private String firstName;

    /**
     * Last name.
     */
    private String lastName;

    /**
     * Team IDs the user belongs to.
     */
    private List<String> teamIds;

    /**
     * Manager ID (Keycloak user ID of line manager).
     */
    private String managerId;

    /**
     * Roles assigned to the user.
     */
    private List<String> roles;

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
