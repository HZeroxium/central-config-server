# Resilience4j Patterns Implementation

## Overview

This document describes the comprehensive resilience patterns implemented in the Config Control Service using Resilience4j. The implementation follows best practices for fault-tolerant microservices architecture.

## Resilience Patterns Implemented

### 1. Circuit Breaker

Circuit breakers prevent cascading failures by stopping requests to failing services after a threshold is exceeded.

**Configured Instances:**

- `configserver` - For Config Server calls
- `consul` - For Consul registry calls
- `keycloak` - For Keycloak IAM calls
- `email` - For email notification sends

**Configuration:**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      configserver:
        slidingWindowSize: 10
        failureRateThreshold: 50%
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
```

**States:**

- **CLOSED** - Normal operation, requests flow through
- **OPEN** - Threshold exceeded, requests fail fast
- **HALF_OPEN** - Testing if service recovered

### 2. Retry with Exponential Backoff + Jitter

Automatic retry of failed operations with exponential backoff and randomized wait to prevent thundering herd.

**Configuration:**

```yaml
resilience4j:
  retry:
    instances:
      configserver:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2.0
        enableRandomizedWait: true # Jitter
        randomizedWaitFactor: 0.5
```

**Retry Sequence:**

1. Initial attempt fails
2. Wait ~500ms (with jitter)
3. 2nd attempt fails
4. Wait ~1000ms (with jitter)
5. 3rd attempt

### 3. Retry Budget

Custom implementation to prevent cascading failures by limiting retry rate within a sliding time window.

**Configuration:**

```yaml
resilience:
  retry-budget:
    enabled: true
    window-size: 10s
    max-retry-percentage: 20 # Max 20% of requests can be retries
    per-service:
      configserver:
        max-retry-percentage: 20
      consul:
        max-retry-percentage: 25
```

**Implementation:**

- `RetryBudgetTracker` tracks request/retry ratio per service
- Sliding window of 10 seconds
- Rejects retries if budget exceeded
- Metrics: `retry.budget.allowed`, `retry.budget.rejected`

### 4. Bulkhead (Concurrency Limiting)

Limits concurrent calls to prevent resource exhaustion and isolate failures.

**Two Types Configured:**

**Semaphore Bulkhead:**

```yaml
resilience4j:
  bulkhead:
    instances:
      configserver:
        maxConcurrentCalls: 20
        maxWaitDuration: 100ms
```

**ThreadPool Bulkhead:**

```yaml
resilience4j:
  thread-pool-bulkhead:
    instances:
      configserver:
        maxThreadPoolSize: 8
        coreThreadPoolSize: 4
        queueCapacity: 20
```

### 5. Time Limiter

Sets maximum execution time for operations to prevent hanging requests.

**Configuration:**

```yaml
resilience4j:
  timelimiter:
    instances:
      configserver:
        timeoutDuration: 5s
        cancelRunningFuture: true
      consul:
        timeoutDuration: 3s
```

### 6. Rate Limiting

Protects public endpoints from abuse with request rate limits per IP address.

**Configuration:**

```yaml
resilience4j:
  ratelimiter:
    instances:
      heartbeat-endpoint:
        limitForPeriod: 50
        limitRefreshPeriod: 10s
        timeoutDuration: 0 # Fail immediately
```

**Implementation:**

- `RateLimitingFilter` applies to `/api/heartbeat`
- Extracts client IP from X-Forwarded-For or remote address
- Returns HTTP 429 (Too Many Requests) when exceeded
- Metrics: `ratelimit.allowed`, `ratelimit.rejected`

### 7. Deadline Propagation

Propagates request deadlines across service call chains for coordinated timeouts.

**Configuration:**

```yaml
resilience:
  deadline-propagation:
    enabled: true
    default-timeout: 30s
    header-name: X-Request-Deadline
```

**Implementation:**

- `RequestDeadlineContext` - ThreadLocal deadline storage
- `DeadlinePropagationFilter` - Extracts deadline from headers
- Components check `RequestDeadlineContext.isExpired()` before operations
- Integrated with MDC for distributed tracing

## Architecture Components

### Core Infrastructure

#### 1. ResilienceDecoratorsFactory

Central factory for creating resilience-decorated callables and suppliers.

**Usage:**

```java
@Autowired
private ResilienceDecoratorsFactory resilienceFactory;

