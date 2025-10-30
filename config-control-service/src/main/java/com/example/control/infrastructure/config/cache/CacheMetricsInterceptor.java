package com.example.control.infrastructure.config.cache;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;

/**
 * AOP interceptor for tracking cache operation metrics.
 * <p>
 * Intercepts cache operations annotated with @Cacheable, @CachePut, @CacheEvict
 * and records metrics using CacheMetrics.
 *
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheMetricsInterceptor {

  private final CacheMetrics cacheMetrics;

  /**
   * Intercept @Cacheable operations.
   */
  @Around("@annotation(cacheable)")
  public Object interceptCacheable(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
    String[] cacheNames = cacheable.value();
    if (cacheNames.length == 0) {
      return joinPoint.proceed();
    }

    String cacheName = cacheNames[0]; // Use first cache name for metrics
    Timer.Sample sample = cacheMetrics.startLoadTimer();

    try {
      Object result = joinPoint.proceed();

      // If result is not null, it's a hit (from cache or loaded)
      if (result != null) {
        cacheMetrics.recordHit(cacheName);
      } else {
        cacheMetrics.recordMiss(cacheName);
      }

      cacheMetrics.recordLoadDuration(cacheName, sample);
      return result;
    } catch (Throwable t) {
      cacheMetrics.recordMiss(cacheName);
      throw t;
    }
  }

  /**
   * Intercept @CachePut operations.
   */
  @Around("@annotation(cachePut)")
  public Object interceptCachePut(ProceedingJoinPoint joinPoint, CachePut cachePut) throws Throwable {
    String[] cacheNames = cachePut.value();
    if (cacheNames.length == 0) {
      return joinPoint.proceed();
    }

    String cacheName = cacheNames[0];
    Timer.Sample sample = cacheMetrics.startLoadTimer();

    try {
      Object result = joinPoint.proceed();
      cacheMetrics.recordPut(cacheName);
      cacheMetrics.recordLoadDuration(cacheName, sample);
      return result;
    } catch (Throwable t) {
      throw t;
    }
  }

  /**
   * Intercept @CacheEvict operations.
   */
  @Around("@annotation(cacheEvict)")
  public Object interceptCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
    String[] cacheNames = cacheEvict.value();
    if (cacheNames.length == 0) {
      return joinPoint.proceed();
    }

    String cacheName = cacheNames[0];

    try {
      Object result = joinPoint.proceed();

      // If evicting all entries, record multiple evictions
      if (cacheEvict.allEntries()) {
        // For bulk eviction, record once (actual count may vary)
        cacheMetrics.recordEviction(cacheName);
      } else {
        cacheMetrics.recordEviction(cacheName);
      }

      return result;
    } catch (Throwable t) {
      throw t;
    }
  }
}
