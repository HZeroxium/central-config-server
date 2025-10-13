package com.example.control.kv.etcd;

import io.etcd.jetcd.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * Wrapper for etcd jetcd clients providing unified access to all etcd services.
 * Manages client lifecycle and provides convenient access to KV, Lease, Watch, and Lock clients.
 */
@Slf4j
@Getter
public final class EtcdClients implements AutoCloseable {
  
  private final Client client;
  private final KV kv;
  private final Lease lease;
  private final Watch watch;
  private final Lock lock;

  /**
   * Create etcd clients with the specified endpoints and connection timeout.
   * 
   * @param endpoints list of etcd server endpoints
   * @param connectTimeout connection timeout
   */
  public EtcdClients(List<URI> endpoints, Duration connectTimeout) {
    log.info("Creating etcd clients with endpoints: {}", endpoints);
    
    String[] endpointStrings = endpoints.stream()
        .map(URI::toString)
        .toArray(String[]::new);
    
    this.client = Client.builder()
        .endpoints(endpointStrings)
        .connectTimeout(connectTimeout)
        .build();
    
    this.kv = client.getKVClient();
    this.lease = client.getLeaseClient();
    this.watch = client.getWatchClient();
    this.lock = client.getLockClient();
    
    log.debug("etcd clients created successfully");
  }

  /**
   * Close all etcd clients and release resources.
   */
  @Override 
  public void close() {
    try {
      log.debug("Closing etcd clients");
      client.close();
      log.info("etcd clients closed successfully");
    } catch (Exception e) {
      log.error("Error closing etcd clients", e);
    }
  }
  
  /**
   * Check if the etcd client is connected and healthy.
   * 
   * @return true if connected, false otherwise
   */
  public boolean isHealthy() {
    try {
      // Simple health check by getting cluster info
      client.getClusterClient().listMember().get();
      return true;
    } catch (Exception e) {
      log.debug("etcd health check failed", e);
      return false;
    }
  }
}
