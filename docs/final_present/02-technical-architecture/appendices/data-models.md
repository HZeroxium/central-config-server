# Data Models & Database Schema
## MongoDB Collections and Domain Models

---

## Overview

The system uses MongoDB for domain data persistence with a schema-less design that supports flexible evolution. All collections use Spring Data MongoDB with compound indexes for performance optimization.

**Database:** `config_control`

**Reference:** `config-control-service/src/main/java/com/example/control/domain/model/`

---

## MongoDB Collections

### Collection Overview

| Collection | Purpose | Indexes | TTL |
|-----------|---------|---------|-----|
| **application_services** | Service metadata and ownership | 4 compound, 1 text | None |
| **service_instances** | Runtime instance metadata | 4 compound | 1 hour (lastSeenAt) |
| **drift_events** | Configuration drift detection events | 4 compound, 1 text | 30 days (detectedAt) |
| **service_shares** | Service sharing and permissions | Compound indexes | None |
| **approval_requests** | Multi-gate approval workflow | 3 compound, 1 partial unique | None |
| **approval_decisions** | Individual approval decisions | Compound indexes | None |
| **iam_users** | Cached user projections from Keycloak | Indexes | None |
| **iam_teams** | Cached team projections from Keycloak | Indexes | None |

---

## Domain Models

### ApplicationService

**Collection:** `application_services`

**Purpose:** Public service metadata and team ownership

**Key Fields:**
- `id` (String, UUID): Unique service identifier
- `displayName` (String, unique, indexed): Human-readable service name
- `ownerTeamId` (String, indexed): Team that owns this service (null for orphan)
- `environments` (List<String>): List of deployment environments
- `tags` (List<String>): Service categorization tags
- `repoUrl` (String): Source code repository URL
- `lifecycle` (Enum: ACTIVE, DEPRECATED, RETIRED): Service lifecycle status
- `createdAt`, `updatedAt` (Instant): Audit timestamps
- `createdBy`, `updatedBy` (String): Audit user IDs
- `attributes` (Map<String, String>): Additional metadata

**Indexes:**
- Unique index on `displayName`
- Text index on `displayName` (full-text search)
- Compound index: `{ownerTeamId: 1, lifecycle: 1}`
- Compound index: `{ownerTeamId: 1, createdAt: -1}`

**Relationships:**
- One-to-many: `ServiceInstance`
- One-to-many: `DriftEvent`
- One-to-many: `ServiceShare`
- One-to-many: `ApprovalRequest`

**Reference:** `config-control-service/src/main/java/com/example/control/domain/model/ApplicationService.java`

**Document:** `config-control-service/src/main/java/com/example/control/infrastructure/adapter/persistence/mongo/documents/ApplicationServiceDocument.java`

---

### ServiceInstance

**Collection:** `service_instances`

**Purpose:** Runtime instance metadata and drift status

**Key Fields:**
- `id` (String): Composite ID (serviceId + instanceId)
- `serviceId` (String, indexed): Reference to ApplicationService
- `teamId` (String, indexed): Team that owns this instance
- `host` (String): Instance hostname or IP
- `port` (Integer): Instance TCP port
- `environment` (String): Deployment environment
- `version` (String): Service version
- `configHash` (String): Current configuration hash (reported by instance)
- `expectedHash` (String): Expected configuration hash (from Config Server)
- `lastAppliedHash` (String): Last applied configuration hash
- `status` (Enum: HEALTHY, UNHEALTHY, DRIFT, indexed): Instance status
- `hasDrift` (Boolean): Drift flag
- `lastSeenAt` (Instant, TTL indexed, 1 hour): Last heartbeat timestamp
- `createdAt`, `updatedAt` (Instant): Audit timestamps
- `driftDetectedAt` (Instant): Drift detection timestamp
- `metadata` (Map<String, String>): Additional instance metadata

**Indexes:**
- Index on `serviceId`
- Index on `teamId`
- Index on `status`
- TTL index on `lastSeenAt` (expires after 1 hour)
- Compound index: `{serviceId: 1, teamId: 1}`
- Compound index: `{teamId: 1, status: 1}`
- Compound index: `{serviceId: 1, environment: 1}`
- Compound index: `{teamId: 1, hasDrift: 1}`

**TTL Behavior:**
- Instances inactive for > 1 hour are automatically deleted
- Ensures only active instances are stored

**Relationships:**
- Many-to-one: `ApplicationService` (via `serviceId`)
- One-to-many: `DriftEvent` (via `instanceId`)

