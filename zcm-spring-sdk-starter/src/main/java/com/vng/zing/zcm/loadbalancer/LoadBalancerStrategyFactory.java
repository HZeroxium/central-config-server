package com.vng.zing.zcm.loadbalancer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoadBalancerStrategyFactory {
  
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
  
  public static LoadBalancerStrategy create(String policyString) {
    LoadBalancerStrategy.Policy policy = LoadBalancerStrategy.Policy.fromString(policyString);
    return create(policy);
  }
}
