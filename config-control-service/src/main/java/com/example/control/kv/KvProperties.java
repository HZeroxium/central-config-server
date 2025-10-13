package com.example.control.kv;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Configuration properties for KV Store backends.
 * Supports both Consul KV and etcd backends with configurable parameters.
 */
@Data
@ConfigurationProperties(prefix = "kv")
public class KvProperties {
  
  /**
   * Supported KV Store backends.
   */
  public enum Backend { 
    consul, 
    etcd 
  }
  
  /**
   * The backend to use (default: consul).
   */
  private Backend backend = Backend.consul;
  
  /**
   * Consul-specific configuration.
   */
  private Consul consul = new Consul();
  
  /**
   * etcd-specific configuration.
   */
  private Etcd etcd = new Etcd();
  
  /**
   * Consul KV configuration.
   */
  @Data
  public static class Consul {
    /**
     * Consul base URL (default: http://localhost:8500).
     */
    private URI baseUrl = URI.create("http://localhost:8500");
    
    /**
     * Wait time for blocking queries (default: 5 minutes).
     */
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration wait = Duration.ofMinutes(5);
    
    /**
     * Consul ACL token (empty for no authentication).
     */
    private String token = "";
  }
  
  /**
   * etcd configuration.
   */
  @Data
  public static class Etcd {
    /**
     * etcd endpoints (default: http://localhost:2379).
     */
    private List<URI> endpoints = List.of(URI.create("http://localhost:2379"));
    
    /**
     * Connection timeout (default: 5 seconds).
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration connectTimeout = Duration.ofSeconds(5);
    
    /**
     * Keepalive time (default: 10 seconds).
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration keepaliveTime = Duration.ofSeconds(10);
    
    /**
     * Key namespace prefix (empty for no prefix).
     */
    private String namespace = "";
  }
}
