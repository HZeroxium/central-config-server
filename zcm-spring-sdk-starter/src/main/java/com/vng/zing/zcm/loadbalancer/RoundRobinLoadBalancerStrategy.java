package com.vng.zing.zcm.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements a deterministic {@link LoadBalancerStrategy} using the Round Robin algorithm.
 * <p>
 * Each request cycles through available instances in sequential order, maintaining
 * independent counters per service to ensure fairness and even distribution.
 */
public class RoundRobinLoadBalancerStrategy implements LoadBalancerStrategy {

  /** Tracks per-service sequence counters to support multi-service usage. */
  private final ConcurrentHashMap<String, AtomicInteger> seq = new ConcurrentHashMap<>();

  /**
   * Selects the next instance in the round-robin sequence for a given service.
   *
   * @param serviceName the service name (used as sequence key)
   * @param instances   list of available service instances
   * @return the next instance, or {@code null} if none exist
   */
  @Override
  public ServiceInstance choose(String serviceName, List<ServiceInstance> instances) {
    if (instances == null || instances.isEmpty()) {
      return null;
    }

    int size = instances.size();
    AtomicInteger idx = seq.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
    int i = Math.floorMod(idx.getAndIncrement(), size);
    return instances.get(i);
  }

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return LoadBalancerStrategy.Policy.ROUND_ROBIN.getValue();
  }
}
