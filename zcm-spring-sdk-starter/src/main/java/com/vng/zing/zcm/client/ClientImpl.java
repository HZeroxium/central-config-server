package com.vng.zing.zcm.client;

import com.vng.zing.zcm.client.config.ConfigApi;
import com.vng.zing.zcm.client.config.ConfigApiImpl;
import com.vng.zing.zcm.client.featureflag.FeatureFlagApi;
import com.vng.zing.zcm.client.http.HttpApi;
import com.vng.zing.zcm.client.http.HttpApiImpl;
import com.vng.zing.zcm.client.kv.KVApi;
import com.vng.zing.zcm.client.loadbalancer.LoadBalancerApi;
import com.vng.zing.zcm.client.loadbalancer.LoadBalancerApiImpl;
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
  private FeatureFlagApi featureFlagApi;
  private KVApi kvApi;
  
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
  public FeatureFlagApi featureFlags() {
    if (featureFlagApi == null) {
      throw new IllegalStateException("FeatureFlagApi is not initialized. Ensure Unleash is configured and enabled.");
    }
    return featureFlagApi;
  }

  @Override
  public KVApi kv() {
    if (kvApi == null) {
      throw new IllegalStateException("KVApi is not initialized. Ensure KV is enabled (zcm.sdk.kv.enabled=true) and configured.");
    }
    return kvApi;
  }
  
  @Override
  public void pingNow() {
    pingSender.send();
  }
  
  /**
   * Sets the FeatureFlagApi instance (used by auto-configuration).
   * 
   * @param featureFlagApi the FeatureFlagApi instance
   */
  public void setFeatureFlagApi(FeatureFlagApi featureFlagApi) {
    this.featureFlagApi = featureFlagApi;
  }

  /**
   * Sets the KVApi instance (used by auto-configuration).
   *
   * @param kvApi the KVApi instance
   */
  public void setKVApi(KVApi kvApi) {
    this.kvApi = kvApi;
  }
}
