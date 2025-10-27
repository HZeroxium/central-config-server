package com.example.control.infrastructure.mongo.documents;

import com.example.control.domain.object.ServiceInstance;
import com.example.control.domain.id.ServiceInstanceId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

/**
 * MongoDB document representation of {@link ServiceInstance}.
 * <p>
 * This persistence model is used by Spring Data MongoDB to store instance
 * metadata and
 * health/drift status in the {@code service_instances} collection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "service_instances")
@CompoundIndex(def = "{'serviceId': 1, 'teamId': 1}")
@CompoundIndex(def = "{'teamId': 1, 'status': 1}")
@CompoundIndex(def = "{'serviceId': 1, 'environment': 1}")
@CompoundIndex(def = "{'teamId': 1, 'hasDrift': 1}")
public class ServiceInstanceDocument {

  /** Document identifier: instanceId (globally unique). */
  @Id
  private String id;

  /** Service ID from ApplicationService (for team-based access control). */
  @Indexed
  @Field("serviceId")
  private String serviceId;

  /**
   * Team ID that owns this service instance (from
   * ApplicationService.ownerTeamId).
   */
  @Indexed
  @Field("teamId")
  private String teamId;

  @Field("host")
  private String host;

  @Field("port")
  private Integer port;

  @Field("environment")
  private String environment;

  @Field("version")
  private String version;

  @Field("configHash")
  private String configHash;

  @Field("lastAppliedHash")
  private String lastAppliedHash;

  @Field("expectedHash")
  private String expectedHash;

  /** Current status of the instance (stored as string value). */
  @Indexed
  @Field("status")
  private String status;

  /**
   * Timestamp of last heartbeat from the instance.
   * <p>
   * Indexed with TTL = 1 hour to automatically expire inactive instances.
   */
  @Indexed(expireAfter = "1h", name = "lastSeenAt_ttl")
  @Field("lastSeenAt")
  private Instant lastSeenAt;

  @Field("createdAt")
  @CreatedDate
  private Instant createdAt;

  @Field("updatedAt")
  @LastModifiedDate
  private Instant updatedAt;

  @Field("metadata")
  private Map<String, String> metadata;

  @Field("hasDrift")
  private Boolean hasDrift;

  @Field("driftDetectedAt")
  @CreatedDate
  private Instant driftDetectedAt;

  /**
   * Maps a {@link ServiceInstance} domain object to a MongoDB document
   * representation.
   *
   * @param domain domain model
   * @return new {@link ServiceInstanceDocument} populated from domain
   */
  public static ServiceInstanceDocument fromDomain(ServiceInstance domain) {
    return ServiceInstanceDocument.builder()
        .id(domain.getId().toDocumentId())
        .serviceId(domain.getServiceId())
        .teamId(domain.getTeamId())
        .host(domain.getHost())
        .port(domain.getPort())
        .environment(domain.getEnvironment())
        .version(domain.getVersion())
        .configHash(domain.getConfigHash())
        .lastAppliedHash(domain.getLastAppliedHash())
        .expectedHash(domain.getExpectedHash())
        .status(domain.getStatus() != null ? domain.getStatus().name() : null)
        .lastSeenAt(domain.getLastSeenAt())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .metadata(domain.getMetadata())
        .hasDrift(domain.getHasDrift())
        .driftDetectedAt(domain.getDriftDetectedAt())
        .build();
  }

  /**
   * Converts this document back into its domain representation.
   *
   * @return new {@link ServiceInstance} populated from document
   */
  public ServiceInstance toDomain() {
    return ServiceInstance.builder()
        .id(ServiceInstanceId.fromDocumentId(id))
        .serviceId(serviceId)
        .teamId(teamId)
        .host(host)
        .port(port)
        .environment(environment)
        .version(version)
        .configHash(configHash)
        .lastAppliedHash(lastAppliedHash)
        .expectedHash(expectedHash)
        .status(status != null
            ? ServiceInstance.InstanceStatus.valueOf(status)
            : ServiceInstance.InstanceStatus.UNKNOWN)
        .lastSeenAt(lastSeenAt)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .metadata(metadata)
        .hasDrift(hasDrift)
        .driftDetectedAt(driftDetectedAt)
        .build();
  }
}
