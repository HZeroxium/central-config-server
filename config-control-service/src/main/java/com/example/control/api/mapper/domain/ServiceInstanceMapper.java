package com.example.control.api.mapper.domain;

import com.example.control.api.dto.common.ApiResponseDto;
import com.example.control.domain.object.ServiceInstance;

public final class ServiceInstanceMapper {

  private ServiceInstanceMapper() {}

  public static ApiResponseDto.ServiceInstanceSummary toSummary(ServiceInstance instance) {
    if (instance == null) return null;
    return ApiResponseDto.ServiceInstanceSummary.builder()
        .serviceName(instance.getServiceId())
        .instanceId(instance.getInstanceId())
        .host(instance.getHost())
        .port(instance.getPort())
        .status(instance.getStatus() != null ? instance.getStatus().name() : null)
        .scheme("http")
        .uri(buildUri(instance))
        .healthy(instance.getStatus() == ServiceInstance.InstanceStatus.HEALTHY)
        .lastSeenAt(instance.getLastSeenAt() != null ? instance.getLastSeenAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
        .build();
  }

  private static String buildUri(ServiceInstance instance) {
    if (instance.getHost() == null || instance.getPort() == null) return null;
    return "http://" + instance.getHost() + ":" + instance.getPort();
  }
}


