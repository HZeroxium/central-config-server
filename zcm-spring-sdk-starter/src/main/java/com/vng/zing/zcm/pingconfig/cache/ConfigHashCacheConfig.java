package com.vng.zing.zcm.pingconfig.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.vng.zing.zcm.config.SdkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for config hash caching using Caffeine.
 * <p>
 * Provides in-memory caching for computed configuration hashes to avoid
 * expensive recalculation on every ping. Cache is invalidated when refresh
 * events are received from Kafka.
 * <p>
 * Cache configuration:
 * <ul>
 *   <li>Cache name: {@code config-hash-cache}</li>
 *   <li>TTL: Configurable via {@code zcm.sdk.ping.hash-cache.ttl} (default: 60s)</li>
 *   <li>Max size: Configurable via {@code zcm.sdk.ping.hash-cache.max-size} (default: 1000)</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "zcm.sdk.ping.hash-cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ConfigHashCacheConfig {

    private final SdkProperties sdkProperties;

    /**
     * Creates a Caffeine cache manager for config hash caching.
     * <p>
     * Configures a single cache named "config-hash-cache" with TTL and size
     * limits from SdkProperties.
     *
     * @return configured CacheManager
     */
    @Bean(name = "configHashCacheManager")
    @Primary
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager configHashCacheManager() {
        SdkProperties.Ping.HashCache cacheConfig = sdkProperties.getPing().getHashCache();
        
        long ttl = cacheConfig.getTtl();
        int maxSize = cacheConfig.getMaxSize();

        log.info("Creating config hash cache: TTL={}ms, maxSize={}", ttl, maxSize);

        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .expireAfterWrite(ttl, TimeUnit.MILLISECONDS)
                .maximumSize(maxSize)
                .recordStats(); // Enable cache statistics for monitoring

        CaffeineCacheManager cacheManager = new CaffeineCacheManager("config-hash-cache");
        cacheManager.setCaffeine(caffeine);

        return cacheManager;
    }
}

