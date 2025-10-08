package com.vng.zing.zcm.loadbalancer;

import lombok.extern.slf4j.Slf4j;

/**
 * Factory class for creating {@link LoadBalancerStrategy} instances
 * based on configuration or policy enumeration.
 * <p>
 * Provides centralized construction and logging for selected strategies.
 */
@Slf4j
public class LoadBalancerStrategyFactory {

  /**
   * Creates a {@link LoadBalancerStrategy} implementation for a given policy.
   *
   * @param policy the selected {@link LoadBalancerStrategy.Policy}
   * @return the corresponding {@link LoadBalancerStrategy} implementation
   */
  public static LoadBalancerStrategy create(LoadBalancerStrategy.Policy policy) {
    return switch (policy) {
      case ROUND_ROBIN -> {
        log.info("Using RoundRobin load balancer strategy");
        yield new RoundRobinLoadBalancerStrategy();
      }
      case RANDOM -> {
        log.info("Using Random load balancer strategy");
        yield new RandomLoadBalancerStrategy();
      }
      case WEIGHTED_RANDOM -> {
        log.info("Using WeightedRandom load balancer strategy");
        yield new WeightedRandomLoadBalancerStrategy();
      }
    };
  }

  /**
   * Parses a string and creates a {@link LoadBalancerStrategy}
   * corresponding to the provided policy string.
   *
   * @param policyString textual representation of the policy
   * @return a matching {@link LoadBalancerStrategy} instance
   */
  public static LoadBalancerStrategy create(String policyString) {
    LoadBalancerStrategy.Policy policy = LoadBalancerStrategy.Policy.fromString(policyString);
    return create(policy);
  }
}