// For GET operations (with retry)
Supplier<String> decoratedCall = resilienceFactory.decorateSupplier(
    "configserver",
    () -> restClient.get(url),
    fallbackValue
);

// For POST/PUT/DELETE operations (no retry for idempotency)
Supplier<Boolean> decoratedWrite = resilienceFactory.decorateSupplierWithoutRetry(
    "consul",
    () -> consulClient.put(key, value),
    false
);
```

**Decorator Chain:**

1. Deadline check (via RequestDeadlineContext)
2. Record request (for retry budget)
3. Circuit Breaker
4. Retry (with budget check)
5. Bulkhead
6. Time Limiter
7. Fallback

#### 2. RequestDeadlineContext

ThreadLocal-based context for deadline propagation.

**Methods:**

```java
// Set deadline
RequestDeadlineContext.setDeadline(Instant.now().plusSeconds(30));
RequestDeadlineContext.setDeadlineFromTimeout(Duration.ofSeconds(30));

// Check deadline
if (RequestDeadlineContext.isExpired()) {
    throw new DeadlineExceededException(...);
}

Optional<Duration> remaining = RequestDeadlineContext.getRemainingTime();

// Cleanup (in finally blocks)
RequestDeadlineContext.clear();
```

#### 3. RetryBudgetTracker

Tracks retry budget per service with sliding window.

**Methods:**

```java
// Check if retry allowed
boolean canRetry = retryBudgetTracker.canRetry("configserver");

// Record request
retryBudgetTracker.recordRequest("configserver");

