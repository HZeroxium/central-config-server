package com.example.control.infrastructure.config.cache;

import com.example.control.infrastructure.cache.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
// import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Factory for creating {@link CacheManager} instances according to
 * {@link CacheProperties}.
 * <p>
 * <strong>Goals</strong>
 * <ul>
 * <li>Provide a single place to instantiate cache providers (Caffeine, Redis,
 * Two-level, NoOp).</li>
 * <li>Support dynamic provider switching and graceful fallback based on
 * configuration &amp; environment.</li>
 * <li>Apply sensible defaults and per-cache overrides (e.g., TTL/null caching
 * for Redis).</li>
 * </ul>
 *
 * <h2>Providers</h2>
 * <ul>
 * <li><b>Caffeine</b> — in-memory, ultra-low latency. Built via
 * {@link CaffeineCacheManager}.</li>
 * <li><b>Redis</b> — distributed, cross-instance sharing. Built via
 * {@link RedisCacheManager} with {@link RedisCacheConfiguration}.</li>
 * <li><b>Two-level</b> — L1: Caffeine, L2: Redis, composed by
 * {@code TwoLevelCacheManager} for read-through &amp; optional write-through.
 * (See your {@code TwoLevelCacheManager}).</li>
 * <li><b>NoOp</b> — disabled caching for troubleshooting or environments where
 * cache is unavailable.</li>
 * </ul>
 *
 * <h2>Serialization (Redis)</h2>
 * Values are serialized with {@link GenericJackson2JsonRedisSerializer} using a
 * tuned {@link ObjectMapper}:
 * <ul>
 * <li>Registers {@link JavaTimeModule} to properly handle
 * {@code java.time}.</li>
 * <li>Enables guarded polymorphic typing via
 * {@link ObjectMapper#activateDefaultTyping}: required for heterogeneous value
 * types,
 * but should be <em>carefully constrained</em> (see Security notes).</li>
 * </ul>
 *
 * <h2>Security Note (Jackson Typing)</h2>
 * Global default typing can be risky if overbroad; this class uses
 * {@link BasicPolymorphicTypeValidator} but you should restrict base types
 * or packages to your domain model to minimize gadget exposure.
 *
 * <p>
 * Instances returned by this factory comply with Spring's {@link CacheManager}
 * contract
 * (e.g., {@link CacheManager#getCache(String)},
 * {@link CacheManager#getCacheNames()}).
 */
@Slf4j
@Component
public class CacheManagerFactory {

    /**
     * Application-level cache properties, including provider selection, L1/L2
     * settings,
     * and per-cache overrides (e.g., TTL/null handling).
     */
    private final CacheProperties cacheProperties;

    /**
     * Optional Redis connection factory. Absence typically means Redis is not
     * available
     * in the current runtime (local dev/test or degraded environments).
     */
    private final Optional<RedisConnectionFactory> redisConnectionFactory;

    /**
     * Optional invalidation publisher for cross-instance cache invalidation.
     */
    private final Optional<CacheInvalidationPublisher> invalidationPublisher;

    /**
     * Optional cache operation executor for resilience patterns.
     */
    private final Optional<CacheOperationExecutor> cacheOperationExecutor;

    /**
     * Optional cache metrics for tracking L1/L2 hits.
     */
    private final Optional<CacheMetrics> cacheMetrics;

    /**
     * Constructor with invalidation publisher, operation executor, and metrics
     * support.
     */
    public CacheManagerFactory(CacheProperties cacheProperties,
            Optional<RedisConnectionFactory> redisConnectionFactory,
            Optional<CacheInvalidationPublisher> invalidationPublisher,
            Optional<CacheOperationExecutor> cacheOperationExecutor,
            Optional<CacheMetrics> cacheMetrics) {
        this.cacheProperties = cacheProperties;
        this.redisConnectionFactory = redisConnectionFactory;
        this.invalidationPublisher = invalidationPublisher;
        this.cacheOperationExecutor = cacheOperationExecutor;
        this.cacheMetrics = cacheMetrics;
    }

    /**
     * Constructor with invalidation publisher and operation executor support.
     */
    public CacheManagerFactory(CacheProperties cacheProperties,
            Optional<RedisConnectionFactory> redisConnectionFactory,
            Optional<CacheInvalidationPublisher> invalidationPublisher,
            Optional<CacheOperationExecutor> cacheOperationExecutor) {
        this(cacheProperties, redisConnectionFactory, invalidationPublisher, cacheOperationExecutor, Optional.empty());
    }

    /**
     * Constructor with invalidation publisher support.
     */
    public CacheManagerFactory(CacheProperties cacheProperties,
            Optional<RedisConnectionFactory> redisConnectionFactory,
            Optional<CacheInvalidationPublisher> invalidationPublisher) {
        this(cacheProperties, redisConnectionFactory, invalidationPublisher, Optional.empty());
    }

    /**
     * Constructor without invalidation publisher (for backward compatibility).
     */
    public CacheManagerFactory(CacheProperties cacheProperties,
            Optional<RedisConnectionFactory> redisConnectionFactory) {
        this(cacheProperties, redisConnectionFactory, Optional.empty(), Optional.empty());
    }

    /**
     * Create a {@link CacheManager} according to
     * {@link CacheProperties#getProvider()} and
     * the current environment, applying graceful fallback when enabled.
     * <p>
     * <strong>Behavior</strong>
     * <ul>
     * <li>CAFFEINE → {@link #createCaffeineCacheManager()}</li>
     * <li>REDIS → {@link #createRedisCacheManagerWithFallback()}</li>
     * <li>TWO_LEVEL → {@link #createTwoLevelCacheManager()}</li>
     * <li>NOOP → {@link #createNoOpCacheManager()}</li>
     * <li>Unknown → log warning, fall back to Caffeine</li>
     * </ul>
     * If an unexpected error occurs during construction and
     * <code>enableFallback=true</code>,
     * a {@link NoOpCacheManager} is returned; otherwise the exception is
     * propagated.
     *
     * @return a fully initialized {@link CacheManager} (possibly a fallback)
     */
    public CacheManager createCacheManager() {
        CacheProperties.CacheProvider provider = cacheProperties.getProvider();

        try {
            log.info("Creating cache manager for provider: {}", provider);

            switch (provider) {
                case CAFFEINE:
                    return createCaffeineCacheManager();

                case REDIS:
                    return createRedisCacheManagerWithFallback();

                case TWO_LEVEL:
                    return createTwoLevelCacheManager();

                case NOOP:
                    return createNoOpCacheManager();

                default:
                    log.warn("Unknown cache provider: {}, falling back to Caffeine", provider);
                    return createCaffeineCacheManager();
            }
        } catch (Exception e) {
            log.error("Failed to create cache manager for provider: {}, falling back to NoOp", provider, e);
            if (cacheProperties.isEnableFallback()) {
                return createNoOpCacheManager();
            }
            throw e;
        }
    }

    /**
     * Build a {@link CaffeineCacheManager} using global Caffeine policies from
     * {@link CacheProperties.CaffeineConfig}.
     * <p>
     * Applied settings:
     * <ul>
     * <li>{@code maximumSize}</li>
     * <li>{@code expireAfterWrite}</li>
     * <li>{@code expireAfterAccess}</li>
     * <li>{@code recordStats} (optional)</li>
     * <li>Static cache names via {@link CaffeineCacheManager#setCacheNames}</li>
     * </ul>
     * Note: These policies are global for all configured Caffeine caches managed
     * here.
     *
     * @return a configured {@link CaffeineCacheManager}
     */
    public CacheManager createCaffeineCacheManager() {
        log.info("Creating Caffeine cache manager");

        CacheProperties.CaffeineConfig config = cacheProperties.getCaffeine();

        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(config.getMaximumSize())
                .expireAfterWrite(config.getExpireAfterWrite())
                .expireAfterAccess(config.getExpireAfterAccess());

        if (config.isRecordStats()) {
            builder.recordStats();
        }

        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(builder);

        // Configure cache names (static mode).
        manager.setCacheNames(cacheProperties.getCaches().keySet());

        return manager;
    }

    /**
     * Attempt to create a {@link RedisCacheManager}; if Redis is not available or
     * fails to initialize,
     * optionally fall back to Caffeine according to
     * {@link CacheProperties.RedisConfig#isFallbackToCaffeine()}
     * and {@link CacheProperties#isEnableFallback()}.
     *
     * @return a {@link RedisCacheManager} or a fallback {@link CacheManager}
     * @throws IllegalStateException when Redis is unavailable and fallback is
     *                               disabled
     */
    public CacheManager createRedisCacheManagerWithFallback() {
        if (redisConnectionFactory.isEmpty()) {
            log.warn("Redis connection factory not available, falling back to Caffeine");
            if (cacheProperties.isEnableFallback()) {
                return createCaffeineCacheManager();
            }
            throw new IllegalStateException("Redis connection factory not available and fallback disabled");
        }

        log.info("Creating Redis cache manager");

        try {
            return createRedisCacheManager(redisConnectionFactory.get());
        } catch (Exception e) {
            log.error("Failed to create Redis cache manager", e);
            if (cacheProperties.getRedis().isFallbackToCaffeine() && cacheProperties.isEnableFallback()) {
                log.info("Falling back to Caffeine cache manager");
                return createCaffeineCacheManager();
            }
            throw e;
        }
    }

    /**
     * Create a {@link RedisCacheManager} using {@link RedisCacheConfiguration}
     * defaults and per-cache overrides.
     * <p>
     * <strong>Serialization</strong> uses
     * {@link GenericJackson2JsonRedisSerializer} with a custom {@link ObjectMapper}
     * that registers {@link JavaTimeModule} and activates guarded default typing
     * via {@link BasicPolymorphicTypeValidator}.
     * (Required when your cached values are heterogeneous, e.g.,
     * interfaces/abstract classes.)
     * <p>
     * <strong>Per-Cache Overrides</strong>:
     * For each configured cache name in {@link CacheProperties#getCaches()},
     * applies an individual TTL and
     * optional {@code allowNullValues=false}. Other aspects inherit from
     * {@code defaultConfig}.
     *
     * @param connectionFactory the Redis connection factory (must be present)
     * @return a configured {@link RedisCacheManager}
     */
    private CacheManager createRedisCacheManager(RedisConnectionFactory connectionFactory) {
        CacheProperties.RedisConfig config = cacheProperties.getRedis();

        // Configure JSON serialization (values)
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // java.time support
        

        // Ignore unknown properties when deserializing from cache
        // This is essential for cache compatibility when domain models evolve
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        mapper.setConfig(mapper.getDeserializationConfig()
                .without(MapperFeature.USE_GETTERS_AS_SETTERS));
                
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.example.")
                .allowIfSubType("org.springframework.data.domain.")
                .allowIfBaseType(java.util.Collection.class)
                .allowIfBaseType(java.util.Map.class)
                .allowIfBaseType(java.util.Optional.class)
                // Currently 
                .allowIfBaseType(java.lang.Object.class)
                // .allowIfSubType("java.util.*")
                .build();
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        mapper.registerModule(new com.example.control.infrastructure.cache.jackson.PageJacksonModule());

        GenericJackson2JsonRedisSerializer baseSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        // Wrap with compression if enabled
        RedisSerializer<Object> serializer = baseSerializer;
        CacheProperties.CompressionConfig compressionConfig = cacheProperties.getCompression();
        if (compressionConfig.isEnabled()) {
            serializer = new CacheCompressionSerializer<>(baseSerializer,
                    compressionConfig.getThreshold(), compressionConfig.getAlgorithm());
            log.info("Cache compression enabled: threshold={} bytes, algorithm={}",
                    compressionConfig.getThreshold(), compressionConfig.getAlgorithm());
        }

        // Default config (value serializer + default TTL). Key serializer can also be
        // customized if needed.
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer.UTF_8))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(config.getDefaultTtl());

        // Apply service-level cache name prefix for Redis
        String prefix = cacheProperties.getCacheNamePrefix();
        if (prefix != null && !prefix.isBlank()) {
            defaultConfig.prefixCacheNameWith(prefix);
        }

        // Configure per-cache settings
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheProperties.getCaches().forEach((cacheName, cacheConfig) -> {
            RedisCacheConfiguration specificConfig = defaultConfig.entryTtl(cacheConfig.getTtl());
            if (!cacheConfig.isAllowNullValues()) {
                specificConfig = specificConfig.disableCachingNullValues();
            }

            // Apply per-cache compression if configured
            if (cacheConfig.getCompression() != null && cacheConfig.getCompression().isEnabled()) {
                CacheCompressionSerializer<Object> perCacheCompression = new CacheCompressionSerializer<>(
                        baseSerializer,
                        cacheConfig.getCompression().getThreshold(),
                        cacheConfig.getCompression().getAlgorithm());
                specificConfig = specificConfig.serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(perCacheCompression));
                log.debug("Per-cache compression enabled for cache: {} (threshold: {} bytes)",
                        cacheName, cacheConfig.getCompression().getThreshold());
            }

            cacheConfigs.put(cacheName, specificConfig);
        });

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs);

        if (config.isEnableStatistics()) {
            builder.enableStatistics();
        }

        if (config.isTransactionAware()) {
            builder.transactionAware();
        }

        return builder.build();
    }

    /**
     * Create a two-level cache manager (L1: Caffeine; L2: Redis).
     * <p>
     * If Redis is unavailable or fails to initialize, the returned manager uses L1
     * only
     * and logs a warning. Read-through and (optional) write-through semantics are
     * enforced
     * by the {@code TwoLevelCacheManager} / {@code TwoLevelCache}. (See its Javadoc
     * for details.)
     *
     * @return a {@link CacheManager} that composes L1/L2 tiers
     */
    public CacheManager createTwoLevelCacheManager() {
        log.info("Creating two-level cache manager");

        CacheManager l1Cache = createCaffeineCacheManager();
        CacheManager l2Cache = null;

        if (redisConnectionFactory.isPresent()) {
            try {
                l2Cache = createRedisCacheManager(redisConnectionFactory.get());
            } catch (Exception e) {
                log.warn("Failed to create L2 (Redis) cache, using L1 (Caffeine) only", e);
            }
        }

        return new TwoLevelCacheManager(l1Cache, l2Cache, cacheProperties.getTwoLevel(),
                invalidationPublisher.orElse(null),
                cacheOperationExecutor.orElse(null),
                cacheMetrics.orElse(null));
    }

    /**
     * Create a {@link NoOpCacheManager} to effectively disable caching.
     * Useful for tests, local dev, or when feature-flagging cache off while keeping
     * code paths intact.
     *
     * @return a {@link NoOpCacheManager} instance
     */
    public CacheManager createNoOpCacheManager() {
        log.info("Creating NoOp cache manager (caching disabled)");
        return new NoOpCacheManager();
    }

    /**
     * Determine if the given provider is operationally available based on
     * environment capabilities.
     * <p>
     * For CAFFEINE/NOOP, availability is unconditional. For REDIS/TWO_LEVEL, a
     * redis connection factory
     * must be present.
     *
     * @param provider the provider to check
     * @return {@code true} if the provider is usable in the current environment;
     *         {@code false} otherwise
     */
    public boolean isProviderAvailable(CacheProperties.CacheProvider provider) {
        switch (provider) {
            case CAFFEINE:
            case NOOP:
                return true;

            case REDIS:
            case TWO_LEVEL:
                return redisConnectionFactory.isPresent();

            default:
                return false;
        }
    }
}
