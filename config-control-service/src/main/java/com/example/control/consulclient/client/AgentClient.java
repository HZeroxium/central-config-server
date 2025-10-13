package com.example.control.consulclient.client;

import com.example.control.consulclient.core.ConsulResponse;
import com.example.control.consulclient.core.QueryOptions;
import com.example.control.consulclient.core.WriteOptions;
import com.example.control.consulclient.model.Node;
import com.example.control.consulclient.model.Service;
import com.example.control.consulclient.model.HealthCheck;

import java.util.List;
import java.util.Map;

/**
 * Client for Consul Agent API operations.
 * 
 * <p>This interface provides access to the Consul Agent API, which manages
 * the local Consul agent. The agent is responsible for registering services,
 * performing health checks, and coordinating with other agents in the cluster.</p>
 * 
 * <p>All methods return {@link ConsulResponse} objects that contain both the
 * response data and Consul metadata (index, leader status, etc.).</p>
 * 
 * @see <a href="https://www.consul.io/api/agent.html">Consul Agent API</a>
 */
public interface AgentClient {
    
    /**
     * Get agent configuration and member information.
     * @param options Query options
     * @return Agent configuration and member information
     */
    ConsulResponse<Map<String, Object>> self(QueryOptions options);
    
    /**
     * Get agent configuration and member information for a specific node.
     * @param nodeName The node name to query
     * @param options Query options
     * @return Agent configuration and member information
     */
    ConsulResponse<Map<String, Object>> selfNode(String nodeName, QueryOptions options);
    
    /**
     * List all services registered with the agent.
     * @param options Query options
     * @return Map of service ID to service details
     */
    ConsulResponse<Map<String, Service>> services(QueryOptions options);
    
    /**
     * List all health checks registered with the agent.
     * @param options Query options
     * @return Map of check ID to health check details
     */
    ConsulResponse<Map<String, HealthCheck>> checks(QueryOptions options);
    
    /**
     * Get members of the Consul cluster.
     * @param options Query options
     * @return List of cluster members
     */
    ConsulResponse<List<Map<String, Object>>> members(QueryOptions options);
    
    /**
     * Get members of the Consul cluster for a specific datacenter.
     * @param datacenter The datacenter to query
     * @param options Query options
     * @return List of cluster members
     */
    ConsulResponse<List<Map<String, Object>>> membersDatacenter(String datacenter, QueryOptions options);
    
    /**
     * Get the local agent's configuration.
     * @param options Query options
     * @return Agent configuration
     */
    ConsulResponse<Map<String, Object>> config(QueryOptions options);
    
    /**
     * Get the local agent's configuration for a specific node.
     * @param nodeName The node name to query
     * @param options Query options
     * @return Agent configuration
     */
    ConsulResponse<Map<String, Object>> configNode(String nodeName, QueryOptions options);
    
    /**
     * Get the local agent's metrics.
     * @param options Query options
     * @return Agent metrics
     */
    ConsulResponse<Map<String, Object>> metrics(QueryOptions options);
    
    /**
     * Get the local agent's metrics for a specific node.
     * @param nodeName The node name to query
     * @param options Query options
     * @return Agent metrics
     */
    ConsulResponse<Map<String, Object>> metricsNode(String nodeName, QueryOptions options);
    
    /**
     * Reload the agent configuration.
     * @param options Write options
     * @return Success status
     */
    ConsulResponse<Boolean> reload(WriteOptions options);
    
    /**
     * Reload the agent configuration for a specific node.
     * @param nodeName The node name to reload
     * @param options Write options
     * @return Success status
     */
    ConsulResponse<Boolean> reloadNode(String nodeName, WriteOptions options);
    
    /**
     * Force leave a node from the cluster.
     * @param nodeName The node name to force leave
     * @param options Write options
     * @return Success status
     */
    ConsulResponse<Boolean> forceLeave(String nodeName, WriteOptions options);
    
    /**
     * Force leave a node from the cluster in a specific datacenter.
     * @param nodeName The node name to force leave
     * @param datacenter The datacenter
     * @param options Write options
     * @return Success status
     */
    ConsulResponse<Boolean> forceLeaveDatacenter(String nodeName, String datacenter, WriteOptions options);
    
    /**
     * Join a node to the cluster.
     * @param address The address to join
     * @param options Write options
     * @return Success status
     */
    ConsulResponse<Boolean> join(String address, WriteOptions options);
    
    /**
     * Join a node to the cluster in a specific datacenter.
     * @param address The address to join
     * @param datacenter The datacenter
     * @param options Write options
     * @return Success status
     */
    ConsulResponse<Boolean> joinDatacenter(String address, String datacenter, WriteOptions options);
    
    /**
     * Gracefully leave the cluster.
     * @param options Write options
     * @return Success status
     */
    ConsulResponse<Boolean> leave(WriteOptions options);
    
    /**
     * Gracefully leave the cluster for a specific node.
     * @param nodeName The node name to leave
     * @param options Write options
     * @return Success status
     */
    ConsulResponse<Boolean> leaveNode(String nodeName, WriteOptions options);
    
    /**
     * Enable maintenance mode for a node.
     * @param enable Whether to enable maintenance mode
     * @param reason The reason for maintenance mode
     * @param options Write options
     * @return Success status
     */
    ConsulResponse<Boolean> maintenance(boolean enable, String reason, WriteOptions options);
    
    /**
     * Enable maintenance mode for a specific node.
     * @param nodeName The node name
     * @param enable Whether to enable maintenance mode
     * @param reason The reason for maintenance mode
     * @param options Write options
     * @return Success status
     */
    ConsulResponse<Boolean> maintenanceNode(String nodeName, boolean enable, String reason, WriteOptions options);
}
