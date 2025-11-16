# Design Patterns Deep Dive
## Implementation Details and Code References

---

## 1. Strategy Pattern

### Purpose
Support multiple protocols and algorithms with interchangeable implementations.

### Implementations

#### Ping Strategy Pattern

**Location:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/pingconfig/strategy/`

**Interfaces:**
- `PingStrategy` - Base interface for ping protocols

**Implementations:**
- `HttpRestPingStrategy` - HTTP REST protocol
- `ThriftRpcPingStrategy` - Apache Thrift RPC
- `GrpcPingStrategy` - gRPC protocol
- `KafkaPingStrategy` - Kafka messaging

**Configuration:** SDK auto-configures based on `zcm.sdk.ping.protocol` property

**Benefits:**
- Easy to add new protocols
- Protocol-specific optimizations
- Client flexibility

#### Load Balancing Strategy Pattern

**Location:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/client/loadbalancer/strategy/`

**Implementations:**
- `RoundRobinStrategy` - Sequential instance selection
- `RandomStrategy` - Random instance selection
- `WeightedRandomStrategy` - Weighted random selection
- `RendezvousHashingStrategy` - Rendezvous hashing
- `ConsistentHashingStrategy` - Consistent hashing

**Configuration:** `zcm.sdk.loadbalancer.strategy` property

**Benefits:**
- Algorithm flexibility
- Performance optimization
- Client-side load balancing

---

## 2. Circuit Breaker Pattern

### Purpose
Prevent cascading failures by stopping requests to failing services.

### Implementation

**Library:** Resilience4j

**Configuration:** `application-resilience.yml:4-59`

**Circuit Breaker States:**
- **CLOSED**: Normal operation, requests flow through
- **OPEN**: Threshold exceeded, requests fail fast
- **HALF_OPEN**: Testing if service recovered

**Per-Service Configuration:**

| Service | Failure Rate | Wait Duration | Slow Call Threshold |
|---------|--------------|---------------|---------------------|
| ConfigServer | 50% | 30s | 5s |
| Consul | 50% | 20s | 3s |
| Keycloak | 60% | 40s | 5s |
| MongoDB | 50% | 30s | 3s |

**Implementation:** `infrastructure/resilience/ResilienceDecoratorsFactory.java`

**Metrics:**
- `resilience4j.circuitbreaker.calls{name, kind}`
- `resilience4j.circuitbreaker.state{name}` - 0=CLOSED, 1=HALF_OPEN, 2=OPEN

---

## 3. Retry Pattern with Exponential Backoff

### Purpose
Handle transient failures with automatic retry and exponential backoff.

### Implementation

**Configuration:** `application-resilience.yml:61-105`

**Features:**
- Exponential backoff multiplier: 2.0
- Randomized jitter: 0.5 factor
- Max attempts: 3

**Retry Sequence:**
1. Initial attempt fails
2. Wait ~500ms (with jitter)
3. 2nd attempt fails
4. Wait ~1000ms (with jitter)
5. 3rd attempt

**Retry Budget:**
- Custom implementation to prevent cascading failures
- Max retry percentage: 20% of requests
- Sliding window: 10 seconds

**Implementation:** `infrastructure/resilience/RetryBudgetTracker.java`

**Metrics:**
- `retry.budget.allowed{service}`
- `retry.budget.rejected{service}`
- `retry.budget.utilization{service}`

---

## 4. Bulkhead Pattern

### Purpose
Limit concurrent calls to prevent resource exhaustion.

### Implementation

**Two Types:**

#### Semaphore Bulkhead
- Limits concurrent calls using semaphore
- Configuration: `maxConcurrentCalls: 20-25`
- Max wait duration: 100ms

#### Thread Pool Bulkhead
- Isolates calls in separate thread pool
- Configuration: `maxThreadPoolSize: 8-10`, `coreThreadPoolSize: 4-5`

**Configuration:** `application-resilience.yml:107-152`

