package com.example.rest.config.cache;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Unified cache configuration properties.
 * Supports multiple cache providers with fallback mechanisms.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

  /**
   * Cache provider type - can be switched at runtime
   */
  @NotNull
  private CacheProvider provider = CacheProvider.CAFFEINE;

  /**
   * Enable automatic fallback when primary cache provider fails
   */
  private boolean enableFallback = true;

  /**
   * Caffeine cache configuration
   */
  private CaffeineConfig caffeine = new CaffeineConfig();

  /**
   * Redis cache configuration
   */
  private RedisConfig redis = new RedisConfig();

  /**
   * Two-level cache configuration
   */
  private TwoLevelConfig twoLevel = new TwoLevelConfig();

  /**
   * Per-cache configurations
   */
  private Map<String, CacheConfig> caches = new HashMap<>();

  public enum CacheProvider {
    CAFFEINE, REDIS, TWO_LEVEL, NOOP
  }

  @Data
  public static class CaffeineConfig {
    @Min(1)
    private long maximumSize = 10_000L;

    @NotNull
    private Duration expireAfterWrite = Duration.ofMinutes(10);

    @NotNull
    private Duration expireAfterAccess = Duration.ofMinutes(30);

    private boolean recordStats = true;
  }

  @Data
  public static class RedisConfig {
    @NotNull
    private Duration defaultTtl = Duration.ofMinutes(10);

    private boolean enableStatistics = true;

    private boolean transactionAware = true;

    /**
     * Fallback to Caffeine when Redis is unavailable
     */
    private boolean fallbackToCaffeine = true;

    /**
     * Circuit breaker configuration for Redis
     */
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
  }

  @Data
  public static class TwoLevelConfig {
    /**
     * L1 cache (local) configuration
     */
    private CaffeineConfig l1 = new CaffeineConfig();

    /**
     * L2 cache (distributed) configuration
     */
    private RedisConfig l2 = new RedisConfig();

    /**
     * Whether to write-through to L2 on L1 misses
     */
    private boolean writeThrough = true;

    /**
     * Whether to invalidate L1 when L2 is updated
     */
    private boolean invalidateL1OnL2Update = true;
  }

  @Data
  public static class CircuitBreakerConfig {
    private boolean enabled = true;

    @Min(1)
    private int slidingWindowSize = 10;

    @Min(1)
    private int minimumNumberOfCalls = 5;

    @Min(1)
    private float failureRateThreshold = 50.0f;

    @NotNull
    private Duration waitDurationInOpenState = Duration.ofSeconds(30);

    @Min(1)
    private int permittedNumberOfCallsInHalfOpenState = 3;
  }

  @Data
  public static class CacheConfig {
    @NotNull
    private Duration ttl = Duration.ofMinutes(10);

    @Min(1)
    private long maximumSize = 1_000L;

    private boolean allowNullValues = false;

    /**
     * Cache-specific provider override
     */
    private CacheProvider providerOverride;
  }

  // Initialize default cache configurations
  public CacheProperties() {
    caches.put("userById", createUserByIdConfig());
    caches.put("usersByCriteria", createUsersByCriteriaConfig());
    caches.put("countByCriteria", createCountByCriteriaConfig());
  }

  private CacheConfig createUserByIdConfig() {
    CacheConfig config = new CacheConfig();
    config.setTtl(Duration.ofMinutes(15));
    config.setMaximumSize(5_000L);
    return config;
  }

  private CacheConfig createUsersByCriteriaConfig() {
    CacheConfig config = new CacheConfig();
    config.setTtl(Duration.ofMinutes(2));
    config.setMaximumSize(1_000L);
    return config;
  }

  private CacheConfig createCountByCriteriaConfig() {
    CacheConfig config = new CacheConfig();
    config.setTtl(Duration.ofMinutes(1));
    config.setMaximumSize(500L);
    return config;
  }
}
