package com.vng.zing.zcm.loadbalancer.strategy;

import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategy;
import org.springframework.cloud.client.ServiceInstance;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implements a weighted random load-balancing strategy.
 * <p>
 * Each instance can define a custom weight via metadata key {@code "weight"}.
 * Instances with higher weights have a proportionally greater chance
 * of being selected.
 * <p>
 * Falls back to uniform random selection if all weights are missing or invalid.
 */
public class WeightedRandomLoadBalancerStrategy implements LoadBalancerStrategy {

  /**
   * Selects an instance based on weighted probability.
   *
   * @param serviceName the service name
   * @param instances   list of instances to choose from
   * @return the selected instance, or {@code null} if none available
   */
  @Override
  public ServiceInstance choose(String serviceName, List<ServiceInstance> instances) {
    if (instances == null || instances.isEmpty()) {
      return null;
    }

    // Calculate total weight from instance metadata
    int totalWeight = instances.stream()
        .mapToInt(this::getWeight)
        .sum();

    // Fallback to uniform random if no weights
    if (totalWeight == 0) {
      return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
    }

    int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
    int currentWeight = 0;

    // Traverse cumulative weight intervals
    for (ServiceInstance instance : instances) {
      currentWeight += getWeight(instance);
      if (randomWeight < currentWeight) {
        return instance;
      }
    }

    // Should not reach here, but fallback to last instance for safety
    return instances.get(instances.size() - 1);
  }

  /**
   * Extracts the instance's weight from metadata, with a default of 1.
   *
   * @param instance the service instance
   * @return integer weight value (>=1)
   */
  private int getWeight(ServiceInstance instance) {
    String weight = instance.getMetadata().get("weight");
    if (weight != null) {
      try {
        return Integer.parseInt(weight);
      } catch (NumberFormatException e) {
        // ignore invalid weight values, default to 1
      }
    }
    return 1;
  }

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return LoadBalancerStrategy.Policy.WEIGHTED_RANDOM.getValue();
  }
}
