# Technology Choices

## Why Spring Boot 3.3 + Java 21?

### Decision

Use Spring Boot 3.3 with Java 21 for the backend services.

### Context

The system requires high performance, modern language features, and long-term support. Spring Boot 3.x requires Java 17+, and Java 21 provides significant improvements.

### Alternatives Considered

1. **Spring Boot 2.x + Java 17**
   - More stable, widely adopted
   - Missing virtual threads, records improvements
   - Less performance optimization

2. **Spring Boot 3.3 + Java 17**
   - Good balance
   - Missing Java 21 features (virtual threads, pattern matching)
   - Less future-proof

### Trade-offs

| Aspect | Spring Boot 3.3 + Java 21 | Spring Boot 2.x + Java 17 |
|--------|--------------------------|---------------------------|
| **Performance** | High (virtual threads, optimizations) | Medium |
| **Language Features** | Records, sealed classes, pattern matching | Limited |
| **Stability** | Newer, less battle-tested | More stable |
| **Learning Curve** | Higher | Lower |
| **Long-term Support** | Better (LTS) | Good (LTS) |

### Rationale

1. **Virtual Threads**: Better concurrency for I/O-bound operations
2. **Records**: Perfect for DTOs and value objects
3. **Sealed Classes**: Type-safe hierarchies
4. **Performance**: Better GC, optimized runtime
5. **Future-proof**: Java 21 is LTS, Spring Boot 3.x is the future

### When to Reconsider

- If team lacks Java 21 experience
- If stability is more important than features
- If third-party libraries don't support Java 21

---

## Why React 18 + TypeScript?

### Decision

Use React 18 with TypeScript for the admin dashboard.

### Context

The dashboard needs type safety, modern React features, and excellent developer experience.

### Alternatives Considered

1. **Vue.js 3**
   - Simpler learning curve
   - Less ecosystem
   - Less type safety

2. **Angular**
   - Full framework
   - More opinionated
   - Heavier bundle

3. **React + JavaScript**
   - Simpler setup
   - No type safety
   - More runtime errors

### Trade-offs

| Aspect | React 18 + TypeScript | Vue.js 3 | Angular |
|--------|----------------------|----------|---------|
| **Type Safety** | High | Medium | High |
| **Ecosystem** | Largest | Good | Good |
| **Learning Curve** | Medium | Low | High |
| **Bundle Size** | Medium | Small | Large |
| **Developer Experience** | Excellent | Good | Good |

### Rationale

1. **Type Safety**: Catch errors at compile time
2. **React 18 Features**: Concurrent rendering, Suspense, automatic batching
3. **Ecosystem**: Largest package ecosystem
4. **Developer Experience**: Excellent tooling (Vite, ESLint, Prettier)
5. **Team Familiarity**: React is widely known

### When to Reconsider

- If team prefers Vue.js or Angular
- If bundle size becomes critical
- If React ecosystem doesn't meet needs

---

## Why Keycloak over Auth0/Okta?

### Decision

Use self-hosted Keycloak for identity and access management.

### Context

The system needs OAuth2/OIDC, custom mappers, team-based access control, and cost-effective IAM.

### Alternatives Considered

1. **Auth0**
   - SaaS, no maintenance
   - Per-user pricing
   - Limited customization

2. **Okta**
   - Enterprise-grade
   - Expensive
   - Less flexible

3. **Spring Security OAuth2 Server**
   - Full control
   - More development effort
   - Less features out-of-box

### Trade-offs

| Aspect | Keycloak | Auth0 | Okta |
|--------|----------|-------|------|
| **Cost** | Low (self-hosted) | Per-user pricing | Expensive |
| **Customization** | High (custom mappers) | Limited | Limited |
| **Maintenance** | Self-managed | Managed | Managed |
| **Features** | Comprehensive | Good | Enterprise |
| **Setup Complexity** | Medium | Low | Low |

### Rationale

1. **Cost**: No per-user fees, self-hosted
2. **Customization**: Custom mappers for team claims
3. **Control**: Full control over configuration
4. **Features**: Comprehensive OAuth2/OIDC support
5. **Open Source**: No vendor lock-in

### Implementation

**Keycloak Features Used:**
- OAuth2/OIDC provider
- Custom protocol mappers (team membership, manager ID)
- Realm roles (SYS_ADMIN, USER)
- Groups (team-based access)
- Audience validator

**Reference:** `config-control-service/README-KEYCLOAK.md`

### When to Reconsider

- If maintenance overhead becomes too high
- If team lacks Keycloak expertise
- If per-user pricing becomes acceptable
- If enterprise features are required

---

## Why Kafka over RabbitMQ?

### Decision

Use Apache Kafka for event bus and messaging.

### Context

The system needs high-throughput event streaming for heartbeat processing and config refresh events.

### Alternatives Considered

1. **RabbitMQ**
   - Simpler setup
   - Lower throughput
   - Less suitable for event streaming

2. **Redis Pub/Sub**
   - Very fast
   - No persistence
   - Less features

3. **NATS**
   - Lightweight
   - Less ecosystem
   - Less features

### Trade-offs

| Aspect | Kafka | RabbitMQ | Redis Pub/Sub |
|--------|-------|----------|---------------|
| **Throughput** | Very High | High | Very High |
| **Persistence** | Yes (log) | Yes (queue) | No |
| **Event Streaming** | Native | Limited | No |
| **Complexity** | High | Medium | Low |
| **Scalability** | Excellent | Good | Good |

### Rationale

