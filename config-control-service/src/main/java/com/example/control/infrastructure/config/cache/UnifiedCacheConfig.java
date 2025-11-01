package com.example.control.infrastructure.config.cache;

import com.example.control.infrastructure.cache.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.IamTeamQueryService;
import com.example.control.application.query.IamUserQueryService;

import java.util.Arrays;
import java.util.Optional;

/**
 * Unified cache configuration for the application that centralizes cache
 * provider setup,
 * key generation strategy, and health reporting beans.
 *
 * <p>
 * <strong>What this configuration does</strong>
 * </p>
 * <ul>
 * <li>Enables Spring's cache abstraction via {@link EnableCaching} so that
 * method-level
 * annotations like {@code @Cacheable}, {@code @CachePut}, and
 * {@code @CacheEvict}
 * become active. Spring Boot will also auto-configure the infrastructure around
 * it.</li>
 * <li>Enables binding of {@link CacheProperties} using
 * {@link EnableConfigurationProperties},
 * allowing externalized configuration (application.yml/properties) to drive
 * cache behavior.</li>
 * <li>Exposes a primary {@link CacheManager} as a
 * {@link DelegatingCacheManager} which
 * supports runtime provider switching without downtime.</li>
 * <li>Provides a custom {@link KeyGenerator} to build consistent cache
 * keys.</li>
 * <li>Registers a cache health indicator bean for observability.</li>
 * </ul>
 *
 * <p>
 * <strong>Notes</strong>
 * </p>
 * <ul>
 * <li>This configuration replaces multiple profile-specific cache configs by
 * delegating
 * provider selection to {@link CacheProperties} and
 * {@link CacheManagerFactory}.</li>
 * <li>Keep in mind that the <em>actual</em> provider at runtime may differ from
 * the configured one
 * if fallback is enabled and the primary provider is unavailable.</li>
 * </ul>
 *
 * @see EnableCaching
 * @see EnableConfigurationProperties
 * @see CacheManager
 * @see DelegatingCacheManager
 */
@Slf4j
@Configuration
@EnableCaching
@EnableScheduling
@RequiredArgsConstructor
@EnableConfigurationProperties(CacheProperties.class)
public class UnifiedCacheConfig {

    /**
     * Strongly-typed cache configuration properties bound from external
     * configuration.
     */
    private final CacheProperties cacheProperties;

    /**
     * Optional Redis connection factory; its presence typically indicates that a
     * Redis-backed cache
     * provider may be provisioned by {@link CacheManagerFactory}.
     */
    private final Optional<RedisConnectionFactory> redisConnectionFactory;

    /**
     * Application context for accessing query services in cache warmers.
     */
    private final ApplicationContext applicationContext;

    /**
     * Primary {@link CacheManager} bean that the application will use by default.
     *
     * <p>
     * <strong>Semantics</strong>
     * </p>
     * <ul>
     * <li>Creates a {@link DelegatingCacheManager} initialized with the provider
     * determined by
     * {@link CacheManagerFactory#createCacheManager()} at startup.</li>
     * <li>At runtime, the delegate can be swapped atomically (e.g., to change
     * provider) while
     * in-flight operations continue on the old delegate until the call
     * completes.</li>
     * </ul>
     *
     * <p>
     * <strong>Why {@link Primary}</strong>
     * </p>
     * Marked as {@code @Primary} so type-based injection of {@link CacheManager}
     * resolves to this bean
     * even if other {@code CacheManager} beans exist in the context.
     *
     * @return a {@link DelegatingCacheManager} wrapping the initial provider
     */
    /**
     * ObjectMapper bean for serializing cache invalidation messages.
     */
    @Bean
    public ObjectMapper cacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * RedisTemplate bean for publishing cache invalidation messages.
     */
    @Bean
    public RedisTemplate<String, String> cacheRedisTemplate(Optional<RedisConnectionFactory> redisConnectionFactory) {
        if (redisConnectionFactory.isEmpty()) {
            return null;
        }

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory.get());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Publisher for cache invalidation events via Redis pub/sub.
     */
    @Bean
    public CacheInvalidationPublisher cacheInvalidationPublisher(
            Optional<RedisTemplate<String, String>> cacheRedisTemplate,
            ObjectMapper cacheObjectMapper) {
        return new CacheInvalidationPublisher(
                cacheRedisTemplate.orElse(null),
                cacheObjectMapper);
    }