// Get utilization
double utilization = retryBudgetTracker.getRetryUtilization("configserver");
```

### Service Clients with Resilience

#### ConfigServerClient

- **READ** operations: Circuit Breaker + Retry + Bulkhead + TimeLimiter
- **Fallback**: Returns cached configuration
- **Cache**: `@Cacheable` on `getEnvironment()` methods

#### ConsulClient

- **READ** operations: Circuit Breaker + Retry + Bulkhead + TimeLimiter
- **WRITE** operations: Circuit Breaker + Bulkhead only (no retry for idempotency)
- **Fallback**: Returns cached service registry data

#### EmailNotificationAdapter

- **Operations**: Circuit Breaker + Retry + Bulkhead
- **Fallback**: Logs failure, doesn't throw exception (best-effort)
- **Rationale**: Email notifications shouldn't fail business operations

### Fallback Strategies

#### CachedFallbackProvider

Generic cache-based fallback using Spring Cache abstraction.

**Service-Specific Fallbacks:**

- `ConfigServerFallback` - Returns last known config
- `ConsulFallback` - Returns cached service list/health data
- `KeycloakFallback` - Returns cached IAM data

**Usage:**

```java
Optional<String> cached = cachedFallbackProvider.getFromCache(
    "consul-services",
    "catalog",
    String.class
);
```

## Metrics & Observability

### Resilience4j Metrics

Automatically exported to Micrometer:

**Circuit Breaker:**

- `resilience4j.circuitbreaker.calls{name, kind}`
- `resilience4j.circuitbreaker.state{name}` - 0=CLOSED, 1=HALF_OPEN, 2=OPEN

**Retry:**

- `resilience4j.retry.calls{name, kind}`

**Bulkhead:**

- `resilience4j.bulkhead.available.concurrent.calls{name}`
- `resilience4j.bulkhead.max.allowed.concurrent.calls{name}`

**Rate Limiter:**

- `resilience4j.ratelimiter.available.permissions{name}`
- `resilience4j.ratelimiter.waiting.threads{name}`

### Custom Metrics

#### ResilienceMetrics

- `retry.budget.utilization{service}` - Retry budget usage percentage
- `circuitbreaker.state{name}` - Circuit breaker state gauge

#### Rate Limiting Filter

- `ratelimit.allowed{endpoint}` - Allowed requests counter
- `ratelimit.rejected{endpoint}` - Rejected requests counter

### Health Indicators

#### CircuitBreakerHealthIndicator

Custom health indicator at `/actuator/health`:

```json
{
  "circuitBreakerHealth": {
    "status": "UP",
    "details": {
      "configserver": {
        "state": "CLOSED",
        "failureRate": "0.00%",
        "slowCallRate": "0.00%",
        "bufferedCalls": 5
      },
      "summary": {
        "total": 4,
        "open": 0,
        "closed": 4
      }
    }
  }
}
```

**Health Status:**

- **DOWN** - If any critical circuit breaker (`configserver`, `consul`) is OPEN
- **UP** with warning - If non-critical circuit breaker is OPEN
- **UP** - All circuit breakers CLOSED

## Configuration Tuning Guide

### Circuit Breaker Tuning

**For stable dependencies (ConfigServer, Consul):**

```yaml
failureRateThreshold: 50 # Open after 50% failures
slidingWindowSize: 10 # Over last 10 calls
waitDurationInOpenState: 30s # Wait 30s before half-open
```

**For flaky dependencies:**

```yaml
failureRateThreshold: 70 # More lenient threshold
waitDurationInOpenState: 60s # Longer recovery time
```

### Retry Tuning

**For transient failures:**

```yaml
maxAttempts: 3
waitDuration: 500ms
exponentialBackoffMultiplier: 2.0
```

**For expensive operations:**

```yaml
maxAttempts: 2 # Fewer retries
waitDuration: 1s # Longer initial wait
```

### Bulkhead Tuning

**Calculate max concurrent calls:**

```
maxConcurrentCalls = (Expected_RPS × Timeout_Seconds) × Safety_Factor
```

Example: 100 RPS, 3s timeout, 1.5x safety:

```
maxConcurrentCalls = (100 × 3) × 1.5 = 450
```

### Retry Budget Tuning

**Production recommendation:**

```yaml
max-retry-percentage: 20 # Conservative for production
window-size: 10s
```

**Development/testing:**

```yaml
max-retry-percentage: 50 # More lenient for testing
```

## Monitoring & Alerting Recommendations

### Critical Alerts

1. **Circuit Breaker OPEN (Critical Services)**

   ```promql
   circuitbreaker_state{name=~"configserver|consul"} == 2
   ```

2. **High Retry Budget Utilization**

   ```promql
   retry_budget_utilization{service="configserver"} > 15
   ```

3. **Rate Limit Rejections Spike**
   ```promql
   rate(ratelimit_rejected_total[1m]) > 10
   ```

### Warning Alerts

1. **Circuit Breaker HALF_OPEN**

   ```promql
   circuitbreaker_state == 1
   ```

2. **Bulkhead Queue Filling**

   ```promql
   resilience4j_bulkhead_available_concurrent_calls < 5
   ```

3. **High Failure Rate (approaching threshold)**
   ```promql
   resilience4j_circuitbreaker_failure_rate > 40
   ```

## Runbook: Circuit Breaker Scenarios

### Scenario: ConfigServer Circuit OPEN

**Symptoms:**

- `/actuator/health` shows DOWN
- Logs show "ConfigServer circuit breaker OPEN"
- Applications using cached config

**Diagnosis:**

1. Check ConfigServer health: `curl http://configserver:8888/actuator/health`
2. Check circuit breaker metrics in Grafana
3. Review ConfigServer logs for errors

**Resolution:**

1. If ConfigServer is down: Restart ConfigServer
2. If network issue: Check connectivity
3. If transient: Wait for circuit to half-open (30s)
4. Manual reset: Call `/actuator/circuitbreakers/configserver` with state=CLOSED (if safe)

**Prevention:**

- Increase ConfigServer resources if under load
- Tune circuit breaker thresholds if too sensitive
- Implement ConfigServer HA setup

### Scenario: Retry Budget Exceeded

**Symptoms:**

- Logs show "Retry budget exceeded for configserver"
- Metric `retry.budget.rejected` increasing

**Diagnosis:**

1. Check failure rate of underlying service
2. Review retry metrics: `retry.budget.utilization`
3. Check if circuit breaker should have opened

**Resolution:**

