package com.example.control.infrastructure.repository;

import com.example.control.domain.DriftEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "drift_events")
public class DriftEventDocument {

  @Id
  private String id;

  @Indexed
  private String serviceName;

  @Indexed
  private String instanceId;

  private String expectedHash;
  private String appliedHash;

  @Indexed
  private String severity;

  @Indexed
  private String status;

  @Indexed(expireAfterSeconds = 2592000) // TTL index - expire after 30 days
  private LocalDateTime detectedAt;

  private LocalDateTime resolvedAt;

  private String detectedBy;
  private String resolvedBy;

  private String notes;

  public static DriftEventDocument fromDomain(DriftEvent domain) {
    return DriftEventDocument.builder()
        .id(domain.getId())
        .serviceName(domain.getServiceName())
        .instanceId(domain.getInstanceId())
        .expectedHash(domain.getExpectedHash())
        .appliedHash(domain.getAppliedHash())
        .severity(domain.getSeverity() != null ? domain.getSeverity().name() : null)
        .status(domain.getStatus() != null ? domain.getStatus().name() : null)
        .detectedAt(domain.getDetectedAt())
        .resolvedAt(domain.getResolvedAt())
        .detectedBy(domain.getDetectedBy())
        .resolvedBy(domain.getResolvedBy())
        .notes(domain.getNotes())
        .build();
  }

  public DriftEvent toDomain() {
    return DriftEvent.builder()
        .id(id)
        .serviceName(serviceName)
        .instanceId(instanceId)
        .expectedHash(expectedHash)
        .appliedHash(appliedHash)
        .severity(severity != null ? DriftEvent.DriftSeverity.valueOf(severity) : null)
        .status(status != null ? DriftEvent.DriftStatus.valueOf(status) : null)
        .detectedAt(detectedAt)
        .resolvedAt(resolvedAt)
        .detectedBy(detectedBy)
        .resolvedBy(resolvedBy)
        .notes(notes)
        .build();
  }
}