    /**
     * Redis message listener container for cache invalidation events.
     */
    @Bean
    public RedisMessageListenerContainer cacheInvalidationListenerContainer(
            Optional<RedisConnectionFactory> redisConnectionFactory,
            CacheInvalidationListener cacheInvalidationListener) {
        if (redisConnectionFactory.isEmpty()) {
            return null;
        }

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory.get());
        container.addMessageListener(cacheInvalidationListener, new ChannelTopic("cache:invalidation"));
        return container;
    }

    /**
     * Listener for cache invalidation events from Redis pub/sub.
     */
    @Bean
    public CacheInvalidationListener cacheInvalidationListener(
            CacheManager cacheManager,
            ObjectMapper cacheObjectMapper) {
        return new CacheInvalidationListener(cacheManager, cacheObjectMapper);
    }

    @Bean("delegatingCacheManager")
    @Primary
    public DelegatingCacheManager delegatingCacheManager(CacheInvalidationPublisher cacheInvalidationPublisher,
            Optional<CacheOperationExecutor> cacheOperationExecutor,
            @Lazy Optional<CacheMetrics> cacheMetrics) {
        CacheManagerFactory factory = new CacheManagerFactory(
                cacheProperties,
                redisConnectionFactory,
                Optional.of(cacheInvalidationPublisher),
                cacheOperationExecutor,
                cacheMetrics);
        CacheManager initialManager = factory.createCacheManager();

        log.info("Initialized DelegatingCacheManager with provider: {}",
                cacheProperties.getProvider());

        return new DelegatingCacheManager(initialManager);
    }

    /**
     * Factory bean that can create cache managers for different providers
     * (Caffeine, Redis,
     * two-level, NoOp) according to {@link CacheProperties}.
     *
     * <p>
     * Having this factory as a Spring bean allows other components (e.g., health
     * checks,
     * administrative endpoints) to discover provider availability and create or
     * switch managers.
     * </p>
     *
     * @return a singleton {@link CacheManagerFactory}
     */
    @Bean
    public CacheManagerFactory cacheManagerFactory(CacheInvalidationPublisher cacheInvalidationPublisher,
            Optional<CacheOperationExecutor> cacheOperationExecutor,
            @Lazy Optional<CacheMetrics> cacheMetrics) {
        return new CacheManagerFactory(cacheProperties, redisConnectionFactory,
                Optional.of(cacheInvalidationPublisher), cacheOperationExecutor, cacheMetrics);
    }

    /**
     * Cache operation executor for resilience patterns (retry + circuit breaker).
     */
    @Bean
    public CacheOperationExecutor cacheOperationExecutor(
            io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakerRegistry,
            io.github.resilience4j.retry.RetryRegistry retryRegistry,
            @Lazy Optional<CacheMetrics> cacheMetrics) {
        return new CacheOperationExecutor(circuitBreakerRegistry, retryRegistry, cacheProperties, cacheMetrics);
    }

    /**
     * Custom {@link KeyGenerator} used by the cache abstraction when a key is not
     * explicitly provided.
     *
     * <p>
     * <strong>Key format</strong>
     * </p>
     * 
     * <pre>
     *   {SimpleClassName}:{methodName}:v1:[params...]
     * </pre>
     * 
     * where {@code params} are rendered with {@link Arrays#deepToString(Object[])}
     * to safely
     * handle nested arrays and varargs.
     *
     * <p>
     * <strong>Considerations</strong>
     * </p>
     * <ul>
     * <li>Keys are human-readable and deterministic. This simplifies debugging and
     * cache inspection.</li>
     * <li>For large or sensitive parameters, consider hashing the parameter portion
     * to avoid very long keys
     * and accidental exposure of sensitive values in logs or cache stores.</li>
     * </ul>
     *
     * @return a lambda-based {@link KeyGenerator} implementation
     */
    @Bean
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            String applicationName = cacheProperties.getApplicationName();
            String cacheName = inferCacheName(target, method);
            int version = cacheProperties.getDefaultVersion();

            // Get per-cache version if available
            CacheProperties.CacheConfig cacheConfig = cacheProperties.getCaches().get(cacheName);
            if (cacheConfig != null && cacheConfig.getVersion() != null) {
                version = cacheConfig.getVersion();
            }

            // Build key parts
            String[] keyParts = new String[params.length + 2];
            keyParts[0] = target.getClass().getSimpleName();
            keyParts[1] = method.getName();
            for (int i = 0; i < params.length; i++) {
                keyParts[i + 2] = params[i] != null ? params[i].toString() : "null";
            }

            return CacheKeyGenerator.generateStandardKey(applicationName, cacheName, version, keyParts);
        };
    }

    /**
     * Infer cache name from target class and method.
     * This is a best-effort approach - actual cache names come from @Cacheable
     * annotations.
     */
    private String inferCacheName(Object target, java.lang.reflect.Method method) {
        // Try to extract from class name or method name
        String className = target.getClass().getSimpleName();
        if (className.contains("Query")) {
            return className.replace("QueryService", "").toLowerCase().replace("service", "");
        }
        return "default";
    }

    /**
     * Cache metrics collector bean.
     * <p>
     * Initialization is deferred until after the application context is fully
     * initialized
     * to avoid circular dependency issues. The {@link CacheMetrics} class uses
     * {@link ApplicationReadyEvent} listener to initialize metrics once the
     * {@link CacheManager} is ready.
     * <p>
     * Note: Spring Boot Actuator automatically instruments standard cache metrics
     * ({@code cache.gets}, {@code cache.puts}, {@code cache.evicts}, etc.).
     * This bean only provides custom metrics for L1/L2 hit tracking and error
     * tracking.
     */
    @Bean
    public CacheMetrics cacheMetrics(MeterRegistry meterRegistry, @Lazy CacheManager cacheManager) {
        // Defer initialization to avoid circular dependency - CacheMetrics will
        // initialize itself via ApplicationReadyEvent listener
        return new CacheMetrics(meterRegistry, cacheManager);
    }

    /**
     * Critical cache warmer for startup warmup.
     */
    @Bean
    public CriticalCacheWarmer criticalCacheWarmer() {
        return new CriticalCacheWarmer(
                applicationContext.getBean(IamUserQueryService.class),
                applicationContext.getBean(IamTeamQueryService.class),
                applicationContext.getBean(ApplicationServiceQueryService.class));
    }

    /**
     * Lazy cache warmer for on-demand warmup.
     */
    @Bean
    public LazyCacheWarmer lazyCacheWarmer(CacheMetrics cacheMetrics, CacheManager cacheManager) {
        return new LazyCacheWarmer(cacheMetrics, cacheManager);
    }

    /**
     * Registers the cache {@code HealthIndicator} that reports provider
     * availability and
     * runtime diagnostics. When Spring Boot Actuator's health endpoint is enabled,
     * these
     * details can be exposed (subject to your
     * {@code management.endpoint.health.show-details} setting).
     *
     * @param cacheManagerFactory    the factory used to determine provider
     *                               availability
     * @param delegatingCacheManager the primary cache manager bean
     * @param cacheMetrics           optional cache metrics (may be null)
     * @return a configured {@link CacheHealthIndicator} bean
     */
    @Bean
    public CacheHealthIndicator cacheHealthIndicator(CacheManagerFactory cacheManagerFactory,
            @Qualifier("delegatingCacheManager") DelegatingCacheManager delegatingCacheManager,
            Optional<CacheMetrics> cacheMetrics) {
        return new CacheHealthIndicator(cacheManagerFactory, cacheProperties, delegatingCacheManager,
                cacheMetrics.orElse(null));
    }
}
