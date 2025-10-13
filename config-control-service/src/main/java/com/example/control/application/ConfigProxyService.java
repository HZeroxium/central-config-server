package com.example.control.application;

import com.example.control.api.exception.ExternalServiceException;
import com.example.control.api.exception.ServiceNotFoundException;
import com.example.control.configsnapshot.ConfigSnapshot;
import com.example.control.configsnapshot.ConfigSnapshotBuilderFromConfigServer;
import com.example.control.configsnapshot.Sha256Hasher;
import com.example.control.config.ConfigServerProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Service that proxies requests to Config Server and provides config-related
 * operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigProxyService {

  private final DiscoveryClient discoveryClient;
  private final ConfigServerProperties configServerProperties;
  private final ObjectMapper objectMapper;
  private final RestClient restClient = RestClient.create();
  private final ConfigSnapshotBuilderFromConfigServer snapshotBuilder = new ConfigSnapshotBuilderFromConfigServer();

  /**
   * Get effective configuration hash for a service in an environment.
   * Fetches config from Config Server and computes SHA-256 hash.
   * 
   * @param serviceName service name
   * @param profile     environment profile (e.g., dev, prod)
   * @return SHA-256 hash of effective configuration
   */
  @Cacheable(value = "config-hashes", key = "#serviceName + ':' + #profile")
  public String getEffectiveConfigHash(String serviceName, String profile) {
    if (serviceName == null || serviceName.trim().isEmpty()) {
      throw new IllegalArgumentException("Service name cannot be null or empty");
    }

    try {
      log.debug("Fetching effective config from Config Server for {}:{}", serviceName, profile);
      
      // Call Config Server: GET /serviceName/profile
      String configUrl = normalizeUrl(configServerProperties.getUrl()) + "/" + serviceName + "/" + 
                        (profile != null && !profile.trim().isEmpty() ? profile : "default");
      
      String configJson = restClient.get()
          .uri(configUrl)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
      
      if (configJson == null || configJson.trim().isEmpty()) {
        log.warn("Empty config response from Config Server for {}:{}", serviceName, profile);
        return null;
      }
      
      JsonNode configNode = objectMapper.readTree(configJson);
      ConfigSnapshot snapshot = snapshotBuilder.build(serviceName, profile, null, configNode);
      String hash = Sha256Hasher.hash(snapshot.toCanonicalString());
      log.debug("Computed config hash for {}:{} keys={} hash={}", serviceName, profile,
          snapshot.getProperties().size(), hash);
      return hash;

    } catch (Exception e) {
      log.error("Failed to compute effective config hash for {}:{}", serviceName, profile, e);
      throw new ExternalServiceException("config-server",
          "Failed to compute effective config hash: " + e.getMessage(), e);
    }
  }

  /**
   * Get configuration difference between expected (SoT) and applied.
   * 
   * @param serviceName service name
   * @param profile     environment profile
   * @param appliedHash currently applied config hash
   * @return map with drift status and hashes
   */
  public Map<String, Object> getConfigDiff(String serviceName, String profile, String appliedHash) {
    String expectedHash = getEffectiveConfigHash(serviceName, profile);

    boolean hasDrift = expectedHash != null &&
        appliedHash != null &&
        !expectedHash.equals(appliedHash);

    return Map.of(
        "serviceName", serviceName,
        "profile", profile != null ? profile : "default",
        "expectedHash", expectedHash != null ? expectedHash : "unknown",
        "appliedHash", appliedHash != null ? appliedHash : "unknown",
        "hasDrift", hasDrift);
  }

  /**
   * Get list of healthy instances for a service from Consul.
   * 
   * @param serviceName service name
   * @return list of service instances
   */
  @Cacheable(value = "consul-services", key = "#serviceName")
  public List<ServiceInstance> getServiceInstances(String serviceName) {
    if (serviceName == null || serviceName.trim().isEmpty()) {
      throw new IllegalArgumentException("Service name cannot be null or empty");
    }

    try {
      List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
      if (instances == null || instances.isEmpty()) {
        throw new ServiceNotFoundException(serviceName);
      }
      return instances;
    } catch (ServiceNotFoundException e) {
      throw e; // Re-throw service not found exceptions
    } catch (Exception e) {
      log.error("Failed to get instances for service: {}", serviceName, e);
      throw new ExternalServiceException("consul",
          "Failed to get service instances: " + e.getMessage(), e);
    }
  }

  /**
   * Get list of all registered services from Consul.
   * 
   * @return list of service names
   */
  @Cacheable(value = "consul-services", key = "'all'")
  public List<String> getAllServices() {
    try {
      return discoveryClient.getServices();
    } catch (Exception e) {
      log.error("Failed to get service list", e);
      throw new ExternalServiceException("consul",
          "Failed to get service list: " + e.getMessage(), e);
    }
  }

  /**
   * Trigger config refresh via Config Server's /busrefresh endpoint.
   * This uses Spring Cloud Bus to broadcast refresh events.
   * 
   * @param destination optional destination pattern (service:instance or service:** for all)
   * @return response from Config Server
   */
  public String triggerBusRefresh(String destination) {
    try {
      log.info("Triggering bus refresh via Config Server for destination: {}", destination);
      
      String base = normalizeUrl(configServerProperties.getUrl()) + "/actuator/busrefresh";
      // String busRefreshUrl = (destination != null && !destination.trim().isEmpty())
      //     ? base + "/" + destination
      //     : base;

      String busRefreshUrl = base;
      
      String response;
      response = restClient.post()
          .uri(busRefreshUrl)
          .contentType(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
      
      log.info("Bus refresh triggered successfully for destination: {}", destination);
      return response;
      
    } catch (Exception e) {
      log.error("Failed to trigger bus refresh for destination: {}", destination, e);
      throw new ExternalServiceException("config-server",
          "Failed to trigger bus refresh: " + e.getMessage(), e);
    }
  }

  private String normalizeUrl(String url) {
    if (url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    }
    return url;
  }
}
