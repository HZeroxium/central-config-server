package com.example.control.infrastructure.config.misc;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration properties for async execution thread pools.
 * <p>
 * Binds to {@code app.async.*} properties in application.yml and provides
 * type-safe access to async configuration with validation.
 * </p>
 *
 * <p>
 * Example configuration:
 *
 * <pre>
 * app:
 *   async:
 *     notification:
 *       core-pool-size: 4
 *       max-pool-size: 8
 *       queue-capacity: 100
 *       keep-alive-seconds: 60
 *       thread-name-prefix: async-notify-
 *     rpc:
 *       core-pool-size: 1
 *       max-pool-size: 2
 *       queue-capacity: 10
 *       keep-alive-seconds: 30
 *       thread-name-prefix: async-rpc-
 *     default:
 *       core-pool-size: 8
 *       max-pool-size: 16
 *       queue-capacity: 200
 *       keep-alive-seconds: 60
 *       thread-name-prefix: async-default-
 *     shutdown:
 *       wait-timeout-seconds: 30
 *       force-shutdown: false
 * </pre>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.async")
public class AsyncProperties {

  /**
   * Notification executor pool configuration.
   * Used for email notifications (I/O bound, moderate throughput).
   */
  @NotNull(message = "Notification pool configuration is required")
  private PoolProps notification = new PoolProps();

  /**
   * RPC executor pool configuration.
   * Used for RPC server startup (rare, minimal concurrency).
   */
  @NotNull(message = "RPC pool configuration is required")
  private PoolProps rpc = new PoolProps();

  /**
   * Shutdown configuration for all async executors.
   */
  @NotNull(message = "Shutdown configuration is required")
  private ShutdownProps shutdown = new ShutdownProps();

  /**
   * Default executor pool configuration.
   * Fallback for other async operations.
   * Spring Boot maps YAML key "default" to this via getter/setter.
   */
  @NotNull(message = "Default pool configuration is required")
  private PoolProps defaultPool = new PoolProps();

  /**
   * Config hash fetch executor pool configuration.
   * Used for parallel fetching of configuration hashes from Config Server
   * during heartbeat batch processing (I/O bound, controlled concurrency).
   */
  @NotNull(message = "Config hash fetch pool configuration is required")
  private PoolProps configHashFetch = new PoolProps();

  /**
   * Thread pool configuration properties.
   */
  @Data
  public static class PoolProps {
    /**
     * Core pool size.
     * <p>
     * Note: Ignored when {@code useVirtualThreads} is true.
     */
    @Min(value = 1, message = "Core pool size must be at least 1")
    private int corePoolSize = 8;

    /**
     * Maximum pool size.
     * <p>
     * Note: Ignored when {@code useVirtualThreads} is true.
     */
    @Min(value = 1, message = "Max pool size must be at least 1")
    private int maxPoolSize = 16;

    /**
     * Queue capacity.
     * <p>
     * Note: Ignored when {@code useVirtualThreads} is true.
     */
    @Min(value = 0, message = "Queue capacity must be non-negative")
    private int queueCapacity = 200;

    /**
     * Keep alive duration.
     * Spring Boot supports binding from various formats (e.g., "60s", "1m",
     * "PT60S").
     * <p>
     * Note: Ignored when {@code useVirtualThreads} is true.
     */
    @NotNull(message = "Keep alive duration is required")
    private Duration keepAlive = Duration.ofSeconds(60);

    /**
     * Thread name prefix.
     */
    @NotBlank(message = "Thread name prefix cannot be blank")
    private String threadNamePrefix = "async-default-";

    /**
     * Whether to use virtual threads (Java 21+).
     * <p>
     * When true, uses {@code Executors.newVirtualThreadPerTaskExecutor()} instead
     * of {@code ThreadPoolTaskExecutor}.
     * Recommended for I/O-bound tasks (e.g., email notifications).
     * <p>
     * Default: false (uses platform threads for backward compatibility).
     */
    private boolean useVirtualThreads = false;
  }

  /**
   * Shutdown configuration properties.
   */
  @Data
  public static class ShutdownProps {
    /**
     * Wait timeout duration for graceful shutdown.
     * Spring Boot supports binding from various formats (e.g., "30s", "1m",
     * "PT30S").
     */
    @NotNull(message = "Wait timeout duration is required")
    private Duration awaitTimeout = Duration.ofSeconds(30);

    /**
     * Whether to force shutdown if timeout is exceeded.
     * When true, executor will forcefully terminate threads after awaitTimeout.
     */
    private boolean forceShutdown = false;
  }

  /**
   * Get keep alive duration for notification executor.
   *
   * @return Duration
   */
  public Duration getNotificationKeepAlive() {
    return notification.getKeepAlive();
  }

  /**
   * Get keep alive duration for RPC executor.
   *
   * @return Duration
   */
  public Duration getRpcKeepAlive() {
    return rpc.getKeepAlive();
  }

  /**
   * Get keep alive duration for default executor.
   *
   * @return Duration
   */
  public Duration getDefaultKeepAlive() {
    return defaultPool.getKeepAlive();
  }

  /**
   * Get keep alive duration for config hash fetch executor.
   *
   * @return Duration
   */
  public Duration getConfigHashFetchKeepAlive() {
    return configHashFetch.getKeepAlive();
  }

  /**
   * Get shutdown wait timeout duration.
   *
   * @return Duration
   */
  public Duration getShutdownWaitTimeout() {
    return shutdown.getAwaitTimeout();
  }

  /**
   * Get shutdown force shutdown flag.
   *
   * @return true if force shutdown is enabled
   */
  public boolean isForceShutdown() {
    return shutdown.isForceShutdown();
  }

  /**
   * Getter for default pool configuration.
   * Spring Boot maps YAML key "default" to this getter via relaxed binding.
   *
   * @return the default pool configuration
   */
  public PoolProps getDefault() {
    return this.defaultPool;
  }

  /**
   * Setter for default pool configuration.
   * Spring Boot maps YAML key "default" to this setter via relaxed binding.
   *
   * @param defaultPool the default pool configuration
   */
  public void setDefault(PoolProps defaultPool) {
    this.defaultPool = defaultPool;
  }

  /**
   * Getter for config hash fetch pool configuration.
   * Spring Boot maps YAML key "config-hash-fetch" to this getter via relaxed binding.
   *
   * @return the config hash fetch pool configuration
   */
  public PoolProps getConfigHashFetch() {
    return this.configHashFetch;
  }

  /**
   * Setter for config hash fetch pool configuration.
   * Spring Boot maps YAML key "config-hash-fetch" to this setter via relaxed binding.
   *
   * @param configHashFetch the config hash fetch pool configuration
   */
  public void setConfigHashFetch(PoolProps configHashFetch) {
    this.configHashFetch = configHashFetch;
  }
}
