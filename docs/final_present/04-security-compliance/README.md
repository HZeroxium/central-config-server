# Security & Compliance
## Centralized Configuration Management System

**Presentation Time:** 5-7 minutes  
**Target Audience:** Manager, Tech Lead, Head of Department

---

## Security Overview

The system implements comprehensive security controls with OAuth2/OIDC authentication, fine-grained authorization, and full audit trails for compliance.

---

## Authentication Flow

### OAuth2/OIDC Integration

```mermaid
sequenceDiagram
    participant UI as Admin Dashboard
    participant KC as Keycloak
    participant API as Config Control Service
    participant SDK as ZCM SDK
    
    Note over UI: User Login
    UI->>KC: OAuth2 Authorization Code<br/>PKCE Flow
    KC->>KC: Authenticate user
    KC-->>UI: Authorization code
    UI->>KC: Exchange code for tokens
    KC-->>UI: Access token (JWT)<br/>Refresh token
    
    Note over UI: API Request
    UI->>API: GET /api/services<br/>Header: Authorization: Bearer {JWT}
    API->>KC: Validate JWT token<br/>(issuer, audience, signature)
    KC-->>API: Token valid
    API->>API: Extract user context<br/>(userId, teams, roles)
    API->>API: Check permissions
    API-->>UI: Service data
    
    Note over SDK: SDK Authentication
    SDK->>KC: Client credentials grant<br/>(API key or service account)
    KC-->>SDK: Access token (JWT)
    SDK->>API: POST /api/heartbeat<br/>Header: Authorization: Bearer {JWT}
    API->>API: Validate token
    API-->>SDK: 200 OK
```

### Keycloak Configuration

**Realm:** `config-control-realm`

**Clients:**
- `admin-dashboard` - Public client (PKCE)
- `config-control-service` - Confidential client
- `zcm-sdk` - Service account client

**User Attributes:**
- `groups` → Team IDs
- `realm_access.roles` → System roles (SYS_ADMIN, USER)
- `manager_id` → Line manager ID (for approval workflows)

**Reference:** `config-control-service/README-KEYCLOAK.md`

---

## Authorization Model

### RBAC + ABAC Hybrid

```mermaid
graph TB
    subgraph "Roles (RBAC)"
        SYS_ADMIN[SYS_ADMIN]
        USER[USER]
    end
    
    subgraph "Attributes (ABAC)"
        TEAM[Team Membership]
        MANAGER[Manager ID]
        SERVICE_OWNER[Service Owner]
    end
    
    subgraph "Permissions"
        VIEW_ALL[View All Services]
        EDIT_OWNED[Edit Owned Services]
        MANAGE_SHARES[Manage Shares]
        APPROVE[Approve Requests]
    end
    
    SYS_ADMIN --> VIEW_ALL
    SYS_ADMIN --> EDIT_OWNED
    SYS_ADMIN --> MANAGE_SHARES
    SYS_ADMIN --> APPROVE
    
    USER --> EDIT_OWNED
    USER --> MANAGE_SHARES
    
    TEAM --> EDIT_OWNED
    SERVICE_OWNER --> MANAGE_SHARES
    MANAGER --> APPROVE
```

### Access Control Rules

| Resource | Owner Team | Shared Team | Orphan | SYS_ADMIN |
|----------|-----------|-------------|--------|-----------|
| **View Service** | ✅ | ✅ (if shared) | ✅ | ✅ |
| **Edit Service** | ✅ | ❌ | ❌ | ✅ |
| **Delete Service** | ✅ | ❌ | ❌ | ✅ |
| **Manage Shares** | ✅ | ❌ | ❌ | ✅ |
| **View Instances** | ✅ | ✅ (if shared) | ✅ | ✅ |
| **Edit Instances** | ✅ | ✅ (if shared) | ❌ | ✅ |
| **View Drift** | ✅ | ✅ (if shared) | ✅ | ✅ |
| **Approve Requests** | ❌ | ❌ | ❌ | ✅ (or LINE_MANAGER) |

### Permission Evaluation

**Implementation:** `config-control-service/src/main/java/com/example/control/infrastructure/config/security/DomainPermissionEvaluator.java`

**Method Security:**
```java
@PreAuthorize("hasPermission(#serviceId, 'ApplicationService', 'EDIT_SERVICE')")
public void updateService(String serviceId, UpdateRequest request) {
    // ...
}
```

---

## Service Sharing Model

### Fine-Grained Sharing

```mermaid
graph TB
    subgraph "Sharing Levels"
        SERVICE[Service Level<br/>All Instances]
        INSTANCE[Instance Level<br/>Specific Instance]
    end
    
    subgraph "Grantee Types"
        TEAM[Team]
        USER[User]
    end
    
    subgraph "Permissions"
        VIEW_SVC[VIEW_SERVICE]
        VIEW_INST[VIEW_INSTANCE]
        VIEW_DRIFT[VIEW_DRIFT]
        EDIT_SVC[EDIT_SERVICE]
        EDIT_INST[EDIT_INSTANCE]
        RESTART[RESTART_INSTANCE]
    end
    
    subgraph "Filters"
        ENV[Environment Filter]
        EXPIRY[Expiration Date]
    end
    
    SERVICE --> TEAM
    SERVICE --> USER
    INSTANCE --> TEAM
    INSTANCE --> USER
    
    TEAM --> VIEW_SVC
    TEAM --> VIEW_INST
    TEAM --> VIEW_DRIFT
    TEAM --> EDIT_SVC
    TEAM --> EDIT_INST
    TEAM --> RESTART
    
    SERVICE --> ENV
    SERVICE --> EXPIRY
```

