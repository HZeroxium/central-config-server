# Security Decisions

## Why OAuth2 Resource Server over Session-based?

### Decision

Use OAuth2 Resource Server (JWT-based) authentication instead of session-based authentication.

### Context

The system is a microservices architecture with multiple services (Config Control Service, Config Server, Admin Dashboard). Services need to validate user identity without shared session state.

### Alternatives Considered

1. **Session-based Authentication**
   - Simpler
   - Requires shared session store
   - Not stateless
   - Less suitable for microservices

2. **API Keys Only**
   - Very simple
   - No user context
   - Less secure
   - No fine-grained permissions

3. **mTLS (Mutual TLS)**
   - Strong security
   - Complex setup
   - No user-level authentication
   - Service-to-service only

### Trade-offs

| Aspect | OAuth2 Resource Server | Session-based | API Keys | mTLS |
|--------|----------------------|---------------|---------|------|
| **Stateless** | Yes | No | Yes | Yes |
| **Microservices-friendly** | Yes | No | Yes | Yes |
| **User Context** | Yes (JWT claims) | Yes | No | No |
| **Complexity** | Medium | Low | Low | High |
| **Scalability** | Excellent | Limited | Good | Good |
| **Token Management** | Required | Not needed | Simple | Certificates |

### Rationale

1. **Stateless**: No shared session store needed
2. **Microservices-friendly**: Each service validates JWT independently
3. **JWT Claims**: User context (roles, teams) embedded in token
4. **Scalability**: No session replication needed
5. **Standard Protocol**: OAuth2/OIDC is industry standard

### Implementation

**OAuth2 Resource Server Configuration:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI}
          audience: config-control-service
```

**JWT Claims Used:**
- `sub` - User ID
- `preferred_username` - Username
- `email` - Email address
- `groups` - Team membership
- `realm_access.roles` - Realm roles
- `manager_id` - Manager ID (custom claim)

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/config/security/SecurityConfig.java`

### When to Reconsider

- If single service architecture
- If session management becomes acceptable
- If token management becomes too complex
- If user context is not needed

---

## Why Team-based ABAC + RBAC?

### Decision

Combine Role-Based Access Control (RBAC) with Attribute-Based Access Control (ABAC) using team membership as the primary attribute.

### Context

The system needs fine-grained permissions:
- **RBAC**: System-level roles (SYS_ADMIN, USER)
- **ABAC**: Team-based access (service ownership, team membership)
- **Resource-level**: Service and instance-level permissions

### Alternatives Considered

1. **RBAC Only**
   - Simpler
   - Less flexible
   - Hard to model team ownership

2. **ABAC Only**
   - Very flexible
   - More complex
   - Harder to understand

3. **ACL (Access Control Lists) Only**
   - Simple
   - Doesn't scale
   - Hard to manage

### Trade-offs

| Aspect | RBAC + ABAC | RBAC Only | ABAC Only | ACL Only |
|--------|-------------|-----------|-----------|----------|
| **Flexibility** | High | Medium | Very High | Low |
| **Complexity** | Medium | Low | High | Low |
| **Scalability** | Good | Good | Good | Poor |
| **Team Ownership** | Native | Difficult | Native | Manual |
| **Performance** | Good | Excellent | Medium | Poor |

### Rationale

1. **Team Ownership**: Services belong to teams (Keycloak groups)
2. **Fine-grained Permissions**: Service-level and instance-level control
3. **Scalability**: Team-based grouping scales better than per-resource ACLs
4. **Flexibility**: RBAC for system roles, ABAC for team-based access
5. **Compliance**: Supports approval workflows and audit trails

### Implementation

**Permission Model:**
- **Roles**: SYS_ADMIN (full access), USER (team-based access)
- **Team Attributes**: Team membership from Keycloak groups
- **Resource Attributes**: Service ownership (ownerTeamId)
- **Permissions**: VIEW, EDIT, DELETE, MANAGE_SHARES, etc.

