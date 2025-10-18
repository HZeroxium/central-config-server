package com.example.control.infrastructure.repository.documents;

import com.example.control.domain.IamUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB document representation of {@link IamUser}.
 * <p>
 * This persistence model is used by Spring Data MongoDB to store cached user
 * projections from Keycloak in the {@code iam_users} collection.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "iam_users")
public class IamUserDocument {

    /** Document identifier: Keycloak user ID (sub claim). */
    @Id
    private String userId;

    /** Username. */
    private String username;

    /** Email address. */
    @Indexed
    private String email;

    /** First name. */
    private String firstName;

    /** Last name. */
    private String lastName;

    /** Team IDs the user belongs to. */
    private List<String> teamIds;

    /** Manager ID (Keycloak user ID of line manager). */
    @Indexed
    private String managerId;

    /** Roles assigned to the user. */
    private List<String> roles;

    /** Timestamp when this projection was last synced from Keycloak. */
    private Instant syncedAt;

    /**
     * Maps a {@link IamUser} domain object to a MongoDB document representation.
     *
     * @param domain domain model
     * @return new {@link IamUserDocument} populated from domain
     */
    public static IamUserDocument fromDomain(IamUser domain) {
        return IamUserDocument.builder()
                .userId(domain.getUserId())
                .username(domain.getUsername())
                .email(domain.getEmail())
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .teamIds(domain.getTeamIds())
                .managerId(domain.getManagerId())
                .roles(domain.getRoles())
                .syncedAt(domain.getSyncedAt())
                .build();
    }

    /**
     * Converts this document back into its domain representation.
     *
     * @return new {@link IamUser} populated from document
     */
    public IamUser toDomain() {
        return IamUser.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .teamIds(teamIds)
                .managerId(managerId)
                .roles(roles)
                .syncedAt(syncedAt)
                .build();
    }
}
