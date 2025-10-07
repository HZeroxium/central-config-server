package com.vng.zing.zcm.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedRandomLoadBalancerStrategy implements LoadBalancerStrategy {
  
  @Override
  public ServiceInstance choose(String serviceName, List<ServiceInstance> instances) {
    if (instances == null || instances.isEmpty()) {
      return null;
    }
    
    // Calculate total weight from instance metadata
    int totalWeight = instances.stream()
        .mapToInt(this::getWeight)
        .sum();
    
    if (totalWeight == 0) {
      // Fallback to random if no weights
      return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
    }
    
    int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
    int currentWeight = 0;
    
    for (ServiceInstance instance : instances) {
      currentWeight += getWeight(instance);
      if (randomWeight < currentWeight) {
        return instance;
      }
    }
    
    return instances.get(instances.size() - 1);
  }
  
  private int getWeight(ServiceInstance instance) {
    // Consul stores weight in metadata
    String weight = instance.getMetadata().get("weight");
    if (weight != null) {
      try {
        return Integer.parseInt(weight);
      } catch (NumberFormatException e) {
        // ignore invalid weight values
      }
    }
    return 1; // default weight
  }

  @Override
  public String getName() {
    return LoadBalancerStrategy.Policy.WEIGHTED_RANDOM.getValue();
  }
}
