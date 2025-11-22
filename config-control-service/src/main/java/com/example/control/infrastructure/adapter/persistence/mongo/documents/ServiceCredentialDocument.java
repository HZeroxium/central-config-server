package com.example.control.infrastructure.adapter.persistence.mongo.documents;

import com.example.control.domain.model.ServiceCredential;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.valueobject.id.ServiceCredentialId;
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
import java.util.UUID;

/**
 * MongoDB document representation of {@link ServiceCredential}.
 * <p>
 * This persistence model is used by Spring Data MongoDB to store service
 * credential metadata in the {@code service_credentials} collection.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "service_credentials")
public class ServiceCredentialDocument {

    /**
     * Document identifier: UUID string.
     */
    @Id
    private String id;

    /**
     * Foreign key to ApplicationService (UUID string).
     * Unique constraint: one credential per service.
     */
    @Indexed(unique = true)
    @Field("serviceId")
    private String serviceId;

    /**
     * Keycloak client ID (used in JWT claims).
     * Unique constraint for efficient lookup during authentication.
     */
    @Indexed(unique = true)
    @Field("keycloakClientId")
    private String keycloakClientId;

    /**
     * Keycloak client UUID (internal Keycloak ID).
     */
    @Field("keycloakClientUuid")
    private String keycloakClientUuid;

    /**
     * Credential status (stored as string value).
     */
    @Indexed
    @Field("status")
    private String status;

    /**
     * When credentials expire (optional).
     */
    @Field("expiresAt")
    private Instant expiresAt;

    /**
     * When credentials were revoked (if status = REVOKED).
     */
    @Field("revokedAt")
    private Instant revokedAt;

    /**
     * User who created these credentials (Keycloak user ID).
     */
    @Field("createdBy")
    @CreatedBy
    private String createdBy;

    /**
     * User who last updated/revoked these credentials (Keycloak user ID).
     */
    @Field("updatedBy")
    @LastModifiedBy
    private String updatedBy;

    /**
     * Timestamp when the credential was first created.
     */
    @Field("createdAt")
    @CreatedDate
    private Instant createdAt;

    /**
     * Timestamp when the credential was last updated.
     */
    @Field("updatedAt")
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Version for optimistic locking.
     */
    @Field("version")
    private Integer version;

    /**
     * Maps a {@link ServiceCredential} domain object to a MongoDB document
     * representation.
     *
     * @param domain domain model
     * @return new {@link ServiceCredentialDocument} populated from domain
     */
    public static ServiceCredentialDocument fromDomain(ServiceCredential domain) {
        ServiceCredentialDocumentBuilder builder = ServiceCredentialDocument.builder()
                .serviceId(domain.getServiceId() != null ? domain.getServiceId().id() : null)
                .keycloakClientId(domain.getKeycloakClientId())
                .keycloakClientUuid(domain.getKeycloakClientUuid())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .expiresAt(domain.getExpiresAt())
                .revokedAt(domain.getRevokedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion());

        // Set ID if it exists (for updates), otherwise generate UUID
        if (domain.getId() != null && domain.getId().id() != null) {
            builder.id(domain.getId().id());
        } else {
            // Generate UUID for new credentials
            builder.id(UUID.randomUUID().toString());
        }

        return builder.build();
    }

    /**
     * Converts this document back into its domain representation.
     *
     * @return new {@link ServiceCredential} populated from document
     */
    public ServiceCredential toDomain() {
        return ServiceCredential.builder()
                .id(ServiceCredentialId.of(id != null ? id : UUID.randomUUID().toString()))
                .serviceId(serviceId != null ? ApplicationServiceId.of(serviceId) : null)
                .keycloakClientId(keycloakClientId)
                .keycloakClientUuid(keycloakClientUuid)
                .status(status != null
                        ? ServiceCredential.CredentialStatus.valueOf(status)
                        : ServiceCredential.CredentialStatus.PENDING)
                .expiresAt(expiresAt)
                .revokedAt(revokedAt)
                .createdBy(createdBy)
                .updatedBy(updatedBy)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(version != null ? version : 0)
                .build();
    }
}

