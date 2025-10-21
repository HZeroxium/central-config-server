package com.example.control.api.dto.consul;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTOs for Consul API responses
 */
public class ConsulDto {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Consul service details response")
  public static class ConsulServiceResponse {
    @JsonProperty("ID")
    private String id;
    @JsonProperty("Node")
    private String node;
    @JsonProperty("Address")
    private String address;
    @JsonProperty("Datacenter")
    private String datacenter;
    @JsonProperty("TaggedAddresses")
    private TaggedAddresses taggedAddresses;
    @JsonProperty("NodeMeta")
    private NodeMeta nodeMeta;
    @JsonProperty("ServiceKind")
    private String serviceKind;
    @JsonProperty("ServiceID")
    private String serviceId;
    @JsonProperty("ServiceName")
    private String serviceName;
    @JsonProperty("ServiceTags")
    private List<String> serviceTags;
    @JsonProperty("ServiceAddress")
    private String serviceAddress;
    @JsonProperty("ServiceWeights")
    private ServiceWeights serviceWeights;
    @JsonProperty("ServiceMeta")
    private Map<String, String> serviceMeta;
    @JsonProperty("ServicePort")
    private Integer servicePort;
    @JsonProperty("ServiceSocketPath")
    private String serviceSocketPath;
    @JsonProperty("ServiceEnableTagOverride")
    private Boolean serviceEnableTagOverride;
    @JsonProperty("ServiceProxy")
    private ServiceProxy serviceProxy;
    @JsonProperty("ServiceConnect")
    private ServiceConnect serviceConnect;
    @JsonProperty("ServiceLocality")
    private Object serviceLocality;
    @JsonProperty("CreateIndex")
    private Long createIndex;
    @JsonProperty("ModifyIndex")
    private Long modifyIndex;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TaggedAddresses {
    @JsonProperty("lan")
    private String lan;
    @JsonProperty("lan_ipv4")
    private String lanIpv4;
    @JsonProperty("wan")
    private String wan;
    @JsonProperty("wan_ipv4")
    private String wanIpv4;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NodeMeta {
    @JsonProperty("consul-network-segment")
    private String consulNetworkSegment;
    @JsonProperty("consul-version")
    private String consulVersion;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ServiceWeights {
    @JsonProperty("Passing")
    private Integer passing;
    @JsonProperty("Warning")
    private Integer warning;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ServiceProxy {
    @JsonProperty("Mode")
    private String mode;
    @JsonProperty("MeshGateway")
    private Map<String, Object> meshGateway;
    @JsonProperty("Expose")
    private Map<String, Object> expose;
  }

  @Data
  @Builder
  @NoArgsConstructor
  public static class ServiceConnect {
    // Empty for now, can be extended
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Consul health check response")
  public static class ConsulHealthResponse {
    @JsonProperty("Node")
    private NodeInfo node;
    @JsonProperty("Service")
    private ServiceInfo service;
    @JsonProperty("Checks")
    private List<HealthCheck> checks;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NodeInfo {
    @JsonProperty("ID")
    private String id;
    @JsonProperty("Node")
    private String node;
    @JsonProperty("Address")
    private String address;
    @JsonProperty("Datacenter")
    private String datacenter;
    @JsonProperty("TaggedAddresses")
    private TaggedAddresses taggedAddresses;
    @JsonProperty("Meta")
    private Map<String, String> meta;
    @JsonProperty("CreateIndex")
    private Long createIndex;
    @JsonProperty("ModifyIndex")
    private Long modifyIndex;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Service information from Consul")
  public static class ServiceInfo {
    @JsonProperty("ID")
    private String id;
    @JsonProperty("Service")
    private String service;
    @JsonProperty("Tags")
    private List<String> tags;
    @JsonProperty("Address")
    private String address;
    @JsonProperty("Meta")
    private Map<String, String> meta;
    @JsonProperty("Port")
    private Integer port;
    @JsonProperty("Weights")
    private ServiceWeights weights;
    @JsonProperty("EnableTagOverride")
    private Boolean enableTagOverride;
    @JsonProperty("Proxy")
    private ServiceProxy proxy;
    @JsonProperty("Connect")
    private ServiceConnect connect;
    @JsonProperty("PeerName")
    private String peerName;
    @JsonProperty("CreateIndex")
    private Long createIndex;
    @JsonProperty("ModifyIndex")
    private Long modifyIndex;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Health check information")
  public static class HealthCheck {
    @JsonProperty("Node")
    private String node;
    @JsonProperty("CheckID")
    private String checkId;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Status")
    private String status;
    @JsonProperty("Notes")
    private String notes;
    @JsonProperty("Output")
    private String output;
    @JsonProperty("ServiceID")
    private String serviceId;
    @JsonProperty("ServiceName")
    private String serviceName;
    @JsonProperty("ServiceTags")
    private List<String> serviceTags;
    @JsonProperty("Type")
    private String type;
    @JsonProperty("Interval")
    private String interval;
    @JsonProperty("Timeout")
    private String timeout;
    @JsonProperty("ExposedPort")
    private Integer exposedPort;
    @JsonProperty("Definition")
    private Map<String, Object> definition;
    @JsonProperty("CreateIndex")
    private Long createIndex;
    @JsonProperty("ModifyIndex")
    private Long modifyIndex;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ConsulNodeInfo {
    private String id;
    private String node;
    private String address;
    private String datacenter;
    private TaggedAddresses taggedAddresses;
    private Map<String, String> meta;
    private Long createIndex;
    private Long modifyIndex;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ConsulKVResponse {
    @JsonProperty("LockIndex")
    private Long lockIndex;
    @JsonProperty("Key")
    private String key;
    @JsonProperty("Flags")
    private Long flags;
    @JsonProperty("Value")
    private String value;
    @JsonProperty("CreateIndex")
    private Long createIndex;
    @JsonProperty("ModifyIndex")
    private Long modifyIndex;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ConsulMemberInfo {
    private String name;
    private String addr;
    private Integer port;
    private Map<String, String> tags;
    private Integer status;
    private Integer protocolMin;
    private Integer protocolMax;
    private Integer protocolCur;
    private Integer delegateMin;
    private Integer delegateMax;
    private Integer delegateCur;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Map of all registered services in Consul")
  public static class ConsulServicesMap {
    private Map<String, List<String>> services;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ConsulAgentService {
    private String id;
    private String service;
    private List<String> tags;
    private Map<String, String> meta;
    private Integer port;
    private String address;
    private ServiceWeights weights;
    private Boolean enableTagOverride;
    private String datacenter;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ConsulAgentCheck {
    private String node;
    private String checkId;
    private String name;
    private String status;
    private String notes;
    private String output;
    private String serviceId;
    private String serviceName;
    private List<String> serviceTags;
    private String type;
    private String interval;
    private String timeout;
    private Integer exposedPort;
    private Map<String, Object> definition;
    private Long createIndex;
    private Long modifyIndex;
  }
}