1. **High Throughput**: 10,000+ heartbeats/minute
2. **Event Streaming**: Native support for event streams
3. **Durability**: Persistent log for reliability
4. **Scalability**: Horizontal scaling
5. **Batch Processing**: Native batch consumer support

### Implementation

**Kafka Topics:**
- `heartbeat-queue` - Heartbeat ingestion
- `config-refresh` - Config refresh events

**Reference:** `config-control-service/src/main/java/com/example/control/application/service/infra/HeartbeatBatchProcessor.java`

### When to Reconsider

- If throughput requirements are lower
- If setup complexity becomes prohibitive
- If simpler messaging is sufficient

---

## Why Consul over Eureka?

### Decision

Use Consul for service discovery and KV store.

### Context

The system needs service discovery, health checks, and a KV store for future features.

### Alternatives Considered

1. **Eureka**
   - Simpler
   - No KV store
   - Netflix OSS (less active)

2. **Kubernetes Service Discovery**
   - Native if on K8s
   - Not applicable for non-K8s
   - Less flexible

3. **Zookeeper**
   - Good for coordination
   - Less suitable for service discovery
   - More complex

### Trade-offs

| Aspect | Consul | Eureka | K8s Service Discovery |
|--------|--------|--------|----------------------|
| **Service Discovery** | Excellent | Good | Native (K8s only) |
| **KV Store** | Yes | No | No |
| **Health Checks** | Comprehensive | Basic | Native (K8s) |
| **Multi-Datacenter** | Yes | Limited | Yes (K8s) |
| **Complexity** | Medium | Low | Low (if on K8s) |

### Rationale

1. **KV Store**: Future feature flags and configuration
2. **Health Checks**: Comprehensive health check support
3. **Multi-Datacenter**: Support for distributed deployments
4. **Active Development**: HashiCorp actively maintains
5. **Flexibility**: Works in any environment (not K8s-specific)

### Implementation

**Consul Features Used:**
- Service registration (SDK auto-registers)
- Health checks (TTL-based)
- Service discovery (client-side load balancing)
- KV store (future use)

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/client/http/HttpApiImpl.java`

### When to Reconsider

- If running on Kubernetes exclusively
- If KV store is not needed
- If simpler service discovery is sufficient

---

## Why Spring Cloud Gateway?

### Decision

Use Spring Cloud Gateway as the API gateway for routing, rate limiting, and resilience.

### Context

The system needs a single entry point for API requests from the Admin Dashboard and external clients. The gateway should provide service discovery, load balancing, rate limiting, circuit breaking, and CORS handling.

### Alternatives Considered

1. **Nginx**
   - High performance, battle-tested
   - Requires separate configuration management
   - Less Spring ecosystem integration
   - No built-in service discovery

2. **Kong**
   - Enterprise features (API management, plugins)
   - Requires separate infrastructure
   - More complex setup
   - Additional operational overhead

3. **Zuul (Netflix)**
   - Spring Cloud integration
   - Deprecated in favor of Gateway
   - Blocking I/O (less performant)
   - Not recommended for new projects

4. **No Gateway (Direct Access)**
   - Simpler architecture
   - No single entry point
   - Rate limiting and CORS in each service
   - More complex client configuration

### Trade-offs

| Aspect | Spring Cloud Gateway | Nginx | Kong | No Gateway |
|--------|---------------------|-------|------|------------|
| **Performance** | High (reactive) | Very High | High | N/A |
| **Spring Integration** | Excellent | None | Limited | N/A |
| **Service Discovery** | Built-in (Consul) | Manual config | Plugin | N/A |
| **Rate Limiting** | Built-in (Redis) | Lua scripts | Plugins | Per-service |
| **Setup Complexity** | Low | Medium | High | Low |
| **Operational Overhead** | Low | Medium | High | Distributed |

### Rationale

1. **Reactive Performance**: Built on Spring WebFlux, non-blocking I/O for high concurrency
2. **Spring Ecosystem**: Seamless integration with Spring Cloud (Consul, LoadBalancer)
3. **Built-in Features**: Circuit breaker, rate limiting, retry, CORS out-of-the-box
4. **Service Discovery**: Automatic integration with Consul for dynamic routing
5. **Developer Experience**: Java-based, familiar to Spring developers
6. **Configuration**: YAML-based route and filter configuration
7. **Observability**: Built-in metrics and tracing support

### Implementation

**Gateway Features:**
- Service discovery via Consul
- Load balancing (round-robin) across backend instances
- Circuit breaker (Resilience4j) with fallback
- Rate limiting (Redis-based, per-user)
- CORS handling
- JWT forwarding
- Correlation ID tracking

**Configuration:**
- Routes: `/api/**` → `lb://config-control-service`
- Filters: Circuit breaker, rate limiter, retry
- HTTP client: 500 max connections, 5s timeout

**Reference:** `gateway-service/README.md`, `gateway-service/src/main/resources/application-gateway.yml`

### When to Reconsider

- If performance requirements exceed Gateway capabilities
- If enterprise API management features are required (consider Kong)
- If infrastructure prefers Nginx for other reasons
- If running on Kubernetes with native ingress (consider K8s Ingress)

---

## Summary

Technology choices prioritize:
1. **Modern and Future-proof** (Spring Boot 3.3, Java 21, React 18)
2. **Cost-effectiveness** (Keycloak self-hosted)
3. **Performance and Scalability** (Kafka, Consul, Spring Cloud Gateway)
4. **Type Safety** (TypeScript)
5. **Best Tool for Job** (Each technology chosen for specific strengths)
6. **Spring Ecosystem Integration** (Gateway, Config Server, Consul Discovery)

