package com.vng.zing.zcm.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancerStrategy implements LoadBalancerStrategy {
  
  private final ConcurrentHashMap<String, AtomicInteger> seq = new ConcurrentHashMap<>();

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

  @Override
  public String getName() {
    return LoadBalancerStrategy.Policy.ROUND_ROBIN.getValue();
  }
}
