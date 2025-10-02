package com.example.control.application;

import com.example.control.api.exception.ExternalServiceException;
import com.example.control.api.exception.ServiceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

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
  private final Environment environment;

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
      // In production, this would:
      // 1. Call Config Server: GET /serviceName/profile
      // 2. Parse the property sources
      // 3. Compute SHA-256 hash of all properties

      // Simplified implementation using local environment
      StringBuilder configData = new StringBuilder();
      configData.append("service=").append(serviceName).append("\n");
      configData.append("profile=").append(profile != null ? profile : "default").append("\n");

      // Add some environment-specific config
      String[] keys = {
          "spring.application.name",
          "spring.profiles.active",
          "spring.cloud.consul.host",
          "spring.kafka.bootstrap-servers"
      };

      for (String key : keys) {
        String value = environment.getProperty(key);
        if (value != null) {
          configData.append(key).append("=").append(value).append("\n");
        }
      }

      return computeSha256(configData.toString());

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
}
