package com.example.sample.web;

import com.vng.zing.zcm.client.ClientApi;
import com.vng.zing.zcm.loadbalancer.LoadBalancerStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sdk")
@RequiredArgsConstructor
public class SdkTestController {

  private final ClientApi client;

  @GetMapping("/snapshot")
  public ResponseEntity<Map<String, Object>> getSnapshot() {
    String hash = client.configHash();
    Map<String, Object> snap = client.configSnapshotMap();

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", "ok");
    body.put("hash", hash);
    body.putAll(snap);
    body.put("keyCount", ((Map<?, ?>) snap.get("properties")).size());
    return ResponseEntity.ok(body);
  }

  @GetMapping("/info")
  public ResponseEntity<Map<String, Object>> getSdkInfo() {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("status", "ok");
    info.put("configHash", client.configHash());
    info.put("loadBalancerStrategy", client.loadBalancerStrategy());
    return ResponseEntity.ok(info);
  }

  @GetMapping("/discovery/{serviceName}")
  public ResponseEntity<Map<String, Object>> discoverService(@PathVariable String serviceName) {
    List<ServiceInstance> instances = client.instances(serviceName);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "ok");
    result.put("serviceName", serviceName);
    result.put("instanceCount", instances.size());
    result.put("instances", instances.stream().map(i -> Map.of(
        "instanceId", i.getInstanceId(),
        "host", i.getHost(),
        "port", i.getPort(),
        "metadata", i.getMetadata()
    )).toList());
    return ResponseEntity.ok(result);
  }

  @GetMapping("/choose/{serviceName}")
  public ResponseEntity<Map<String, Object>> chooseInstance(@PathVariable String serviceName) {
    ServiceInstance chosen = client.choose(serviceName);
    if (chosen == null) {
      return ResponseEntity.notFound().build();
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "ok");
    result.put("serviceName", serviceName);
    result.put("strategy", client.loadBalancerStrategy());
    result.put("chosen", Map.of(
        "instanceId", chosen.getInstanceId(),
        "host", chosen.getHost(),
        "port", chosen.getPort(),
        "uri", chosen.getUri().toString()
    ));
    return ResponseEntity.ok(result);
  }

  @GetMapping("/choose/{serviceName}/{policy}")
  public ResponseEntity<Map<String, Object>> chooseInstanceWithPolicy(
      @PathVariable String serviceName, 
      @PathVariable String policy) {
    try {
      LoadBalancerStrategy.Policy policyEnum = LoadBalancerStrategy.Policy.fromString(policy);
      ServiceInstance chosen = client.choose(serviceName, policyEnum);
      if (chosen == null) {
        return ResponseEntity.notFound().build();
      }
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("status", "ok");
      result.put("serviceName", serviceName);
      result.put("strategy", policyEnum.getValue());
      result.put("chosen", Map.of(
          "instanceId", chosen.getInstanceId(),
          "host", chosen.getHost(),
          "port", chosen.getPort(),
          "uri", chosen.getUri().toString()
      ));
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("status", "error");
      error.put("message", "Invalid policy: " + policy);
      error.put("validPolicies", new String[]{"ROUND_ROBIN", "RANDOM", "WEIGHTED_RANDOM"});
      return ResponseEntity.badRequest().body(error);
    }
  }

  @PostMapping("/ping")
  public ResponseEntity<Map<String, String>> triggerPing() {
    client.pingNow();
    return ResponseEntity.ok(Map.of("status", "ok", "message", "Ping triggered"));
  }

  @GetMapping("/config/{key}")
  public ResponseEntity<Map<String, Object>> getConfigValue(@PathVariable String key) {
    String value = client.get(key);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "ok");
    result.put("key", key);
    result.put("value", value != null ? value : "null");
    return ResponseEntity.ok(result);
  }

  @GetMapping("/policies")
  public ResponseEntity<Map<String, Object>> getAvailablePolicies() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "ok");
    result.put("currentStrategy", client.loadBalancerStrategy());
    result.put("availablePolicies", Map.of(
        "ROUND_ROBIN", "Round-robin load balancing (default)",
        "RANDOM", "Random instance selection",
        "WEIGHTED_RANDOM", "Weighted random selection based on instance metadata"
    ));
    return ResponseEntity.ok(result);
  }
}


