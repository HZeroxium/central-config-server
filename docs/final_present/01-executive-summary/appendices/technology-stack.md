# Technology Stack Details
## Centralized Configuration Management System

---

## Backend Stack

### Core Framework

#### Spring Boot 3.3
- **Purpose**: Core application framework
- **Key Features**:
  - Auto-configuration for rapid development
  - Actuator for health checks and metrics
  - Spring Cloud integration
- **Configuration**: `config-control-service/build.gradle`

#### Java 21
- **Purpose**: Runtime environment
- **Key Features**:
  - Records for immutable DTOs
  - Sealed classes for closed hierarchies
  - Pattern matching
  - Virtual threads (prepared for future use)
- **Reference**: JDK 21 LTS

### Data Layer

#### MongoDB 8.0
- **Purpose**: Primary data store for domain entities
- **Usage**:
  - ApplicationService, ServiceInstance, DriftEvent
  - ApprovalRequest, ServiceShare, ApprovalDecision
- **Features**:
  - Document-based storage
  - Optimistic locking for ApprovalRequest
  - Auditing (createdBy, updatedBy, timestamps)
- **Configuration**: `application.yml` → MongoDB connection

#### Redis (Latest)
- **Purpose**: Multi-level caching layer
- **Architecture**: L1 (Caffeine) + L2 (Redis)
- **Cache Types**:
  - Service instances (5m TTL)
  - Config hashes (30m TTL)
  - Drift events (2m TTL)
  - IAM users/teams (15m/30m TTL)
- **Configuration**: `application-app.yml:76-124`

### Messaging

#### Apache Kafka
- **Purpose**: Event bus for configuration refresh
- **Topics**:
  - `config-refresh`: Configuration refresh events
  - `heartbeat-queue`: Heartbeat ingestion (async processing)
  - `heartbeat-queue-dlq`: Dead letter queue
- **Configuration**: `application-messaging.yml`

### Service Discovery

#### Consul 1.17
- **Purpose**: Service registry and health checks
- **Features**:
  - Service registration
  - Health checks (TTL-based)
  - Service discovery
  - Key-Value store (future use)
- **Integration**: Spring Cloud Consul Discovery

### Configuration Server

#### Spring Cloud Config Server
- **Purpose**: Git-backed source of truth for configurations
- **Features**:
  - Environment-specific configurations
  - Version control integration
  - Configuration encryption
- **Location**: `config-server/`

### API Gateway

#### Spring Cloud Gateway
- **Purpose**: Single entry point for API requests, centralized routing and resilience
- **Features**:
  - Reactive, non-blocking architecture
  - Service discovery integration (Consul)
  - Load balancing across backend instances
  - Circuit breaker with fallback
  - Per-user rate limiting (Redis-based)
  - CORS handling
  - JWT token forwarding
  - Correlation ID tracking
- **Configuration**: `gateway-service/src/main/resources/application-gateway.yml`
- **Location**: `gateway-service/`
- **Reference**: `gateway-service/README.md`

---

## Resilience Stack

### Resilience4j

#### Circuit Breaker
- **Purpose**: Prevent cascading failures
- **Configuration**: `application-resilience.yml:4-59`
- **Instances**:
  - ConfigServer: 50% failure rate, 30s wait
  - Consul: 50% failure rate, 20s wait
  - Keycloak: 60% failure rate, 40s wait
  - MongoDB: 50% failure rate, 30s wait

#### Retry
- **Purpose**: Handle transient failures
- **Features**:
  - Exponential backoff
  - Randomized jitter
  - Retry budget tracking
- **Configuration**: `application-resilience.yml:61-105`

#### Bulkhead
- **Purpose**: Limit concurrent calls
- **Types**:
  - Semaphore bulkhead (max concurrent calls)
  - Thread pool bulkhead (thread pool isolation)
- **Configuration**: `application-resilience.yml:107-152`

#### Time Limiter
- **Purpose**: Prevent hanging requests
- **Configuration**: `application-resilience.yml:154-172`
- **Timeouts**:
  - ConfigServer: 5s
  - Consul: 3s
  - Keycloak: 5s

#### Rate Limiter
- **Purpose**: Protect public endpoints
- **Configuration**: `application-resilience.yml:174-188`
- **Limits**:
  - Heartbeat endpoint: 50 req/10s per IP
  - Admin endpoints: 100 req/10s

---

## Security Stack

### Keycloak
- **Purpose**: Identity and Access Management (IAM)
- **Protocols**: OAuth2, OpenID Connect (OIDC)
- **Features**:
  - JWT token issuance
  - Role-based access control (RBAC)
  - Group-based team membership
  - Custom mappers for team IDs
  - Audience validator
