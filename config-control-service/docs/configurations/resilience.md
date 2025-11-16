# Resilience Configuration

## Overview

Circuit breaker, retry, bulkhead, time limiter, and rate limiter configuration using Resilience4j. These settings control fault tolerance and resource protection, affecting additional threads (thread pool bulkhead), memory (buffering), and network I/O (retries).

**Configuration File**: `application-resilience.yml`

## Circuit Breaker Configuration

Circuit breakers prevent cascading failures by opening when failure rate exceeds thresholds.

### Global Configuration (`resilience4j.circuitbreaker.configs.default`)

#### `slidingWindowSize`
- **Default**: `10`
- **What it does**: Number of calls to consider when calculating failure rate
- **Resource impact**:
  - **Memory**: Tracks last N call results in memory (minimal)
  - **CPU**: Sliding window calculation overhead (minimal)
- **Configuration location**: `resilience4j.circuitbreaker.configs.default.slidingWindowSize` in `application-resilience.yml`

#### `slidingWindowType`
- **Default**: `COUNT_BASED`
- **Options**: `COUNT_BASED`, `TIME_BASED`
- **What it does**: How to calculate sliding window (by call count or time)
- **Resource impact**: Same as `slidingWindowSize`
- **Configuration location**: `resilience4j.circuitbreaker.configs.default.slidingWindowType` in `application-resilience.yml`

#### `minimumNumberOfCalls`
- **Default**: `5`
- **What it does**: Minimum number of calls before calculating failure rate
- **Resource impact**: Prevents opening circuit on insufficient data
- **Configuration location**: `resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls` in `application-resilience.yml`

#### `failureRateThreshold`
- **Default**: `50` (50%)
- **What it does**: Failure rate percentage that triggers circuit opening
- **Resource impact**:
  - **Network I/O**: Opens circuit to prevent further calls (reduces network traffic)
  - **Latency**: Failed calls return immediately when circuit is open (faster failure)
- **Configuration location**: `resilience4j.circuitbreaker.configs.default.failureRateThreshold` in `application-resilience.yml`

#### `slowCallRateThreshold`
- **Default**: `50` (50%)
- **What it does**: Slow call rate percentage that triggers circuit opening
- **Resource impact**: Same as `failureRateThreshold`, but for slow calls
- **Configuration location**: `resilience4j.circuitbreaker.configs.default.slowCallRateThreshold` in `application-resilience.yml`

#### `slowCallDurationThreshold`
- **Default**: `3s`
- **What it does**: Duration threshold for considering a call "slow"
- **Resource impact**: Identifies slow calls that contribute to circuit opening
- **Configuration location**: `resilience4j.circuitbreaker.configs.default.slowCallDurationThreshold` in `application-resilience.yml`

#### `waitDurationInOpenState`
- **Default**: `30s`
- **What it does**: Time to wait in OPEN state before transitioning to HALF_OPEN
- **Resource impact**:
  - **Network I/O**: No calls are made during OPEN state (reduces network traffic)
  - **Latency**: Failed calls return immediately during OPEN state
- **Configuration location**: `resilience4j.circuitbreaker.configs.default.waitDurationInOpenState` in `application-resilience.yml`

#### `permittedNumberOfCallsInHalfOpenState`
- **Default**: `3`
- **What it does**: Number of calls allowed in HALF_OPEN state to test recovery
- **Resource impact**: 
  - **Network I/O**: Limited calls during HALF_OPEN state (reduces network traffic)
  - **Reliability**: Tests if service has recovered before fully opening circuit
- **Configuration location**: `resilience4j.circuitbreaker.configs.default.permittedNumberOfCallsInHalfOpenState` in `application-resilience.yml`

### Per-Service Circuit Breaker Instances

Individual services have custom circuit breaker configurations:

- **configserver**: `failureRateThreshold=50`, `slowCallDurationThreshold=5s`, `waitDurationInOpenState=30s`
- **consul**: `failureRateThreshold=50`, `slowCallDurationThreshold=3s`, `waitDurationInOpenState=20s`
- **keycloak**: `failureRateThreshold=60`, `slowCallDurationThreshold=5s`, `waitDurationInOpenState=40s`
- **email**: `failureRateThreshold=70`, `waitDurationInOpenState=60s`
- **mongodb**: `failureRateThreshold=50`, `slowCallDurationThreshold=3s`, `waitDurationInOpenState=30s`
- **kafka-producer**: `failureRateThreshold=50`, `slowCallDurationThreshold=5s`, `waitDurationInOpenState=30s`
- **cache-redis**: `failureRateThreshold=50`, `slowCallDurationThreshold=3s`, `waitDurationInOpenState=30s`

## Retry Configuration

Retries automatically retry failed operations with exponential backoff and jitter.

### Global Configuration (`resilience4j.retry.configs.default`)

#### `maxAttempts`
- **Default**: `3`
- **What it does**: Maximum number of retry attempts (initial attempt + retries)
- **Resource impact**:
  - **Network I/O**: Retries consume additional network resources
  - **CPU**: Re-processing consumes CPU cycles
  - **Latency**: Retries add latency (see backoff configuration)
- **Configuration location**: `resilience4j.retry.configs.default.maxAttempts` in `application-resilience.yml`

#### `waitDuration`
- **Default**: `500ms`
- **What it does**: Initial wait duration before first retry
- **Resource impact**: Adds latency to retry attempts
- **Configuration location**: `resilience4j.retry.configs.default.waitDuration` in `application-resilience.yml`

#### `enableExponentialBackoff`
- **Default**: `true`
- **What it does**: Enables exponential backoff (wait time doubles with each retry)
- **Resource impact**: Reduces retry frequency as failures persist
- **Configuration location**: `resilience4j.retry.configs.default.enableExponentialBackoff` in `application-resilience.yml`

#### `exponentialBackoffMultiplier`
- **Default**: `2.0`
- **What it does**: Multiplier for exponential backoff (delay = waitDuration × multiplier^attempt)
- **Resource impact**: Determines retry delay progression
- **Configuration location**: `resilience4j.retry.configs.default.exponentialBackoffMultiplier` in `application-resilience.yml`

#### `enableRandomizedWait`
- **Default**: `true`
- **What it does**: Enables jitter (randomized wait time to avoid thundering herd)
- **Resource impact**: Reduces simultaneous retries from multiple clients
- **Configuration location**: `resilience4j.retry.configs.default.enableRandomizedWait` in `application-resilience.yml`

#### `randomizedWaitFactor`
- **Default**: `0.5`
- **What it does**: Jitter factor (random wait = baseWait × (1 ± randomizedWaitFactor))
- **Resource impact**: Determines jitter range (50% variance)
- **Configuration location**: `resilience4j.retry.configs.default.randomizedWaitFactor` in `application-resilience.yml`

### Per-Service Retry Instances

Individual services have custom retry configurations:

- **configserver**: `maxAttempts=3`, `waitDuration=500ms`
- **consul**: `maxAttempts=3`, `waitDuration=300ms`
- **cache-redis**: `maxAttempts=3`, `waitDuration=100ms`, exponential backoff enabled
- **keycloak**: `maxAttempts=2`, `waitDuration=1s`
- **email-send**: `maxAttempts=3`, `waitDuration=1s`

## Bulkhead Configuration

Bulkheads limit concurrent calls to prevent resource exhaustion.

### Semaphore Bulkhead (`resilience4j.bulkhead`)

#### `maxConcurrentCalls`
- **Default**: `25`
- **What it does**: Maximum number of concurrent calls allowed
- **Resource impact**:
  - **Concurrency**: Limits concurrent operations to prevent resource exhaustion
  - **Latency**: Calls beyond limit wait (see `maxWaitDuration`)
- **Configuration location**: `resilience4j.bulkhead.configs.default.maxConcurrentCalls` in `application-resilience.yml`

#### `maxWaitDuration`
- **Default**: `100ms`
- **What it does**: Maximum time to wait for permit before rejection
- **Resource impact**: Determines timeout for waiting calls
- **Configuration location**: `resilience4j.bulkhead.configs.default.maxWaitDuration` in `application-resilience.yml`

### Per-Service Bulkhead Instances

Individual services have custom bulkhead configurations:

- **configserver**: `maxConcurrentCalls=20`
- **consul**: `maxConcurrentCalls=25`
- **keycloak**: `maxConcurrentCalls=15`
- **email**: `maxConcurrentCalls=10`
- **mongodb**: `maxConcurrentCalls=20`
- **kafka-producer**: `maxConcurrentCalls=15`

## Thread Pool Bulkhead Configuration

Thread pool bulkheads use dedicated thread pools to isolate resource usage.

### Global Configuration (`resilience4j.thread-pool-bulkhead.configs.default`)

#### `maxThreadPoolSize`
- **Default**: `10`
- **What it does**: Maximum number of threads in the bulkhead thread pool
- **Resource impact**:
  - **Memory**: Each thread consumes stack memory (~1MB per thread)
  - **CPU**: Context switching overhead for thread pool
  - **Threads**: Directly adds threads to the application (separate from executor pools)
- **Configuration location**: `resilience4j.thread-pool-bulkhead.configs.default.maxThreadPoolSize` in `application-resilience.yml`

#### `coreThreadPoolSize`
- **Default**: `5`
- **What it does**: Minimum number of threads kept alive in the pool
- **Resource impact**: Reserves memory for minimum threads
- **Configuration location**: `resilience4j.thread-pool-bulkhead.configs.default.coreThreadPoolSize` in `application-resilience.yml`

#### `queueCapacity`
- **Default**: `20`
- **What it does**: Maximum number of tasks that can wait in the queue
- **Resource impact**: Memory for queued tasks
- **Configuration location**: `resilience4j.thread-pool-bulkhead.configs.default.queueCapacity` in `application-resilience.yml`

#### `keepAliveDuration`
- **Default**: `20ms`
- **What it does**: Time idle threads beyond core size are kept alive
- **Resource impact**: Idle threads consume memory during keep-alive period
- **Configuration location**: `resilience4j.thread-pool-bulkhead.configs.default.keepAliveDuration` in `application-resilience.yml`

### Per-Service Thread Pool Bulkhead Instances

Individual services have custom thread pool bulkhead configurations:

- **configserver**: `maxThreadPoolSize=8`, `coreThreadPoolSize=4`
- **consul**: `maxThreadPoolSize=10`, `coreThreadPoolSize=5`
- **email**: `maxThreadPoolSize=5`, `coreThreadPoolSize=2`

## Time Limiter Configuration

Time limiters enforce maximum execution time for operations.

### Global Configuration (`resilience4j.timelimiter.configs.default`)

#### `timeoutDuration`
- **Default**: `5s`
- **What it does**: Maximum time allowed for operation execution
- **Resource impact**:
  - **Latency**: Operations exceeding timeout are cancelled/failed
  - **CPU**: Timeout checking overhead (minimal)
- **Configuration location**: `resilience4j.timelimiter.configs.default.timeoutDuration` in `application-resilience.yml`

#### `cancelRunningFuture`
- **Default**: `true`
- **What it does**: Whether to cancel running operation when timeout expires
- **Resource impact**: Frees resources by cancelling timed-out operations
- **Configuration location**: `resilience4j.timelimiter.configs.default.cancelRunningFuture` in `application-resilience.yml`

### Per-Service Time Limiter Instances

Individual services have custom time limiter configurations:

- **configserver**: `timeoutDuration=5s`
- **consul**: `timeoutDuration=3s`
- **keycloak**: `timeoutDuration=5s`
- **email**: `timeoutDuration=10s`
- **mongodb**: `timeoutDuration=3s`
- **kafka-producer**: `timeoutDuration=5s`

## Rate Limiter Configuration

Rate limiters limit the rate of operations (calls per time period).

### Global Configuration (`resilience4j.ratelimiter.configs.default`)

#### `limitForPeriod`
- **Default**: `50`
- **What it does**: Maximum number of calls allowed in a refresh period
- **Resource impact**:
  - **Throughput**: Limits maximum request rate
  - **Latency**: Calls beyond limit wait or are rejected
- **Configuration location**: `resilience4j.ratelimiter.configs.default.limitForPeriod` in `application-resilience.yml`

