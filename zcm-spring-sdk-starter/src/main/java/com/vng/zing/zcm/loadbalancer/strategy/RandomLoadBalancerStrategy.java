package com.vng.zing.zcm.loadbalancer.strategy;

import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategy;
import org.springframework.cloud.client.ServiceInstance;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implements a {@link LoadBalancerStrategy} that selects a random instance.
 * <p>
 * Each request chooses an instance uniformly at random, ensuring
 * probabilistic load distribution without maintaining state.
 */
public class RandomLoadBalancerStrategy implements LoadBalancerStrategy {

  /**
   * Chooses a random instance from the available list.
   *
   * @param serviceName the service name
   * @param instances   list of available instances
   * @return randomly selected instance or {@code null} if empty
   */
  @Override
  public ServiceInstance choose(String serviceName, List<ServiceInstance> instances) {
    if (instances == null || instances.isEmpty()) {
      return null;
    }

    int index = ThreadLocalRandom.current().nextInt(instances.size());
    return instances.get(index);
  }

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return LoadBalancerStrategy.Policy.RANDOM.getValue();
  }
}
