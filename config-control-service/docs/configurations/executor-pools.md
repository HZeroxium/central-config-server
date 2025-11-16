# Executor Pools Configuration

## Overview

Async task executor configuration for different workload types (notifications, RPC, config fetching, default). These settings control thread pool size, queue capacity, keep-alive time, and shutdown behavior, directly affecting thread count, memory usage (thread stacks), and CPU context switching.

**Configuration File**: `application-app.yml` (app.async.*)
**Java Configuration**: `AsyncConfig.java`

## Executor Pool Types

The application uses four executor pools:

1. **Notification Executor**: For email notifications (I/O-bound, virtual threads enabled)
2. **RPC Executor**: For RPC server startup (rare, minimal concurrency)
3. **Default Executor**: For general-purpose async tasks (fallback)
4. **Config Hash Fetch Executor**: For parallel fetching of configuration hashes (I/O-bound)

## Common Pool Configuration Properties

All executor pools share these configuration properties:

### `core-pool-size`
- **What it does**: Minimum number of threads to keep alive in the pool
- **Resource impact**:
  - **Memory**: Each thread consumes stack memory (~1MB per thread on most JVMs)
  - **CPU**: Low impact when idle (threads are idle)
  - **Threads**: Directly determines minimum thread count
- **Configuration location**: `app.async.<executor>.core-pool-size` in `application-app.yml`

### `max-pool-size`
- **What it does**: Maximum number of threads that can be created in the pool
- **Resource impact**:
  - **Memory**: Maximum stack memory = max-pool-size × ~1MB
  - **CPU**: Higher values increase context switching overhead
  - **Threads**: Directly determines maximum thread count
- **Configuration location**: `app.async.<executor>.max-pool-size` in `application-app.yml`

### `queue-capacity`
- **What it does**: Maximum number of tasks that can wait in the queue before threads are created
- **Resource impact**:
  - **Memory**: Each queued task consumes memory (task object + captured state)
  - **Latency**: Tasks wait in queue before execution
  - **Threads**: Prevents thread creation until queue is full
- **Configuration location**: `app.async.<executor>.queue-capacity` in `application-app.yml`