**Reference:** `config-control-service/src/main/java/com/example/control/domain/model/ServiceInstance.java`

**Document:** `config-control-service/src/main/java/com/example/control/infrastructure/adapter/persistence/mongo/documents/ServiceInstanceDocument.java`

---

### DriftEvent

**Collection:** `drift_events`

**Purpose:** Configuration drift detection events and resolution tracking

**Key Fields:**
- `id` (String, UUID): Unique drift event identifier
- `serviceName` (String, indexed): Service name where drift occurred
- `instanceId` (String, indexed): Instance identifier
- `serviceId` (String, indexed): Reference to ApplicationService
- `teamId` (String, indexed): Team that owns this service
- `environment` (String, indexed): Environment where drift occurred
- `expectedHash` (String): Expected configuration hash
- `appliedHash` (String): Actual configuration hash from instance
- `severity` (Enum: LOW, MEDIUM, HIGH, CRITICAL, indexed): Drift severity
- `status` (Enum: DETECTED, ACKNOWLEDGED, RESOLVING, RESOLVED, IGNORED, indexed): Event status
- `detectedAt` (Instant, TTL indexed, 30 days): Drift detection timestamp
- `resolvedAt` (Instant): Resolution timestamp
- `detectedBy` (String): User/system that detected drift
- `resolvedBy` (String): User/system that resolved drift
- `notes` (String): Investigation or resolution notes

**Indexes:**
- Index on `serviceName`
- Index on `instanceId`
- Index on `serviceId`
- Index on `teamId`
- Index on `environment`
- Index on `severity`
- Index on `status`
- Text index on `serviceName` (full-text search)
- TTL index on `detectedAt` (expires after 30 days)
- Compound index: `{serviceId: 1, teamId: 1, status: 1}`
- Compound index: `{teamId: 1, status: 1}`
- Compound index: `{serviceId: 1, detectedAt: -1}`
- Compound index: `{teamId: 1, severity: 1}`

**TTL Behavior:**
- Events older than 30 days are automatically deleted
- Provides automatic cleanup for audit trail

**Relationships:**
- Many-to-one: `ApplicationService` (via `serviceId`)
- Many-to-one: `ServiceInstance` (via `instanceId`)

**Reference:** `config-control-service/src/main/java/com/example/control/domain/model/DriftEvent.java`

**Document:** `config-control-service/src/main/java/com/example/control/infrastructure/adapter/persistence/mongo/documents/DriftEventDocument.java`

---

### ServiceShare

**Collection:** `service_shares`

**Purpose:** Service sharing and fine-grained permission management

**Key Fields:**
- `id` (String, UUID): Unique share identifier
- `resourceLevel` (Enum: SERVICE, INSTANCE): Resource level for sharing
- `serviceId` (String, indexed): Reference to ApplicationService
- `instanceId` (String, optional): Instance ID if sharing at instance level
- `grantToType` (Enum: TEAM, USER): Type of grantee
- `grantToId` (String): Team ID or user ID
- `permissions` (List<SharePermission>): Permissions granted
  - VIEW_SERVICE, VIEW_INSTANCE, VIEW_DRIFT
  - EDIT_SERVICE, EDIT_INSTANCE, RESTART_INSTANCE
- `environments` (List<String>, optional): Environment filter (null = all)
- `grantedBy` (String): User who created this share
- `createdAt`, `updatedAt` (Instant): Audit timestamps
- `expiresAt` (Instant, optional): Share expiration timestamp

**Indexes:**
- Compound indexes for efficient querying:
  - `{serviceId: 1, grantToType: 1, grantToId: 1}`
  - `{grantToType: 1, grantToId: 1}`

**Relationships:**
- Many-to-one: `ApplicationService` (via `serviceId`)
- Many-to-one: `ServiceInstance` (via `instanceId`, optional)

**Reference:** `config-control-service/src/main/java/com/example/control/domain/model/ServiceShare.java`

---

### ApprovalRequest

**Collection:** `approval_requests`

**Purpose:** Multi-gate approval workflow for service ownership requests

**Key Fields:**
- `id` (String, UUID): Unique request identifier
- `requesterUserId` (String, indexed): User who created request
- `requestType` (Enum: ASSIGN_SERVICE_TO_TEAM, SERVICE_OWNERSHIP_TRANSFER): Request type
- `target` (ApprovalTarget): Target service and team
  - `serviceId` (String): Service being requested
  - `teamId` (String): Target team for assignment
