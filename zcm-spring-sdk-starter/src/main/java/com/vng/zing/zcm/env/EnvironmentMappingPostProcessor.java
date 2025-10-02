package com.vng.zing.zcm.env;

import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentMappingPostProcessor implements EnvironmentPostProcessor, Ordered {

  private final org.apache.commons.logging.Log log;

  public EnvironmentMappingPostProcessor(DeferredLogFactory dlf) {
    this.log = dlf.getLog(EnvironmentMappingPostProcessor.class);
  }

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment env,
      org.springframework.boot.SpringApplication application) {
    Map<String, Object> add = new HashMap<>();

    // Map service name -> spring.application.name + consul discovery service-name
    String svc = env.getProperty("zcm.sdk.service.name");
    if (svc != null && !svc.isBlank()) {
      add.put("spring.application.name", svc);
      add.put("spring.cloud.consul.discovery.service-name", svc);
    }

    // Config Server via Config Data import
    String cfgUrl = env.getProperty("zcm.sdk.config.server.url");
    if (cfgUrl != null && !cfgUrl.isBlank()) {
      add.put("spring.config.import", "optional:configserver:" + cfgUrl);
    }

    // Consul host/port + heartbeat settings
    String ch = env.getProperty("zcm.sdk.discovery.consul.host");
    String cp = env.getProperty("zcm.sdk.discovery.consul.port");
    if (ch != null)
      add.put("spring.cloud.consul.host", ch);
    if (cp != null)
      add.put("spring.cloud.consul.port", cp);

    String hbEnabled = env.getProperty("zcm.sdk.discovery.consul.heartbeat.enabled");
    if (hbEnabled != null)
      add.put("spring.cloud.consul.discovery.heartbeat.enabled", hbEnabled);

    String hbTtl = env.getProperty("zcm.sdk.discovery.consul.heartbeat.ttl");
    if (hbTtl != null)
      add.put("spring.cloud.consul.discovery.heartbeat.ttl", hbTtl);

    // Ensure register by default
    add.putIfAbsent("spring.cloud.consul.discovery.register", "true");

    // Kafka bootstrap (for refresh listener or Spring Cloud Bus Kafka)
    String bs = env.getProperty("zcm.sdk.bus.kafka.bootstrap-servers");
    if (bs != null && !bs.isBlank())
      add.put("spring.kafka.bootstrap-servers", bs);

    // Expose busrefresh endpoint for convenience (if not explicitly set)
    String exposure = env.getProperty("management.endpoints.web.exposure.include");
    if (exposure == null || exposure.isBlank()) {
      add.put("management.endpoints.web.exposure.include", "health,info,refresh,busrefresh");
    }

    if (!add.isEmpty()) {
      env.getPropertySources().addFirst(new MapPropertySource("zcm-sdk-mappings", add));
      log.info("ZCM-SDK mappings applied: " + add.keySet());
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
