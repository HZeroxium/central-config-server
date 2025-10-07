package com.example.control.application;

import com.example.control.api.exception.ExternalServiceException;
import com.example.control.api.exception.ServiceNotFoundException;
import com.example.control.config.ConfigServerProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

  /**
   * Get effective configuration hash for a service in an environment.
   * Fetches config from Config Server and computes SHA-256 hash.
   * 
   * @param serviceName service name
   * @param profile     environment profile (e.g., dev, prod)
   * @return SHA-256 hash of effective configuration
   */
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
      
      // Parse JSON response and extract property sources
      JsonNode configNode = objectMapper.readTree(configJson);
      StringBuilder configData = new StringBuilder();
      
      // Add metadata
      configData.append("service=").append(serviceName).append("\n");
      configData.append("profile=").append(profile != null ? profile : "default").append("\n");
      
      // Add version if available
      JsonNode versionNode = configNode.get("version");
      if (versionNode != null && !versionNode.isNull()) {
        configData.append("version=").append(versionNode.asText()).append("\n");
      }
      
      // Extract and hash all property sources
      JsonNode propertySourcesNode = configNode.get("propertySources");
      if (propertySourcesNode != null && propertySourcesNode.isArray()) {
        for (JsonNode propertySource : propertySourcesNode) {
          JsonNode nameNode = propertySource.get("name");
          JsonNode sourceNode = propertySource.get("source");
          
          if (nameNode != null && !nameNode.isNull()) {
            configData.append("source=").append(nameNode.asText()).append("\n");
          }
          
          if (sourceNode != null && sourceNode.isObject()) {
            sourceNode.fieldNames().forEachRemaining(key -> {
              JsonNode valueNode = sourceNode.get(key);
              if (valueNode != null && !valueNode.isNull()) {
                configData.append(key).append("=").append(valueNode.asText()).append("\n");
              }
            });
          }
        }
      }
      
      String hash = computeSha256(configData.toString());
      log.debug("Computed config hash for {}:{} = {}", serviceName, profile, hash);
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
      
      String busRefreshUrl = normalizeUrl(configServerProperties.getUrl()) + "/actuator/busrefresh";
      
      String response;
      if (destination != null && !destination.trim().isEmpty()) {
        // Refresh specific destination
        busRefreshUrl += "/" + destination;
        response = restClient.post()
            .uri(busRefreshUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(String.class);
      } else {
        // Refresh all services
        response = restClient.post()
            .uri(busRefreshUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(String.class);
      }
      
      log.info("Bus refresh triggered successfully for destination: {}", destination);
      return response;
      
    } catch (Exception e) {
      log.error("Failed to trigger bus refresh for destination: {}", destination, e);
      throw new ExternalServiceException("config-server",
          "Failed to trigger bus refresh: " + e.getMessage(), e);
    }
  }

  private String computeSha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1)
          hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      log.error("Failed to compute SHA-256", e);
      return null;
    }
  }

  private String normalizeUrl(String url) {
    if (url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    }
    return url;
  }
}
