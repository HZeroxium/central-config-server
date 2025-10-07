package com.vng.zing.zcm.client;

import com.vng.zing.zcm.config.SdkProperties;
import com.vng.zing.zcm.discovery.RoundRobinChooser;
import com.vng.zing.zcm.pingconfig.ConfigHashCalculator;
import com.vng.zing.zcm.pingconfig.PingSender;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ClientImpl implements ClientApi {

  private final SdkProperties props;
  private final WebClient.Builder lbWebClientBuilder;
  private final DiscoveryClient discoveryClient;
  private final ConfigHashCalculator hashCalc;
  private final PingSender pingSender;
  private final RoundRobinChooser rr = new RoundRobinChooser();

  @Override
  public String get(String key) {
    return env().getProperty(key);
  }

  @Override
  public Map<String, Object> getAll(String prefix) {
    Map<String, Object> out = new LinkedHashMap<>();
    if (env() instanceof ConfigurableEnvironment configurableEnv) {
      configurableEnv.getPropertySources().forEach(ps -> {
        if (ps.containsProperty(prefix)) {
          out.put(ps.getName(), ps.getProperty(prefix));
        }
      });
    }
    return out;
  }

  @Override
  public String configHash() {
    return hashCalc.currentHash();
  }

  @Override
  public List<ServiceInstance> instances(String serviceName) {
    return discoveryClient.getInstances(serviceName).stream().collect(Collectors.toList());
  }

  @Override
  public ServiceInstance choose(String serviceName) {
    List<ServiceInstance> list = instances(serviceName);
    return rr.choose(serviceName, list);
  }

  @Override
  public WebClient http() {
    return lbWebClientBuilder.build();
  }

  @Override
  public void pingNow() {
    pingSender.send();
  }

  private Environment env() {
    return hashCalc.getEnvironment();
  }
}
