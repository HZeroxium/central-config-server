package com.example.control.infrastructure.consulclient.client;

import com.example.control.infrastructure.consulclient.core.ConsulResponse;
import com.example.control.infrastructure.consulclient.core.QueryOptions;
import com.example.control.infrastructure.consulclient.model.HealthCheck;
import com.example.control.infrastructure.consulclient.model.HealthService;

import java.util.List;

/**
 * Client for Consul Health API operations.
 *
 * <p>This interface provides access to the Consul Health API, which manages
 * health checks for nodes and services in the Consul cluster. Health checks
 * are used to determine the availability and status of services.</p>
 *
 * <p>All methods return {@link ConsulResponse} objects that contain both the
 * response data and Consul metadata (index, leader status, etc.).</p>
 *
 * @see <a href="https://www.consul.io/api/health.html">Consul Health API</a>
 */
public interface HealthClient {

    /**
     * List health checks for a specific node.
     *
     * @param nodeName The node name to query
     * @param options  Query options
     * @return List of health checks for the node
     */
    ConsulResponse<List<HealthCheck>> node(String nodeName, QueryOptions options);

    /**
     * List health checks for a specific service.
     *
     * @param serviceName The service name to query
     * @param options     Query options
     * @return List of health checks for the service
     */
    ConsulResponse<List<HealthCheck>> service(String serviceName, QueryOptions options);

    /**
     * List healthy instances for a specific service.
     *
     * @param serviceName The service name to query
     * @param options     Query options
     * @return List of healthy service instances
     */
    ConsulResponse<List<HealthService>> serviceHealthy(String serviceName, QueryOptions options);

    /**
     * List any instances for a specific service (healthy, warning, critical).
     *
     * @param serviceName The service name to query
     * @param options     Query options
     * @return List of all service instances
     */
    ConsulResponse<List<HealthService>> serviceAny(String serviceName, QueryOptions options);

    /**
     * List service instances with specific health state.
     *
     * @param serviceName The service name to query
     * @param state       The health state (passing, warning, critical, maintenance)
     * @param options     Query options
     * @return List of service instances with the specified health state
     */
    ConsulResponse<List<HealthService>> serviceState(String serviceName, String state, QueryOptions options);

    /**
     * List health checks with specific state.
     *
     * @param state   The health state (passing, warning, critical, maintenance)
     * @param options Query options
     * @return List of health checks with the specified state
     */
    ConsulResponse<List<HealthCheck>> state(String state, QueryOptions options);

    /**
     * List connect-capable instances for a service.
     *
     * @param serviceName The service name to query
     * @param options     Query options
     * @return List of connect-capable service instances
     */
    ConsulResponse<List<HealthService>> connect(String serviceName, QueryOptions options);

    /**
     * List ingress gateway instances for a service.
     *
     * @param serviceName The service name to query
     * @param options     Query options
     * @return List of ingress gateway instances
     */
    ConsulResponse<List<HealthService>> ingress(String serviceName, QueryOptions options);
}