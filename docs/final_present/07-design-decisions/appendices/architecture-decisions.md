# Architecture Decisions

## Why Hexagonal Architecture?

### Decision

Use Hexagonal Architecture (Ports & Adapters) pattern to structure the Config Control Service.

### Context

The system needs to support multiple protocols (HTTP, Thrift, gRPC, Kafka), multiple data stores (MongoDB, Redis, Consul), and external services (Config Server, Keycloak). The business logic must remain independent of these infrastructure concerns.

### Alternatives Considered

1. **Layered Architecture (Traditional)**
   - Simple and familiar
   - Tight coupling between layers
   - Difficult to test in isolation

2. **MVC Pattern**
   - Good for simple CRUD applications
   - Not suitable for complex domain logic
   - Framework-dependent

3. **Clean Architecture**
   - Similar to Hexagonal but more prescriptive
   - More layers and complexity
   - Overkill for this project size

### Trade-offs

| Aspect | Hexagonal Architecture | Layered Architecture |
|--------|----------------------|---------------------|
| **Testability** | High (easy to mock ports) | Medium (requires integration tests) |
| **Maintainability** | High (clear boundaries) | Medium (coupling between layers) |
| **Complexity** | Medium (more abstraction) | Low (simple structure) |
| **Framework Independence** | High (domain isolated) | Low (framework-dependent) |
| **Initial Setup** | More complex | Simpler |

### Rationale

1. **Testability**: Domain logic can be tested without infrastructure dependencies
2. **Flexibility**: Easy to swap implementations (e.g., MongoDB → PostgreSQL adapter)
3. **Domain Independence**: Business logic doesn't depend on Spring, MongoDB, or other frameworks
4. **Protocol Support**: Multiple protocols (HTTP, Thrift, gRPC) can be added as adapters
5. **Long-term Maintainability**: Clear boundaries make the codebase easier to understand and modify

### Implementation

**Package Structure:**
```
com.example.control/
├── api/              # Adapters (Inbound)
├── application/      # Application Services
├── domain/           # Domain Models & Ports
└── infrastructure/   # Adapters (Outbound)
```

**Example:**
- **Port**: `domain/port/repository/ServiceInstanceRepositoryPort.java`
- **Adapter**: `infrastructure/adapter/persistence/mongo/repository/ServiceInstanceMongoRepository.java`

**Reference:** `config-control-service/src/main/java/com/example/control/`

### When to Reconsider

- If the team lacks experience with Hexagonal Architecture
- If the project becomes too small to justify the complexity
- If framework coupling becomes necessary for performance

---

## Why MongoDB for Domain Data?

### Decision

Use MongoDB for storing domain objects (ApplicationService, ServiceInstance, DriftEvent, etc.) while using PostgreSQL for Keycloak.

### Context

The system needs to store:
- Application services with flexible metadata
- Service instances with dynamic properties
- Drift events with varying structures
- Approval requests with complex state

### Alternatives Considered

1. **PostgreSQL for Everything**
   - ACID transactions
   - SQL queries and joins
   - Schema migrations required
   - Less flexible for dynamic structures

2. **MySQL**
   - Similar to PostgreSQL
   - Less JSON support
   - More rigid schema

3. **MongoDB Only**
   - Simpler architecture
   - Keycloak requires PostgreSQL
   - Not feasible

### Trade-offs

| Aspect | MongoDB | PostgreSQL |
|--------|---------|------------|
| **Schema Flexibility** | High (document model) | Low (rigid schema) |
| **ACID Transactions** | Limited (single document) | Full support |
| **Query Performance** | Good for document queries | Excellent for joins |
| **Scalability** | Horizontal scaling | Vertical scaling preferred |
| **JSON Support** | Native | Good (JSONB) |
| **Learning Curve** | Medium | Low (SQL familiar) |

### Rationale

1. **Document Model**: Domain objects (ApplicationService, ServiceInstance) map naturally to documents
2. **Flexible Schema**: Metadata fields can vary without migrations
3. **No Joins Needed**: Most queries are by ID or simple filters
4. **Performance**: Fast reads/writes for document-based operations
5. **Keycloak Requirement**: Keycloak requires PostgreSQL, so we use both databases

### Implementation

**MongoDB Collections:**
- `application_services` - Service catalog
- `service_instances` - Instance tracking
- `drift_events` - Drift detection events
- `approval_requests` - Approval workflows
- `service_shares` - Service sharing

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/adapter/persistence/mongo/`

### When to Reconsider

- If ACID transactions become critical
- If complex joins are required frequently
- If the team lacks MongoDB expertise
- If operational costs become prohibitive

---

## Why Separate PostgreSQL for Keycloak?

### Decision

Use a separate PostgreSQL database for Keycloak while using MongoDB for domain data.

### Context

Keycloak requires a relational database (PostgreSQL, MySQL, or MariaDB) for its internal data structures. The domain data is better suited for MongoDB's document model.

### Alternatives Considered

1. **PostgreSQL for Everything**
   - Single database
   - Keycloak compatibility
   - Less flexible for domain objects
   - Schema migrations for domain changes

2. **MongoDB for Keycloak**
   - Not supported by Keycloak
   - Not feasible

### Trade-offs

| Aspect | Separate Databases | Single Database |
|--------|-------------------|-----------------|
| **Complexity** | Higher (two databases) | Lower (one database) |
| **Data Isolation** | High (clear separation) | Low (shared schema) |
| **Keycloak Compatibility** | Full support | Full support |
| **Domain Flexibility** | High (MongoDB) | Low (PostgreSQL) |
| **Operational Overhead** | Higher | Lower |

### Rationale

1. **Keycloak Requirement**: Keycloak requires PostgreSQL/MySQL/MariaDB
2. **Data Isolation**: IAM data (Keycloak) is separate from domain data (MongoDB)
3. **Domain Flexibility**: MongoDB better fits domain objects
4. **Best Tool for Job**: Each database optimized for its use case
5. **Security**: IAM data isolated from application data

### Implementation

**PostgreSQL (Keycloak):**
- Keycloak realm, client, user data
- Managed by Keycloak
- Port: 25432

**MongoDB (Domain Data):**
- Application services, instances, drift events
- Managed by Config Control Service
- Port: 20017

**Reference:** `docker-compose.kc.yml` (Keycloak + PostgreSQL), `docker-compose.infra.yml` (MongoDB)

### When to Reconsider

- If operational overhead becomes too high
- If data consistency across databases becomes critical
- If Keycloak adds MongoDB support

---

## Summary

These architectural decisions prioritize:
1. **Testability and maintainability** (Hexagonal Architecture)
2. **Best Tool for Job** (MongoDB for domain, PostgreSQL for Keycloak)
3. **Flexibility and Scalability** (Document model, protocol adapters)

The trade-offs are acceptable given the system's requirements for multiple protocols, flexible schemas, and long-term maintainability.

