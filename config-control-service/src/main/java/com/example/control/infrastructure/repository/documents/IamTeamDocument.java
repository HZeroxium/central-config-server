package com.example.control.infrastructure.repository.documents;

import com.example.control.domain.IamTeam;
import com.example.control.domain.id.IamTeamId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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

    /** Document identifier: Keycloak group name (team ID). */
    @Id
    private String teamId;

    /** Display name of the team. */
    private String displayName;

    /** List of member user IDs in this team. */
    private List<String> members;

    /** Timestamp when this projection was last synced from Keycloak. */
    private Instant syncedAt;

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
                .syncedAt(syncedAt)
                .build();
    }
}