### `keep-alive`
- **What it does**: Time idle threads beyond core-pool-size are kept alive before termination
- **Resource impact**:
  - **Memory**: Idle threads consume stack memory during keep-alive period
  - **CPU**: Low impact (idle threads don't consume CPU)
- **Configuration location**: `app.async.<executor>.keep-alive` in `application-app.yml`
- **Notes**: Duration format (e.g., `60s`, `1m`)

### `thread-name-prefix`
- **What it does**: Prefix for thread names (useful for debugging and monitoring)
- **Resource impact**: None (naming only)
- **Configuration location**: `app.async.<executor>.thread-name-prefix` in `application-app.yml`

### `use-virtual-threads`
- **What it does**: Whether to use Java 21+ virtual threads instead of platform threads
- **Resource impact**:
  - **Memory**: Virtual threads use minimal memory (~few KB per thread vs ~1MB for platform threads)
  - **CPU**: Virtual threads have lower context switching overhead
  - **Scalability**: Virtual threads can scale to millions, platform threads limited to thousands
- **Configuration location**: `app.async.<executor>.use-virtual-threads` in `application-app.yml`
- **Notes**:
  - Only applicable to Java 21+
  - Ideal for I/O-bound tasks (network, disk I/O)
  - Not suitable for CPU-bound tasks (still use platform threads)

## Notification Executor

**Purpose**: Email notifications (I/O-bound, moderate throughput)

### Configuration (`app.async.notification`)
- **Core Pool Size**: `4`
- **Max Pool Size**: `8`
- **Queue Capacity**: `100`
- **Keep-Alive**: `60s`
- **Thread Name Prefix**: `async-notify-`
- **Virtual Threads**: `true` (enabled)

### Resource Impact
- **Memory**: Minimal (virtual threads use ~few KB each, total ~32-64KB vs ~4-8MB for platform threads)
- **CPU**: Low context switching overhead (virtual threads)
- **Scalability**: Can handle thousands of concurrent email operations
- **Threads**: Uses virtual threads (mapped to carrier threads by JVM)

### Notes
- Virtual threads are ideal for I/O-bound operations like email sending
- Email operations block on network I/O, which virtual threads handle efficiently
- Thread pool size is less relevant with virtual threads (JVM manages carrier threads)

## RPC Executor

**Purpose**: RPC server startup (rare, minimal concurrency)

### Configuration (`app.async.rpc`)
- **Core Pool Size**: `1`
- **Max Pool Size**: `2`
- **Queue Capacity**: `10`
- **Keep-Alive**: `30s`
- **Thread Name Prefix**: `async-rpc-`
- **Virtual Threads**: `false` (platform threads)

### Resource Impact
- **Memory**: Low (~1-2MB for platform thread stacks)
- **CPU**: Low (minimal concurrency)
- **Threads**: Minimal thread count (1-2 threads)

### Notes
- Used for Thrift server startup (see [RPC Servers](rpc-servers.md))
- Rare operations, so minimal pool size is appropriate
- Platform threads used to ensure server startup completes reliably

## Default Executor

**Purpose**: General-purpose async tasks (fallback for @Async without executor name)

### Configuration (`app.async.default`)
- **Core Pool Size**: `8`
- **Max Pool Size**: `16`
- **Queue Capacity**: `200`
- **Keep-Alive**: `60s`
- **Thread Name Prefix**: `async-default-`
- **Virtual Threads**: `false` (platform threads)

### Resource Impact
- **Memory**: Medium (~8-16MB for platform thread stacks)
- **CPU**: Medium context switching overhead
- **Threads**: Moderate thread count (8-16 threads)

### Notes
- Fallback executor for @Async operations without explicit executor name
- Used for general-purpose async tasks
- Platform threads used for predictable behavior across different workloads

## Config Hash Fetch Executor

**Purpose**: Parallel fetching of configuration hashes from Config Server (I/O-bound, controlled concurrency)

### Configuration (`app.async.config-hash-fetch`)
- **Core Pool Size**: `10`
- **Max Pool Size**: `20`
- **Queue Capacity**: `100`
- **Keep-Alive**: `60s`
- **Thread Name Prefix**: `async-config-hash-fetch-`
- **Virtual Threads**: `false` (platform threads)

### Resource Impact
- **Memory**: Medium (~10-20MB for platform thread stacks)
- **CPU**: Medium context switching overhead
- **Network I/O**: Parallel fetching improves throughput but increases concurrent connections
- **Threads**: Moderate thread count (10-20 threads)

### Notes
- Used during heartbeat batch processing to fetch configuration hashes in parallel
- Platform threads used to control concurrency (prevent overwhelming Config Server)
- Queue capacity limits memory usage during bursts

## Shutdown Configuration

All executor pools share shutdown configuration:

### `app.async.shutdown.await-timeout`
- **Default**: `30s`
- **What it does**: Maximum time to wait for tasks to complete during shutdown
- **Resource impact**:
  - **Latency**: Shutdown may wait up to 30s for tasks to complete
  - **Memory**: Tasks continue to consume memory during await period
- **Configuration location**: `app.async.shutdown.await-timeout` in `application-app.yml`

### `app.async.shutdown.force-shutdown`
- **Default**: `false`
- **What it does**: Whether to force shutdown without waiting for tasks to complete
- **Resource impact**:
  - **Reliability**: If false, waits for tasks to complete (may delay shutdown)
  - **Latency**: If true, forces shutdown immediately (may interrupt tasks)
- **Configuration location**: `app.async.shutdown.force-shutdown` in `application-app.yml`

## Thread Pool Growth Behavior

All executor pools follow this growth pattern:

1. **Initial**: Pool starts with `core-pool-size` threads
2. **Queue Filling**: New tasks go to queue until `queue-capacity` is reached
3. **Growth**: When queue is full, pool creates new threads up to `max-pool-size`
4. **Rejection**: When `max-pool-size` is reached and queue is full, tasks are rejected (policy: CallerRunsPolicy or AbortPolicy)
5. **Shrinking**: Idle threads beyond `core-pool-size` are terminated after `keep-alive` time

## Rejected Execution Policy

Executor pools use different policies:

- **Notification Executor**: `CallerRunsPolicy` (calling thread executes task)
- **RPC Executor**: `AbortPolicy` (throws exception on rejection)
- **Default Executor**: `CallerRunsPolicy`
- **Config Hash Fetch Executor**: `CallerRunsPolicy`

### CallerRunsPolicy
- **Behavior**: If pool is full, the calling thread executes the task
- **Resource impact**: Throttles task submission naturally (calling thread blocks)
- **Use case**: Backpressure mechanism for high-load scenarios

### AbortPolicy
- **Behavior**: If pool is full, throws RejectedExecutionException
- **Resource impact**: Fails fast (no throttling)
- **Use case**: Critical operations that should fail rather than block

## Context Propagation

All executor pools propagate:

1. **MDC (Mapped Diagnostic Context)**: Logging context for distributed tracing
2. **SecurityContext**: Security authentication context for authorized operations

This ensures async operations maintain observability and security context.

## Metrics

All executor pools are automatically instrumented by Spring Boot:

- **Platform Thread Executors**: Instrumented by `TaskExecutorMetricsAutoConfiguration`
- **Virtual Thread Executors**: Instrumented by Micrometer Java 21+ integration

Metrics include:
- `executor.active`: Number of active tasks
- `executor.completed`: Total completed tasks
- `executor.queue.size`: Current queue size
- `executor.pool.size`: Current pool size

Metrics are tagged by executor bean name (e.g., `notificationExecutor`, `defaultExecutor`).

## Resource Usage Summary

| Executor | Core Pool | Max Pool | Queue | Virtual Threads | Memory Impact | CPU Impact |
|----------|-----------|----------|-------|-----------------|---------------|------------|
| Notification | 4 | 8 | 100 | Yes | Very Low (~32-64KB) | Low |
| RPC | 1 | 2 | 10 | No | Low (~1-2MB) | Very Low |
| Default | 8 | 16 | 200 | No | Medium (~8-16MB) | Medium |
| Config Hash Fetch | 10 | 20 | 100 | No | Medium (~10-20MB) | Medium |

**Total Platform Threads**: 19-38 threads (excluding virtual threads, which use carrier threads)

## See Also

- [HTTP Server](http-server.md) - Tomcat thread pool for HTTP requests
- [RPC Servers](rpc-servers.md) - RPC server thread pools
- [Resilience](resilience.md) - Thread pool bulkhead configuration
- [Java Virtual Threads Documentation](https://openjdk.org/jeps/444)

