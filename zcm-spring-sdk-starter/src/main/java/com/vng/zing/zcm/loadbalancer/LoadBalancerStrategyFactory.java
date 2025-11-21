package com.vng.zing.zcm.loadbalancer;

import com.vng.zing.zcm.loadbalancer.strategy.*;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory class for creating {@link LoadBalancerStrategy} instances
 * based on configuration or policy enumeration.
 * <p>
 * Provides centralized construction and caching for selected strategies.
 * Strategies are cached per policy to maintain state (e.g., round-robin counters).
 */
@Slf4j
public class LoadBalancerStrategyFactory {

  /** Cache of strategy instances per policy to maintain state across requests. */
  private static final ConcurrentHashMap<LoadBalancerStrategy.Policy, LoadBalancerStrategy> STRATEGY_CACHE = new ConcurrentHashMap<>();

  /**
   * Creates or retrieves a cached {@link LoadBalancerStrategy} implementation for a given policy.
   * <p>
   * Strategies are cached to maintain state (e.g., round-robin sequence counters).
   * This ensures that multiple requests using the same policy share the same strategy instance.
   *
   * @param policy the selected {@link LoadBalancerStrategy.Policy}
   * @return the corresponding {@link LoadBalancerStrategy} implementation (cached)
   */
  public static LoadBalancerStrategy create(LoadBalancerStrategy.Policy policy) {
    return STRATEGY_CACHE.computeIfAbsent(policy, p -> {
      log.info("Creating new load balancer strategy: {}", p.getValue());
      return switch (p) {
        case ROUND_ROBIN -> new RoundRobinLoadBalancerStrategy();
        case RANDOM -> new RandomLoadBalancerStrategy();
        case WEIGHTED_RANDOM -> new WeightedRandomLoadBalancerStrategy();
        case RENDEZVOUS -> new RendezvousLoadBalancerStrategy();
        case CONSISTENT_HASHING -> new ConsistentHashingLoadBalancerStrategy();
      };
    });
  }

  /**
   * Parses a string and creates or retrieves a cached {@link LoadBalancerStrategy}
   * corresponding to the provided policy string.
   *
   * @param policyString textual representation of the policy
   * @return a matching {@link LoadBalancerStrategy} instance (cached)
   */
  public static LoadBalancerStrategy create(String policyString) {
    LoadBalancerStrategy.Policy policy = LoadBalancerStrategy.Policy.fromString(policyString);
    return create(policy);
  }

  /**
   * Clears the strategy cache (useful for testing or dynamic reconfiguration).
   */
  public static void clearCache() {
    STRATEGY_CACHE.clear();
  }
}
