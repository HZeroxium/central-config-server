package com.vng.zing.zcm.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalancerStrategy implements LoadBalancerStrategy {
  
  @Override
  public ServiceInstance choose(String serviceName, List<ServiceInstance> instances) {
    if (instances == null || instances.isEmpty()) {
      return null;
    }
    
    int index = ThreadLocalRandom.current().nextInt(instances.size());
    return instances.get(index);
  }

  @Override
  public String getName() {
    return LoadBalancerStrategy.Policy.RANDOM.getValue();
  }
}
