package com.vng.zing.zcm.client;

/**
 * Main SDK Client API providing access to configuration, load balancing, and HTTP services.
 * 
 * <p>This is the primary entry point for ZCM SDK functionality, organized into:
 * <ul>
 *   <li>{@link ConfigApi} - Configuration management and snapshots</li>
 *   <li>{@link LoadBalancerApi} - Service discovery and load balancing</li>
 *   <li>{@link HttpApi} - Load-balanced HTTP client</li>
 * </ul>
 */
public interface ClientApi {
  
  /**
   * Access configuration management API.
   * 
   * @return the ConfigApi instance
   */
  ConfigApi config();
  
  /**
   * Access load balancer and service discovery API.
   * 
   * @return the LoadBalancerApi instance
   */
  LoadBalancerApi loadBalancer();
  
  /**
   * Access HTTP client API.
   * 
   * @return the HttpApi instance
   */
  HttpApi http();
  
  /**
   * Sends an immediate ping to the control service.
   * Convenience method for health checks.
   */
  void pingNow();
}
