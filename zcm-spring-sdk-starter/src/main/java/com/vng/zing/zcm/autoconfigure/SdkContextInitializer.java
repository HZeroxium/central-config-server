package com.vng.zing.zcm.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SdkContextInitializer implements ApplicationListener<ApplicationPreparedEvent> {

  @Override
  public void onApplicationEvent(@NonNull ApplicationPreparedEvent event) {
    log.info("ZCM-SDK ContextInitializer executing...");
    ConfigurableEnvironment env = event.getApplicationContext().getEnvironment();
    Map<String, Object> add = new HashMap<>();

    // Map service name -> spring.application.name + consul discovery service-name
    String svc = env.getProperty("zcm.sdk.service.name");
    if (svc != null && !svc.isBlank()) {
      add.put("spring.application.name", svc);
      add.put("spring.cloud.consul.discovery.service-name", svc);
      log.info("ZCM-SDK mapped service name: {}", svc);
    }

    // Map instance ID -> consul discovery instance-id
    String instanceId = env.getProperty("zcm.sdk.instance.id");
    String hostname = System.getenv("HOSTNAME");
    if (instanceId == null || instanceId.isBlank()) {
      // Derive a stable instance-id if not explicitly provided
      String base = (svc != null && !svc.isBlank()) ? svc : env.getProperty("spring.application.name", "app");
      if (hostname != null && !hostname.isBlank()) {
        add.put("spring.cloud.consul.discovery.instance-id", base + "-${server.port}-" + hostname);
        log.info("ZCM-SDK derived instance ID from service/hostname: {}-{{port}}-{}", base, hostname);
      } else {
        add.put("spring.cloud.consul.discovery.instance-id", base + "-${server.port}-${random.value}");
        log.info("ZCM-SDK derived instance ID with random suffix for service: {}", base);
      }
    } else {
      if (hostname != null && !hostname.isBlank()) {
        add.put("spring.cloud.consul.discovery.instance-id", instanceId + "-${server.port}-" + hostname);
        log.info("ZCM-SDK mapped instance ID: {} with hostname: {}", instanceId, hostname);
      } else {
        add.put("spring.cloud.consul.discovery.instance-id", instanceId + "-${server.port}-${random.value}");
        log.info("ZCM-SDK mapped instance ID: {} with random value", instanceId);
      }
    }

    // Config Server via Config Data import
    String cfgUrl = env.getProperty("zcm.sdk.config.server.url");
    log.info("ZCM-SDK config.server.url property: {}", cfgUrl);
    if (cfgUrl != null && !cfgUrl.isBlank()) {
      add.put("spring.config.import", "optional:configserver:" + cfgUrl);
      log.info("ZCM-SDK added spring.config.import: optional:configserver:{}", cfgUrl);
    }

    // Consul host/port + heartbeat settings
    String ch = env.getProperty("zcm.sdk.discovery.consul.host");
    String cp = env.getProperty("zcm.sdk.discovery.consul.port");
    if (ch != null)
      add.put("spring.cloud.consul.host", ch);
    if (cp != null)
      add.put("spring.cloud.consul.port", cp);

    String hbEnabled = env.getProperty("zcm.sdk.discovery.consul.heartbeat.enabled");
    if (hbEnabled != null) {
      add.put("spring.cloud.consul.discovery.heartbeat.enabled", hbEnabled);
    } else {
      add.putIfAbsent("spring.cloud.consul.discovery.heartbeat.enabled", "true");
    }

    String hbTtl = env.getProperty("zcm.sdk.discovery.consul.heartbeat.ttl");
    if (hbTtl != null) {
      add.put("spring.cloud.consul.discovery.heartbeat.ttl", hbTtl);
    } else {
      add.putIfAbsent("spring.cloud.consul.discovery.heartbeat.ttl", "10s");
    }

    // Consul registration setting
    String register = env.getProperty("zcm.sdk.discovery.consul.register");
    if (register != null) {
      add.put("spring.cloud.consul.discovery.register", register);
    } else {
      add.putIfAbsent("spring.cloud.consul.discovery.register", "true");
    }

    // Prefer IP address for registration to avoid hostname resolution issues in containers
    String preferIp = env.getProperty("zcm.sdk.discovery.consul.prefer-ip-address");
    if (preferIp != null && !preferIp.isBlank()) {
      add.put("spring.cloud.consul.discovery.prefer-ip-address", preferIp);
    } else {
      add.putIfAbsent("spring.cloud.consul.discovery.prefer-ip-address", "true");
    }

    // Auto-deregister critical services after grace period to avoid stale instances
    String deregAfter = env.getProperty("zcm.sdk.discovery.consul.deregister-critical-service-after");
    if (deregAfter != null && !deregAfter.isBlank()) {
      add.put("spring.cloud.consul.discovery.deregister-critical-service-after", deregAfter);
    } else {
      add.putIfAbsent("spring.cloud.consul.discovery.deregister-critical-service-after", "30s");
    }

    // Disable Consul catalog watch (reduces polling)
    add.put("spring.cloud.consul.discovery.catalog-services-watch.enabled", "false");
    
    // Enable graceful shutdown for Consul deregistration
    add.put("spring.cloud.consul.discovery.deregister", "true");
    add.put("spring.cloud.consul.discovery.fail-fast", "false");

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
      log.info("ZCM-SDK mappings applied: {}", add.keySet());
    } else {
      log.warn("ZCM-SDK no properties to map - check configuration");
    }
  }
}
