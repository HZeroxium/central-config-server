# Pattern Rationale

## Why Strategy Pattern for Ping Protocols?

### Decision

Use Strategy Pattern to support multiple ping protocols (HTTP, Thrift, gRPC, Kafka).

### Context

Different client environments may require different protocols. The SDK needs to support HTTP (standard), Thrift (high-performance RPC), gRPC (modern microservices), and Kafka (high-throughput async).

### Alternatives Considered

1. **Template Method Pattern**
   - Less flexible
   - Harder to add new protocols
   - More inheritance coupling

2. **Single Protocol (HTTP only)**
   - Simpler
   - Less flexible
   - Performance limitations

3. **Factory with if/else**
   - Simple
   - Not extensible
   - Violates Open/Closed Principle

### Trade-offs

| Aspect | Strategy Pattern | Template Method | Single Protocol |
|--------|-----------------|-----------------|-----------------|
| **Flexibility** | High | Medium | Low |
| **Extensibility** | Easy to add protocols | Harder | Not applicable |
| **Testability** | High (mock strategies) | Medium | Low |
| **Complexity** | Medium | Low | Low |
| **Performance** | Protocol-specific | Generic | Limited |

### Rationale

1. **Extensibility**: Easy to add new protocols without modifying existing code
2. **Testability**: Each strategy can be tested independently
3. **Protocol Optimization**: Each protocol can be optimized independently
4. **Open/Closed Principle**: Open for extension, closed for modification
5. **Client Flexibility**: Clients can choose the best protocol for their environment

### Implementation

**Strategy Interface:**
```java
public interface PingStrategy {
    void ping(HeartbeatPayload payload);
    PingProtocol getProtocol();
}
```

**Implementations:**
- `HttpRestPingStrategy` - HTTP REST
- `ThriftRpcPingStrategy` - Apache Thrift
- `GrpcPingStrategy` - gRPC
- `KafkaPingStrategy` - Kafka messaging

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/pingconfig/strategy/`

### When to Reconsider

- If only one protocol is ever needed
- If protocol switching becomes too complex
- If performance differences are negligible

---

## Why CQRS for Service Management?

### Decision

Separate command and query services for service management operations.

### Context

The system has different read and write patterns:
- **Writes**: Infrequent, need validation and business logic
- **Reads**: Frequent, need optimized queries and aggregations

### Alternatives Considered

1. **Traditional CRUD**
   - Simpler
   - Less optimized
   - Mixed concerns

2. **Event Sourcing**
   - Full audit trail
   - More complex
   - Overkill for this use case

3. **Repository Pattern Only**
   - Simple
   - No separation of concerns
   - Less optimized

### Trade-offs

| Aspect | CQRS | Traditional CRUD | Event Sourcing |
|--------|------|-----------------|----------------|
| **Read Optimization** | High | Medium | High |
| **Write Optimization** | High | Medium | Medium |
| **Complexity** | Medium | Low | High |
| **Scalability** | High (independent scaling) | Medium | High |
| **Audit Trail** | Basic | Basic | Complete |

### Rationale

1. **Read Optimization**: Query services can use aggregations, projections, and optimized queries
2. **Write Optimization**: Command services focus on validation and business logic
3. **Independent Scaling**: Read and write workloads can scale independently
4. **Clear Separation**: Clear boundaries between read and write operations
5. **Future-Proof**: Easy to add read replicas or caching layers

### Implementation

**Command Services:**
- `ApplicationServiceCommandService` - Create, update, delete services
- `ServiceInstanceCommandService` - Update instances
- `ApprovalRequestCommandService` - Create, approve, reject requests

**Query Services:**
- `ApplicationServiceQueryService` - Find services with optimized queries
- `ServiceInstanceQueryService` - Find instances with filters
- `DriftEventQueryService` - Query drift events with aggregations

**Reference:** `config-control-service/src/main/java/com/example/control/application/command/` and `query/`

### When to Reconsider

- If read/write patterns become similar
- If complexity outweighs benefits
- If team lacks CQRS experience

---

## Why Circuit Breaker Pattern?

### Decision

Use Resilience4j Circuit Breaker pattern for external service calls.

### Context

The system depends on external services (Config Server, Consul, Keycloak, MongoDB) that can fail. Cascading failures must be prevented.

### Alternatives Considered

1. **Retry Only**
   - Simpler
   - Doesn't prevent cascading failures
   - Can overwhelm failing services

2. **Timeout Only**
   - Simple
   - Doesn't prevent repeated calls to failing services
   - No state tracking

3. **No Resilience**
   - Simplest
   - Vulnerable to cascading failures
   - Poor user experience

### Trade-offs

| Aspect | Circuit Breaker | Retry Only | No Resilience |
|--------|----------------|------------|---------------|
| **Cascading Failure Prevention** | High | Low | None |
| **Complexity** | Medium | Low | None |
| **State Tracking** | Yes | No | No |
| **Fail-Fast** | Yes | No | No |
| **Recovery Detection** | Yes | No | No |

### Rationale

1. **Cascading Failure Prevention**: Prevents overwhelming failing services
2. **Fail-Fast**: Quick failure detection and response
3. **Recovery Detection**: Automatically detects when services recover
4. **State Tracking**: Tracks service health over time
5. **User Experience**: Better error messages and fallback handling

### Implementation

**Circuit Breaker Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      configserver:
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        slidingWindowSize: 10
```

