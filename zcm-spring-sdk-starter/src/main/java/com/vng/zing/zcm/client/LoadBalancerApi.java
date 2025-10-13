package com.vng.zing.zcm.client;

import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategy;
import com.vng.zing.zcm.loadbalancer.LbRequest;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * Load Balancer API for service discovery and instance selection.
 */
public interface LoadBalancerApi {
  
  /**
   * Retrieves all healthy instances for a service.
   * 
   * @param serviceName the target service name
   * @return list of discovered instances
   */
  List<ServiceInstance> instances(String serviceName);
  
  /**
   * Selects an instance using the default load-balancer strategy.
   * 
   * @param serviceName the target service name
   * @return the chosen instance or null if none available
   */
  ServiceInstance choose(String serviceName);
  
  /**
   * Selects an instance using a specified policy.
   * 
   * @param serviceName the target service name
   * @param policy the load-balancer policy to use
   * @return the chosen instance or null if none available
   */
  ServiceInstance choose(String serviceName, LoadBalancerStrategy.Policy policy);
  
  /**
   * Selects an instance using default strategy with request context.
   * 
   * @param serviceName the target service name
   * @param request the load balancing request with context
   * @return the chosen instance or null if none available
   */
  ServiceInstance choose(String serviceName, LbRequest request);
  
  /**
   * Selects an instance using specified policy with request context.
   * 
   * @param serviceName the target service name
   * @param policy the load-balancer policy to use
   * @param request the load balancing request with context
   * @return the chosen instance or null if none available
   */
  ServiceInstance choose(String serviceName, LoadBalancerStrategy.Policy policy, LbRequest request);
  
  /**
   * Returns the name of the currently active load-balancer strategy.
   * 
   * @return the strategy name
   */
  String strategy();
}
