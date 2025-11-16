# Cache Configuration

## Overview

Cache provider configuration (Caffeine, Redis, or NOOP), including two-level cache, per-cache settings, eviction policies, and error handling. Cache configuration affects memory usage (Caffeine heap cache) and network I/O (Redis cache), as well as CPU usage for cache operations.

**Configuration File**: `application-app.yml` (app.cache.*)

## Cache Provider Selection

### `app.cache.provider`
- **Default**: `REDIS`
- **Options**: `CAFFEINE`, `REDIS`, `NOOP`
- **What it does**: Selects the cache provider implementation
- **Resource impact**:
  - **CAFFEINE**: Uses local heap memory (fast, but consumes JVM heap)
  - **REDIS**: Uses Redis server (distributed, but network I/O)
  - **NOOP**: Disables caching (no resource usage, but no performance benefit)
- **Configuration location**: `app.cache.provider` in `application-app.yml`
- **Notes**:
  - Can be overridden via `CACHE_PROVIDER` environment variable
  - Provider selection affects all caches unless overridden per-cache

## Caffeine Cache Configuration

Caffeine is a local in-memory cache provider that stores entries in the JVM heap.

### `app.cache.caffeine.maximum-size`
- **Default**: `10000`
- **What it does**: Maximum number of entries in the Caffeine cache before eviction
- **Resource impact**:
  - **Memory**: Directly determines heap memory usage (entries × average entry size)
  - **CPU**: Eviction operations consume CPU when limit is reached
- **Configuration location**: `app.cache.caffeine.maximum-size` in `application-app.yml`
- **Notes**:
  - LRU (Least Recently Used) eviction when limit is reached
  - Memory usage = entries × average entry size (unpredictable, depends on data)

### `app.cache.caffeine.expire-after-write`
- **Default**: `10m`
- **What it does**: Time after which entries expire since last write
- **Resource impact**:
  - **Memory**: Entries removed after expiry, freeing memory
  - **CPU**: Expiry checking consumes periodic CPU cycles
- **Configuration location**: `app.cache.caffeine.expire-after-write` in `application-app.yml`
- **Notes**: Entries expire regardless of access after write time

### `app.cache.caffeine.expire-after-access`
- **Default**: `30m`
- **What it does**: Time after which entries expire since last access (read or write)
- **Resource impact**:
  - **Memory**: Frequently accessed entries stay longer, less frequently accessed expire sooner
  - **CPU**: Expiry checking consumes periodic CPU cycles
- **Configuration location**: `app.cache.caffeine.expire-after-access` in `application-app.yml`
- **Notes**: 
  - Entries expire only if not accessed within the time window
  - Longer expiry increases memory usage but improves hit ratio

### `app.cache.caffeine.record-stats`
- **Default**: `true`
- **What it does**: Enables cache statistics (hit ratio, eviction count, etc.)
- **Resource impact**:
  - **Memory**: Minimal overhead for statistics tracking
  - **CPU**: Minimal overhead for statistics collection
- **Configuration location**: `app.cache.caffeine.record-stats` in `application-app.yml`
- **Notes**: Statistics available via Micrometer metrics and JMX

## Redis Cache Configuration

Redis is a distributed cache provider that stores entries in a remote Redis server.

### `app.cache.redis.default-ttl`
- **Default**: `10m`
- **What it does**: Default time-to-live (TTL) for cache entries in Redis
- **Resource impact**:
  - **Network I/O**: Redis expiration reduces storage, less network traffic
  - **Memory**: Entries removed from Redis after expiry, freeing Redis memory
- **Configuration location**: `app.cache.redis.default-ttl` in `application-app.yml`
- **Notes**: 
  - Applied to all caches unless overridden per-cache
  - Redis automatically evicts entries after TTL expires

### `app.cache.redis.enable-statistics`
- **Default**: `true`
- **What it does**: Enables cache statistics for Redis operations
- **Resource impact**:
  - **Memory**: Minimal overhead for statistics tracking
  - **CPU**: Minimal overhead for statistics collection
- **Configuration location**: `app.cache.redis.enable-statistics` in `application-app.yml`
- **Notes**: Statistics available via Micrometer metrics

### `app.cache.redis.transaction-aware`
- **Default**: `true`
- **What it does**: Integrates cache operations with Spring transactions
- **Resource impact**:
  - **Memory**: Transaction context overhead (minimal)
  - **CPU**: Transaction management overhead (minimal)
- **Configuration location**: `app.cache.redis.transaction-aware` in `application-app.yml`
- **Notes**: 
  - Cache operations participate in transaction rollback
  - Useful for consistency with database transactions

