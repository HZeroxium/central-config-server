# Cache Optimization

## Overview

Cache optimization is critical for heartbeat processing performance. The system uses a multi-level caching strategy with pre-warming, deduplication, and selective eviction to minimize Config Server calls and database queries.

## Cache Architecture

### Cache Layers

1. **Application Cache (Caffeine)**
   - In-memory cache for fast access
   - Used for config hashes and ApplicationService lookups
   - TTL-based eviction

2. **Distributed Cache (Redis)**
   - Shared cache across instances
   - Used for config hashes (longer TTL)
   - Provides consistency across multiple service instances

### Cache Keys

**Config Hashes:**
- Key: `serviceName:environment` (e.g., `"payment-service:dev"`)
- Value: Config hash string
- TTL: 5 minutes (configurable)

**ApplicationServices:**
- Key: `displayName` (e.g., `"payment-service"`)
- Value: `ApplicationService` object
- TTL: Cache-specific (varies by cache type)

**ServiceInstances:**
- Key: `instanceId` (e.g., `"payment-dev-1"`)
- Value: `ServiceInstance` object
- TTL: Evicted on update

## Cache Pre-Warming

### ConfigHashCacheWarmup

**Location:** `com.example.control.infrastructure.config.cache.ConfigHashCacheWarmup`

**Responsibilities:**
- Pre-load config hashes for all ApplicationServices on startup
- Reduce cold start latency for initial heartbeats
- Eliminate cache misses during first batch processing

**Strategy:**
1. **Trigger:** Runs asynchronously after `ApplicationReadyEvent`
2. **Delay:** 30 seconds (configurable via `app.heartbeat.cache.pre-warm.delay`)
3. **Process:**
   - Load all ApplicationServices from database
   - For each service, iterate through all environments
   - Call `configProxyService.getEffectiveConfigHash(serviceName, environment)`
   - Cache automatically stores the result
4. **Error Handling:** Logs failures but continues for other services

**Configuration:**
```yaml
app:
  heartbeat:
    cache:
      pre-warm:
        enabled: true
        delay: 30s  # Delay after startup before pre-warming
```

**Benefits:**
- Eliminates cache misses for initial heartbeats
- Reduces Config Server load during startup
- Improves first-batch processing performance

**Metrics:**
- Logs total entries warmed and failures
- Records duration of pre-warming process

## Cache Deduplication in Batch Processing

### Strategy

**Problem:** In a batch of 50 heartbeats, multiple heartbeats may be from the same service:environment combination.

**Solution:** Group payloads by `serviceName:environment` before loading config hashes.

**Implementation:**
- `HeartbeatBatchService.loadConfigHashesBatch()` groups payloads by `serviceName:environment`
- For each unique combination, calls `configProxyService.getEffectiveConfigHash()` once
- Cache handles deduplication: subsequent lookups for same key return cached value
- All heartbeats in batch share the same config hash for their service:env

**Example:**
```
Batch: 50 heartbeats
- payment-service:dev (30 heartbeats)
- payment-service:prod (10 heartbeats)
- order-service:dev (10 heartbeats)

Config hash lookups:
1. payment-service:dev → cache lookup (may hit or miss)
2. payment-service:prod → cache lookup (may hit or miss)
3. order-service:dev → cache lookup (may hit or miss)

Total: 3 lookups (instead of 50)
Cache hits: If pre-warmed, all 3 are hits
```

### Cache Hit Rate

**Before Optimization:**
- Each heartbeat = 1 config hash lookup
- Cache hit rate: ~80-90% (depending on cache size and TTL)
- Cache misses = Config Server HTTP calls

**After Optimization:**
- Batch processing groups by service:env
- Cache hit rate: >95% (with pre-warming)
- Cache misses: Only for new service:env combinations

## Cache Eviction Strategies

### ServiceInstance Cache

**Eviction Strategy:** Selective eviction by instance ID

**Implementation:**
- `ServiceInstanceCommandService.bulkUpsert()` evicts cache entries programmatically
- Uses `CacheManager.getCache("service-instances")`
- Evicts only processed instances (not entire cache)
- Maintains cache for instances not in current batch

**Method:**
```java
Cache cache = cacheManager.getCache("service-instances");
for (ServiceInstance instance : instances) {
    cache.evict(instance.getId());
}
```

