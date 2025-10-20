package com.example.control.consulclient.client;

import com.example.control.consulclient.core.ConsulResponse;
import com.example.control.consulclient.core.QueryOptions;
import com.example.control.consulclient.core.WriteOptions;
import com.example.control.consulclient.model.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Main Consul client facade providing access to all sub-clients.
 * 
 * <p>This interface serves as the primary entry point for interacting with
 * the Consul API. It provides both high-level convenience methods and access
 * to specialized sub-clients for advanced use cases.</p>
 * 
 * <p>The client is organized into logical sub-clients:</p>
 * <ul>
 *   <li>{@link KVClient} - Key-Value store operations</li>
 *   <li>{@link SessionClient} - Session management</li>
 *   <li>{@link TxnClient} - Atomic transactions</li>
 *   <li>{@link HealthClient} - Health check operations</li>
 *   <li>{@link CatalogClient} - Service catalog operations</li>
 *   <li>{@link AgentClient} - Agent operations</li>
 *   <li>{@link StatusClient} - Cluster status</li>
 *   <li>{@link EventClient} - Event system</li>
 * </ul>
 * 
 * <p>All methods return {@link ConsulResponse} objects that contain both the
 * response data and Consul metadata (index, leader status, etc.).</p>
 * 
 * @see <a href="https://www.consul.io/api/index.html">Consul API Documentation</a>
 */
public interface ConsulClient {
    
    // KV Operations
    ConsulResponse<Optional<KVPair>> get(String key, QueryOptions opts);
    ConsulResponse<List<KVPair>> list(String prefix, QueryOptions opts);
    ConsulResponse<Boolean> put(String key, byte[] value, WriteOptions opts, Long cas);
    ConsulResponse<Boolean> acquire(String key, byte[] value, String sessionId, WriteOptions opts);
    ConsulResponse<Boolean> release(String key, byte[] value, String sessionId, WriteOptions opts);
    ConsulResponse<Boolean> delete(String key, WriteOptions opts, Long cas);
    
    // Session Operations
    ConsulResponse<Session> createSession(SessionCreateRequest request, WriteOptions opts);
    ConsulResponse<Void> destroySession(String sessionId, WriteOptions opts);
    ConsulResponse<List<Session>> listSessions(QueryOptions opts);
    ConsulResponse<Optional<Session>> readSession(String sessionId, QueryOptions opts);
    
    // Transaction Operations
    ConsulResponse<TxnResult> executeTransaction(List<TxnOp> ops, WriteOptions opts);
    
    // Health Operations
    ConsulResponse<List<HealthCheck>> nodeHealth(String nodeName, QueryOptions opts);
    ConsulResponse<List<HealthCheck>> serviceHealth(String serviceName, QueryOptions opts);
    ConsulResponse<List<HealthService>> serviceHealthy(String serviceName, QueryOptions opts);
    ConsulResponse<List<HealthService>> serviceAny(String serviceName, QueryOptions opts);
    ConsulResponse<List<HealthService>> serviceState(String serviceName, String state, QueryOptions opts);
    ConsulResponse<List<HealthCheck>> healthState(String state, QueryOptions opts);
    ConsulResponse<List<HealthService>> serviceConnect(String serviceName, QueryOptions opts);
    ConsulResponse<List<HealthService>> serviceIngress(String serviceName, QueryOptions opts);
    
    // Catalog Operations
    ConsulResponse<Map<String, List<String>>> catalogServices(QueryOptions opts);
    ConsulResponse<List<ServiceEntry>> catalogService(String serviceName, QueryOptions opts);
    ConsulResponse<List<Node>> catalogNodes(QueryOptions opts);
    ConsulResponse<List<ServiceEntry>> catalogNodeServices(String nodeName, QueryOptions opts);
    ConsulResponse<List<String>> catalogDatacenters();
    