**Permission Evaluation:**
```java
// DomainPermissionEvaluator checks:
// 1. Role (SYS_ADMIN has full access)
// 2. Team ownership (team member has access)
// 3. Service sharing (shared services have limited access)
// 4. Orphan services (all authenticated users can view)
```

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/config/security/DomainPermissionEvaluator.java`

### When to Reconsider

- If team structure changes significantly
- If permissions become too complex
- If performance becomes an issue
- If simpler model is sufficient

---

## Why Keycloak Custom Mappers?

### Decision

Use Keycloak custom protocol mappers to add team membership and manager ID to JWT tokens.

### Context

The system needs team membership and manager information in JWT tokens for permission evaluation. Keycloak's built-in mappers don't support custom attributes like manager ID.

### Alternatives Considered

1. **Built-in Keycloak Mappers Only**
   - Simpler
   - Limited customization
   - No manager ID support

2. **Post-Token Processing**
   - Flexible
   - Additional API calls
   - Performance overhead

3. **Separate IAM Service**
   - Full control
   - More development
   - Duplicate user management

### Trade-offs

| Aspect | Custom Mappers | Built-in Only | Post-Processing | Separate Service |
|--------|---------------|---------------|-----------------|------------------|
| **Customization** | High | Low | High | Very High |
| **Performance** | Excellent | Excellent | Poor | Good |
| **Complexity** | Medium | Low | Medium | High |
| **Maintenance** | Medium | Low | Low | High |
| **Token Size** | Slightly larger | Small | Small | N/A |

### Rationale

1. **Performance**: Team/manager info in token, no additional API calls
2. **Custom Attributes**: Manager ID not available in built-in mappers
3. **Token Completeness**: All needed info in single token
4. **Efficiency**: No post-processing needed
5. **Flexibility**: Easy to add more custom claims

### Implementation

**Custom Mappers:**
- **GroupMembershipMapper**: Maps Keycloak groups to `groups` claim
- **UserAttributeMapper**: Maps user attributes (manager_id) to custom claims
- **AudienceValidator**: Validates token audience

**JWT Token Structure:**
```json
{
  "sub": "user-id",
  "preferred_username": "user1",
  "email": "user1@example.com",
  "groups": ["/teams/team_core"],
  "realm_access": {
    "roles": ["USER"]
  },
  "manager_id": "manager-user-id"
}
```

**Reference:** `config-control-service/keycloak-providers/`

### When to Reconsider

- If Keycloak adds built-in support
- If custom mappers become too complex
- If token size becomes an issue
- If post-processing becomes acceptable

---

## Why PKCE for Admin Dashboard?

### Decision

Use PKCE (Proof Key for Code Exchange) flow for Admin Dashboard OAuth2 authentication.

### Context

The Admin Dashboard is a public client (SPA) that cannot securely store client secrets. PKCE provides additional security for public clients.

### Alternatives Considered

1. **Authorization Code Flow (without PKCE)**
   - Simpler
   - Less secure for public clients
   - Vulnerable to code interception

2. **Implicit Flow**
   - Simple
   - Deprecated
   - Less secure

3. **Client Credentials**
   - Simple
   - No user context
   - Not applicable for user authentication

### Trade-offs

| Aspect | PKCE | Authorization Code | Implicit | Client Credentials |
|--------|------|-------------------|----------|-------------------|
| **Security** | High | Medium | Low | High (but no user) |
| **Public Client Support** | Excellent | Good | Good | Not applicable |
| **Complexity** | Medium | Low | Low | Low |
| **User Context** | Yes | Yes | Yes | No |
| **Modern Standard** | Yes | Yes | No | Yes |

### Rationale

1. **Public Client Security**: PKCE designed for public clients (SPAs)
2. **Code Interception Protection**: Prevents authorization code interception
3. **Modern Standard**: Recommended by OAuth2.1
4. **Keycloak Support**: Native PKCE support in Keycloak
5. **Best Practice**: Industry best practice for SPA authentication

### Implementation

**PKCE Flow:**
1. Generate code verifier and challenge
2. Redirect to Keycloak with code challenge
3. Receive authorization code
4. Exchange code + verifier for tokens
5. Store tokens securely

**Reference:** `admin-dashboard/src/features/auth/authContext.tsx`

### When to Reconsider

- If dashboard becomes confidential client
- If PKCE becomes unnecessary
- If alternative flow is required

---

## Summary

Security decisions prioritize:
1. **Microservices Architecture** (OAuth2 Resource Server, stateless)
2. **Fine-grained Access Control** (RBAC + ABAC)
3. **Performance** (JWT claims, custom mappers)
4. **Modern Standards** (PKCE, OAuth2.1)

These choices provide secure, scalable, and maintainable authentication and authorization.