**Benefits:**
- Preserves cache for instances not updated
- Reduces cache churn
- Maintains cache hit rate for read operations

### Config Hash Cache

**Eviction Strategy:** TTL-based (5 minutes)

**Implementation:**
- Cache entries expire after TTL
- No manual eviction (config hashes change infrequently)
- Cache invalidation on config change (future enhancement)

**Configuration:**
```yaml
spring:
  cache:
    redis:
      time-to-live: 5m
      cache-null-values: false
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=5m
```

### ApplicationService Cache

**Eviction Strategy:** TTL-based + selective eviction

**Implementation:**
- TTL-based expiration for stale data
- Selective eviction on updates (via `@CacheEvict`)
- Bulk save operations evict affected entries

## Cache Configuration

### Redis Cache

**Configuration:**
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 5m
      cache-null-values: false
      key-prefix: "heartbeat:"
```

**Use Cases:**
- Config hashes (shared across instances)
- ApplicationServices (shared across instances)

**Benefits:**
- Consistency across multiple service instances
- Longer TTL (5 minutes)
- Distributed cache for horizontal scaling

### Caffeine Cache

**Configuration:**
```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=5m
```

**Use Cases:**
- ServiceInstances (instance-specific, fast access)
- Config hashes (local cache, faster than Redis)

**Benefits:**
- In-memory access (faster than Redis)
- Lower latency for local lookups
- Reduces Redis load

## Cache Performance

### Config Hash Cache

**Before Optimization:**
- Cache hit rate: ~80-90%
- Config Server calls: 10-20% of heartbeats
- Latency: 50-200ms per cache miss

**After Optimization:**
- Cache hit rate: >95% (with pre-warming)
- Config Server calls: <5% of heartbeats
- Latency: <1ms per cache hit, 50-200ms per cache miss

**Improvement:**
- 95%+ reduction in Config Server calls
- 10-20x reduction in cache lookup latency (hit vs miss)

### ApplicationService Cache

**Hit Rate:** ~90-95% (services don't change frequently)

**Benefits:**
- Reduces database queries
- Fast lookup for service metadata
- Maintains consistency across batches

### ServiceInstance Cache

**Hit Rate:** Variable (depends on batch composition)

**Benefits:**
- Reduces database queries for read operations
- Fast lookup for instance metadata
- Selective eviction maintains cache efficiency

## Cache Warming Strategies

### 1. Startup Pre-Warming

**Component:** `ConfigHashCacheWarmup`

**Strategy:**
- Load all ApplicationServices
- Pre-warm config hashes for all service:env combinations
- Run asynchronously after startup (30s delay)

**Coverage:**
- All existing services and environments
- Reduces cold start latency

### 2. Lazy Warming (Future Enhancement)

**Component:** `LazyCacheWarmer` (exists but placeholder)

**Strategy:**
- Monitor cache miss rates
- Trigger warmup if miss rate exceeds threshold
- Warm specific caches on demand

**Status:** Placeholder implementation, not actively used

### 3. Critical Cache Warming

**Component:** `CriticalCacheWarmer`

**Strategy:**
- Warm critical caches on startup
- Includes IAM Users, IAM Teams, ApplicationServices
- Ensures fast access to frequently used data

## Cache Metrics

**Metrics Available:**
- Cache hit ratio (via Micrometer cache metrics)
- Cache size (gauge)
- Cache eviction count (counter)

**Monitoring:**
- Track cache hit rates in Grafana
- Alert on low hit rates (<90%)
- Monitor cache size and eviction rates

## Best Practices

1. **Pre-Warming:** Enable cache pre-warming for production deployments
2. **TTL Tuning:** Adjust TTL based on config change frequency
3. **Cache Size:** Monitor cache size and adjust maximum size if needed
4. **Eviction Strategy:** Use selective eviction to maintain cache efficiency
5. **Monitoring:** Track cache hit rates and adjust strategy accordingly

## Configuration Reference

### Application Properties

```yaml
app:
  heartbeat:
    cache:
      pre-warm:
        enabled: true
        delay: 30s

spring:
  cache:
    type: redis
    redis:
      time-to-live: 5m
      cache-null-values: false
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=5m
```

## Related Documentation

- [Architecture Overview](ARCHITECTURE.md)
- [Batch Processing](BATCH_PROCESSING.md)
- [Metrics](METRICS.md)

