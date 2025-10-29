package com.example.control.infrastructure.consulclient.client;

import com.example.control.infrastructure.consulclient.core.ConsulResponse;
import com.example.control.infrastructure.consulclient.core.QueryOptions;
import com.example.control.infrastructure.consulclient.core.WriteOptions;
import com.example.control.infrastructure.consulclient.model.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the main Consul client facade.
 */
public class ConsulClientImpl implements ConsulClient {

    private final KVClient kvClient;
    private final SessionClient sessionClient;
    private final TxnClient txnClient;
    private final HealthClient healthClient;
    private final CatalogClient catalogClient;
    private final AgentClient agentClient;
    private final StatusClient statusClient;
    private final EventClient eventClient;

    public ConsulClientImpl(KVClient kvClient, SessionClient sessionClient, TxnClient txnClient,
                            HealthClient healthClient, CatalogClient catalogClient, AgentClient agentClient,
                            StatusClient statusClient, EventClient eventClient) {
        this.kvClient = kvClient;
        this.sessionClient = sessionClient;
        this.txnClient = txnClient;
        this.healthClient = healthClient;
        this.catalogClient = catalogClient;
        this.agentClient = agentClient;
        this.statusClient = statusClient;
        this.eventClient = eventClient;
    }

    // KV Operations
    @Override
    public ConsulResponse<Optional<KVPair>> get(String key, QueryOptions opts) {
        return kvClient.get(key, opts);
    }

    @Override
    public ConsulResponse<List<KVPair>> list(String prefix, QueryOptions opts) {
        return kvClient.list(prefix, opts);
    }

    @Override
    public ConsulResponse<Boolean> put(String key, byte[] value, WriteOptions opts, Long cas) {
        return kvClient.put(key, value, opts, cas);
    }

    @Override
    public ConsulResponse<Boolean> acquire(String key, byte[] value, String sessionId, WriteOptions opts) {
        return kvClient.acquire(key, value, sessionId, opts);
    }

    @Override
    public ConsulResponse<Boolean> release(String key, byte[] value, String sessionId, WriteOptions opts) {
        return kvClient.release(key, value, sessionId, opts);
    }

    @Override
    public ConsulResponse<Boolean> delete(String key, WriteOptions opts, Long cas) {
        return kvClient.delete(key, opts, cas);
    }

    // Session Operations
    @Override
    public ConsulResponse<Session> createSession(SessionCreateRequest request, WriteOptions opts) {
        return sessionClient.create(request, opts);
    }

    @Override
    public ConsulResponse<Void> destroySession(String sessionId, WriteOptions opts) {
        return sessionClient.destroy(sessionId, opts);
    }

    @Override
    public ConsulResponse<List<Session>> listSessions(QueryOptions opts) {
        return sessionClient.list(opts);
    }

    @Override
    public ConsulResponse<Optional<Session>> readSession(String sessionId, QueryOptions opts) {
        return sessionClient.read(sessionId, opts);
    }

    // Transaction Operations
    @Override
    public ConsulResponse<TxnResult> executeTransaction(List<TxnOp> ops, WriteOptions opts) {
        return txnClient.execute(ops, opts);
    }

    // Health Operations
    @Override
    public ConsulResponse<List<HealthCheck>> nodeHealth(String nodeName, QueryOptions opts) {
        return healthClient.node(nodeName, opts);
    }

    @Override
    public ConsulResponse<List<HealthCheck>> serviceHealth(String serviceName, QueryOptions opts) {
        return healthClient.service(serviceName, opts);
    }

    @Override
    public ConsulResponse<List<HealthService>> serviceHealthy(String serviceName, QueryOptions opts) {
        return healthClient.serviceHealthy(serviceName, opts);
    }

    @Override
    public ConsulResponse<List<HealthService>> serviceAny(String serviceName, QueryOptions opts) {
        return healthClient.serviceAny(serviceName, opts);
    }

    @Override
    public ConsulResponse<List<HealthService>> serviceState(String serviceName, String state, QueryOptions opts) {
        return healthClient.serviceState(serviceName, state, opts);
    }

    @Override
    public ConsulResponse<List<HealthCheck>> healthState(String state, QueryOptions opts) {
        return healthClient.state(state, opts);
    }

    @Override
    public ConsulResponse<List<HealthService>> serviceConnect(String serviceName, QueryOptions opts) {
        return healthClient.connect(serviceName, opts);
    }