- `required` (List<ApprovalGate>): Required approval gates
  - `gate` (String): Gate name (e.g., "SYS_ADMIN", "LINE_MANAGER")
  - `minApprovals` (Integer): Minimum approvals required
  - `status` (Enum: PENDING, APPROVED, REJECTED): Gate status
- `status` (Enum: PENDING, APPROVED, REJECTED, CANCELLED, indexed): Request status
- `snapshot` (RequesterSnapshot): Snapshot of requester info at request time
  - `teamIds` (List<String>): Requester's teams
  - `managerId` (String): Requester's manager ID
  - `roles` (List<String>): Requester's roles
- `counts` (Map<String, Integer>): Current approval counts per gate
- `version` (Integer, optimistic locking): Version for optimistic locking
- `createdAt`, `updatedAt` (Instant): Audit timestamps
- `note` (String, optional): Requester note
- `cancelReason` (String, optional): Cancellation reason

**Indexes:**
- Index on `requesterUserId`
- Index on `status`
- Index on `requiredGates` (array of gate names)
- Compound index: `{status: 1, requiredGates: 1, createdAt: -1}` (for pending requests)
- Compound index: `{targetServiceId: 1, status: 1}`
- Compound index: `{requesterUserId: 1, status: 1}`
- **Partial unique index:** `{requesterUserId: 1, targetServiceId: 1, status: 1}` (unique only when status = "PENDING")

**Optimistic Locking:**
- Uses `@Version` annotation for optimistic locking
- Prevents race conditions during concurrent approvals
- Version increments on each update

**Relationships:**
- Many-to-one: `ApplicationService` (via `target.serviceId`)
- One-to-many: `ApprovalDecision` (via request ID)

**Reference:** `config-control-service/src/main/java/com/example/control/domain/model/ApprovalRequest.java`

**Document:** `config-control-service/src/main/java/com/example/control/infrastructure/adapter/persistence/mongo/documents/ApprovalRequestDocument.java`

---

### ApprovalDecision

**Collection:** `approval_decisions`

**Purpose:** Individual approval decisions for audit trail

**Key Fields:**
- `id` (String, UUID): Unique decision identifier
- `requestId` (String, indexed): Reference to ApprovalRequest
- `gate` (String): Approval gate name
- `decision` (Enum: APPROVED, REJECTED): Decision type
- `approverUserId` (String): User who made decision
- `note` (String, optional): Approval note
- `createdAt` (Instant): Decision timestamp

**Indexes:**
- Compound indexes for efficient querying:
  - `{requestId: 1, gate: 1}`
  - `{approverUserId: 1, createdAt: -1}`

**Relationships:**
- Many-to-one: `ApprovalRequest` (via `requestId`)

**Reference:** `config-control-service/src/main/java/com/example/control/domain/model/ApprovalDecision.java`

---

### IamUser

**Collection:** `iam_users`

**Purpose:** Cached user projection from Keycloak

**Key Fields:**
- `userId` (String, indexed): Keycloak user ID (sub claim)
- `username` (String): Username
- `email` (String): Email address
- `firstName`, `lastName` (String): Name fields
- `teamIds` (List<String>): Team IDs user belongs to
- `managerId` (String): Line manager user ID
- `roles` (List<String>): User roles
- `createdAt`, `updatedAt`, `syncedAt` (Instant): Timestamps

**Purpose:**
- Provides faster access to user information
- Reduces Keycloak Admin API calls
- Supports audit and reporting

**Reference:** `config-control-service/src/main/java/com/example/control/domain/model/IamUser.java`

---

### IamTeam

**Collection:** `iam_teams`

**Purpose:** Cached team projection from Keycloak

**Key Fields:**
- `teamId` (String, indexed): Keycloak group ID
- `name` (String): Team name
- `path` (String): Keycloak group path
- `createdAt`, `updatedAt`, `syncedAt` (Instant): Timestamps

**Purpose:**
- Provides faster access to team information
- Reduces Keycloak Admin API calls
- Supports audit and reporting

**Reference:** `config-control-service/src/main/java/com/example/control/domain/model/IamTeam.java`

---

## Indexes Configuration

### Index Creation Strategy

