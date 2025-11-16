# Database Connections Configuration

## Overview

MongoDB and Redis connection pool configuration. These settings control how the application connects to databases, affecting connection resources, memory usage (connection objects), and network I/O (connection establishment).

**Configuration File**: `application-datasources.yml`

## MongoDB Connection Configuration

### `spring.data.mongodb.uri`
- **Default**: `mongodb://mongodb:27017/config_control`
- **What it does**: MongoDB connection URI including host, port, and database name
- **Resource impact**: None (connection string only)
- **Configuration location**: `spring.data.mongodb.uri` in `application-datasources.yml`
- **Notes**: Can be overridden via `MONGODB_URI` environment variable

### `spring.data.mongodb.auto-index-creation`
- **Default**: `true`
- **What it does**: Automatically create indexes defined in entities
- **Resource impact**:
  - **CPU**: Index creation consumes CPU during startup
  - **Disk I/O**: Index creation writes to disk
- **Configuration location**: `spring.data.mongodb.auto-index-creation` in `application-datasources.yml`
- **Notes**: Useful for development; production may prefer manual index management

### `spring.data.mongodb.options.max-pool-size`
- **Default**: `100`
- **What it does**: Maximum number of connections in the MongoDB connection pool
- **Resource impact**:
  - **Memory**: Each connection consumes memory (~100KB-1MB per connection depending on configuration)
  - **Network**: Maintains up to 100 TCP connections to MongoDB
  - **CPU**: Connection pool management overhead
- **Configuration location**: `spring.data.mongodb.options.max-pool-size` in `application-datasources.yml`
- **Notes**:
  - Pool grows from `min-pool-size` to `max-pool-size` based on demand
  - Connections beyond max wait in queue or are rejected
  - Higher values improve concurrency but consume more resources

### `spring.data.mongodb.options.min-pool-size`
- **Default**: `20`
- **What it does**: Minimum number of connections kept alive in the pool
- **Resource impact**:
  - **Memory**: Reserves memory for at least 20 connections (~2-20MB)
  - **Network**: Maintains at least 20 TCP connections to MongoDB even when idle
  - **Latency**: Pre-warmed connections reduce connection establishment latency
- **Configuration location**: `spring.data.mongodb.options.min-pool-size` in `application-datasources.yml`
- **Notes**:
  - Pool starts with this many connections and grows up to `max-pool-size` under load
  - Idle connections above `min-pool-size` are closed after `max-connection-idle-time`

### `spring.data.mongodb.options.max-connection-idle-time`
- **Default**: `30s`
- **What it does**: Maximum time an idle connection can remain in the pool before being closed
- **Resource impact**:
  - **Memory**: Frees memory by closing idle connections
  - **Network**: Closes TCP connections that are idle too long
  - **Latency**: May require connection establishment if pool shrinks too much
- **Configuration location**: `spring.data.mongodb.options.max-connection-idle-time` in `application-datasources.yml`
- **Notes**:
  - Prevents holding connections indefinitely during low traffic
  - Connections idle longer than this are closed (but pool maintains `min-pool-size`)

## Redis Connection Configuration

### `spring.data.redis.url`
- **Default**: `redis://:redis123@redis:6379`
- **What it does**: Redis connection URL including host, port, and optional password
- **Resource impact**: None (connection string only)
- **Configuration location**: `spring.data.redis.url` in `application-datasources.yml`
- **Notes**: Can be overridden via `REDIS_URL` environment variable

### `spring.data.redis.timeout`
- **Default**: `2000ms` (2s)
- **What it does**: Socket timeout for Redis operations (read/write timeout)
- **Resource impact**:
  - **Latency**: Operations that exceed 2s timeout are aborted
  - **Network**: Prevents hanging on network issues
  - **CPU**: Low impact (timeout checking)
- **Configuration location**: `spring.data.redis.timeout` in `application-datasources.yml`
- **Notes**: 
  - Different from connection timeout (handled by Lettuce)
  - Operations that exceed this timeout throw exceptions