    @Override
    public ConsulResponse<List<HealthService>> serviceIngress(String serviceName, QueryOptions opts) {
        return healthClient.ingress(serviceName, opts);
    }

    // Catalog Operations
    @Override
    public ConsulResponse<Map<String, List<String>>> catalogServices(QueryOptions opts) {
        return catalogClient.services(opts);
    }

    @Override
    public ConsulResponse<List<ServiceEntry>> catalogService(String serviceName, QueryOptions opts) {
        return catalogClient.service(serviceName, opts);
    }

    @Override
    public ConsulResponse<List<Node>> catalogNodes(QueryOptions opts) {
        return catalogClient.nodes(opts);
    }

    @Override
    public ConsulResponse<List<ServiceEntry>> catalogNodeServices(String nodeName, QueryOptions opts) {
        return catalogClient.nodeServices(nodeName, opts);
    }

    @Override
    public ConsulResponse<List<String>> catalogDatacenters() {
        return catalogClient.datacenters();
    }

    // Agent Operations
    @Override
    public ConsulResponse<Map<String, Object>> agentSelf(QueryOptions opts) {
        return agentClient.self(opts);
    }

    @Override
    public ConsulResponse<Map<String, Object>> agentSelfNode(String nodeName, QueryOptions opts) {
        return agentClient.selfNode(nodeName, opts);
    }

    @Override
    public ConsulResponse<Map<String, Service>> agentServices(QueryOptions opts) {
        return agentClient.services(opts);
    }

    @Override
    public ConsulResponse<Map<String, HealthCheck>> agentChecks(QueryOptions opts) {
        return agentClient.checks(opts);
    }

    @Override
    public ConsulResponse<List<Map<String, Object>>> agentMembers(QueryOptions opts) {
        return agentClient.members(opts);
    }

    @Override
    public ConsulResponse<List<Map<String, Object>>> agentMembersDatacenter(String datacenter, QueryOptions opts) {
        return agentClient.membersDatacenter(datacenter, opts);
    }

    @Override
    public ConsulResponse<Map<String, Object>> agentConfig(QueryOptions opts) {
        return agentClient.config(opts);
    }

    @Override
    public ConsulResponse<Map<String, Object>> agentConfigNode(String nodeName, QueryOptions opts) {
        return agentClient.configNode(nodeName, opts);
    }

    @Override
    public ConsulResponse<Map<String, Object>> agentMetrics(QueryOptions opts) {
        return agentClient.metrics(opts);
    }

    @Override
    public ConsulResponse<Map<String, Object>> agentMetricsNode(String nodeName, QueryOptions opts) {
        return agentClient.metricsNode(nodeName, opts);
    }

    @Override
    public ConsulResponse<Boolean> agentReload(WriteOptions opts) {
        return agentClient.reload(opts);
    }

    @Override
    public ConsulResponse<Boolean> agentReloadNode(String nodeName, WriteOptions opts) {
        return agentClient.reloadNode(nodeName, opts);
    }

    @Override
    public ConsulResponse<Boolean> agentForceLeave(String nodeName, WriteOptions opts) {
        return agentClient.forceLeave(nodeName, opts);
    }

    @Override
    public ConsulResponse<Boolean> agentForceLeaveDatacenter(String nodeName, String datacenter, WriteOptions opts) {
        return agentClient.forceLeaveDatacenter(nodeName, datacenter, opts);
    }

    @Override
    public ConsulResponse<Boolean> agentJoin(String address, WriteOptions opts) {
        return agentClient.join(address, opts);
    }

    @Override
    public ConsulResponse<Boolean> agentJoinDatacenter(String address, String datacenter, WriteOptions opts) {
        return agentClient.joinDatacenter(address, datacenter, opts);
    }

    @Override
    public ConsulResponse<Boolean> agentLeave(WriteOptions opts) {
        return agentClient.leave(opts);
    }

    @Override
    public ConsulResponse<Boolean> agentLeaveNode(String nodeName, WriteOptions opts) {
        return agentClient.leaveNode(nodeName, opts);
    }

    @Override
    public ConsulResponse<Boolean> agentMaintenance(boolean enable, String reason, WriteOptions opts) {
        return agentClient.maintenance(enable, reason, opts);
    }

    @Override
    public ConsulResponse<Boolean> agentMaintenanceNode(String nodeName, boolean enable, String reason, WriteOptions opts) {
        return agentClient.maintenanceNode(nodeName, enable, reason, opts);
    }

    // Status Operations
    @Override
    public ConsulResponse<String> statusLeader(QueryOptions opts) {
        return statusClient.leader(opts);
    }