Indexes are automatically created via `MongoIndexesConfig`:

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/config/persistence/mongo/MongoIndexesConfig.java`

### Index Types

1. **Single Field Indexes:**
   - Direct field indexing for simple queries
   - Example: `@Indexed` on `serviceId`, `teamId`

2. **Compound Indexes:**
   - Multiple fields for complex queries
   - Example: `{serviceId: 1, teamId: 1, status: 1}`

3. **Text Indexes:**
   - Full-text search capabilities
   - Example: Text index on `displayName`, `serviceName`

4. **Partial Indexes:**
   - Index only documents matching filter
   - Example: Unique partial index on `ApprovalRequest` (only PENDING)

5. **TTL Indexes:**
   - Automatic document expiration
   - Example: `lastSeenAt` (1 hour), `detectedAt` (30 days)

---

## Relationships Diagram

```mermaid
erDiagram
    ApplicationService ||--o{ ServiceInstance : "has"
    ApplicationService ||--o{ DriftEvent : "generates"
    ApplicationService ||--o{ ServiceShare : "shares"
    ApplicationService ||--o{ ApprovalRequest : "requests"
    
    ServiceInstance ||--o{ DriftEvent : "triggers"
    ServiceInstance ||--o| ServiceShare : "shared"
    
    ApprovalRequest ||--o{ ApprovalDecision : "has"
    
    ApplicationService {
        string id PK
        string displayName UK
        string ownerTeamId FK
        list environments
        enum lifecycle
    }
    
    ServiceInstance {
        string id PK
        string serviceId FK
        string teamId FK
        string configHash
        string expectedHash
        enum status
        boolean hasDrift
        instant lastSeenAt TTL
    }
    
    DriftEvent {
        string id PK
        string serviceId FK
        string instanceId FK
        string teamId FK
        enum severity
        enum status
        instant detectedAt TTL
    }
    
    ServiceShare {
        string id PK
        string serviceId FK
        string instanceId FK
        enum resourceLevel
        enum grantToType
        string grantToId FK
        list permissions
    }
    
    ApprovalRequest {
        string id PK
        string requesterUserId FK
        string targetServiceId FK
        string targetTeamId FK
        enum status
        int version OCC
    }
    
    ApprovalDecision {
        string id PK
        string requestId FK
        string gate
        enum decision
    }
```

---

## Schema Evolution

### MongoDB Schema-Less Design

MongoDB's schema-less nature allows flexible evolution:

**Benefits:**
- Add new fields without migrations
- Support different document structures
- Gradual schema changes

**Best Practices:**
- Use domain models for validation
- Document schema changes in ADRs
- Use indexes for performance-critical queries
- Handle missing fields gracefully in code

**Migration Strategy:**
- Backward compatible changes (add optional fields)
- Forward compatible code (handle missing fields)
- Index migrations via `MongoIndexesConfig`

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/config/persistence/mongo/MongoIndexesConfig.java`

---

## Query Performance Considerations

### Optimized Query Patterns

1. **Team-Based Access Control:**
   - Use compound index: `{teamId: 1, status: 1}`
   - Efficient filtering by team ownership

2. **Drift Event Queries:**
   - Use compound index: `{serviceId: 1, teamId: 1, status: 1}`
   - Efficient filtering by service and team

3. **Service Instance Lookups:**
   - Use compound index: `{serviceId: 1, environment: 1}`
   - Efficient filtering by service and environment

4. **Text Search:**
   - Use text index on `displayName`, `serviceName`
   - Efficient full-text search queries

### Index Maintenance

**Automatic Index Creation:**
- Indexes created on application startup via `@PostConstruct`
- Logged for visibility and debugging

**Index Monitoring:**
- Monitor index usage via MongoDB explain plans
- Review slow query logs
- Optimize based on actual query patterns

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/config/persistence/mongo/MongoIndexesConfig.java`

---

## Data Retention

### TTL-Based Automatic Cleanup

1. **ServiceInstance TTL (1 hour):**
   - `lastSeenAt` field with TTL index
   - Automatically removes inactive instances
   - Ensures only active instances are stored

2. **DriftEvent TTL (30 days):**
   - `detectedAt` field with TTL index
   - Automatically removes old drift events
   - Provides audit trail retention period

### Manual Cleanup

- Approval requests: No TTL (kept for audit)
- Service shares: No TTL (kept until revoked)
- Application services: No TTL (kept indefinitely)

---

## References

- [Domain Models](../../../config-control-service/src/main/java/com/example/control/domain/model/)
- [MongoDB Documents](../../../config-control-service/src/main/java/com/example/control/infrastructure/adapter/persistence/mongo/documents/)
- [Index Configuration](../../../config-control-service/src/main/java/com/example/control/infrastructure/config/persistence/mongo/MongoIndexesConfig.java)
- [Domain Model Relationships](../README.md#domain-model-relationships)

