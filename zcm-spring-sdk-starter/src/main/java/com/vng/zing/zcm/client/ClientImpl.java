package com.vng.zing.zcm.client;

import com.vng.zing.zcm.configsnapshot.ConfigSnapshotBuilder;
import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategy;
import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategyFactory;
import com.vng.zing.zcm.loadbalancer.LbRequest;
import com.vng.zing.zcm.pingconfig.ConfigHashCalculator;
import com.vng.zing.zcm.pingconfig.PingSender;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ClientApi}.
 * <p>
 * This class provides environment property access, configuration snapshot generation,
 * service discovery integration, and load-balanced client capabilities.
 * <p>
 * It relies on Spring components such as:
 * <ul>
 *   <li>{@link DiscoveryClient} – for service discovery</li>
 *   <li>{@link RestClient.Builder} – for load-balanced HTTP calls</li>
 *   <li>{@link ConfigHashCalculator} – for configuration drift detection</li>
 *   <li>{@link PingSender} – for lightweight connectivity checks</li>
 *   <li>{@link LoadBalancerStrategy} – for instance selection logic</li>
 * </ul>
 */
@RequiredArgsConstructor
public class ClientImpl implements ClientApi {

  private final RestClient.Builder lbRestClientBuilder;
  private final DiscoveryClient discoveryClient;
  private final ConfigHashCalculator hashCalc;
  private final PingSender pingSender;
  private final LoadBalancerStrategy defaultLoadBalancerStrategy;

  /** {@inheritDoc} */
  @Override
  public String get(String key) {
    return env().getProperty(key);
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Object> getAll(String prefix) {
    Map<String, Object> out = new LinkedHashMap<>();

    // Iterate over all property sources to collect matching properties.
    if (env() instanceof ConfigurableEnvironment configurableEnv) {
      configurableEnv.getPropertySources().forEach(ps -> {
        if (ps.containsProperty(prefix)) {
          out.put(ps.getName(), ps.getProperty(prefix));
        }
      });
    }

    return out;
  }

  /** {@inheritDoc} */
  @Override
  public String configHash() {
    return hashCalc.currentHash();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Builds a full configuration snapshot including application metadata such as
   * application name, profile, label, and version.
   */
  @Override
  public Map<String, Object> configSnapshotMap() {
    String application = env().getProperty("spring.application.name", "unknown");
    String[] profiles = env().getActiveProfiles();
    String profile = profiles.length > 0 ? profiles[0] : "default";
    String label = env().getProperty("spring.cloud.config.label");
    String version = env().getProperty("config.client.version");

    var snapshot = new ConfigSnapshotBuilder((ConfigurableEnvironment) env())
        .build(application, profile, label, version);

    Map<String, Object> map = new LinkedHashMap<>();
    map.put("application", application);
    map.put("profile", profile);
    map.put("label", label);
    map.put("version", version);
    map.put("properties", snapshot.getProperties());

    return map;
  }

  /** {@inheritDoc} */
  @Override
  public List<ServiceInstance> instances(String serviceName) {
    // Delegates to Spring DiscoveryClient and returns all instances.
    return discoveryClient.getInstances(serviceName).stream().collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public ServiceInstance choose(String serviceName) {
    List<ServiceInstance> list = instances(serviceName);
    return defaultLoadBalancerStrategy.choose(serviceName, list);
  }

  /** {@inheritDoc} */
  @Override
  public ServiceInstance choose(String serviceName, LoadBalancerStrategy.Policy policy) {
    List<ServiceInstance> list = instances(serviceName);
    LoadBalancerStrategy strategy = LoadBalancerStrategyFactory.create(policy);
    return strategy.choose(serviceName, list);
  }

  /** {@inheritDoc} */
  @Override
  public ServiceInstance choose(String serviceName, LbRequest request) {
    List<ServiceInstance> list = instances(serviceName);
    return defaultLoadBalancerStrategy.choose(serviceName, list, request);
  }

  /** {@inheritDoc} */
  @Override
  public ServiceInstance choose(String serviceName, LoadBalancerStrategy.Policy policy, LbRequest request) {
    List<ServiceInstance> list = instances(serviceName);
    LoadBalancerStrategy strategy = LoadBalancerStrategyFactory.create(policy);
    return strategy.choose(serviceName, list, request);
  }

  /** {@inheritDoc} */
  @Override
  public String loadBalancerStrategy() {
    return defaultLoadBalancerStrategy.getName();
  }

  /** {@inheritDoc} */
  @Override
  public RestClient http() {
    return lbRestClientBuilder.build();
  }

  /** {@inheritDoc} */
  @Override
  public void pingNow() {
    // Sends a synchronous ping without throwing errors.
    pingSender.send();
  }

  /**
   * Returns the current Spring {@link Environment} instance associated with
   * the {@link ConfigHashCalculator}. This indirection ensures a single
   * authoritative environment context.
   *
   * @return the active {@link Environment}
   */
  private Environment env() {
    return hashCalc.getEnvironment();
  }
}
