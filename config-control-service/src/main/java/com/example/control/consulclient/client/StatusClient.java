package com.example.control.consulclient.client;

import com.example.control.consulclient.core.ConsulResponse;
import com.example.control.consulclient.core.QueryOptions;

import java.util.List;
import java.util.Map;

/**
 * Client for Consul Status API operations.
 * 
 * <p>This interface provides access to the Consul Status API, which provides
 * information about the status of the Consul cluster. This includes cluster
 * leadership, peer information, and cluster health status.</p>
 * 
 * <p>All methods return {@link ConsulResponse} objects that contain both the
 * response data and Consul metadata (index, leader status, etc.).</p>
 * 
 * @see <a href="https://www.consul.io/api/status.html">Consul Status API</a>
 */
public interface StatusClient {
    
    /**
     * Get the status of the Consul cluster leader.
     * @param options Query options
     * @return The leader's address
     */
    ConsulResponse<String> leader(QueryOptions options);
    
    /**
     * Get the status of the Consul cluster leader for a specific datacenter.
     * @param datacenter The datacenter to query
     * @param options Query options
     * @return The leader's address
     */
    ConsulResponse<String> leaderDatacenter(String datacenter, QueryOptions options);
    
    /**
     * Get the list of Consul cluster peers.
     * @param options Query options
     * @return List of peer addresses
     */
    ConsulResponse<List<String>> peers(QueryOptions options);
    
    /**
     * Get the list of Consul cluster peers for a specific datacenter.
     * @param datacenter The datacenter to query
     * @param options Query options
     * @return List of peer addresses
     */
    ConsulResponse<List<String>> peersDatacenter(String datacenter, QueryOptions options);
    
    /**
     * Get the status of the Consul cluster.
     * @param options Query options
     * @return Cluster status information
     */
    ConsulResponse<Map<String, Object>> status(QueryOptions options);
    
    /**
     * Get the status of the Consul cluster for a specific datacenter.
     * @param datacenter The datacenter to query
     * @param options Query options
     * @return Cluster status information
     */
    ConsulResponse<Map<String, Object>> statusDatacenter(String datacenter, QueryOptions options);
    
    /**
     * Get the status of the Consul cluster for a specific node.
     * @param nodeName The node name to query
     * @param options Query options
     * @return Cluster status information
     */
    ConsulResponse<Map<String, Object>> statusNode(String nodeName, QueryOptions options);
    
    /**
     * Get the status of the Consul cluster for a specific node in a specific datacenter.
     * @param nodeName The node name to query
     * @param datacenter The datacenter to query
     * @param options Query options
     * @return Cluster status information
     */
    ConsulResponse<Map<String, Object>> statusNodeDatacenter(String nodeName, String datacenter, QueryOptions options);
    
    /**
     * Get the status of the Consul cluster for a specific service.
     * @param serviceName The service name to query
     * @param options Query options
     * @return Cluster status information
     */
    ConsulResponse<Map<String, Object>> statusService(String serviceName, QueryOptions options);
    
    /**
     * Get the status of the Consul cluster for a specific service in a specific datacenter.
     * @param serviceName The service name to query
     * @param datacenter The datacenter to query
     * @param options Query options
     * @return Cluster status information
     */
    ConsulResponse<Map<String, Object>> statusServiceDatacenter(String serviceName, String datacenter, QueryOptions options);
    
    /**
     * Get the status of the Consul cluster for a specific check.
     * @param checkId The check ID to query
     * @param options Query options
     * @return Cluster status information
     */
    ConsulResponse<Map<String, Object>> statusCheck(String checkId, QueryOptions options);
    
    /**
     * Get the status of the Consul cluster for a specific check in a specific datacenter.
     * @param checkId The check ID to query
     * @param datacenter The datacenter to query
     * @param options Query options
     * @return Cluster status information
     */
    ConsulResponse<Map<String, Object>> statusCheckDatacenter(String checkId, String datacenter, QueryOptions options);
}
