package com.example.control.infrastructure.repository;

import com.example.control.domain.ServiceInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB document representation of {@link ServiceInstance}.
 * <p>
 * This persistence model is used by Spring Data MongoDB to store instance metadata and
 * health/drift status in the {@code service_instances} collection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "service_instances")
public class ServiceInstanceDocument {

  /** Document identifier: concatenation of serviceName and instanceId. */
  @Id
  private String id;

  /** Service name for indexing and query grouping. */
  @Indexed
  private String serviceName;

  /** Instance identifier for unique lookup. */
  @Indexed
  private String instanceId;

  private String host;
  private Integer port;
  private String environment;
  private String version;

  private String configHash;
  private String lastAppliedHash;

  /** Current status of the instance (stored as string value). */
  @Indexed
  private String status;

  /**
   * Timestamp of last heartbeat from the instance.
   * <p>
   * Indexed with TTL = 1 hour to automatically expire inactive instances.
   */
  @Indexed(expireAfterSeconds = 3600)
  private LocalDateTime lastSeenAt;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private Map<String, String> metadata;

  private Boolean hasDrift;
  private LocalDateTime driftDetectedAt;

  /**
   * Maps a {@link ServiceInstance} domain object to a MongoDB document representation.
   *
   * @param domain domain model
   * @return new {@link ServiceInstanceDocument} populated from domain
   */
  public static ServiceInstanceDocument fromDomain(ServiceInstance domain) {
    return ServiceInstanceDocument.builder()
        .id(domain.getServiceName() + ":" + domain.getInstanceId())
        .serviceName(domain.getServiceName())
        .instanceId(domain.getInstanceId())
        .host(domain.getHost())
        .port(domain.getPort())
        .environment(domain.getEnvironment())
        .version(domain.getVersion())
        .configHash(domain.getConfigHash())
        .lastAppliedHash(domain.getLastAppliedHash())
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
        .serviceName(serviceName)
        .instanceId(instanceId)
        .host(host)
        .port(port)
        .environment(environment)
        .version(version)
        .configHash(configHash)
        .lastAppliedHash(lastAppliedHash)
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
