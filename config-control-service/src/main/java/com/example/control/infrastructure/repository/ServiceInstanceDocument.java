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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "service_instances")
public class ServiceInstanceDocument {

  @Id
  private String id; // serviceName:instanceId

  @Indexed
  private String serviceName;

  @Indexed
  private String instanceId;

  private String host;
  private Integer port;
  private String environment;
  private String version;

  private String configHash;
  private String lastAppliedHash;

  @Indexed
  private String status;

  @Indexed(expireAfterSeconds = 3600) // TTL index - expire after 1 hour of inactivity
  private LocalDateTime lastSeenAt;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private Map<String, String> metadata;

  private Boolean hasDrift;
  private LocalDateTime driftDetectedAt;

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
        .status(
            status != null ? ServiceInstance.InstanceStatus.valueOf(status) : ServiceInstance.InstanceStatus.UNKNOWN)
        .lastSeenAt(lastSeenAt)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .metadata(metadata)
        .hasDrift(hasDrift)
        .driftDetectedAt(driftDetectedAt)
        .build();
  }
}
