package com.example.control.infrastructure.config.misc.restclient;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for RestClient timeout settings.
 * <p>
 * Maps properties from {@code rest-client.*} in application.yml.
 * Supports default timeouts and per-client overrides.
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "rest-client")
public class RestClientProperties {

  /**
   * Default connection timeout.
   */
  private Duration connectTimeout = Duration.ofSeconds(5);

  /**
   * Default read timeout.
   */
  private Duration readTimeout = Duration.ofSeconds(10);

  /**
   * Default write timeout.
   */
  private Duration writeTimeout = Duration.ofSeconds(10);

  /**
   * Per-client timeout overrides.
   */
  private Map<String, ClientTimeout> clients = new HashMap<>();

  @Data
  public static class ClientTimeout {
    /**
     * Connection timeout override.
     */
    private Duration connectTimeout;

    /**
     * Read timeout override.
     */
    private Duration readTimeout;

    /**
     * Write timeout override.
     */
    private Duration writeTimeout;
  }
}
