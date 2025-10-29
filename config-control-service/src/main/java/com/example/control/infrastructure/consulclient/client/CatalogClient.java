package com.example.control.infrastructure.consulclient.client;

import com.example.control.infrastructure.consulclient.core.ConsulResponse;
import com.example.control.infrastructure.consulclient.core.QueryOptions;
import com.example.control.infrastructure.consulclient.model.Node;
import com.example.control.infrastructure.consulclient.model.ServiceEntry;

import java.util.List;
import java.util.Map;

/**
 * Client for Consul catalog operations.
 *
 * <p>This interface provides access to the Consul Catalog API, which manages
 * the registry of all nodes and services in the Consul cluster. The catalog
 * provides a centralized view of the cluster's topology.</p>
 *
 * <p>All methods return {@link ConsulResponse} objects that contain both the
 * response data and Consul metadata (index, leader status, etc.).</p>
 *
 * @see <a href="https://www.consul.io/api/catalog.html">Consul Catalog API</a>
 */
public interface CatalogClient {

    /**
     * List all services.
     *
     * @param options query options
     * @return response with map of service names to tags
     */
    ConsulResponse<Map<String, List<String>>> services(QueryOptions options);

    /**
     * List all services with default options.
     *
     * @return response with map of service names to tags
     */
    default ConsulResponse<Map<String, List<String>>> services() {
        return services(null);
    }

    /**
     * Get service instances.
     *
     * @param serviceName the service name
     * @param options     query options
     * @return response with list of service entries
     */
    ConsulResponse<List<ServiceEntry>> service(String serviceName, QueryOptions options);

    /**
     * Get service instances with default options.
     *
     * @param serviceName the service name
     * @return response with list of service entries
     */
    default ConsulResponse<List<ServiceEntry>> service(String serviceName) {
        return service(serviceName, null);
    }

    /**
     * List all nodes.
     *
     * @param options query options
     * @return response with list of nodes
     */
    ConsulResponse<List<Node>> nodes(QueryOptions options);

    /**
     * List all nodes with default options.
     *
     * @return response with list of nodes
     */
    default ConsulResponse<List<Node>> nodes() {
        return nodes(null);
    }

    /**
     * Get node services.
     *
     * @param nodeName the node name
     * @param options  query options
     * @return response with list of service entries
     */
    ConsulResponse<List<ServiceEntry>> nodeServices(String nodeName, QueryOptions options);

    /**
     * Get node services with default options.
     *
     * @param nodeName the node name
     * @return response with list of service entries
     */
    default ConsulResponse<List<ServiceEntry>> nodeServices(String nodeName) {
        return nodeServices(nodeName, null);
    }

    /**
     * List datacenters.
     *
     * @return response with list of datacenter names
     */
    ConsulResponse<List<String>> datacenters();
}
