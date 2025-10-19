package com.example.control.infrastructure.repository.documents;

import com.example.control.domain.ServiceShare;
import com.example.control.domain.id.ServiceShareId;
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
public class ServiceShareDocument {

    /** Document identifier. */
    @Id
    private String id;

    /** Level of resource being shared (stored as string value). */
    private String resourceLevel;

    /** Service ID being shared. */
    @Indexed
    private String serviceId;

    /** Instance ID if sharing at instance level (optional). */
    private String instanceId;

    /** Type of grantee (stored as string value). */
    private String grantToType;

    /** ID of the grantee (team ID or user ID). */
    @Indexed
    private String grantToId;

    /** Permissions being granted (stored as string values). */
    private List<String> permissions;

    /** Environment filter (optional, null means all environments). */
    private List<String> environments;

    /** User who created this share (Keycloak user ID). */
    private String grantedBy;

    /** Timestamp when the share was created. */
    private Instant createdAt;

    /** Optional expiration timestamp. */
    private Instant expiresAt;

    /**
     * Maps a {@link ServiceShare} domain object to a MongoDB document representation.
     *
     * @param domain domain model
     * @return new {@link ServiceShareDocument} populated from domain
     */
    public static ServiceShareDocument fromDomain(ServiceShare domain) {
        return ServiceShareDocument.builder()
                .id(domain.getId().id())
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
                .id(ServiceShareId.of(id))
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
