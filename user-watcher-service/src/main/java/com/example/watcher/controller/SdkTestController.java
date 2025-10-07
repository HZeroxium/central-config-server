package com.example.watcher.controller;

import com.vng.zing.zcm.client.ClientApi;
import com.vng.zing.zcm.pingconfig.ConfigRefresher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sdk")
@RequiredArgsConstructor
@Slf4j
public class SdkTestController {

  private final ClientApi zcmClient;
  private final ConfigRefresher configRefresher;

  @GetMapping("/config")
  public Map<String, Object> getConfig() {
    log.info("Fetching all configurations via SDK");
    Map<String, Object> result = new HashMap<>();

    // Test getting some common config keys
    result.put("serviceName", zcmClient.get("spring.application.name"));
    result.put("serverPort", zcmClient.get("server.port"));
    result.put("configHash", zcmClient.configHash());

    // Get all config with a prefix
    result.put("appConfigs", zcmClient.getAll("app."));
    result.put("kafkaConfigs", zcmClient.getAll("kafka."));

    return result;
  }

  @GetMapping("/discovery")
  public Map<String, Object> getDiscovery() {
    log.info("Testing service discovery via SDK");
    Map<String, Object> result = new HashMap<>();

    try {
      // Test discovering different services
      List<String> services = List.of("user-rest-spring-service", "user-thrift-server-service", "consul");

      for (String serviceName : services) {
        try {
          var instances = zcmClient.instances(serviceName);
          result.put(serviceName + "_instances", instances.stream()
              .map(instance -> instance.getHost() + ":" + instance.getPort())
              .collect(Collectors.toList()));

          var chosen = zcmClient.choose(serviceName);
          if (chosen != null) {
            result.put(serviceName + "_chosen", chosen.getHost() + ":" + chosen.getPort());
          }
        } catch (Exception e) {
          log.warn("Failed to discover service {}: {}", serviceName, e.getMessage());
          result.put(serviceName + "_error", e.getMessage());
        }
      }
    } catch (Exception e) {
      log.error("Discovery test failed: {}", e.getMessage(), e);
      result.put("error", e.getMessage());
    }

    return result;
  }

  @PostMapping("/refresh")
  public ResponseEntity<String> refreshConfig() {
    log.info("Triggering config refresh via SDK");
    try {
      var changedKeys = configRefresher.refresh();
      return ResponseEntity.ok("Config refresh triggered. Changed keys: " + changedKeys);
    } catch (Exception e) {
      log.error("Config refresh failed: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().body("Config refresh failed: " + e.getMessage());
    }
  }

  @GetMapping("/ping")
  public Map<String, Object> ping() {
    log.info("Testing ping functionality via SDK");
    Map<String, Object> result = new HashMap<>();

    try {
      zcmClient.pingNow();
      result.put("pingStatus", "success");
      result.put("message", "Ping sent successfully");
    } catch (Exception e) {
      log.error("Ping test failed: {}", e.getMessage(), e);
      result.put("pingStatus", "failed");
      result.put("error", e.getMessage());
    }

    return result;
  }

  @GetMapping("/http")
  public Mono<Map<String, Object>> testHttpClient() {
    log.info("Testing HTTP client via SDK's WebClient");

    return zcmClient.http()
        .get()
        .uri("http://user-rest-spring-service/users/ping")
        .retrieve()
        .bodyToMono(String.class)
        .map(response -> {
          Map<String, Object> result = new HashMap<>();
          result.put("httpTest", "success");
          result.put("response", response);
          return result;
        })
        .onErrorResume(e -> {
          log.warn("HTTP test failed: {}", e.getMessage());
          Map<String, Object> result = new HashMap<>();
          result.put("httpTest", "failed");
          result.put("error", e.getMessage());
          return Mono.just(result);
        });
  }

  @GetMapping("/info")
  public Map<String, Object> getSdkInfo() {
    log.info("Getting SDK info");
    Map<String, Object> result = new HashMap<>();

    result.put("configHash", zcmClient.configHash());
    result.put("serviceName", zcmClient.get("spring.application.name"));
    result.put("serverPort", zcmClient.get("server.port"));
    result.put("kafkaBootstrapServers", zcmClient.get("spring.kafka.bootstrap-servers"));

    return result;
  }
}
