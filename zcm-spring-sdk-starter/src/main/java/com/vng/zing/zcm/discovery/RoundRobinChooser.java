package com.vng.zing.zcm.discovery;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinChooser {

  private final ConcurrentHashMap<String, AtomicInteger> seq = new ConcurrentHashMap<>();

  public ServiceInstance choose(String serviceName, List<ServiceInstance> instances) {
    if (instances == null || instances.isEmpty())
      return null;
    int size = instances.size();
    AtomicInteger idx = seq.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
    int i = Math.floorMod(idx.getAndIncrement(), size);
    return instances.get(i);
  }
}
