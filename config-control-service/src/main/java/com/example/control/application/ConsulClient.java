package com.example.control.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for interacting with Consul HTTP API
 */
@Slf4j
@Component("customConsulClient")
@RequiredArgsConstructor
public class ConsulClient {

  private final RestClient restClient = RestClient.create();

  @Value("${consul.url:http://localhost:8500}")
  private String consulUrl;

  /**
   * Get all services from Consul catalog
   */
  public String getServices() {
    String url = consulUrl + "/v1/catalog/services";
    log.debug("Getting services from: {}", url);
    try {
      return restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("Failed to get services from Consul", e);
      throw new RuntimeException("Consul API call failed: getServices", e);
    }
  }

  /**
   * Get service details by name
   */
  public String getService(String serviceName) {
    String url = consulUrl + "/v1/catalog/service/" + serviceName;
    log.debug("Getting service details from: {}", url);
    try {
      return restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("Failed to get service details for: {}", serviceName, e);
      throw new RuntimeException("Consul API call failed: getService", e);
    }
  }

  /**
   * Get all instances of a service (including unhealthy)
   */
  public String getServiceInstances(String serviceName) {
    String url = consulUrl + "/v1/health/service/" + serviceName;
    log.debug("Getting service instances from: {}", url);
    try {
      return restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("Failed to get service instances for: {}", serviceName, e);
      throw new RuntimeException("Consul API call failed: getServiceInstances", e);
    }
  }

  /**
   * Get only healthy instances of a service
   */
  public String getHealthyServiceInstances(String serviceName) {
    String url = consulUrl + "/v1/health/service/" + serviceName + "?passing";
    log.debug("Getting healthy service instances from: {}", url);
    try {
      return restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("Failed to get healthy service instances for: {}", serviceName, e);
      throw new RuntimeException("Consul API call failed: getHealthyServiceInstances", e);
    }
  }

  /**
   * Get health checks for a service
   */
  public String getServiceHealth(String serviceName) {
    String url = consulUrl + "/v1/health/checks/" + serviceName;
    log.debug("Getting service health from: {}", url);
    try {
      return restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("Failed to get service health for: {}", serviceName, e);
      throw new RuntimeException("Consul API call failed: getServiceHealth", e);
    }
  }

  /**
   * Get all nodes in the cluster
   */
  public String getNodes() {
    String url = consulUrl + "/v1/catalog/nodes";
    log.debug("Getting nodes from: {}", url);
    try {
      return restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("Failed to get nodes from Consul", e);
      throw new RuntimeException("Consul API call failed: getNodes", e);
    }
  }

  /**
   * Get value from KV store
   */
  public String getKVValue(String key, boolean recurse) {
    String url = consulUrl + "/v1/kv/" + key;
    if (recurse) {
      url += "?recurse";
    }
    log.debug("Getting KV value from: {}", url);
    try {
      return restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("Failed to get KV value for key: {}", key, e);
      throw new RuntimeException("Consul API call failed: getKVValue", e);
    }
  }

  /**
   * Set value in KV store
   */
  public boolean setKVValue(String key, String value) {
    String url = consulUrl + "/v1/kv/" + key;
    log.debug("Setting KV value at: {}", url);
    try {
      restClient.put()
          .uri(url)
          .contentType(MediaType.TEXT_PLAIN)
          .body(value)
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (Exception e) {
      log.error("Failed to set KV value for key: {}", key, e);
      return false;
    }
  }

  /**
   * Delete value from KV store
   */
  public boolean deleteKVValue(String key) {
    String url = consulUrl + "/v1/kv/" + key;
    log.debug("Deleting KV value at: {}", url);
    try {
      restClient.delete()
          .uri(url)
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (Exception e) {
      log.error("Failed to delete KV value for key: {}", key, e);
      return false;
    }
  }

  /**
   * Get services registered on local agent
   */
  public String getAgentServices() {
    String url = consulUrl + "/v1/agent/services";
    log.debug("Getting agent services from: {}", url);
    try {
      return restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("Failed to get agent services from Consul", e);
      throw new RuntimeException("Consul API call failed: getAgentServices", e);
    }
  }

  /**
   * Get health checks on local agent
   */
  public String getAgentChecks() {
    String url = consulUrl + "/v1/agent/checks";
    log.debug("Getting agent checks from: {}", url);
    try {
      return restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("Failed to get agent checks from Consul", e);
      throw new RuntimeException("Consul API call failed: getAgentChecks", e);
    }
  }

  /**
   * Get cluster members from local agent
   */
  public String getAgentMembers() {
    String url = consulUrl + "/v1/agent/members";
    log.debug("Getting agent members from: {}", url);
    try {
      return restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("Failed to get agent members from Consul", e);
      throw new RuntimeException("Consul API call failed: getAgentMembers", e);
    }
  }

  /**
   * Register a service with the local agent
   */
  public boolean registerService(String serviceJson) {
    String url = consulUrl + "/v1/agent/service/register";
    log.debug("Registering service at: {}", url);
    try {
      restClient.put()
          .uri(url)
          .contentType(MediaType.APPLICATION_JSON)
          .body(serviceJson)
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (Exception e) {
      log.error("Failed to register service", e);
      return false;
    }
  }

  /**
   * Deregister a service from the local agent
   */
  public boolean deregisterService(String serviceId) {
    String url = consulUrl + "/v1/agent/service/deregister/" + serviceId;
    log.debug("Deregistering service at: {}", url);
    try {
      restClient.put()
          .uri(url)
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (Exception e) {
      log.error("Failed to deregister service: {}", serviceId, e);
      return false;
    }
  }

  /**
   * Mark a TTL check as passing
   */
  public boolean passCheck(String checkId) {
    String url = consulUrl + "/v1/agent/check/pass/" + checkId;
    log.debug("Passing check at: {}", url);
    try {
      restClient.put()
          .uri(url)
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (Exception e) {
      log.error("Failed to pass check: {}", checkId, e);
      return false;
    }
  }

  /**
   * Mark a TTL check as failing
   */
  public boolean failCheck(String checkId) {
    String url = consulUrl + "/v1/agent/check/fail/" + checkId;
    log.debug("Failing check at: {}", url);
    try {
      restClient.put()
          .uri(url)
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (Exception e) {
      log.error("Failed to fail check: {}", checkId, e);
      return false;
    }
  }

  /**
   * Get health state (passing, warning, critical)
   */
  public String getHealthState(String state) {
    String url = consulUrl + "/v1/health/state/" + state;
    log.debug("Getting health state from: {}", url);
    try {
      return restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("Failed to get health state: {}", state, e);
      throw new RuntimeException("Consul API call failed: getHealthState", e);
    }
  }
}