    @Override
    public ConsulResponse<String> statusLeaderDatacenter(String datacenter, QueryOptions opts) {
        return statusClient.leaderDatacenter(datacenter, opts);
    }

    @Override
    public ConsulResponse<List<String>> statusPeers(QueryOptions opts) {
        return statusClient.peers(opts);
    }

    @Override
    public ConsulResponse<List<String>> statusPeersDatacenter(String datacenter, QueryOptions opts) {
        return statusClient.peersDatacenter(datacenter, opts);
    }

    @Override
    public ConsulResponse<Map<String, Object>> statusCluster(QueryOptions opts) {
        return statusClient.status(opts);
    }

    @Override
    public ConsulResponse<Map<String, Object>> statusClusterDatacenter(String datacenter, QueryOptions opts) {
        return statusClient.statusDatacenter(datacenter, opts);
    }

    @Override
    public ConsulResponse<Map<String, Object>> statusNode(String nodeName, QueryOptions opts) {
        return statusClient.statusNode(nodeName, opts);
    }

    @Override
    public ConsulResponse<Map<String, Object>> statusNodeDatacenter(String nodeName, String datacenter, QueryOptions opts) {
        return statusClient.statusNodeDatacenter(nodeName, datacenter, opts);
    }

    @Override
    public ConsulResponse<Map<String, Object>> statusService(String serviceName, QueryOptions opts) {
        return statusClient.statusService(serviceName, opts);
    }

    @Override
    public ConsulResponse<Map<String, Object>> statusServiceDatacenter(String serviceName, String datacenter, QueryOptions opts) {
        return statusClient.statusServiceDatacenter(serviceName, datacenter, opts);
    }

    @Override
    public ConsulResponse<Map<String, Object>> statusCheck(String checkId, QueryOptions opts) {
        return statusClient.statusCheck(checkId, opts);
    }

    @Override
    public ConsulResponse<Map<String, Object>> statusCheckDatacenter(String checkId, String datacenter, QueryOptions opts) {
        return statusClient.statusCheckDatacenter(checkId, datacenter, opts);
    }

    // Event Operations
    @Override
    public ConsulResponse<List<Event>> eventList(QueryOptions opts) {
        return eventClient.list(opts);
    }

    @Override
    public ConsulResponse<List<Event>> eventListName(String name, QueryOptions opts) {
        return eventClient.listName(name, opts);
    }

    @Override
    public ConsulResponse<List<Event>> eventListNameDatacenter(String name, String datacenter, QueryOptions opts) {
        return eventClient.listNameDatacenter(name, datacenter, opts);
    }

    @Override
    public ConsulResponse<String> eventFire(String name, byte[] payload, WriteOptions opts) {
        return eventClient.fire(name, payload, opts);
    }

    @Override
    public ConsulResponse<String> eventFireDatacenter(String name, byte[] payload, String datacenter, WriteOptions opts) {
        return eventClient.fireDatacenter(name, payload, datacenter, opts);
    }

    @Override
    public ConsulResponse<String> eventFireNode(String name, byte[] payload, String nodeFilter, WriteOptions opts) {
        return eventClient.fireNode(name, payload, nodeFilter, opts);
    }

    @Override
    public ConsulResponse<String> eventFireService(String name, byte[] payload, String serviceFilter, WriteOptions opts) {
        return eventClient.fireService(name, payload, serviceFilter, opts);
    }

    @Override
    public ConsulResponse<String> eventFireTag(String name, byte[] payload, String tagFilter, WriteOptions opts) {
        return eventClient.fireTag(name, payload, tagFilter, opts);
    }

    @Override
    public ConsulResponse<String> eventFireFilters(String name, byte[] payload, String nodeFilter, String serviceFilter, String tagFilter, WriteOptions opts) {
        return eventClient.fireFilters(name, payload, nodeFilter, serviceFilter, tagFilter, opts);
    }

    // Sub-client access
    @Override
    public KVClient kv() {
        return kvClient;
    }

    @Override
    public SessionClient session() {
        return sessionClient;
    }

    @Override
    public TxnClient txn() {
        return txnClient;
    }

    @Override
    public HealthClient health() {
        return healthClient;
    }

    @Override
    public CatalogClient catalog() {
        return catalogClient;
    }

    @Override
    public AgentClient agent() {
        return agentClient;
    }

    @Override
    public StatusClient status() {
        return statusClient;
    }

    @Override
    public EventClient event() {
        return eventClient;
    }
}