- **Configuration**: `config-control-service/README-KEYCLOAK.md`

### Spring Security 6
- **Purpose**: Security framework integration
- **Features**:
  - OAuth2 Resource Server
  - JWT validation
  - Method-level security
  - Domain-level permission evaluation
- **Configuration**: `application-security.yml`

---

## Observability Stack

### Micrometer
- **Purpose**: Metrics collection
- **Export**: Prometheus format
- **Metrics**:
  - HTTP server requests (latency, count)
  - Heartbeat processing (latency, throughput)
  - Drift detection (count, severity)
  - Circuit breaker states
  - Custom business metrics
- **Configuration**: `application-observability.yml`

### OpenTelemetry
- **Purpose**: Distributed tracing
- **Protocol**: OTLP (OpenTelemetry Protocol)
- **Export**: OTLP endpoint (Grafana Alloy)
- **Features**:
  - W3C trace context propagation
  - Span creation and correlation
  - Sampling configuration
- **Configuration**: `application-observability.yml:2-7`

### Log4j2
- **Purpose**: Structured logging
- **Format**: JSON
- **Features**:
  - MDC enrichment (trace ID, user context)
  - Configurable log levels
  - File and console appenders
- **Configuration**: `log4j2-spring.xml`

---

## Frontend Stack

### React 18
- **Purpose**: UI framework
- **Features**:
  - Component-based architecture
  - Hooks for state management
  - Server-side rendering (SSR) ready
- **Location**: `admin-dashboard/`

### TypeScript
- **Purpose**: Type-safe JavaScript
- **Features**:
  - Static type checking
  - Enhanced IDE support
  - Better refactoring capabilities

### Vite
- **Purpose**: Build tool and dev server
- **Features**:
  - Fast HMR (Hot Module Replacement)
  - Optimized production builds
  - Plugin ecosystem

### React Query
- **Purpose**: Server state management
- **Features**:
  - Automatic caching
  - Background refetching
  - Optimistic updates

---

## Infrastructure Stack

### Docker
- **Purpose**: Containerization
- **Usage**: All services containerized
- **Configuration**: `Dockerfile` in each service

### Docker Compose
- **Purpose**: Local development orchestration
- **Files**:
  - `docker-compose.yml`: Full stack
  - `docker-compose.infra.yml`: Infrastructure only
  - `docker-compose.kc.yml`: Keycloak setup

### Prometheus
- **Purpose**: Metrics collection and storage
- **Configuration**: `config/prometheus/`

### Grafana
- **Purpose**: Metrics visualization
- **Configuration**: `config/grafana/`

### Grafana Alloy
- **Purpose**: Observability data pipeline
- **Features**:
  - OTLP receiver
  - Prometheus remote write
  - Loki log shipping
- **Configuration**: `config/alloy/`

---

## SDK Stack

### ZCM Spring SDK Starter
- **Purpose**: Client-side SDK for service integration
- **Features**:
  - Automatic service registration
  - Periodic heartbeat
  - Configuration refresh listener
  - Service discovery
  - Load balancing
- **Location**: `zcm-spring-sdk-starter/`
- **Documentation**: `zcm-spring-sdk-starter/README.md`

### Spring Cloud Components
- **Spring Cloud Config Client**: Configuration fetching
- **Spring Cloud Consul Discovery**: Service registration
- **Spring Cloud LoadBalancer**: Client-side load balancing
- **Spring Cloud Bus**: Event-driven refresh

---

## Build & Deployment

### Gradle (Kotlin DSL)
- **Purpose**: Build automation
- **Features**:
  - Version catalogs (`libs.versions.toml`)
  - Dependency management
  - Task configuration
- **Location**: `build.gradle`, `settings.gradle`

### Testing Stack
- **JUnit 5**: Unit and integration tests
- **Mockito**: Mocking framework
- **Testcontainers**: Integration testing with containers
- **AssertJ**: Fluent assertions

---

## Version Compatibility

| Component | Version | Compatibility |
|-----------|---------|---------------|
| Java | 21 | LTS |
| Spring Boot | 3.3 | Latest stable |
| Spring Cloud | Latest | Compatible with Boot 3.3 |
| MongoDB | 8.0 | Latest stable |
| Redis | Latest | Latest stable |
| Kafka | Latest | Latest stable |
| Keycloak | 26.4.0 | Custom build with providers |

---

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Project Architecture Guidelines](../AGENTS.md)