### `app.cache.redis.fallback-to-caffeine`
- **Default**: `true`
- **What it does**: Falls back to Caffeine cache if Redis is unavailable
- **Resource impact**:
  - **Memory**: Falls back to heap memory (Caffeine) during Redis outages
  - **Network I/O**: Reduces network calls during Redis outages
- **Configuration location**: `app.cache.redis.fallback-to-caffeine` in `application-app.yml`
- **Notes**: 
  - Provides resilience during Redis outages
  - Caffeine fallback uses same configuration as if Caffeine were primary

## Two-Level Cache Configuration

Two-level cache combines Caffeine (L1) and Redis (L2) for optimal performance and distribution.

### `app.cache.two-level.write-through`
- **Default**: `true`
- **What it does**: Writes go through both L1 (Caffeine) and L2 (Redis)
- **Resource impact**:
  - **Network I/O**: Every write goes to both caches, increasing network I/O
  - **Memory**: Writes populate both L1 (heap) and L2 (Redis)
  - **Latency**: Writes must complete for both caches
- **Configuration location**: `app.cache.two-level.write-through` in `application-app.yml`
- **Notes**: Ensures consistency between L1 and L2 caches

### `app.cache.two-level.invalidate-l1-on-l2-update`
- **Default**: `true`
- **What it does**: Invalidates L1 (Caffeine) cache when L2 (Redis) is updated externally
- **Resource impact**:
  - **Network I/O**: Listens for Redis pub/sub events for invalidation
  - **Memory**: L1 entries are removed, freeing heap memory
  - **CPU**: Processing invalidation events
- **Configuration location**: `app.cache.two-level.invalidate-l1-on-l2-update` in `application-app.yml`
- **Notes**: 
  - Prevents stale data in L1 when L2 is updated by other instances
  - Requires Redis pub/sub for invalidation events

### `app.cache.two-level.defer-l2-writes`
- **Default**: `true`
- **What it does**: Defers L2 (Redis) writes for better performance
- **Resource impact**:
  - **Network I/O**: Batches L2 writes, reducing network calls
  - **Latency**: Reduces write latency by deferring L2 writes
  - **Consistency**: Risk of L1/L2 inconsistency if application crashes
- **Configuration location**: `app.cache.two-level.defer-l2-writes` in `application-app.yml`
- **Notes**: Trade-off between performance and consistency

## Per-Cache Configuration

Individual caches can override global settings. The following caches are configured:

### Service Instances Cache (`app.cache.caches.service-instances`)
- **TTL**: `5m`
- **Maximum Size**: `10000`
- **Allow Null Values**: `false`
- **Resource impact**: High memory usage for frequently accessed service instance data

### Drift Events Cache (`app.cache.caches.drift-events`)
- **TTL**: `2m`
- **Maximum Size**: `5000`
- **Allow Null Values**: `false`
- **Resource impact**: Medium memory usage for recent drift events

### Config Hashes Cache (`app.cache.caches.config-hashes`)
- **TTL**: `30m`
- **Maximum Size**: `10000`
- **Allow Null Values**: `false`
- **Resource impact**: High memory usage for configuration hash lookups

### Consul Services Cache (`app.cache.caches.consul-services`)
- **TTL**: `1m`
- **Maximum Size**: `500`
- **Allow Null Values**: `false`
- **Resource impact**: Low memory usage for Consul service registry data

### Consul Health Cache (`app.cache.caches.consul-health`)
- **TTL**: `30s`
- **Maximum Size**: `1000`
- **Allow Null Values**: `false`
- **Resource impact**: Low memory usage for health check results (frequent refresh)

### IAM Users Cache (`app.cache.caches.iam-users`)
- **TTL**: `15m`
- **Maximum Size**: `5000`
- **Allow Null Values**: `false`
- **Resource impact**: Medium memory usage for user data from Keycloak

### IAM Teams Cache (`app.cache.caches.iam-teams`)
- **TTL**: `30m`
- **Maximum Size**: `500`
- **Allow Null Values**: `false`
- **Resource impact**: Low memory usage for team data from Keycloak

### Application Services Cache (`app.cache.caches.application-services`)
- **TTL**: `10m`
- **Maximum Size**: `1000`
- **Allow Null Values**: `false`
- **Resource impact**: Medium memory usage for application service metadata

### Approval Requests Cache (`app.cache.caches.approval-requests`)
- **TTL**: `5m`
- **Maximum Size**: `2000`
- **Allow Null Values**: `false`
- **Resource impact**: Medium memory usage for approval workflow data