1. Fix root cause of failures in dependency
2. Temporarily increase retry budget if justified
3. Review circuit breaker thresholds - may need to fail-fast sooner

### Scenario: Rate Limit Rejecting Legitimate Traffic

**Symptoms:**

- Users report HTTP 429 errors
- Metric `ratelimit.rejected` high

**Diagnosis:**

1. Check if legitimate spike in traffic
2. Review IP distribution of requests
3. Check for DDoS or abuse

**Resolution:**

1. If legitimate: Increase rate limit temporarily
2. If DDoS: Implement upstream rate limiting (WAF, CDN)
3. Consider per-user rate limiting instead of per-IP

## Testing Resilience

### Circuit Breaker Testing

```bash
# Simulate failures to open circuit
for i in {1..20}; do
  curl -w "%{http_code}\n" http://localhost:8080/api/services
done

# Check circuit breaker state
curl http://localhost:8080/actuator/circuitbreakers | jq '.circuitBreakers.configserver.state'
```

### Retry Budget Testing

```bash
# Generate high failure rate
ab -n 100 -c 10 http://localhost:8080/api/config/failing-endpoint

# Check retry budget utilization
curl http://localhost:8080/actuator/metrics/retry.budget.utilization
```

### Rate Limiting Testing

```bash
# Exceed rate limit
for i in {1..100}; do
  curl -w "%{http_code}\n" http://localhost:8080/api/heartbeat
done
```

## Best Practices

### 1. Idempotency Considerations

- **GET operations**: Use full resilience (CB + Retry + Bulkhead)
- **POST/PUT/DELETE**: No retry unless idempotent
- **Use idempotency keys** for non-idempotent retries

### 2. Timeout Hierarchies

Ensure timeouts are properly nested:

```
Client Timeout (30s)
  > Circuit Breaker Slow Call Threshold (5s)
    > Time Limiter (3s)
      > HTTP Client Read Timeout (2s)
```

### 3. Fallback Design

- **Cache-based fallbacks** for read operations
- **Default values** for non-critical data
- **Fail-fast** for write operations
- **Log fallback usage** for monitoring

### 4. Deadline Propagation

Always propagate deadlines in distributed calls:

```java
Instant deadline = RequestDeadlineContext.getDeadline().orElse(null);
if (deadline != null) {
    restClient.header("X-Request-Deadline", deadline.toString());
}
```

### 5. ThreadLocal Cleanup

Always clear ThreadLocal in finally blocks:

```java
try {
    RequestDeadlineContext.setDeadline(...);
    // ... operations
} finally {
    RequestDeadlineContext.clear();
}
```

## Troubleshooting

### Issue: Circuit breaker not opening

**Causes:**

- `minimumNumberOfCalls` not reached
- Sliding window too large
- Wrong exception types configured

**Fix:**

- Lower `minimumNumberOfCalls` for testing
- Review `recordExceptions` configuration
- Check logs for actual exception types

### Issue: Retries not working

**Causes:**

- Retry budget exceeded
- Exception not in `retryExceptions` list
- Circuit breaker already OPEN

**Fix:**

- Check retry budget metrics
- Add exception type to configuration
- Review circuit breaker state

### Issue: Bulkhead rejections under normal load

**Causes:**

- `maxConcurrentCalls` too low
- Slow downstream service
- ThreadPool exhaustion

**Fix:**

- Increase bulkhead limits
- Add time limiter to prevent hanging calls
- Investigate downstream performance

## References

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Cloud CircuitBreaker](https://spring.io/projects/spring-cloud-circuitbreaker)
- [Netflix Hystrix (deprecated, patterns still relevant)](https://github.com/Netflix/Hystrix/wiki)
- [Release It! - Michael Nygard](https://pragprog.com/titles/mnee2/release-it-second-edition/)

## Version History

- **v1.0.0** - Initial implementation with all core resilience patterns
  - Circuit Breaker
  - Retry with exponential backoff + jitter
  - Retry budget
  - Bulkhead (semaphore + thread pool)
  - Time limiter
  - Rate limiting
  - Deadline propagation
  - Comprehensive metrics and health indicators
