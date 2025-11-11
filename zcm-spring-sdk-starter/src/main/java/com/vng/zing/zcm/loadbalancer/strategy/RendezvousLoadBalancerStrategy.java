package com.vng.zing.zcm.loadbalancer.strategy;

import com.vng.zing.zcm.loadbalancer.LbRequest;
import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategy;
import org.springframework.cloud.client.ServiceInstance;
import java.util.List;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Implements Rendezvous Hashing (Highest Random Weight) load balancing strategy.
 * <p>
 * This algorithm provides deterministic instance selection where the same request
 * key always maps to the same instance. It minimizes disruption when instances
 * are added or removed - only 1/N of requests are affected on average.
 * <p>
 * The algorithm works by:
 * 1. For each instance, compute a hash of (requestKey + instanceKey)
 * 2. Select the instance with the highest hash value
 * 3. This ensures deterministic and fair distribution
 */
public class RendezvousLoadBalancerStrategy implements LoadBalancerStrategy {

  private final MessageDigest md5Digest;

  public RendezvousLoadBalancerStrategy() {
    try {
      this.md5Digest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 algorithm not available", e);
    }
  }

  /**
   * Selects an instance using Rendezvous Hashing algorithm.
   *
   * @param serviceName the service name (used as part of request key)
   * @param instances   list of available service instances
   * @return the instance with highest hash value, or null if none available
   */
  @Override
  public ServiceInstance choose(String serviceName, List<ServiceInstance> instances) {
    // Use default request for backward compatibility
    LbRequest defaultRequest = LbRequest.of("default-" + System.currentTimeMillis());
    return choose(serviceName, instances, defaultRequest);
  }

  /**
   * Selects an instance using Rendezvous Hashing algorithm with request context.
   *
   * @param serviceName the service name
   * @param instances   list of available service instances
   * @param request     the load balancing request with context
   * @return the instance with highest hash value, or null if none available
   */
  @Override
  public ServiceInstance choose(String serviceName, List<ServiceInstance> instances, LbRequest request) {
    if (instances == null || instances.isEmpty()) {
      return null;
    }

    ServiceInstance selected = null;
    long maxHash = Long.MIN_VALUE;

    for (ServiceInstance instance : instances) {
      // Create a unique key using the rendezvous hash key method
      String requestKey = request.getRendezvousHashKey(serviceName, instance.getInstanceId());
      long hash = computeHash(requestKey);
      
      if (hash > maxHash) {
        maxHash = hash;
        selected = instance;
      }
    }

    return selected;
  }

  /**
   * Computes MD5 hash of the given key and returns the first 8 bytes as a long.
   * This provides good distribution while being deterministic.
   *
   * @param key the string to hash
   * @return hash value as long
   */
  private long computeHash(String key) {
    synchronized (md5Digest) {
      md5Digest.reset();
      byte[] hash = md5Digest.digest(key.getBytes());
      
      // Use first 8 bytes to create a long value
      long result = 0;
      for (int i = 0; i < 8; i++) {
        result = (result << 8) + (hash[i] & 0xFF);
      }
      
      return Math.abs(result);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return LoadBalancerStrategy.Policy.RENDEZVOUS.getValue();
  }
}
