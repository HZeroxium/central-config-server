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
 * DTOs for Consul API responses.
 * <p>
 * Provides comprehensive data transfer objects for Consul service registry
 * and health check operations with full OpenAPI documentation.
 * </p>
 */
@Schema(name = "ConsulDto", description = "DTOs for Consul API responses")
public class ConsulDto {

    private ConsulDto() {
        throw new UnsupportedOperationException("Utility class");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulServiceResponse", description = "Consul service details with full metadata and registration information")
    public static class ConsulServiceResponse {
        @JsonProperty("ID")
        @Schema(description = "Unique identifier for this service instance", example = "payment-service-node1-8080")
        private String id;

        @JsonProperty("Node")
        @Schema(description = "Name of the Consul node hosting this service", example = "node1")
        private String node;

        @JsonProperty("Address")
        @Schema(description = "IP address of the node", example = "192.168.1.10")
        private String address;

        @JsonProperty("Datacenter")
        @Schema(description = "Datacenter where the service is registered", example = "dc1")
        private String datacenter;

        @JsonProperty("TaggedAddresses")
        @Schema(description = "Tagged network addresses for different networks")
        private TaggedAddresses taggedAddresses;

        @JsonProperty("NodeMeta")
        @Schema(description = "Metadata associated with the Consul node")
        private NodeMeta nodeMeta;

        @JsonProperty("ServiceKind")
        @Schema(description = "Kind of service (e.g., typical, connect-proxy, mesh-gateway)", example = "typical")
        private String serviceKind;

        @JsonProperty("ServiceID")
        @Schema(description = "Unique service instance identifier", example = "payment-service-1")
        private String serviceId;

        @JsonProperty("ServiceName")
        @Schema(description = "Name of the service", example = "payment-service")
        private String serviceName;

        @JsonProperty("ServiceTags")
        @Schema(description = "Tags associated with the service for filtering and routing", example = "[\"v1\", \"production\", \"payment\"]")
        private List<String> serviceTags;

        @JsonProperty("ServiceAddress")
        @Schema(description = "IP address where the service is accessible", example = "192.168.1.10")
        private String serviceAddress;

        @JsonProperty("ServiceWeights")
        @Schema(description = "Service weights for load balancing")
        private ServiceWeights serviceWeights;

        @JsonProperty("ServiceMeta")
        @Schema(description = "Custom metadata key-value pairs for the service", example = "{\"version\": \"1.2.0\", \"region\": \"us-east-1\"}")
        private Map<String, String> serviceMeta;

        @JsonProperty("ServicePort")
        @Schema(description = "Port number where the service is listening", example = "8080")
        private Integer servicePort;

        @JsonProperty("ServiceSocketPath")
        @Schema(description = "Unix socket path if using Unix sockets instead of TCP")
        private String serviceSocketPath;

        @JsonProperty("ServiceEnableTagOverride")
        @Schema(description = "Whether tags can be modified via the API", example = "false")
        private Boolean serviceEnableTagOverride;

        @JsonProperty("ServiceProxy")
        @Schema(description = "Proxy configuration for Connect service mesh")
        private ServiceProxy serviceProxy;

        @JsonProperty("ServiceConnect")
        @Schema(description = "Connect service mesh configuration")
        private ServiceConnect serviceConnect;

        @JsonProperty("ServiceLocality")
        @Schema(description = "Locality information for the service")
        private Object serviceLocality;

        @JsonProperty("CreateIndex")
        @Schema(description = "Raft index when the service was created", example = "100")
        private Long createIndex;

        @JsonProperty("ModifyIndex")
        @Schema(description = "Raft index when the service was last modified", example = "150")
        private Long modifyIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulTaggedAddresses", description = "Tagged network addresses for different network types (LAN/WAN)")
    public static class TaggedAddresses {
        @JsonProperty("lan")
        @Schema(description = "LAN address", example = "192.168.1.10")
        private String lan;

        @JsonProperty("lan_ipv4")
        @Schema(description = "LAN IPv4 address", example = "192.168.1.10")
        private String lanIpv4;

        @JsonProperty("wan")
        @Schema(description = "WAN address", example = "203.0.113.10")
        private String wan;

        @JsonProperty("wan_ipv4")
        @Schema(description = "WAN IPv4 address", example = "203.0.113.10")
        private String wanIpv4;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulNodeMeta", description = "Metadata information about the Consul node")
    public static class NodeMeta {
        @JsonProperty("consul-network-segment")
        @Schema(description = "Network segment of the Consul node", example = "")
        private String consulNetworkSegment;

        @JsonProperty("consul-version")
        @Schema(description = "Version of Consul running on the node", example = "1.15.0")
        private String consulVersion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulServiceWeights", description = "Service weights for health-based load balancing")
    public static class ServiceWeights {
        @JsonProperty("Passing")
        @Schema(description = "Weight when all health checks are passing", example = "1")
        private Integer passing;

        @JsonProperty("Warning")
        @Schema(description = "Weight when health checks are in warning state", example = "1")
        private Integer warning;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulServiceProxy", description = "Proxy configuration for Connect service mesh")
    public static class ServiceProxy {
        @JsonProperty("Mode")
        @Schema(description = "Proxy mode (e.g., transparent, direct)", example = "transparent")
        private String mode;

        @JsonProperty("MeshGateway")
        @Schema(description = "Mesh gateway configuration")
        private Map<String, Object> meshGateway;

        @JsonProperty("Expose")
        @Schema(description = "Exposed paths configuration for the proxy")
        private Map<String, Object> expose;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @Schema(name = "ConsulServiceConnect", description = "Connect service mesh configuration (empty placeholder for extension)")
    public static class ServiceConnect {
        // Empty for now, can be extended
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulHealthResponse", description = "Consul health check response with node, service, and check details")
    public static class ConsulHealthResponse {
        @JsonProperty("Node")
        @Schema(description = "Node information where the service is running")
        private NodeInfo node;

        @JsonProperty("Service")
        @Schema(description = "Service information")
        private ServiceInfo service;

        @JsonProperty("Checks")
        @Schema(description = "List of health checks for this service instance")
        private List<HealthCheck> checks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulNodeInfo", description = "Node information in health check response")
    public static class NodeInfo {
        @JsonProperty("ID")
        @Schema(description = "Unique node identifier", example = "8f246b77-f3e1-ff88-5b48-8ec93abf3e05")
        private String id;

        @JsonProperty("Node")
        @Schema(description = "Node name", example = "consul-server-1")
        private String node;

        @JsonProperty("Address")
        @Schema(description = "Node IP address", example = "192.168.1.10")
        private String address;

        @JsonProperty("Datacenter")
        @Schema(description = "Datacenter name", example = "dc1")
        private String datacenter;

        @JsonProperty("TaggedAddresses")
        @Schema(description = "Tagged addresses for the node")
        private TaggedAddresses taggedAddresses;

        @JsonProperty("Meta")
        @Schema(description = "Node metadata", example = "{\"consul-version\": \"1.15.0\"}")
        private Map<String, String> meta;

        @JsonProperty("CreateIndex")
        @Schema(description = "Raft index when the node was created", example = "10")
        private Long createIndex;

        @JsonProperty("ModifyIndex")
        @Schema(description = "Raft index when the node was last modified", example = "20")
        private Long modifyIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulServiceInfo", description = "Service information from Consul health check")
    public static class ServiceInfo {
        @JsonProperty("ID")
        @Schema(description = "Service instance ID", example = "payment-service-1")
        private String id;

        @JsonProperty("Service")
        @Schema(description = "Service name", example = "payment-service")
        private String service;

        @JsonProperty("Tags")
        @Schema(description = "Service tags", example = "[\"v1\", \"production\"]")
        private List<String> tags;

        @JsonProperty("Address")
        @Schema(description = "Service address", example = "192.168.1.10")
        private String address;

        @JsonProperty("Meta")
        @Schema(description = "Service metadata", example = "{\"version\": \"1.2.0\"}")
        private Map<String, String> meta;

        @JsonProperty("Port")
        @Schema(description = "Service port", example = "8080")
        private Integer port;

        @JsonProperty("Weights")
        @Schema(description = "Service weights for load balancing")
        private ServiceWeights weights;

        @JsonProperty("EnableTagOverride")
        @Schema(description = "Whether tags can be overridden", example = "false")
        private Boolean enableTagOverride;

        @JsonProperty("Proxy")
        @Schema(description = "Proxy configuration")
        private ServiceProxy proxy;

        @JsonProperty("Connect")
        @Schema(description = "Connect configuration")
        private ServiceConnect connect;

        @JsonProperty("PeerName")
        @Schema(description = "Peer name for federated setups", example = "")
        private String peerName;

        @JsonProperty("CreateIndex")
        @Schema(description = "Raft index when service was created", example = "50")
        private Long createIndex;

        @JsonProperty("ModifyIndex")
        @Schema(description = "Raft index when service was last modified", example = "75")
        private Long modifyIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulHealthCheck", description = "Health check information with status and timing details")
    public static class HealthCheck {
        @JsonProperty("Node")
        @Schema(description = "Node name where check runs", example = "consul-server-1")
        private String node;

        @JsonProperty("CheckID")
        @Schema(description = "Unique check identifier", example = "service:payment-service-1")
        private String checkId;

        @JsonProperty("Name")
        @Schema(description = "Human-readable check name", example = "Service 'payment-service' check")
        private String name;

        @JsonProperty("Status")
        @Schema(description = "Check status", example = "passing", allowableValues = {"passing", "warning", "critical"})
        private String status;

        @JsonProperty("Notes")
        @Schema(description = "Optional notes about the check", example = "")
        private String notes;

        @JsonProperty("Output")
        @Schema(description = "Check output message", example = "HTTP GET http://192.168.1.10:8080/health: 200 OK")
        private String output;

        @JsonProperty("ServiceID")
        @Schema(description = "Associated service ID", example = "payment-service-1")
        private String serviceId;

        @JsonProperty("ServiceName")
        @Schema(description = "Associated service name", example = "payment-service")
        private String serviceName;

        @JsonProperty("ServiceTags")
        @Schema(description = "Tags of the associated service", example = "[\"v1\", \"production\"]")
        private List<String> serviceTags;

        @JsonProperty("Type")
        @Schema(description = "Check type", example = "http", allowableValues = {"http", "tcp", "ttl", "docker", "grpc", "script"})
        private String type;

        @JsonProperty("Interval")
        @Schema(description = "Check interval", example = "10s")
        private String interval;

        @JsonProperty("Timeout")
        @Schema(description = "Check timeout", example = "5s")
        private String timeout;

        @JsonProperty("ExposedPort")
        @Schema(description = "Exposed port for check", example = "8080")
        private Integer exposedPort;

        @JsonProperty("Definition")
        @Schema(description = "Check definition details")
        private Map<String, Object> definition;

        @JsonProperty("CreateIndex")
        @Schema(description = "Raft index when check was created", example = "60")
        private Long createIndex;

        @JsonProperty("ModifyIndex")
        @Schema(description = "Raft index when check was last modified", example = "80")
        private Long modifyIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulNodeInfo", description = "Consul node information with network details")
    public static class ConsulNodeInfo {
        @Schema(description = "Unique node identifier", example = "8f246b77-f3e1-ff88-5b48-8ec93abf3e05")
        private String id;

        @Schema(description = "Node name", example = "consul-server-1")
        private String node;

        @Schema(description = "Node IP address", example = "192.168.1.10")
        private String address;

        @Schema(description = "Datacenter name", example = "dc1")
        private String datacenter;

        @Schema(description = "Tagged network addresses")
        private TaggedAddresses taggedAddresses;

        @Schema(description = "Node metadata", example = "{\"consul-version\": \"1.15.0\"}")
        private Map<String, String> meta;

        @Schema(description = "Raft index when node was created", example = "10")
        private Long createIndex;

        @Schema(description = "Raft index when node was last modified", example = "20")
        private Long modifyIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulKVResponse", description = "Consul key-value store response")
    public static class ConsulKVResponse {
        @JsonProperty("LockIndex")
        @Schema(description = "Lock index for distributed locking", example = "0")
        private Long lockIndex;

        @JsonProperty("Key")
        @Schema(description = "Key name", example = "config/payment-service/database")
        private String key;

        @JsonProperty("Flags")
        @Schema(description = "User-defined flags", example = "0")
        private Long flags;

        @JsonProperty("Value")
        @Schema(description = "Base64-encoded value", example = "bW9uZ29kYjovL2xvY2FsaG9zdDoyNzAxNw==")
        private String value;

        @JsonProperty("CreateIndex")
        @Schema(description = "Raft index when key was created", example = "100")
        private Long createIndex;

        @JsonProperty("ModifyIndex")
        @Schema(description = "Raft index when key was last modified", example = "105")
        private Long modifyIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulMemberInfo", description = "Consul cluster member information")
    public static class ConsulMemberInfo {
        @Schema(description = "Member name", example = "consul-server-1")
        private String name;

        @Schema(description = "Member address", example = "192.168.1.10")
        private String addr;

        @Schema(description = "Member port", example = "8301")
        private Integer port;

        @Schema(description = "Member tags", example = "{\"role\": \"consul\", \"dc\": \"dc1\"}")
        private Map<String, String> tags;

        @Schema(description = "Member status (1=alive, 2=leaving, 3=left, 4=failed)", example = "1")
        private Integer status;

        @Schema(description = "Minimum protocol version", example = "1")
        private Integer protocolMin;

        @Schema(description = "Maximum protocol version", example = "5")
        private Integer protocolMax;

        @Schema(description = "Current protocol version", example = "2")
        private Integer protocolCur;

        @Schema(description = "Minimum delegate version", example = "2")
        private Integer delegateMin;

        @Schema(description = "Maximum delegate version", example = "5")
        private Integer delegateMax;

        @Schema(description = "Current delegate version", example = "4")
        private Integer delegateCur;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulServicesMap", description = "Map of all registered services in Consul with their tags")
    public static class ConsulServicesMap {
        @Schema(description = "Map of service names to their tags", example = "{\"payment-service\": [\"v1\", \"production\"], \"order-service\": [\"v2\", \"staging\"]}")
        private Map<String, List<String>> services;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulAgentService", description = "Consul agent service information")
    public static class ConsulAgentService {
        @Schema(description = "Service instance ID", example = "payment-service-1")
        private String id;

        @Schema(description = "Service name", example = "payment-service")
        private String service;

        @Schema(description = "Service tags", example = "[\"v1\", \"production\"]")
        private List<String> tags;

        @Schema(description = "Service metadata", example = "{\"version\": \"1.2.0\", \"region\": \"us-east-1\"}")
        private Map<String, String> meta;

        @Schema(description = "Service port", example = "8080")
        private Integer port;

        @Schema(description = "Service address", example = "192.168.1.10")
        private String address;

        @Schema(description = "Service weights for load balancing")
        private ServiceWeights weights;

        @Schema(description = "Whether tags can be overridden", example = "false")
        private Boolean enableTagOverride;

        @Schema(description = "Datacenter where service is registered", example = "dc1")
        private String datacenter;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConsulAgentCheck", description = "Consul agent health check information")
    public static class ConsulAgentCheck {
        @Schema(description = "Node name", example = "consul-server-1")
        private String node;

        @Schema(description = "Check identifier", example = "service:payment-service-1")
        private String checkId;

        @Schema(description = "Check name", example = "Service 'payment-service' check")
        private String name;

        @Schema(description = "Check status", example = "passing", allowableValues = {"passing", "warning", "critical"})
        private String status;

        @Schema(description = "Optional notes", example = "")
        private String notes;

        @Schema(description = "Check output", example = "HTTP GET http://192.168.1.10:8080/health: 200 OK")
        private String output;

        @Schema(description = "Associated service ID", example = "payment-service-1")
        private String serviceId;

        @Schema(description = "Associated service name", example = "payment-service")
        private String serviceName;

        @Schema(description = "Service tags", example = "[\"v1\", \"production\"]")
        private List<String> serviceTags;

        @Schema(description = "Check type", example = "http", allowableValues = {"http", "tcp", "ttl", "docker", "grpc", "script"})
        private String type;

        @Schema(description = "Check interval", example = "10s")
        private String interval;

        @Schema(description = "Check timeout", example = "5s")
        private String timeout;

        @Schema(description = "Exposed port", example = "8080")
        private Integer exposedPort;

        @Schema(description = "Check definition details")
        private Map<String, Object> definition;

        @Schema(description = "Raft index when check was created", example = "60")
        private Long createIndex;

        @Schema(description = "Raft index when check was last modified", example = "80")
        private Long modifyIndex;
    }
}