**Implementation:** `config-control-service/src/main/java/com/example/control/domain/model/ServiceShare.java`

**Permissions:**
- `VIEW_SERVICE` - View service metadata
- `VIEW_INSTANCE` - View service instances
- `VIEW_DRIFT` - View drift events
- `EDIT_SERVICE` - Edit service metadata
- `EDIT_INSTANCE` - Edit instance configuration
- `RESTART_INSTANCE` - Trigger instance restart

---

## Approval Workflow

### Multi-Gate Approval

```mermaid
stateDiagram-v2
    [*] --> PENDING: Create Request
    
    PENDING --> EVALUATING: Evaluate Gates
    EVALUATING --> PENDING: Gates Not Met
    
    PENDING --> APPROVING: Collect Approvals
    APPROVING --> APPROVED: All Gates Approved
    APPROVING --> REJECTED: Gate Rejected
    
    APPROVED --> TRANSFERRED: Transfer Ownership
    TRANSFERRED --> [*]
    
    PENDING --> CANCELLED: Requester Cancels
    REJECTED --> [*]
    CANCELLED --> [*]
    
    note right of EVALUATING
        Gates:
        - SYS_ADMIN (min 1)
        - LINE_MANAGER (min 1, if managerId exists)
    end note
```

### Approval Gates

1. **SYS_ADMIN Gate** (Required)
   - Minimum approvals: 1
   - Approvers: Users with SYS_ADMIN role

2. **LINE_MANAGER Gate** (Conditional)
   - Minimum approvals: 1 (if requester has managerId)
   - Approvers: Requester's line manager

### Optimistic Locking

**Purpose:** Prevent race conditions during concurrent approvals

**Implementation:** `version` field in `ApprovalRequest`

**Reference:** `config-control-service/src/main/java/com/example/control/domain/model/ApprovalRequest.java:100-101`

---

## Audit & Compliance

### Audit Fields

All domain entities include audit fields:
- `createdBy` - User ID who created the record
- `updatedBy` - User ID who last updated the record
- `createdAt` - Timestamp of creation
- `updatedAt` - Timestamp of last update

**Source:** Extracted from `SecurityContext` (JWT token)

**Implementation:** `config-control-service/src/main/java/com/example/control/infrastructure/config/persistence/MongoAuditingConfig.java`

### Audit Trail

**Tracked Operations:**
- Service creation, update, deletion
- Instance state changes
- Drift event creation and resolution
- Approval request creation and decisions
- Service share creation and revocation

**Compliance Features:**
- Full audit trail for all operations
- User attribution for all changes
- Timestamp tracking
- Approval workflow history

---

## Security Best Practices

### 1. JWT Token Validation

- **Issuer validation**: Verify token issued by Keycloak
- **Audience validation**: Verify token audience matches service
- **Signature verification**: Verify token signature
- **Expiration check**: Reject expired tokens

**Implementation:** `config-control-service/src/main/java/com/example/control/infrastructure/config/security/SecurityConfig.java`

### 2. Input Validation

- **Jakarta Validation**: `@NotNull`, `@NotBlank`, `@Size` annotations
- **DTO validation**: Validate all input DTOs
- **SQL injection prevention**: Use parameterized queries (MongoDB)
- **XSS prevention**: Sanitize user input

### 3. Rate Limiting

- **Heartbeat endpoint**: 50 requests/10s per IP
- **Admin endpoints**: 100 requests/10s per IP
- **Protection**: Prevents abuse and DDoS

**Configuration:** `application-resilience.yml:174-188`

### 4. Secure Communication

- **HTTPS**: All external communication over TLS
- **JWT tokens**: Secure token transmission
- **API keys**: Secure storage for SDK authentication

### 5. Principle of Least Privilege

- **Role-based access**: Users have minimum required permissions
- **Service sharing**: Fine-grained permissions
- **Admin operations**: Restricted to SYS_ADMIN

---

## Security Metrics

| Metric | Value | Purpose |
|--------|-------|---------|
| **JWT Validation Rate** | 100% | All requests validated |
| **Rate Limit Rejections** | < 0.1% | Abuse prevention |
| **Failed Authentication** | < 1% | Security monitoring |
| **Permission Denials** | Tracked | Access control monitoring |

---

## Compliance Features

### 1. Audit Trail
- ✅ Full audit logging for all operations
- ✅ User attribution for all changes
- ✅ Timestamp tracking

### 2. Access Control
- ✅ Role-based access control (RBAC)
- ✅ Attribute-based access control (ABAC)
- ✅ Fine-grained permissions

### 3. Approval Workflows
- ✅ Multi-gate approval for ownership transfers
- ✅ Approval history tracking
- ✅ Optimistic locking for consistency

### 4. Data Protection
- ✅ Secure token storage
- ✅ Encrypted communication (TLS)
- ✅ Secure API key management

---

## Appendices

For detailed information, see:
- [Authentication Implementation](./appendices/authentication.md)
- [Authorization Model](./appendices/authorization.md)
- [Audit Logging](./appendices/audit-logging.md)

---

**Next:** Review [Performance & Scalability](../05-performance-scalability/README.md) for performance metrics.