#### `limitRefreshPeriod`
- **Default**: `10s`
- **What it does**: Time period for rate limit window
- **Resource impact**: Determines rate limit window size
- **Configuration location**: `resilience4j.ratelimiter.configs.default.limitRefreshPeriod` in `application-resilience.yml`

#### `timeoutDuration`
- **Default**: `100ms`
- **What it does**: Maximum time to wait for permit before rejection
- **Resource impact**: Determines timeout for waiting calls
- **Configuration location**: `resilience4j.ratelimiter.configs.default.timeoutDuration` in `application-resilience.yml`

### Per-Service Rate Limiter Instances

Individual services have custom rate limiter configurations:

- **heartbeat-endpoint**: `limitForPeriod=50`, `limitRefreshPeriod=10s`, `timeoutDuration=0` (fail immediately)
- **admin-endpoints**: `limitForPeriod=100`, `limitRefreshPeriod=10s`

## Retry Budget Configuration

Retry budget limits global retry percentage to prevent retry storms.

### `resilience.retry-budget.enabled`
- **Default**: `true`
- **What it does**: Enables retry budget to limit retry percentage
- **Resource impact**: Prevents excessive retries that consume resources
- **Configuration location**: `resilience.retry-budget.enabled` in `application-resilience.yml`

### `resilience.retry-budget.window-size`
- **Default**: `10s`
- **What it does**: Time window for calculating retry percentage
- **Resource impact**: Determines retry budget calculation window
- **Configuration location**: `resilience.retry-budget.window-size` in `application-resilience.yml`

### `resilience.retry-budget.max-retry-percentage`
- **Default**: `20` (20%)
- **What it does**: Maximum percentage of requests that can be retries within window
- **Resource impact**: Limits retry percentage to prevent retry storms
- **Configuration location**: `resilience.retry-budget.max-retry-percentage` in `application-resilience.yml`

### Per-Service Retry Budget

Individual services have custom retry budget configurations:

- **configserver**: `max-retry-percentage=20`
- **consul**: `max-retry-percentage=25`
- **keycloak**: `max-retry-percentage=15`

## Deadline Propagation Configuration

Deadline propagation propagates request deadlines across service calls.

### `resilience.deadline-propagation.enabled`
- **Default**: `true`
- **What it does**: Enables deadline propagation via headers
- **Resource impact**: Minimal (header propagation)
- **Configuration location**: `resilience.deadline-propagation.enabled` in `application-resilience.yml`

### `resilience.deadline-propagation.default-timeout`
- **Default**: `30s`
- **What it does**: Default timeout if no deadline is propagated
- **Resource impact**: Determines default timeout for operations
- **Configuration location**: `resilience.deadline-propagation.default-timeout` in `application-resilience.yml`

### `resilience.deadline-propagation.header-name`
- **Default**: `X-Request-Deadline`
- **What it does**: HTTP header name for propagating deadlines
- **Resource impact**: None (header name only)
- **Configuration location**: `resilience.deadline-propagation.header-name` in `application-resilience.yml`

## Resource Usage Summary

| Component | CPU Impact | Memory Impact | Thread Impact | Network I/O Impact |
|-----------|-----------|---------------|---------------|-------------------|
| Circuit Breaker | Low (state tracking) | Low (state storage) | - | High (prevents calls) |
| Retry | Medium (re-processing) | Low (retry state) | - | High (retry attempts) |
| Semaphore Bulkhead | Low (permit checking) | Low (permit tracking) | - | Indirect (limits concurrency) |
| Thread Pool Bulkhead | Medium (context switching) | Medium (thread stacks) | Direct (5-10 threads) | Indirect (limits concurrency) |
| Time Limiter | Low (timeout checking) | Low | - | Indirect (cancels long ops) |
| Rate Limiter | Low (rate tracking) | Low (rate state) | - | Indirect (limits rate) |

**Total Additional Threads**: 5-10 threads (thread pool bulkhead, varies by service)

## See Also

- [Executor Pools](executor-pools.md) - Async executor pools (separate from thread pool bulkhead)
- [HTTP Clients](http-clients.md) - HTTP client timeouts and retries
- [Resilience4j Documentation](https://resilience4j.readme.io/)

