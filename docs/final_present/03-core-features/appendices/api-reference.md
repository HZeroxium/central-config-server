# API Reference
## OpenAPI/Swagger Documentation

---

## Overview

The Config Control Service exposes a comprehensive REST API for managing services, instances, drift events, and access control. All endpoints are documented using OpenAPI 3.0.1 specification and accessible via Swagger UI.

### API Access Points

- **Config Control Service Swagger UI**: 
  - Local: http://localhost:28081/swagger-ui.html
  - Production: http://10.40.30.161:28081/swagger-ui.html

- **Gateway Service Swagger UI** (if available):
  - Local: http://localhost:28082/swagger-ui.html
  - Production: http://10.40.30.161:28082/swagger-ui.html

### OpenAPI Specification

**Location:** `config-control-service/spec/openapi.json`

**Format:** OpenAPI 3.0.1

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/config/api/OpenApiConfig.java`

---

## Authentication

### OAuth2/OIDC Authentication

The API uses OAuth2/OIDC for authentication with Keycloak as the identity provider.

**Swagger UI OAuth2 Flow:**
1. Click "Authorize" in Swagger UI
2. Redirect to Keycloak login page
3. Enter credentials
4. Redirect back with authorization code
5. Swagger UI exchanges code for access token
6. API requests include JWT token in `Authorization` header

**Authorization Header:**
```
Authorization: Bearer {JWT_TOKEN}
```

**Token Validation:**
- Issuer: `{KEYCLOAK_PUBLIC_URL}/realms/config-control`
- Audience: `config-control-service`
- Signature: Validated against Keycloak public key

**Reference:** `config-control-service/src/main/resources/application-security.yml`

---

## Key Endpoints Overview

### Heartbeat Endpoints

#### POST `/api/heartbeat`

**Purpose:** Service instance heartbeat for drift detection

**Authentication:** Required (JWT or API Key)

**Request Body:**
```json
{
  "serviceName": "sample-service",
  "instanceId": "sample-service-1",
  "configHash": "abc123...",
  "host": "10.0.0.1",
  "port": 8080,
  "environment": "dev",
  "version": "1.0.0"
}
```

**Response:**
```json
{
  "id": "sample-service-1",
  "serviceId": "service-id",
  "status": "HEALTHY",
  "hasDrift": false
}
```

**Reference:** `config-control-service/src/main/java/com/example/control/api/http/controller/infra/HeartbeatController.java`

---

### Service Management Endpoints

#### GET `/api/services`

**Purpose:** List all services (filtered by access control)

**Authentication:** Required (JWT)

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `search`: Search term (displayName)
- `teamId`: Filter by team ID
- `lifecycle`: Filter by lifecycle (ACTIVE, DEPRECATED, RETIRED)

**Response:**
```json
{
  "content": [
    {
      "id": "service-id",
      "displayName": "Sample Service",
      "ownerTeamId": "team-id",
      "environments": ["dev", "prod"],
      "lifecycle": "ACTIVE"
    }
  ],
  "totalElements": 100,
  "totalPages": 5
}
```

**Reference:** `config-control-service/src/main/java/com/example/control/api/http/controller/admin/ApplicationServiceController.java`

#### POST `/api/services`

**Purpose:** Create a new service

**Authentication:** Required (JWT with SYS_ADMIN or team member)

**Request Body:**
```json
{
  "displayName": "New Service",
  "environments": ["dev", "prod"],
  "tags": ["microservice"],
  "repoUrl": "https://github.com/example/new-service"
}
```

#### PUT `/api/services/{serviceId}`

**Purpose:** Update service metadata

**Authentication:** Required (JWT - owner team or SYS_ADMIN)

---

### Service Instance Endpoints

#### GET `/api/service-instances`

**Purpose:** List service instances (filtered by access control)

**Authentication:** Required (JWT)

**Query Parameters:**
- `serviceId`: Filter by service ID
- `teamId`: Filter by team ID
- `status`: Filter by status (HEALTHY, UNHEALTHY, DRIFT)
- `hasDrift`: Filter by drift flag

**Reference:** `config-control-service/src/main/java/com/example/control/api/http/controller/admin/ServiceInstanceController.java`

#### GET `/api/service-instances/{instanceId}`

**Purpose:** Get service instance by ID

**Authentication:** Required (JWT)

---

### Drift Event Endpoints

#### GET `/api/drift-events`

**Purpose:** Query drift events

**Authentication:** Required (JWT)

**Query Parameters:**
- `serviceId`: Filter by service ID
- `teamId`: Filter by team ID
- `status`: Filter by status (DETECTED, RESOLVED, IGNORED)
- `severity`: Filter by severity (LOW, MEDIUM, HIGH, CRITICAL)
- `from`: Start date
- `to`: End date

**Response:**
```json
{
  "content": [
    {
      "id": "event-id",
      "serviceName": "sample-service",
      "instanceId": "sample-service-1",
      "severity": "HIGH",
      "status": "DETECTED",
      "detectedAt": "2024-01-01T00:00:00Z"
    }
  ],
  "totalElements": 50
}
```

**Reference:** `config-control-service/src/main/java/com/example/control/api/http/controller/admin/DriftEventController.java`

#### PUT `/api/drift-events/{eventId}/resolve`

**Purpose:** Mark drift event as resolved

**Authentication:** Required (JWT - owner team or SYS_ADMIN)

---

### Approval Workflow Endpoints

#### POST `/api/approval-requests`

**Purpose:** Create approval request for service ownership

**Authentication:** Required (JWT)

**Request Body:**
```json
{
  "serviceId": "service-id",
  "targetTeamId": "team-id",
  "note": "Request to transfer ownership"
}
```

**Response:**
```json
{
  "id": "request-id",
  "status": "PENDING",
  "requiredGates": [
    {
      "gate": "SYS_ADMIN",
      "minApprovals": 1,
      "status": "PENDING"
    }
  ]
}
```

**Reference:** `config-control-service/src/main/java/com/example/control/api/http/controller/admin/ApprovalRequestController.java`

#### POST `/api/approval-requests/{requestId}/approve`

**Purpose:** Approve an approval request

**Authentication:** Required (JWT - approver role)

**Request Body:**
```json
{
  "decision": "APPROVED",
  "note": "Approved for ownership transfer"
}
```

#### POST `/api/approval-requests/{requestId}/reject`

**Purpose:** Reject an approval request

**Authentication:** Required (JWT - approver role)

---

### Service Sharing Endpoints

#### POST `/api/service-shares`

**Purpose:** Share service with team or user

**Authentication:** Required (JWT - owner team or SYS_ADMIN)

**Request Body:**
```json
{
  "serviceId": "service-id",
  "grantToType": "TEAM",
  "grantToId": "team-id",
  "permissions": ["VIEW_SERVICE", "VIEW_INSTANCE"],
  "environments": ["dev"]
}
```

**Reference:** `config-control-service/src/main/java/com/example/control/api/http/controller/admin/ServiceShareController.java`

#### DELETE `/api/service-shares/{shareId}`

**Purpose:** Revoke service share

**Authentication:** Required (JWT - owner team or SYS_ADMIN)

---

### Key-Value Store Endpoints

#### GET `/api/kv/{key}`

**Purpose:** Get Key-Value store entry

**Authentication:** Required (JWT or API Key)

**Response:**
```json
{
  "key": "my.key",
  "value": "my-value",
  "type": "LEAF"
}
```

**Reference:** `config-control-service/src/main/java/com/example/control/api/http/controller/kv/KVController.java`

#### PUT `/api/kv/{key}`

**Purpose:** Set Key-Value store entry (Admin only)

**Authentication:** Required (JWT with SYS_ADMIN or API Key)

**Request Body:**
```json
{
  "value": "new-value",
  "type": "LEAF"
}
```

---

### Service Discovery Endpoints

#### GET `/api/services/{serviceName}/instances`

**Purpose:** Get service instances from Consul

**Authentication:** Required (JWT or API Key)

**Query Parameters:**
- `passing`: Only healthy instances (default: true)

**Response:**
```json
[
  {
    "id": "instance-id",
    "host": "10.0.0.1",
    "port": 8080,
    "status": "HEALTHY"
  }
]
```

**Reference:** `config-control-service/src/main/java/com/example/control/api/http/controller/infra/ServiceRegistryController.java`

---

### Admin Endpoints

#### POST `/api/admin/refresh`

**Purpose:** Trigger configuration refresh

**Authentication:** Required (JWT with SYS_ADMIN)

**Query Parameters:**
- `destination`: Refresh destination (e.g., `service:*`, `service:instance`)

**Reference:** `config-control-service/src/main/java/com/example/control/api/http/controller/admin/AdminController.java`

#### POST `/api/admin/seeder/clean-and-seed`

**Purpose:** Clean and seed database with mock data

**Authentication:** Required (JWT with SYS_ADMIN)

**Response:**
```json
{
  "servicesSeeded": 100,
  "instancesSeeded": 750,
  "driftEventsSeeded": 300
}
```

---

### User Management Endpoints

#### GET `/api/users/permissions`

**Purpose:** Get current user permissions

**Authentication:** Required (JWT)

**Response:**
```json
{
  "userId": "user-id",
  "username": "user",
  "teams": ["team-id"],
  "roles": ["USER"],
  "ownedServices": ["service-id"],
  "sharedServices": ["service-id"]
}
```

**Reference:** `config-control-service/src/main/java/com/example/control/api/http/controller/iam/UserController.java`

---

## Error Responses

All endpoints return standardized error responses using RFC 7807 Problem Details format:

```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Insufficient permissions to access this resource",
  "instance": "/api/services/123"
}
```

**Error Codes:**
- `400`: Bad Request (validation errors)
- `401`: Unauthorized (authentication required)
- `403`: Forbidden (insufficient permissions)
- `404`: Not Found
- `409`: Conflict (optimistic locking)
- `500`: Internal Server Error

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/exception/`

---

## Rate Limiting

**Gateway Rate Limits:**
- Per-user rate limit: 100 requests/second (JWT `sub` claim)
- IP-based fallback: 100 requests/second (if no JWT)

**Response Headers:**
- `X-RateLimit-Remaining`: Remaining requests
- `X-RateLimit-Reset`: Reset timestamp

---

## References

- [OpenAPI Specification](../../../config-control-service/spec/openapi.json)
- [OpenAPI Configuration](../../../config-control-service/src/main/java/com/example/control/infrastructure/config/api/OpenApiConfig.java)
- [Security Configuration](../../../config-control-service/src/main/resources/application-security.yml)
- [Gateway Service README](../../../gateway-service/README.md)

