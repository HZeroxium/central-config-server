# HTTP Server Configuration

## Overview

The embedded Tomcat server configuration controls HTTP request handling, thread pool management, and connection limits. These settings directly impact CPU usage (thread context switching), memory usage (thread stacks, request/response buffers), and the maximum concurrent request handling capacity.

**Configuration File**: `application.yml`

## Thread Pool Configuration

### `server.tomcat.threads.max`
- **Default**: `200`
- **What it does**: Maximum number of threads in the Tomcat thread pool for handling HTTP requests
- **Resource impact**:
  - **CPU**: Higher values increase context switching overhead when threads compete for CPU
  - **Memory**: Each thread has a default stack size (~1MB per thread on most JVMs), so 200 threads ≈ 200MB just for stacks (plus heap for request/response objects)
  - **Threads**: Directly determines the maximum number of concurrent request handlers
- **Configuration location**: `server.tomcat.threads.max` in `application.yml`
- **Notes**: 
  - Requests beyond this limit wait in the accept queue (see `accept-count`)
  - Actual thread count varies between `min-spare` and `max` based on load
  - Consider virtual threads (Java 21+) for I/O-bound workloads to reduce thread overhead

### `server.tomcat.threads.min-spare`
- **Default**: `50`
- **What it does**: Minimum number of threads kept alive in the thread pool, even when idle
- **Resource impact**:
  - **Memory**: Minimum memory reserved for thread stacks (50 threads ≈ 50MB)
  - **CPU**: Low impact; these threads are idle and don't consume CPU
  - **Threads**: Ensures fast response to incoming requests without thread creation overhead
- **Configuration location**: `server.tomcat.threads.min-spare` in `application.yml`
- **Notes**:
  - Thread pool starts with this many threads and grows up to `max` under load
  - Idle threads above `min-spare` are terminated after a timeout

## Connection Limits

### `server.tomcat.max-connections`
- **Default**: `10000`
- **What it does**: Maximum number of connections the server will accept and queue
- **Resource impact**:
  - **Memory**: Each connection consumes socket buffer memory (~8-64KB per connection depending on TCP settings)
  - **Network**: Maximum concurrent TCP connections the server can handle
  - **Threads**: Connections are handled by threads, but not all connections are active simultaneously
- **Configuration location**: `server.tomcat.max-connections` in `application.yml`
- **Notes**:
  - This is the total connection limit, not the active request limit (controlled by thread pool)
  - Includes connections waiting in accept queue
  - Connections exceeding this limit are rejected immediately

### `server.tomcat.accept-count`
- **Default**: `5000`
- **What it does**: Maximum number of requests that can wait in the accept queue when all threads are busy
- **Resource impact**:
  - **Memory**: Each queued request consumes minimal memory (connection object)
  - **Network**: Determines how many connections can be queued before rejection
  - **Latency**: Higher values allow more requests to wait, increasing max latency under load
- **Configuration location**: `server.tomcat.accept-count` in `application.yml`
- **Notes**:
  - When `max-connections` is reached, new connections are rejected
  - When all threads are busy and accept queue is full, new requests are rejected
  - Combined with `max-connections`, this determines total capacity: `max-connections` + `accept-count` = absolute max

## Connection Timeout

### `server.tomcat.connection-timeout`
- **Default**: `20s`
- **What it does**: Maximum time the server waits for a request line (HTTP request start) after accepting a connection
- **Resource impact**:
  - **Memory**: Connections held open during timeout consume socket buffers
  - **Network**: Prevents connections from being held indefinitely by slow clients
  - **Threads**: Does not directly consume threads (connections are handled asynchronously until request parsing)
- **Configuration location**: `server.tomcat.connection-timeout` in `application.yml`
- **Notes**:
  - This is different from request processing timeout (handled by application code)
  - Prevents slowloris-type attacks by timing out idle connections
  - Lower values free resources faster but may drop legitimate slow clients

## Server Port

### `server.port`
- **Default**: `8080`
- **What it does**: Port number the HTTP server listens on
- **Resource impact**: None (configuration only)
- **Configuration location**: `server.port` in `application.yml`
- **Notes**: Can be overridden via `SERVER_PORT` environment variable

## Related Configurations

- **Executor Pools**: Async operations spawned from HTTP requests use executor pools (see [Executor Pools](executor-pools.md))
- **Resilience**: Circuit breakers and time limiters may terminate long-running requests (see [Resilience](resilience.md))
- **Virtual Threads**: Java 21+ virtual threads can be enabled globally, but Tomcat uses platform threads by default (see `spring.threads.virtual.enabled` in `application.yml` - currently commented out)

## Resource Usage Summary

| Setting | CPU Impact | Memory Impact | Thread Impact | Connection Impact |
|---------|-----------|---------------|---------------|-------------------|
| `threads.max` | High (context switching) | High (stack memory) | Direct (200 max) | Indirect (via threads) |
| `threads.min-spare` | Low (idle threads) | Medium (reserved stacks) | Direct (50 minimum) | - |
| `max-connections` | Low | Medium (socket buffers) | - | Direct (10000 max) |
| `accept-count` | Low | Low (queue memory) | - | Direct (5000 queued) |
| `connection-timeout` | Low | Low (timeout duration) | - | Indirect (connection lifetime) |

## Example Configuration

```yaml
server:
  port: 8080
  tomcat:
    threads:
      max: 200
      min-spare: 50
    max-connections: 10000
    accept-count: 5000
    connection-timeout: 20s
```

## See Also

- [Executor Pools](executor-pools.md) - Async task execution outside HTTP thread pool
- [Resilience](resilience.md) - Request-level timeouts and circuit breakers
- [Spring Boot Tomcat Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#appendix.application-properties.server)

