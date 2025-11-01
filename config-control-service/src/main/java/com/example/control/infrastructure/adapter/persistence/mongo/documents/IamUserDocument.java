package com.example.control.infrastructure.adapter.persistence.mongo.documents;

import com.example.control.domain.model.IamUser;
import com.example.control.domain.valueobject.id.IamUserId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

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

    /**
     * Document identifier: Keycloak user ID (sub claim).
     */
    @Id
    @Field("userId")
    private String userId;

    /**
     * Username.
     */
    @Field("username")
    private String username;

    /**
     * Email address.
     */
    @Indexed
    @Field("email")
    private String email;

    /**
     * First name.
     */
    @Field("firstName")
    private String firstName;

    /**
     * Last name.
     */
    @Field("lastName")
    private String lastName;

    /**
     * Team IDs the user belongs to.
     */
    @Field("teamIds")
    private List<String> teamIds;

    /**
     * Manager ID (Keycloak user ID of line manager).
     */
    @Indexed
    @Field("managerId")
    private String managerId;

    /**
     * Roles assigned to the user.
     */
    @Indexed
    @Field("roles")
    private List<String> roles;

    /**
     * Timestamp when this projection was created.
     */
    @Field("createdAt")
    @CreatedDate
    private Instant createdAt;

    /**
     * Timestamp when this projection was last updated.
     */
    @Field("updatedAt")
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Timestamp when this projection was last synced from Keycloak.
     */
    @Field("syncedAt")
    private Instant syncedAt;

    /**
     * User who created this projection (Keycloak user ID).
     */
    @Field("createdBy")
    @CreatedBy
    private String createdBy;

    /**
     * User who last modified this projection (Keycloak user ID).
     */
    @Field("updatedBy")
    @LastModifiedBy
    private String updatedBy;

    /**
     * Maps a {@link IamUser} domain object to a MongoDB document representation.
     *
     * @param domain domain model
     * @return new {@link IamUserDocument} populated from domain
     */
    public static IamUserDocument fromDomain(IamUser domain) {
        return IamUserDocument.builder()
                .userId(domain.getUserId().userId())
                .username(domain.getUsername())
                .email(domain.getEmail())
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .teamIds(domain.getTeamIds())
                .managerId(domain.getManagerId())
                .roles(domain.getRoles())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
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
                .userId(IamUserId.of(userId))
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .teamIds(teamIds)
                .managerId(managerId)
                .roles(roles)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .syncedAt(syncedAt)
                .build();
    }
}
