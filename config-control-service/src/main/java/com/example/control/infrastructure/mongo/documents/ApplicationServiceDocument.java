package com.example.control.infrastructure.mongo.documents;

import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.id.ApplicationServiceId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MongoDB document representation of {@link ApplicationService}.
 * <p>
 * This persistence model is used by Spring Data MongoDB to store public service
 * metadata in the {@code application_services} collection.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "application_services")
@CompoundIndex(def = "{'ownerTeamId': 1, 'lifecycle': 1}")
@CompoundIndex(def = "{'ownerTeamId': 1, 'createdAt': -1}")
public class ApplicationServiceDocument {

    /**
     * Document identifier: UUID string.
     */
    @Id
    private String id;

    /**
     * Human-readable display name.
     */
    @Indexed(unique = true)
    @Field("displayName")
    private String displayName;

    /**
     * Team that owns this service (Keycloak group ID). Null for orphaned services.
     */
    @Indexed
    @Field("ownerTeamId")
    private String ownerTeamId;

    /**
     * List of environments where this service is deployed.
     */
    @Field("environments")
    private List<String> environments;

    /**
     * Tags for categorization and filtering.
     */
    @Field("tags")
    private List<String> tags;

    /**
     * Repository URL for source code.
     */
    @Field("repoUrl")
    private String repoUrl;

    /**
     * Service lifecycle status (stored as string value).
     */
    @Indexed
    @Field("lifecycle")
    private String lifecycle;

    /**
     * Timestamp when the service was first created.
     */
    @Field("createdAt")
    @CreatedDate
    private Instant createdAt;

    /**
     * Timestamp when the service was last updated.
     */
    @Field("updatedAt")
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * User who created this service (Keycloak user ID).
     */
    @Field("createdBy")
    @CreatedBy
    private String createdBy;

    /**
     * User who last modified this service (Keycloak user ID).
     */
    @Field("updatedBy")
    @LastModifiedBy
    private String updatedBy;

    /**
     * Additional attributes as key-value pairs.
     */
    @Field("attributes")
    private Map<String, String> attributes;

    /**
     * Maps a {@link ApplicationService} domain object to a MongoDB document
     * representation.
     *
     * @param domain domain model
     * @return new {@link ApplicationServiceDocument} populated from domain
     */
    public static ApplicationServiceDocument fromDomain(ApplicationService domain) {
        ApplicationServiceDocumentBuilder builder = ApplicationServiceDocument.builder()
                .displayName(domain.getDisplayName())
                .ownerTeamId(domain.getOwnerTeamId())
                .environments(domain.getEnvironments())
                .tags(domain.getTags())
                .repoUrl(domain.getRepoUrl())
                .lifecycle(domain.getLifecycle() != null ? domain.getLifecycle().name() : null)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .attributes(domain.getAttributes());

        // Set ID if it exists (for updates), otherwise generate UUID
        if (domain.getId() != null && domain.getId().id() != null) {
            builder.id(domain.getId().id());
        } else {
            // Generate UUID for new application services
            builder.id(UUID.randomUUID().toString());
        }

        return builder.build();
    }

    /**
     * Converts this document back into its domain representation.
     *
     * @return new {@link ApplicationService} populated from document
     */
    public ApplicationService toDomain() {
        return ApplicationService.builder()
                .id(ApplicationServiceId.of(id != null ? id : null))
                .displayName(displayName)
                .ownerTeamId(ownerTeamId)
                .environments(environments)
                .tags(tags)
                .repoUrl(repoUrl)
                .lifecycle(lifecycle != null
                        ? ApplicationService.ServiceLifecycle.valueOf(lifecycle)
                        : ApplicationService.ServiceLifecycle.ACTIVE)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .createdBy(createdBy)
                .attributes(attributes)
                .build();
    }
}