**Metrics:**
- `resilience4j.bulkhead.available.concurrent.calls{name}`
- `resilience4j.bulkhead.max.allowed.concurrent.calls{name}`

---

## 5. Batch Processing Pattern

### Purpose
Optimize throughput for high-volume operations.

### Implementation

**Location:** `application/service/infra/HeartbeatBatchService.java`

**Process:**
1. Collect heartbeats in Kafka queue
2. Batch load ServiceInstances by IDs
3. Batch load ApplicationServices by names
4. Batch fetch config hashes (grouped by service:env)
5. Process each heartbeat in memory
6. Bulk upsert ServiceInstances
7. Save drift events if any

**Configuration:**
- Batch size: 50-100 heartbeats (`application-app.yml:133`)
- Consumer concurrency: 10 (`application-app.yml:132`)

**Performance Improvement:** 5x throughput vs single processing

**Metrics:**
- `heartbeat.batch.processing.time` - Batch processing duration
- `heartbeat.batch.size` - Current batch size

---

## 6. CQRS Pattern (Command Query Responsibility Segregation)

### Purpose
Separate read and write operations for performance and scalability.

### Implementation

**Command Services (Write):**
- Location: `application/command/`
- Operations: Create, Update, Delete
- Optimized for consistency

**Query Services (Read):**
- Location: `application/query/`
- Operations: Find, List, Search
- Optimized for performance (aggregations, projections)

**Benefits:**
- Independent scaling
- Optimized read queries
- Clear separation of concerns

**Example:**
- `ApplicationServiceCommandService` - Write operations
- `ApplicationServiceQueryService` - Read operations with filtering

---

## 7. Repository Pattern

### Purpose
Abstract data access layer from business logic.

### Implementation

**Port Interface:** `domain/port/repository/RepositoryPort.java`

**Adapters:**
- `infrastructure/adapter/persistence/mongo/repository/` - MongoDB implementations

**Benefits:**
- Easy to swap data stores
- Testability (mock repositories)
- Clear data access contracts

---

## 8. Factory Pattern

### Purpose
Create complex objects with consistent initialization.

### Implementations

**ApplicationServiceFactory:**
- Location: `infrastructure/seeding/factory/ApplicationServiceFactory.java`
- Purpose: Generate realistic test data

**Other Factories:**
- `ServiceInstanceFactory`
- `DriftEventFactory`
- `ApprovalRequestFactory`

---

## 9. Observer Pattern

### Purpose
Notify multiple listeners of domain events.

### Implementation

**Domain Events:**
- `ApprovalRequestApprovedEvent`
- `ServiceOwnershipTransferred`

**Event Listeners:**
- `EmailNotificationEventListener`
- `ServiceOwnershipEventListener`

**Location:** `application/event/`

---

## 10. Decorator Pattern

### Purpose
Add resilience behaviors to operations dynamically.

### Implementation

**ResilienceDecoratorsFactory:**
- Location: `infrastructure/resilience/ResilienceDecoratorsFactory.java`
- Decorator Chain:
  1. Deadline check
  2. Retry budget
  3. Circuit breaker
  4. Retry with exponential backoff
  5. Bulkhead
  6. Time limiter
  7. Fallback

**Usage:**
```java
Supplier<String> decorated = resilienceFactory.decorateSupplier(
    "configserver",
    () -> restClient.get(url),
    fallbackValue
);
```

---

## Pattern Selection Rationale

### Why Strategy Pattern for Ping?
- Multiple protocols needed (HTTP, Thrift, gRPC, Kafka)
- Easy to add new protocols
- Protocol-specific optimizations

### Why Circuit Breaker?
- Prevent cascading failures
- Fail-fast for better user experience
- Automatic recovery testing

### Why Batch Processing?
- High-volume heartbeat processing
- 5x throughput improvement
- Reduced database round-trips

### Why CQRS?
- Optimized read queries
- Independent scaling
- Clear separation of concerns

---

## References

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Cloud Patterns](https://spring.io/projects/spring-cloud)
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/)

