package com.example.control.api.mapper;

import com.example.control.api.dto.ServiceInstanceDtos;
import com.example.control.domain.ServiceInstance;

public final class ServiceInstanceApiMapper {

  private ServiceInstanceApiMapper() {}

  public static ServiceInstance toDomain(ServiceInstanceDtos.CreateRequest req) {
    return ServiceInstance.builder()
        .serviceName(req.getServiceName())
        .instanceId(req.getInstanceId())
        .host(req.getHost())
        .port(req.getPort())
        .environment(req.getEnvironment())
        .version(req.getVersion())
        .configHash(req.getConfigHash())
        .lastAppliedHash(req.getLastAppliedHash())
        .metadata(req.getMetadata())
        .build();
  }

  public static void apply(ServiceInstance entity, ServiceInstanceDtos.UpdateRequest req) {
    if (req.getHost() != null) entity.setHost(req.getHost());
    if (req.getPort() != null) entity.setPort(req.getPort());
    if (req.getEnvironment() != null) entity.setEnvironment(req.getEnvironment());
    if (req.getVersion() != null) entity.setVersion(req.getVersion());
    if (req.getConfigHash() != null) entity.setConfigHash(req.getConfigHash());
    if (req.getLastAppliedHash() != null) entity.setLastAppliedHash(req.getLastAppliedHash());
    if (req.getHasDrift() != null) entity.setHasDrift(req.getHasDrift());
    if (req.getStatus() != null) entity.setStatus(req.getStatus());
    if (req.getMetadata() != null) entity.setMetadata(req.getMetadata());
  }

  public static ServiceInstanceDtos.Response toResponse(ServiceInstance si) {
    return ServiceInstanceDtos.Response.builder()
        .serviceName(si.getServiceName())
        .instanceId(si.getInstanceId())
        .host(si.getHost())
        .port(si.getPort())
        .environment(si.getEnvironment())
        .version(si.getVersion())
        .configHash(si.getConfigHash())
        .lastAppliedHash(si.getLastAppliedHash())
        .status(si.getStatus())
        .lastSeenAt(si.getLastSeenAt())
        .createdAt(si.getCreatedAt())
        .updatedAt(si.getUpdatedAt())
        .metadata(si.getMetadata())
        .hasDrift(si.getHasDrift())
        .driftDetectedAt(si.getDriftDetectedAt())
        .build();
  }
}