**Services Protected:**
- Config Server
- Consul
- Keycloak
- MongoDB (via Spring Data)

**Reference:** `config-control-service/src/main/resources/application-resilience.yml`

### When to Reconsider

- If external services are always reliable
- If complexity outweighs benefits
- If simpler retry logic is sufficient

---

## Why Load Balancer Strategy Pattern?

### Decision

Use Strategy Pattern for client-side load balancing with multiple algorithms.

### Context

Different use cases require different load balancing strategies:
- **Round Robin**: Even distribution
- **Random**: Simple, no state
- **Weighted Random**: Based on instance health
- **Rendezvous Hashing**: Consistent routing by key
- **Consistent Hashing**: Ring-based distribution

### Alternatives Considered

1. **Single Algorithm (Round Robin)**
   - Simpler
   - Less flexible
   - Not optimal for all use cases

2. **Server-Side Load Balancing**
   - Simpler client
   - Less control
   - Additional infrastructure

3. **No Load Balancing**
   - Simplest
   - Single point of failure
   - No distribution

### Trade-offs

| Aspect | Strategy Pattern | Single Algorithm | Server-Side |
|--------|-----------------|------------------|-------------|
| **Flexibility** | High | Low | Medium |
| **Client Control** | High | High | Low |
| **Complexity** | Medium | Low | Low (client) |
| **Performance** | Optimal per use case | Generic | Good |
| **Infrastructure** | None | None | Required |

### Rationale

1. **Use Case Optimization**: Each strategy optimized for specific use cases
2. **Client-Side Control**: No additional infrastructure needed
3. **Extensibility**: Easy to add new strategies
4. **Performance**: Can choose best strategy per use case
5. **Testability**: Each strategy can be tested independently

### Implementation

**Strategies:**
- `RoundRobinLoadBalancerStrategy`
- `RandomLoadBalancerStrategy`
- `WeightedRandomLoadBalancerStrategy`
- `RendezvousLoadBalancerStrategy`
- `ConsistentHashingLoadBalancerStrategy`

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/client/loadbalancer/strategy/`

### When to Reconsider

- If only one strategy is ever needed
- If server-side load balancing becomes available
- If complexity outweighs benefits

---

## Summary

Design patterns chosen for:
1. **Extensibility** (Strategy Pattern)
2. **Performance** (CQRS)
3. **Resilience** (Circuit Breaker)
4. **Flexibility** (Load Balancer Strategies)

Each pattern addresses specific requirements while maintaining code quality and testability.

