package com.example.control.infrastructure.adapter.persistence.mongo.documents;

import com.example.control.domain.model.IamTeam;
import com.example.control.domain.valueobject.id.IamTeamId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB document representation of {@link IamTeam}.
 * <p>
 * This persistence model is used by Spring Data MongoDB to store cached team
 * projections from Keycloak in the {@code iam_teams} collection.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "iam_teams")
public class IamTeamDocument {

    /**
     * Document identifier: Keycloak group name (team ID).
     */
    @Id
    // @Field("teamId")
    private String teamId;

    /**
     * Display name of the team.
     */
    @Field("displayName")
    private String displayName;

    /**
     * List of member user IDs in this team.
     */
    @Field("members")
    private List<String> members;

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
    @LastModifiedDate
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
     * Maps a {@link IamTeam} domain object to a MongoDB document representation.
     *
     * @param domain domain model
     * @return new {@link IamTeamDocument} populated from domain
     */
    public static IamTeamDocument fromDomain(IamTeam domain) {
        return IamTeamDocument.builder()
                .teamId(domain.getTeamId().teamId())
                .displayName(domain.getDisplayName())
                .members(domain.getMembers())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .syncedAt(domain.getSyncedAt())
                .build();
    }

    /**
     * Converts this document back into its domain representation.
     *
     * @return new {@link IamTeam} populated from document
     */
    public IamTeam toDomain() {
        return IamTeam.builder()
                .teamId(IamTeamId.of(teamId))
                .displayName(displayName)
                .members(members)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .syncedAt(syncedAt)
                .build();
    }
}
