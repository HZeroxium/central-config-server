package com.example.control.api.http.mapper.infra;

import com.example.control.api.http.dto.common.ApiResponseDto;
import com.example.control.api.http.dto.infra.ConsulDto;
import com.example.control.api.http.mapper.domain.ServiceInstanceMapper;
import com.example.control.domain.model.ServiceInstance;
import com.example.control.domain.valueobject.id.ServiceInstanceId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ConsulMapper {

    private ConsulMapper() {
    }

    // ------------------------
    // Domain mapping (instances)
    // ------------------------
    public static ServiceInstance fromHealthResponse(ConsulDto.ConsulHealthResponse health) {
        if (health == null || health.getService() == null) return null;

        ConsulDto.ServiceInfo svc = health.getService();
        Map<String, String> meta = new HashMap<>();
        if (svc.getMeta() != null) meta.putAll(svc.getMeta());

        return ServiceInstance.builder()
                .id(ServiceInstanceId.of(svc.getId()))
                .host(svc.getAddress())
                .port(svc.getPort())
                .status(ServiceInstance.InstanceStatus.HEALTHY)
                .metadata(meta)
                .build();
    }

    public static List<ServiceInstance> toDomainInstancesFromHealthJson(String json, ObjectMapper objectMapper) {
        return parseHealthResponse(json, objectMapper).stream()
                .map(ConsulMapper::fromHealthResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static List<ApiResponseDto.ServiceInstanceSummary> toSummariesFromHealthJson(String json, ObjectMapper objectMapper) {
        return toDomainInstancesFromHealthJson(json, objectMapper).stream()
                .map(ServiceInstanceMapper::toSummary)
                .collect(Collectors.toList());
    }

    // ------------------------
    // Parsing helpers (moved from controller)
    // Keep DTO usage internal to mapper where feasible
    // ------------------------
    public static ConsulDto.ConsulServicesMap parseServicesResponse(String json, ObjectMapper objectMapper) {
        try {
            Map<String, List<String>> services = objectMapper.readValue(json, new TypeReference<Map<String, List<String>>>() {
            });
            return ConsulDto.ConsulServicesMap.builder().services(services).build();
        } catch (Exception e) {
            return ConsulDto.ConsulServicesMap.builder().services(Map.of()).build();
        }
    }

    public static List<ConsulDto.ConsulServiceResponse> parseServiceResponse(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ConsulDto.ConsulServiceResponse>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public static List<ConsulDto.ConsulHealthResponse> parseHealthResponse(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ConsulDto.ConsulHealthResponse>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public static List<ConsulDto.ConsulNodeInfo> parseNodesResponse(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ConsulDto.ConsulNodeInfo>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public static List<ConsulDto.ConsulKVResponse> parseKVResponse(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ConsulDto.ConsulKVResponse>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public static Map<String, ConsulDto.ConsulAgentService> parseAgentServicesResponse(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, ConsulDto.ConsulAgentService>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    public static List<ConsulDto.ConsulMemberInfo> parseMembersResponse(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ConsulDto.ConsulMemberInfo>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
