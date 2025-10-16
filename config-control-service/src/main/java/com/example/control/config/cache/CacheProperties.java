package com.example.control.config.cache;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized cache configuration properties for the {@code config-control-service}.
 * <p>
 * These properties are bound from external configuration (e.g., {@code application.yml})
 * under the prefix {@code app.cache}. The class is validated at bind time via Bean Validation
 * constraints. See Spring Boot {@code @ConfigurationProperties} for hierarchical binding
 * semantics and metadata generation.
 *
 * <h2>Purpose</h2>
 * <ul>
 *   <li>Provide a unified place to choose the default cache provider (Caffeine, Redis, Two-level, Noop).</li>
 *   <li>Offer fine-grained, per-cache overrides (TTL, maximum size, null handling, provider override).</li>
 *   <li>Support operational safeguards (fallback from Redis to Caffeine, circuit breaker)</li>
 *   <li>Enable a Two-level (L1/L2) topology: local in-memory + distributed Redis.</li>
 * </ul>
 *
 * <h2>Binding Example (YAML)</h2>
 * <pre>{@code
 * app:
 *   cache:
 *     provider: REDIS        # CAFFEINE | REDIS | TWO_LEVEL | NOOP
 *     enableFallback: true
 *     caffeine:
 *       maximumSize: 10000
 *       expireAfterWrite: 10m
 *       expireAfterAccess: 30m
 *       recordStats: true
 *     redis:
 *       defaultTtl: 10m
 *       enableStatistics: true
 *       transactionAware: true
 *       fallbackToCaffeine: true
 *       circuitBreaker:
 *         enabled: true
 *         slidingWindowSize: 10
 *         minimumNumberOfCalls: 5
 *         failureRateThreshold: 50.0
 *         waitDurationInOpenState: 30s
 *         permittedNumberOfCallsInHalfOpenState: 3
 *     twoLevel:
 *       writeThrough: true
 *       invalidateL1OnL2Update: true
 *     caches:
 *       service-instances:
 *         ttl: 5m
 *         maximumSize: 10000
 *       drift-events:
 *         ttl: 2m
 *         maximumSize: 5000
 *       config-hashes:
 *         ttl: 10m
 *         maximumSize: 1000
 *       consul-services:
 *         ttl: 1m
 *         maximumSize: 500
 *       consul-health:
 *         ttl: 30s
 *         maximumSize: 1000
 * }</pre>
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li><b>TTL semantics:</b> {@code ttl} and Caffeine's {@code expireAfter*} evict entries after the
 *       configured durations; Redis uses key expiration. Eviction is best-effort.</li>
 *   <li><b>Caffeine:</b> {@code maximumSize}, {@code expireAfterWrite}, {@code expireAfterAccess}
 *       control in-memory eviction behavior and statistics recording.</li>
 *   <li><b>Redis:</b> {@code enableStatistics} toggles local hit/miss stats collection,
 *       {@code transactionAware} makes cache put/evict operations participate in Spring transactions
 *       (where applicable).</li>
 *   <li><b>CircuitBreaker:</b> Parameters mirror common Resilience4j concepts (sliding window,
 *       failure-rate threshold, wait duration in OPEN, and trial calls in HALF_OPEN).</li>
 *   <li><b>Two-level:</b> L1 (Caffeine) for ultra-low latency; L2 (Redis) for distribution. {@code writeThrough}
 *       and {@code invalidateL1OnL2Update} define write/invalidations across tiers.</li>
 * </ul>
 *
 * @author
 * @since 1.0.0
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

  /**
   * Default cache provider to use for all caches that do not specify a per-cache override.
   * <p>
   * Typical choices:
   * <ul>
   *   <li>{@link CacheProvider#CAFFEINE}: in-memory cache with O(1) latency and local scope.</li>
   *   <li>{@link CacheProvider#REDIS}: distributed cache backed by Redis; suitable for sharing across nodes.</li>
   *   <li>{@link CacheProvider#TWO_LEVEL}: hybrid model (L1 = Caffeine, L2 = Redis).</li>
   *   <li>{@link CacheProvider#NOOP}: disabled caching (useful for troubleshooting or testing).</li>
   * </ul>
   */
  @NotNull
  private CacheProvider provider = CacheProvider.CAFFEINE;

  /**
   * When {@code true}, allows the system to fall back to a secondary provider (e.g., Caffeine)
   * if the primary provider (e.g., Redis) becomes unavailable. This can improve resilience at the
   * cost of temporarily reduced consistency across instances.
   */
  private boolean enableFallback = true;

  /**
   * Global defaults for the Caffeine (local in-memory) cache provider.
   * <p>
   * Key concepts:
   * <ul>
   *   <li>{@code maximumSize}: upper bound on cache entries (evicts using size-based policy).</li>
   *   <li>{@code expireAfterWrite}: time-to-live measured from last write.</li>
   *   <li>{@code expireAfterAccess}: time-to-idle measured from last read or write.</li>
   *   <li>{@code recordStats}: enable local hit/miss/eviction metrics.</li>
   * </ul>
   */
  private CaffeineConfig caffeine = new CaffeineConfig();

  /**
   * Global defaults for the Redis (distributed) cache provider.
   * <p>
   * Key concepts:
   * <ul>
   *   <li>{@code defaultTtl}: key expiration at Redis side.</li>
   *   <li>{@code enableStatistics}: collect local cache statistics snapshot if supported by manager.</li>
   *   <li>{@code transactionAware}: align cache operations with Spring-managed transactions.</li>
   *   <li>{@code fallbackToCaffeine}: optionally degrade to local cache when Redis is unavailable.</li>
   *   <li>{@code circuitBreaker}: protect downstream Redis with a circuit breaker to avoid cascading failures.</li>
   * </ul>
   */
  private RedisConfig redis = new RedisConfig();

  /**
   * Global defaults for a Two-level cache: L1 = local Caffeine, L2 = Redis.
   * <p>
   * Use this when you want ultra-fast reads from L1 while maintaining cross-node coherence via L2.
   * {@code writeThrough} and {@code invalidateL1OnL2Update} define how writes/invalidations propagate.
   */
  private TwoLevelConfig twoLevel = new TwoLevelConfig();

  /**
   * Per-cache overrides keyed by logical cache name (e.g., {@code service-instances}, {@code drift-events}).
   * <p>
   * Each entry can override TTL, maximum size, null handling, and even select a different provider
   * than the default {@link #provider}.
   */
  private Map<String, CacheConfig> caches = new HashMap<>();

  /**
   * Supported cache provider types.
   */
  public enum CacheProvider {
    /** Local in-memory cache using Caffeine. */
    CAFFEINE,
    /** Distributed cache using Redis. */
    REDIS,
    /** Hybrid topology with L1 (Caffeine) and L2 (Redis). */
    TWO_LEVEL,
    /** No-operation provider that disables caching. */
    NOOP
  }

  /**
   * Caffeine provider defaults.
   */
  @Data
  public static class CaffeineConfig {
    /**
     * Maximum number of entries to keep in the local cache before evicting by size.
     * Must be {@code >= 1}.
     */
    @Min(1)
    private long maximumSize = 10_000L;

    /**
     * Time-to-live since last write. Entries are eligible for eviction after this duration.
     * Must not be {@code null}.
     */
    @NotNull
    private Duration expireAfterWrite = Duration.ofMinutes(10);

    /**
     * Time-to-idle since last access (read or write). Entries are eligible for eviction after this duration.
     * Must not be {@code null}.
     */
    @NotNull
    private Duration expireAfterAccess = Duration.ofMinutes(30);

    /**
     * Whether to record local cache statistics (hits, misses, evictions).
     */
    private boolean recordStats = true;
  }

  /**
   * Redis provider defaults.
   */
  @Data
  public static class RedisConfig {
    /**
     * Default time-to-live for cache entries stored in Redis (key expiration).
     * Must not be {@code null}.
     */
    @NotNull
    private Duration defaultTtl = Duration.ofMinutes(10);

    /**
     * Enable local statistics collection if supported by the {@code RedisCacheManager}.
     * This does not add server-side Redis metrics; it exposes manager-level snapshots.
     */
    private boolean enableStatistics = true;

    /**
     * Make cache updates/evictions transaction-aware (participate in Spring-managed transactions).
     */
    private boolean transactionAware = true;

    /**
     * If {@code true}, transparently fall back to Caffeine when Redis is unavailable.
     * Useful for degraded-mode operation to preserve read paths.
     */
    private boolean fallbackToCaffeine = true;

    /**
     * Circuit breaker configuration that guards Redis operations to prevent cascading failures.
     */
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
  }

  /**
   * Two-level cache topology defaults (L1 = Caffeine, L2 = Redis).
   */
  @Data
  public static class TwoLevelConfig {
    /**
     * L1 cache (local, per-JVM) configuration.
     */
    private CaffeineConfig l1 = new CaffeineConfig();

    /**
     * L2 cache (distributed) configuration.
     */
    private RedisConfig l2 = new RedisConfig();

    /**
     * If {@code true}, writes propagate to L2 when L1 misses occur (helps warm L2 proactively).
     */
    private boolean writeThrough = true;

    /**
     * If {@code true}, invalidates L1 entries when L2 is updated (helps keep L1 coherent with L2).
     */
    private boolean invalidateL1OnL2Update = true;
  }

  /**
   * Circuit breaker parameters for guarding Redis (or other remote) interactions.
   * <p>
   * The model mirrors common patterns (e.g., Resilience4j): a sliding window records outcomes,
   * the breaker opens when the failure rate exceeds a threshold, stays OPEN for a wait duration,
   * then transitions to HALF_OPEN to trial a limited number of calls.
   */
  @Data
  public static class CircuitBreakerConfig {
    /**
     * Enable/disable the circuit breaker.
     */
    private boolean enabled = true;

    /**
     * Size of the sliding window (number of recorded calls). Must be {@code >= 1}.
     */
    @Min(1)
    private int slidingWindowSize = 10;

    /**
     * Minimum number of calls required in the current window before evaluating failure rate.
     * Must be {@code >= 1}.
     */
    @Min(1)
    private int minimumNumberOfCalls = 5;

    /**
     * Failure rate threshold (percentage) that triggers OPEN state when exceeded. Must be {@code >= 1}.
     */
    @Min(1)
    private float failureRateThreshold = 50.0f;

    /**
     * Duration to remain in OPEN state before attempting HALF_OPEN trials. Must not be {@code null}.
     */
    @NotNull
    private Duration waitDurationInOpenState = Duration.ofSeconds(30);

    /**
     * Number of permitted trial calls in HALF_OPEN state before deciding to close or reopen the circuit.
     * Must be {@code >= 1}.
     */
    @Min(1)
    private int permittedNumberOfCallsInHalfOpenState = 3;
  }

  /**
   * Per-cache override configuration.
   */
  @Data
  public static class CacheConfig {
    /**
     * Cache entry time-to-live. When elapsed, the entry becomes eligible for eviction.
     * Must not be {@code null}.
     */
    @NotNull
    private Duration ttl = Duration.ofMinutes(10);

    /**
     * Per-cache maximum number of entries before size-based eviction. Must be {@code >= 1}.
     */
    @Min(1)
    private long maximumSize = 1_000L;

    /**
     * Whether to allow {@code null} values to be stored for this cache.
     * Note: some {@code CacheManager} implementations may wrap nulls; others may forbid them.
     */
    private boolean allowNullValues = false;

    /**
     * Optional provider override for this specific cache, taking precedence over the global {@link #provider}.
     */
    private CacheProvider providerOverride;
  }

  private String cacheNamePrefix = "config-control-service::";

  /**
   * Initializes default per-cache configurations tailored for the {@code config-control-service}.
   * <p>
   * These defaults are sensible starting points and can be overridden via external configuration.
   */
  public CacheProperties() {
    caches.put("service-instances", createServiceInstancesConfig());
    caches.put("drift-events", createDriftEventsConfig());
    caches.put("config-hashes", createConfigHashesConfig());
    caches.put("consul-services", createConsulServicesConfig());
    caches.put("consul-health", createConsulHealthConfig());
  }

  /**
   * Build default config for the {@code service-instances} cache.
   * <ul>
   *   <li>TTL: 5 minutes — instance membership typically changes infrequently but should refresh regularly.</li>
   *   <li>Maximum size: 10,000 — supports large service topologies.</li>
   * </ul>
   */
  private CacheConfig createServiceInstancesConfig() {
    CacheConfig config = new CacheConfig();
    config.setTtl(Duration.ofMinutes(5));
    config.setMaximumSize(10_000L);
    return config;
  }

  /**
   * Build default config for the {@code drift-events} cache.
   * <ul>
   *   <li>TTL: 2 minutes — short-lived signals to avoid stale drift detection.</li>
   *   <li>Maximum size: 5,000 — accommodates bursty events.</li>
   * </ul>
   */
  private CacheConfig createDriftEventsConfig() {
    CacheConfig config = new CacheConfig();
    config.setTtl(Duration.ofMinutes(2));
    config.setMaximumSize(5_000L);
    return config;
  }

  /**
   * Build default config for the {@code config-hashes} cache.
   * <ul>
   *   <li>TTL: 10 minutes — configuration digests do not change rapidly under normal conditions.</li>
   *   <li>Maximum size: 1,000 — per-service hash footprint.</li>
   * </ul>
   */
  private CacheConfig createConfigHashesConfig() {
    CacheConfig config = new CacheConfig();
    config.setTtl(Duration.ofMinutes(10));
    config.setMaximumSize(1_000L);
    return config;
  }

  /**
   * Build default config for the {@code consul-services} cache.
   * <ul>
   *   <li>TTL: 1 minute — service catalog should refresh frequently.</li>
   *   <li>Maximum size: 500 — typical number of registered services.</li>
   * </ul>
   */
  private CacheConfig createConsulServicesConfig() {
    CacheConfig config = new CacheConfig();
    config.setTtl(Duration.ofMinutes(1));
    config.setMaximumSize(500L);
    return config;
  }

  /**
   * Build default config for the {@code consul-health} cache.
   * <ul>
   *   <li>TTL: 30 seconds — health states change more frequently and should be fresher.</li>
   *   <li>Maximum size: 1,000 — accommodate multiple checks per service.</li>
   * </ul>
   */
  private CacheConfig createConsulHealthConfig() {
    CacheConfig config = new CacheConfig();
    config.setTtl(Duration.ofSeconds(30));
    config.setMaximumSize(1_000L);
    return config;
  }
}
