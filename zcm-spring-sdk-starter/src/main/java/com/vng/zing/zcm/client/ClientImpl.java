package com.vng.zing.zcm.client;

import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategy;
import com.vng.zing.zcm.pingconfig.ConfigHashCalculator;
import com.vng.zing.zcm.pingconfig.PingSender;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.client.RestClient;

/**
 * Default implementation of ClientApi.
 */
@RequiredArgsConstructor
public class ClientImpl implements ClientApi {
  
  private final RestClient.Builder lbRestClientBuilder;
  private final DiscoveryClient discoveryClient;
  private final ConfigHashCalculator hashCalc;
  private final PingSender pingSender;
  private final LoadBalancerStrategy defaultLoadBalancerStrategy;
  
  // Lazy-initialized sub-APIs
  private ConfigApi configApi;
  private LoadBalancerApi loadBalancerApi;
  private HttpApi httpApi;
  
  @Override
  public ConfigApi config() {
    if (configApi == null) {
      configApi = new ConfigApiImpl(hashCalc);
    }
    return configApi;
  }
  
  @Override
  public LoadBalancerApi loadBalancer() {
    if (loadBalancerApi == null) {
      loadBalancerApi = new LoadBalancerApiImpl(discoveryClient, defaultLoadBalancerStrategy);
    }
    return loadBalancerApi;
  }
  
  @Override
  public HttpApi http() {
    if (httpApi == null) {
      httpApi = new HttpApiImpl(lbRestClientBuilder);
    }
    return httpApi;
  }
  
  @Override
  public void pingNow() {
    pingSender.send();
  }
}
