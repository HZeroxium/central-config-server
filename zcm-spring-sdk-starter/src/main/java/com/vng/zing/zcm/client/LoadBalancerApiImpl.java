package com.vng.zing.zcm.client;

import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategy;
import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategyFactory;
import com.vng.zing.zcm.loadbalancer.LbRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
class LoadBalancerApiImpl implements LoadBalancerApi {
  
  private final DiscoveryClient discoveryClient;
  private final LoadBalancerStrategy defaultLoadBalancerStrategy;
  
  @Override
  public List<ServiceInstance> instances(String serviceName) {
    return discoveryClient.getInstances(serviceName).stream().collect(Collectors.toList());
  }
  
  @Override
  public ServiceInstance choose(String serviceName) {
    List<ServiceInstance> list = instances(serviceName);
    return defaultLoadBalancerStrategy.choose(serviceName, list);
  }
  
  @Override
  public ServiceInstance choose(String serviceName, LoadBalancerStrategy.Policy policy) {
    List<ServiceInstance> list = instances(serviceName);
    LoadBalancerStrategy strategy = LoadBalancerStrategyFactory.create(policy);
    return strategy.choose(serviceName, list);
  }
  
  @Override
  public ServiceInstance choose(String serviceName, LbRequest request) {
    List<ServiceInstance> list = instances(serviceName);
    return defaultLoadBalancerStrategy.choose(serviceName, list, request);
  }
  
  @Override
  public ServiceInstance choose(String serviceName, LoadBalancerStrategy.Policy policy, LbRequest request) {
    List<ServiceInstance> list = instances(serviceName);
    LoadBalancerStrategy strategy = LoadBalancerStrategyFactory.create(policy);
    return strategy.choose(serviceName, list, request);
  }
  
  @Override
  public String strategy() {
    return defaultLoadBalancerStrategy.getName();
  }
}
