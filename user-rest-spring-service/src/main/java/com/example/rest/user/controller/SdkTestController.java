package com.example.rest.user.controller;

import com.vng.zing.zcm.client.ClientApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller to test ZCM SDK functionality
 */
@Slf4j
@RestController
@RequestMapping("/api/sdk")
@RequiredArgsConstructor
public class SdkTestController {

  private final ClientApi zcmClient;

  @GetMapping("/config")
  public Map<String, Object> getConfig() {
    log.info("Getting config via SDK");
    Map<String, Object> result = new HashMap<>();

    // Get some config values
    result.put("service.name", zcmClient.get("spring.application.name"));
    result.put("server.port", zcmClient.get("server.port"));
    result.put("config.hash", zcmClient.configHash());

    // Get all config with a prefix
    result.put("spring.config", zcmClient.getAll("spring."));

    return result;
  }

  @GetMapping("/discovery")
  public Map<String, Object> getDiscovery() {
    log.info("Getting discovery info via SDK");
    Map<String, Object> result = new HashMap<>();

    // Get instances of thrift server service
    List<ServiceInstance> instances = zcmClient.instances("user-thrift-server-service");
    result.put("thrift.server.instances", instances.stream()
        .map(instance -> Map.of(
            "host", instance.getHost(),
            "port", instance.getPort(),
            "metadata", instance.getMetadata()))
        .toList());

    // Try to choose an instance
    ServiceInstance chosen = zcmClient.choose("user-thrift-server-service");
    if (chosen != null) {
      result.put("chosen.instance", Map.of(
          "host", chosen.getHost(),
          "port", chosen.getPort(),
          "metadata", chosen.getMetadata()));
    } else {
      result.put("chosen.instance", null);
    }

    return result;
  }

  @GetMapping("/ping")
  public Map<String, Object> ping() {
    log.info("Sending ping via SDK");
    Map<String, Object> result = new HashMap<>();

    try {
      zcmClient.pingNow();
      result.put("status", "success");
      result.put("message", "Ping sent successfully");
    } catch (Exception e) {
      result.put("status", "error");
      result.put("message", e.getMessage());
      log.error("Failed to send ping", e);
    }

    return result;
  }

  @GetMapping("/http")
  public Mono<Map<String, Object>> testHttpClient() {
    log.info("Testing HTTP client via SDK");

    return zcmClient.http()
        .get()
        .uri("http://user-thrift-server-service/actuator/health")
        .retrieve()
        .bodyToMono(Map.class)
        .map(health -> Map.of(
            "status", "success",
            "thrift.server.health", health))
        .onErrorReturn(Map.of(
            "status", "error",
            "message", "Failed to connect to thrift server via HTTP"));
  }

  @GetMapping("/info")
  public Map<String, Object> getSdkInfo() {
    log.info("Getting SDK info");
    Map<String, Object> result = new HashMap<>();

    result.put("config.hash", zcmClient.configHash());
    result.put("timestamp", System.currentTimeMillis());
    result.put("sdk.version", "0.1.0");

    return result;
  }
}