## Lettuce Connection Pool Configuration

Lettuce is the Redis client library used by Spring Data Redis. It uses a connection pool for managing Redis connections.

### `spring.data.redis.lettuce.pool.max-active`
- **Default**: `8`
- **What it does**: Maximum number of active connections in the Lettuce connection pool
- **Resource impact**:
  - **Memory**: Each connection consumes memory (~10-50KB per connection)
  - **Network**: Maintains up to 8 TCP connections to Redis
  - **CPU**: Connection pool management overhead
- **Configuration location**: `spring.data.redis.lettuce.pool.max-active` in `application-datasources.yml`
- **Notes**:
  - Pool grows from `min-idle` to `max-active` based on demand
  - Requests beyond max wait or are rejected
  - Redis is single-threaded, so excessive connections don't improve performance

### `spring.data.redis.lettuce.pool.max-idle`
- **Default**: `8`
- **What it does**: Maximum number of idle connections kept in the pool
- **Resource impact**:
  - **Memory**: Reserves memory for up to 8 idle connections
  - **Network**: Maintains up to 8 idle TCP connections
- **Configuration location**: `spring.data.redis.lettuce.pool.max-idle` in `application-datasources.yml`
- **Notes**:
  - Idle connections above this are closed
  - Should be less than or equal to `max-active`

### `spring.data.redis.lettuce.pool.min-idle`
- **Default**: `0`
- **What it does**: Minimum number of idle connections kept in the pool
- **Resource impact**:
  - **Memory**: Reserves memory for at least this many connections
  - **Network**: Pre-warms connections even when idle
  - **Latency**: Pre-warmed connections reduce connection establishment latency
- **Configuration location**: `spring.data.redis.lettuce.pool.min-idle` in `application-datasources.yml`
- **Notes**:
  - Set to 0 to allow pool to shrink completely when idle
  - Higher values improve response time but consume resources

## Connection Pool Behavior

### Pool Growth
- Both MongoDB and Redis pools start with `min-pool-size`/`min-idle` connections
- Pools grow up to `max-pool-size`/`max-active` under load
- When demand decreases, idle connections above minimum are closed after idle timeout

### Connection Lifecycle
1. **Acquisition**: Request borrows connection from pool (creates new if pool exhausted)
2. **Usage**: Connection used for database operation
3. **Return**: Connection returned to pool after operation completes
4. **Idle**: Connection remains in pool for reuse
5. **Eviction**: Idle connections above minimum are closed after idle timeout

## Resource Usage Summary

| Setting | Memory Impact | Network Impact | Connection Impact |
|---------|---------------|----------------|-------------------|
| MongoDB `max-pool-size` | High (100 connections × ~100KB-1MB) | High (100 TCP connections) | Direct (100 max) |
| MongoDB `min-pool-size` | Medium (20 connections reserved) | Medium (20 TCP connections) | Direct (20 minimum) |
| MongoDB `max-connection-idle-time` | Low (eviction timing) | Low (close idle) | Indirect (pool shrinking) |
| Redis `max-active` | Low (8 connections × ~10-50KB) | Low (8 TCP connections) | Direct (8 max) |
| Redis `max-idle` | Low (8 idle connections) | Low (8 idle TCP) | Direct (8 idle max) |
| Redis `min-idle` | Low (0-8 reserved) | Low (0-8 pre-warmed) | Direct (0 minimum) |
| Redis `timeout` | Low | Low (timeout behavior) | Indirect (operation timeout) |

## See Also

- [Cache](cache.md) - Redis cache configuration and usage
- [HTTP Clients](http-clients.md) - HTTP client connection pools
- [MongoDB Java Driver Connection Pool](https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/connection/connection-pooling/)
- [Lettuce Connection Pooling](https://github.com/lettuce-io/lettuce-core/wiki/Connection-Pooling)

