package com.vng.zing.zcm.client;

import com.vng.zing.zcm.client.config.ConfigApi;
import com.vng.zing.zcm.client.featureflag.FeatureFlagApi;
import com.vng.zing.zcm.client.http.HttpApi;
import com.vng.zing.zcm.client.kv.KVApi;
import com.vng.zing.zcm.client.loadbalancer.LoadBalancerApi;

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
   * Access feature flags API.
   * 
   * @return the FeatureFlagApi instance
   */
  FeatureFlagApi featureFlags();

  /**
   * Access Key-Value store API.
   *
   * @return the KVApi instance
   */
  KVApi kv();
  
  /**
   * Sends an immediate ping to the control service.
   * Convenience method for health checks.
   */
  void pingNow();
}