    // Agent Operations
    ConsulResponse<Map<String, Object>> agentSelf(QueryOptions opts);
    ConsulResponse<Map<String, Object>> agentSelfNode(String nodeName, QueryOptions opts);
    ConsulResponse<Map<String, Service>> agentServices(QueryOptions opts);
    ConsulResponse<Map<String, HealthCheck>> agentChecks(QueryOptions opts);
    ConsulResponse<List<Map<String, Object>>> agentMembers(QueryOptions opts);
    ConsulResponse<List<Map<String, Object>>> agentMembersDatacenter(String datacenter, QueryOptions opts);
    ConsulResponse<Map<String, Object>> agentConfig(QueryOptions opts);
    ConsulResponse<Map<String, Object>> agentConfigNode(String nodeName, QueryOptions opts);
    ConsulResponse<Map<String, Object>> agentMetrics(QueryOptions opts);
    ConsulResponse<Map<String, Object>> agentMetricsNode(String nodeName, QueryOptions opts);
    ConsulResponse<Boolean> agentReload(WriteOptions opts);
    ConsulResponse<Boolean> agentReloadNode(String nodeName, WriteOptions opts);
    ConsulResponse<Boolean> agentForceLeave(String nodeName, WriteOptions opts);
    ConsulResponse<Boolean> agentForceLeaveDatacenter(String nodeName, String datacenter, WriteOptions opts);
    ConsulResponse<Boolean> agentJoin(String address, WriteOptions opts);
    ConsulResponse<Boolean> agentJoinDatacenter(String address, String datacenter, WriteOptions opts);
    ConsulResponse<Boolean> agentLeave(WriteOptions opts);
    ConsulResponse<Boolean> agentLeaveNode(String nodeName, WriteOptions opts);
    ConsulResponse<Boolean> agentMaintenance(boolean enable, String reason, WriteOptions opts);
    ConsulResponse<Boolean> agentMaintenanceNode(String nodeName, boolean enable, String reason, WriteOptions opts);
    
    // Status Operations
    ConsulResponse<String> statusLeader(QueryOptions opts);
    ConsulResponse<String> statusLeaderDatacenter(String datacenter, QueryOptions opts);
    ConsulResponse<List<String>> statusPeers(QueryOptions opts);
    ConsulResponse<List<String>> statusPeersDatacenter(String datacenter, QueryOptions opts);
    ConsulResponse<Map<String, Object>> statusCluster(QueryOptions opts);
    ConsulResponse<Map<String, Object>> statusClusterDatacenter(String datacenter, QueryOptions opts);
    ConsulResponse<Map<String, Object>> statusNode(String nodeName, QueryOptions opts);
    ConsulResponse<Map<String, Object>> statusNodeDatacenter(String nodeName, String datacenter, QueryOptions opts);
    ConsulResponse<Map<String, Object>> statusService(String serviceName, QueryOptions opts);
    ConsulResponse<Map<String, Object>> statusServiceDatacenter(String serviceName, String datacenter, QueryOptions opts);
    ConsulResponse<Map<String, Object>> statusCheck(String checkId, QueryOptions opts);
    ConsulResponse<Map<String, Object>> statusCheckDatacenter(String checkId, String datacenter, QueryOptions opts);
    
    // Event Operations
    ConsulResponse<List<Event>> eventList(QueryOptions opts);
    ConsulResponse<List<Event>> eventListName(String name, QueryOptions opts);
    ConsulResponse<List<Event>> eventListNameDatacenter(String name, String datacenter, QueryOptions opts);
    ConsulResponse<String> eventFire(String name, byte[] payload, WriteOptions opts);
    ConsulResponse<String> eventFireDatacenter(String name, byte[] payload, String datacenter, WriteOptions opts);
    ConsulResponse<String> eventFireNode(String name, byte[] payload, String nodeFilter, WriteOptions opts);
    ConsulResponse<String> eventFireService(String name, byte[] payload, String serviceFilter, WriteOptions opts);
    ConsulResponse<String> eventFireTag(String name, byte[] payload, String tagFilter, WriteOptions opts);
    ConsulResponse<String> eventFireFilters(String name, byte[] payload, String nodeFilter, String serviceFilter, String tagFilter, WriteOptions opts);
    
    // Sub-client access for advanced usage
    KVClient kv();
    SessionClient session();
    TxnClient txn();
    HealthClient health();
    CatalogClient catalog();
    AgentClient agent();
    StatusClient status();
    EventClient event();
}
