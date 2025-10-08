package com.vng.zing.zcm.client;

import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategy;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * The {@code ClientApi} interface defines an abstraction layer for accessing
 * environment configurations, service discovery, and load-balancing behavior.
 * <p>
 * Implementations provide integration with Spring's {@link org.springframework.core.env.Environment},
 * {@link org.springframework.cloud.client.discovery.DiscoveryClient}, and a {@link RestClient}
 * that supports load-balanced HTTP calls to microservices.
 *
 * <p>This API is designed to support:
 * <ul>
 *   <li>Environment key/value access</li>
 *   <li>Configuration hash and snapshot diagnostics</li>
 *   <li>Service discovery and load balancing</li>
 *   <li>Health ping mechanisms</li>
 * </ul>
 */
public interface ClientApi {

  /**
   * Retrieves the value of a given property key from the active Spring {@link org.springframework.core.env.Environment}.
   *
   * @param key the property key
   * @return the resolved value, or {@code null} if not found
   */
  String get(String key);

  /**
   * Returns all key/value pairs from environment property sources that contain the given prefix.
   * Primarily used for debugging or diagnostics.
   *
   * @param prefix the property key prefix to filter
   * @return a map of matching key/value pairs across all property sources
   */
  Map<String, Object> getAll(String prefix);

  /**
   * Returns the hash of the current configuration.
   * Typically used for configuration drift detection between environments or deployments.
   *
   * @return a hexadecimal hash string representing the current config state
   */
  String configHash();

  /**
   * Builds and returns a canonical snapshot of the current configuration as a map,
   * useful for diagnostic or introspection purposes.
   *
   * @return a map representing the configuration snapshot
   */
  Map<String, Object> configSnapshotMap();

  /**
   * Retrieves the list of currently healthy {@link ServiceInstance} objects
   * for the given service name from the discovery provider.
   *
   * @param serviceName the target service name
   * @return a list of discovered instances; may be empty if none are found
   */
  List<ServiceInstance> instances(String serviceName);

  /**
   * Selects a single {@link ServiceInstance} using the default load-balancer strategy.
   *
   * @param serviceName the target service name
   * @return the chosen instance or {@code null} if no healthy instances exist
   */
  ServiceInstance choose(String serviceName);

  /**
   * Selects a single {@link ServiceInstance} using a specified {@link LoadBalancerStrategy.Policy}.
   *
   * @param serviceName the target service name
   * @param policy the load-balancer policy to use (e.g., ROUND_ROBIN, RANDOM)
   * @return the chosen instance or {@code null} if no healthy instances exist
   */
  ServiceInstance choose(String serviceName, LoadBalancerStrategy.Policy policy);

  /**
   * Returns the name of the currently active load-balancer strategy.
   *
   * @return the strategy name (e.g., "RoundRobin", "Random")
   */
  String loadBalancerStrategy();

  /**
   * Returns a pre-configured {@link RestClient} capable of load-balanced HTTP calls.
   * The client typically resolves logical service URLs (e.g., {@code http://user-service/api/users}).
   *
   * @return the load-balanced RestClient instance
   */
  RestClient http();

  /**
   * Sends a synchronous ping request immediately, swallowing any errors that occur.
   * Typically used to verify connection or configuration health.
   */
  void pingNow();
}