### Approval Decisions Cache (`app.cache.caches.approval-decisions`)
- **TTL**: `10m`
- **Maximum Size**: `5000`
- **Allow Null Values**: `false`
- **Resource impact**: Medium memory usage for approval decision history

### Service Shares Cache (`app.cache.caches.service-shares`)
- **TTL**: `10m`
- **Maximum Size**: `2000`
- **Allow Null Values**: `false`
- **Resource impact**: Medium memory usage for service sharing data

### KV Entries Cache (`app.cache.caches.kv-entries`)
- **TTL**: `5m`
- **Maximum Size**: `10000`
- **Allow Null Values**: `false`
- **Resource impact**: High memory usage for key-value store entries

## Cache Eviction Configuration

### `app.cache.eviction.batch-threshold`
- **Default**: `50`
- **What it does**: Number of entries to evict in a single batch operation
- **Resource impact**:
  - **CPU**: Batch eviction reduces CPU overhead per eviction
  - **Memory**: Batch eviction frees memory in chunks
- **Configuration location**: `app.cache.eviction.batch-threshold` in `application-app.yml`
- **Notes**: Improves efficiency of eviction operations

## Cache Error Handling Configuration

### `app.cache.error-handling.enable-retry`
- **Default**: `true`
- **What it does**: Retries cache operations on failure
- **Resource impact**:
  - **Network I/O**: Retries consume additional network resources (for Redis)
  - **CPU**: Re-processing consumes CPU cycles
  - **Latency**: Retries add latency to cache operations
- **Configuration location**: `app.cache.error-handling.enable-retry` in `application-app.yml`

### `app.cache.error-handling.max-attempts`
- **Default**: `3`
- **What it does**: Maximum number of retry attempts
- **Resource impact**: Same as `enable-retry`, multiplied by max attempts
- **Configuration location**: `app.cache.error-handling.max-attempts` in `application-app.yml`

### `app.cache.error-handling.initial-delay`
- **Default**: `100ms`
- **What it does**: Initial delay before first retry
- **Resource impact**: Adds latency to retry attempts
- **Configuration location**: `app.cache.error-handling.initial-delay` in `application-app.yml`

### `app.cache.error-handling.max-delay`
- **Default**: `1s`
- **What it does**: Maximum delay between retries (exponential backoff capped at this)
- **Resource impact**: Caps retry latency
- **Configuration location**: `app.cache.error-handling.max-delay` in `application-app.yml`

### `app.cache.error-handling.multiplier`
- **Default**: `2.0`
- **What it does**: Exponential backoff multiplier (delay = initial × multiplier^attempt)
- **Resource impact**: Determines retry delay progression
- **Configuration location**: `app.cache.error-handling.multiplier` in `application-app.yml`

## Cache Compression Configuration

### `app.cache.compression.enabled`
- **Default**: `false`
- **What it does**: Enables compression of cache entries before storage
- **Resource impact**:
  - **CPU**: Compression consumes CPU cycles
  - **Memory**: Compressed entries use less memory (Caffeine) or network bandwidth (Redis)
  - **Network I/O**: Reduces network bandwidth for Redis (if enabled)
- **Configuration location**: `app.cache.compression.enabled` in `application-app.yml`
- **Notes**: 
  - Currently disabled by default
  - Can be enabled per-cache if needed

### `app.cache.compression.threshold`
- **Default**: `1024` (1KB)
- **What it does**: Minimum entry size to compress (small entries not worth compressing)
- **Resource impact**: Skips compression for small entries, reducing CPU overhead
- **Configuration location**: `app.cache.compression.threshold` in `application-app.yml`

### `app.cache.compression.algorithm`
- **Default**: `GZIP`
- **What it does**: Compression algorithm (GZIP, SNAPPY, LZ4, etc.)
- **Resource impact**: Different algorithms have different CPU/memory trade-offs
- **Configuration location**: `app.cache.compression.algorithm` in `application-app.yml`

## Resource Usage Summary

| Provider | Memory Impact | Network I/O Impact | CPU Impact |
|----------|---------------|-------------------|------------|
| Caffeine | High (heap memory, entries × size) | None (local) | Low (eviction, expiry) |
| Redis | Low (Redis server memory) | High (network calls) | Low (serialization) |
| Two-Level | Very High (heap + network) | High (L2 writes/reads) | Medium (L1/L2 sync) |
| NOOP | None | None | None |

## See Also

- [Database Connections](database-connections.md) - Redis connection pool configuration
- [Caffeine Cache Documentation](https://github.com/ben-manes/caffeine)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)

