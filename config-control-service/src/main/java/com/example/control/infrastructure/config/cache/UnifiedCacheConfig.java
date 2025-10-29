package com.example.control.infrastructure.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Arrays;
import java.util.Optional;

/**
 * Unified cache configuration for the application that centralizes cache provider setup,
 * key generation strategy, and health reporting beans.
 *
 * <p><strong>What this configuration does</strong></p>
 * <ul>
 *   <li>Enables Spring's cache abstraction via {@link EnableCaching} so that method-level
 *       annotations like {@code @Cacheable}, {@code @CachePut}, and {@code @CacheEvict}
 *       become active. Spring Boot will also auto-configure the infrastructure around it. </li>
 *   <li>Enables binding of {@link CacheProperties} using {@link EnableConfigurationProperties},
 *       allowing externalized configuration (application.yml/properties) to drive cache behavior. </li>
 *   <li>Exposes a primary {@link CacheManager} as a {@link DelegatingCacheManager} which
 *       supports runtime provider switching without downtime.</li>
 *   <li>Provides a custom {@link KeyGenerator} to build consistent cache keys.</li>
 *   <li>Registers a cache health indicator bean for observability.</li>
 * </ul>
 *
 * <p><strong>Notes</strong></p>
 * <ul>
 *   <li>This configuration replaces multiple profile-specific cache configs by delegating
 *       provider selection to {@link CacheProperties} and {@link CacheManagerFactory}.</li>
 *   <li>Keep in mind that the <em>actual</em> provider at runtime may differ from the configured one
 *       if fallback is enabled and the primary provider is unavailable.</li>
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
@RequiredArgsConstructor
@EnableConfigurationProperties(CacheProperties.class)
public class UnifiedCacheConfig {

    /**
     * Strongly-typed cache configuration properties bound from external configuration.
     */
    private final CacheProperties cacheProperties;

    /**
     * Optional Redis connection factory; its presence typically indicates that a Redis-backed cache
     * provider may be provisioned by {@link CacheManagerFactory}.
     */
    private final Optional<RedisConnectionFactory> redisConnectionFactory;

    /**
     * Primary {@link CacheManager} bean that the application will use by default.
     *
     * <p><strong>Semantics</strong></p>
     * <ul>
     *   <li>Creates a {@link DelegatingCacheManager} initialized with the provider determined by
     *       {@link CacheManagerFactory#createCacheManager()} at startup.</li>
     *   <li>At runtime, the delegate can be swapped atomically (e.g., to change provider) while
     *       in-flight operations continue on the old delegate until the call completes.</li>
     * </ul>
     *
     * <p><strong>Why {@link Primary}</strong></p>
     * Marked as {@code @Primary} so type-based injection of {@link CacheManager} resolves to this bean
     * even if other {@code CacheManager} beans exist in the context.
     *
     * @return a {@link DelegatingCacheManager} wrapping the initial provider
     */
    @Bean("delegatingCacheManager")
    @Primary
    public DelegatingCacheManager delegatingCacheManager() {
        CacheManagerFactory factory = new CacheManagerFactory(cacheProperties, redisConnectionFactory);
        CacheManager initialManager = factory.createCacheManager();

        log.info("Initialized DelegatingCacheManager with provider: {}",
                cacheProperties.getProvider());

        return new DelegatingCacheManager(initialManager);
    }

    /**
     * Factory bean that can create cache managers for different providers (Caffeine, Redis,
     * two-level, NoOp) according to {@link CacheProperties}.
     *
     * <p>Having this factory as a Spring bean allows other components (e.g., health checks,
     * administrative endpoints) to discover provider availability and create or switch managers.</p>
     *
     * @return a singleton {@link CacheManagerFactory}
     */
    @Bean
    public CacheManagerFactory cacheManagerFactory() {
        return new CacheManagerFactory(cacheProperties, redisConnectionFactory);
    }

    /**
     * Custom {@link KeyGenerator} used by the cache abstraction when a key is not explicitly provided.
     *
     * <p><strong>Key format</strong></p>
     * <pre>
     *   {SimpleClassName}:{methodName}:v1:[params...]
     * </pre>
     * where {@code params} are rendered with {@link Arrays#deepToString(Object[])} to safely
     * handle nested arrays and varargs.
     *
     * <p><strong>Considerations</strong></p>
     * <ul>
     *   <li>Keys are human-readable and deterministic. This simplifies debugging and cache inspection.</li>
     *   <li>For large or sensitive parameters, consider hashing the parameter portion to avoid very long keys
     *       and accidental exposure of sensitive values in logs or cache stores.</li>
     * </ul>
     *
     * @return a lambda-based {@link KeyGenerator} implementation
     */
    @Bean
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(target.getClass().getSimpleName())
                    .append(":")
                    .append(method.getName())
                    .append(":v1:");

            if (params.length > 0) {
                keyBuilder.append(Arrays.deepToString(params));
            }

            String key = keyBuilder.toString();
            log.debug("Generated cache key: {}", key);
            return key;
        };
    }

    /**
     * Registers the cache {@code HealthIndicator} that reports provider availability and
     * runtime diagnostics. When Spring Boot Actuator's health endpoint is enabled, these
     * details can be exposed (subject to your {@code management.endpoint.health.show-details} setting).
     *
     * @param cacheManagerFactory    the factory used to determine provider availability
     * @param delegatingCacheManager the primary cache manager bean
     * @return a configured {@link CacheHealthIndicator} bean
     */
    @Bean
    public CacheHealthIndicator cacheHealthIndicator(CacheManagerFactory cacheManagerFactory,
                                                     @Qualifier("delegatingCacheManager")
                                                     DelegatingCacheManager delegatingCacheManager) {
        return new CacheHealthIndicator(cacheManagerFactory, cacheProperties, delegatingCacheManager);
    }
}
