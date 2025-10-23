package com.example.control.infrastructure.mongo.documents;

import com.example.control.domain.object.ServiceShare;
import com.example.control.domain.id.ServiceShareId;
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
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB document representation of {@link ServiceShare}.
 * <p>
 * This persistence model is used by Spring Data MongoDB to store service sharing
 * ACL (Access Control List) in the {@code service_shares} collection with optional
 * TTL for automatic expiration.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "service_shares")
@CompoundIndexes({
    @CompoundIndex(name = "share_service_grantee", def = "{'serviceId': 1, 'grantToType': 1, 'grantToId': 1}"),
    @CompoundIndex(name = "share_grantee_type", def = "{'grantToType': 1, 'grantToId': 1}")
})
public class ServiceShareDocument {

    /** Document identifier: UUID string. */
    @Id
    private String id;

    /** Level of resource being shared (stored as string value). */
    @Field("resourceLevel")
    private String resourceLevel;

    /** Service ID being shared. */
    @Indexed
    @Field("serviceId")
    private String serviceId;

    /** Instance ID if sharing at instance level (optional). */
    @Field("instanceId")
    private String instanceId;

    /** Type of grantee (stored as string value). */
    @Field("grantToType")
    private String grantToType;

    /** ID of the grantee (team ID or user ID). */
    @Indexed
    @Field("grantToId")
    private String grantToId;

    /** Permissions being granted (stored as string values). */
    @Field("permissions")
    private List<String> permissions;

    /** Environment filter (optional, null means all environments). */
    @Field("environments")
    private List<String> environments;

    /** User who created this share (Keycloak user ID). */
    @Field("grantedBy")
    private String grantedBy;

    /** Timestamp when the share was created. */
    @Field("createdAt")
    @CreatedDate
    private Instant createdAt;

    /** Optional expiration timestamp. */
    @Field("expiresAt")
    private Instant expiresAt;

    /** User who created this share (Keycloak user ID). */
    @Field("createdBy")
    @CreatedBy
    private String createdBy;

    /** User who last modified this share (Keycloak user ID). */
    @Field("updatedBy")
    @LastModifiedBy
    private String updatedBy;

    /** Timestamp when the share was last updated. */
    @Field("updatedAt")
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Maps a {@link ServiceShare} domain object to a MongoDB document representation.
     *
     * @param domain domain model
     * @return new {@link ServiceShareDocument} populated from domain
     */
    public static ServiceShareDocument fromDomain(ServiceShare domain) {
        return ServiceShareDocument.builder()
                .id(domain.getId() != null ? domain.getId().id() : null)
                .resourceLevel(domain.getResourceLevel() != null ? domain.getResourceLevel().name() : null)
                .serviceId(domain.getServiceId())
                .instanceId(domain.getInstanceId())
                .grantToType(domain.getGrantToType() != null ? domain.getGrantToType().name() : null)
                .grantToId(domain.getGrantToId())
                .permissions(domain.getPermissions() != null 
                    ? domain.getPermissions().stream()
                        .map(Enum::name)
                        .toList() 
                    : null)
                .environments(domain.getEnvironments())
                .grantedBy(domain.getGrantedBy())
                .createdAt(domain.getCreatedAt())
                .expiresAt(domain.getExpiresAt())
                .build();
    }

    /**
     * Converts this document back into its domain representation.
     *
     * @return new {@link ServiceShare} populated from document
     */
    public ServiceShare toDomain() {
        return ServiceShare.builder()
                .id(ServiceShareId.of(id != null ? id : null))
                .resourceLevel(resourceLevel != null 
                    ? ServiceShare.ResourceLevel.valueOf(resourceLevel) 
                    : null)
                .serviceId(serviceId)
                .instanceId(instanceId)
                .grantToType(grantToType != null 
                    ? ServiceShare.GranteeType.valueOf(grantToType) 
                    : null)
                .grantToId(grantToId)
                .permissions(permissions != null 
                    ? permissions.stream()
                        .map(ServiceShare.SharePermission::valueOf)
                        .toList() 
                    : null)
                .environments(environments)
                .grantedBy(grantedBy)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();
    }
}
