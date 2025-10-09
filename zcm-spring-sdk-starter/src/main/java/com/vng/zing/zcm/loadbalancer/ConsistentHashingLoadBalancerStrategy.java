package com.vng.zing.zcm.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Implements Consistent Hashing load balancing strategy with virtual nodes.
 * <p>
 * This algorithm creates a hash ring where each instance is represented by
 * multiple virtual nodes. This provides better load distribution compared
 * to basic consistent hashing and minimizes disruption when instances change.
 * <p>
 * The algorithm works by:
 * 1. Creating virtual nodes for each instance on a hash ring
 * 2. For each request, computing a hash and finding the next instance clockwise
 * 3. Using virtual nodes to ensure better load distribution
 */
public class ConsistentHashingLoadBalancerStrategy implements LoadBalancerStrategy {

  private static final int VIRTUAL_NODES_PER_INSTANCE = 100;
  private final MessageDigest md5Digest;

  public ConsistentHashingLoadBalancerStrategy() {
    try {
      this.md5Digest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 algorithm not available", e);
    }
  }

  /**
   * Selects an instance using Consistent Hashing algorithm.
   *
   * @param serviceName the service name (used as part of request key)
   * @param instances   list of available service instances
   * @return the instance selected by consistent hashing, or null if none available
   */
  @Override
  public ServiceInstance choose(String serviceName, List<ServiceInstance> instances) {
    // Use default request for backward compatibility
    LbRequest defaultRequest = LbRequest.of("default-" + System.currentTimeMillis());
    return choose(serviceName, instances, defaultRequest);
  }

  /**
   * Selects an instance using Consistent Hashing algorithm with request context.
   *
   * @param serviceName the service name
   * @param instances   list of available service instances
   * @param request     the load balancing request with context
   * @return the instance selected by consistent hashing, or null if none available
   */
  @Override
  public ServiceInstance choose(String serviceName, List<ServiceInstance> instances, LbRequest request) {
    if (instances == null || instances.isEmpty()) {
      return null;
    }

    // Build hash ring with virtual nodes
    TreeMap<Long, ServiceInstance> hashRing = buildHashRing(instances);

    // Create request key using the consistent hash key method
    String requestKey = request.getConsistentHashKey(serviceName);
    long requestHash = computeHash(requestKey);

    // Find the next instance clockwise on the ring
    Map.Entry<Long, ServiceInstance> entry = hashRing.ceilingEntry(requestHash);
    if (entry == null) {
      // Wrap around to the first entry (ring is circular)
      entry = hashRing.firstEntry();
    }

    return entry.getValue();
  }

  /**
   * Builds a hash ring with virtual nodes for all instances.
   *
   * @param instances the list of service instances
   * @return TreeMap representing the hash ring
   */
  private TreeMap<Long, ServiceInstance> buildHashRing(List<ServiceInstance> instances) {
    TreeMap<Long, ServiceInstance> hashRing = new TreeMap<>();

    for (ServiceInstance instance : instances) {
      // Create virtual nodes for each instance
      for (int i = 0; i < VIRTUAL_NODES_PER_INSTANCE; i++) {
        String virtualNodeKey = instance.getInstanceId() + ":" + i;
        long hash = computeHash(virtualNodeKey);
        hashRing.put(hash, instance);
      }
    }

    return hashRing;
  }

  /**
   * Computes MD5 hash of the given key and returns the first 8 bytes as a long.
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
    return LoadBalancerStrategy.Policy.CONSISTENT_HASHING.getValue();
  }
}
