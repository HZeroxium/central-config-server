package com.example.common.config.cache;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@Profile("redis-cache")
@EnableCaching
public class RedisCacheConfig {

    // Note: Let Spring Boot auto-configure RedisConnectionFactory from SPRING_DATA_REDIS_URL or spring.data.redis.*

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Enable default typing so GenericJackson2JsonRedisSerializer can reconstruct concrete types
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Object.class)
            .build();
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);
        return RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .entryTtl(Duration.ofMinutes(10));
    }

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory cf, RedisCacheConfiguration cfg) {
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("userById", cfg.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("usersByCriteria", cfg.entryTtl(Duration.ofMinutes(2)));
        cacheConfigs.put("countByCriteria", cfg.entryTtl(Duration.ofMinutes(1)));
        RedisCacheManager manager = RedisCacheManager.builder(cf)
            .cacheDefaults(cfg)
            .withInitialCacheConfigurations(cacheConfigs)
            .enableStatistics()
            .transactionAware()
            .build();
        return manager;
    }


    @Bean
    public KeyGenerator keyGenerator() {
        return (target, method, params) ->
            target.getClass().getSimpleName() + ":" + method.getName() + ":v1:" + Arrays.deepToString(params);
    }
}


