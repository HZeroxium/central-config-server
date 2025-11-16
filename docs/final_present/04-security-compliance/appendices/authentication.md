# Authentication Implementation
## OAuth2/OIDC Integration Details

---

## Overview

The system uses Keycloak as the Identity Provider (IdP) for OAuth2/OIDC authentication, supporting both web clients (PKCE flow) and service-to-service communication (client credentials).

---

## Keycloak Configuration

### Realm Setup

**Realm Name:** `config-control-realm`

**Clients:**
1. **admin-dashboard** (Public Client)
   - Client ID: `admin-dashboard`
   - Client Type: Public
   - PKCE: Enabled
   - Redirect URIs: `http://localhost:3000/*`, `https://admin.example.com/*`

2. **config-control-service** (Confidential Client)
   - Client ID: `config-control-service`
   - Client Type: Confidential
   - Client Secret: Stored securely
   - Service Account: Enabled

3. **zcm-sdk** (Service Account Client)
   - Client ID: `zcm-sdk`
   - Client Type: Confidential
   - Service Account: Enabled
   - Used for SDK authentication

**Reference:** `config-control-service/README-KEYCLOAK.md`

---

## Authentication Flows

### Web Client (PKCE Flow)

**Used by:** Admin Dashboard (React)

**Flow:**
1. User initiates login
2. Redirect to Keycloak authorization endpoint
3. User authenticates
4. Keycloak returns authorization code
5. Client exchanges code for tokens (with code verifier)
6. Client receives access token (JWT) and refresh token

**Security:** PKCE prevents authorization code interception

**Implementation:** React OAuth2 client library

---

### Service-to-Service (Client Credentials)

**Used by:** ZCM SDK

**Flow:**
1. SDK requests token with client credentials
2. Keycloak validates credentials
3. Keycloak issues access token (JWT)
4. SDK uses token for API requests

**Configuration:**
```yaml
zcm:
  sdk:
    api-key:
      enabled: true
      key: your-api-key-here
```

**Reference:** `zcm-spring-sdk-starter/README.md`

---

## JWT Token Structure

### Token Claims

**Standard Claims:**
- `sub` - Subject (user ID)
- `preferred_username` - Username
- `email` - Email address
- `given_name` - First name
- `family_name` - Last name
- `exp` - Expiration time
- `iat` - Issued at time
- `iss` - Issuer (Keycloak URL)
- `aud` - Audience (client ID)

**Custom Claims:**
- `groups` - Team IDs (Keycloak groups)
- `realm_access.roles` - System roles (SYS_ADMIN, USER)
- `manager_id` - Line manager ID (for approval workflows)

**Token Validation:**
- Issuer validation
- Audience validation
- Signature verification
- Expiration check

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/config/security/SecurityConfig.java`

---

## User Context Extraction

**Implementation:** `config-control-service/src/main/java/com/example/control/infrastructure/config/security/UserContextExtractor.java`

**Process:**
1. Extract JWT from `Authorization` header
2. Validate token (issuer, audience, signature)
3. Extract claims
4. Build `UserContext` object:
   - `userId` - From `sub` claim
   - `username` - From `preferred_username`
   - `email` - From `email` claim
   - `teamIds` - From `groups` claim
   - `roles` - From `realm_access.roles`
   - `managerId` - From `manager_id` claim

**Usage:**
```java
@Autowired
private UserContextExtractor userContextExtractor;

UserContext context = userContextExtractor.extract(jwt);
```

---

## Security Configuration

### Spring Security Setup

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/config/security/SecurityConfig.java`

**Configuration:**
- OAuth2 Resource Server
- JWT token validation
- Public endpoints (health, metrics)
- Protected endpoints (`/api/**`)
- CORS configuration

**Method Security:**
- `@PreAuthorize` annotations
- Domain-level permission evaluation
- Role-based access control

---

## API Key Authentication (SDK)

**Purpose:** Allow SDK clients to bypass JWT authentication for service-to-service communication

**Configuration:**
```yaml
zcm:
  sdk:
    api-key:
      enabled: true
      key: your-api-key-here
```

**Usage:**
- SDK includes `X-API-Key` header in requests
- Service validates API key
- Grants SYS_ADMIN privileges

**Security:**
- API key stored securely
- Can be overridden via environment variable
- Rotated periodically

**Reference:** `zcm-spring-sdk-starter/README.md`

---

## Token Refresh

**Web Clients:**
- Use refresh token to obtain new access token
- Refresh token stored securely
- Automatic token refresh before expiration

**Service Clients:**
- Request new token when expired
- Client credentials grant
- No refresh token needed

---

## Security Best Practices

1. **Token Storage**
   - Web: Secure HTTP-only cookies or memory
   - Service: In-memory only

2. **Token Validation**
   - Always validate issuer and audience
   - Check expiration
   - Verify signature

3. **HTTPS Only**
   - All communication over TLS
   - No HTTP in production

4. **Token Rotation**
   - Regular token refresh
   - API key rotation

---

## Troubleshooting

### Token Validation Failures

**Common Issues:**
1. Wrong issuer URL
2. Audience mismatch
3. Expired token
4. Invalid signature

**Solutions:**
1. Verify Keycloak issuer URL
2. Check client ID in audience
3. Refresh token
4. Verify Keycloak public key

### Authentication Errors

**Common Issues:**
1. Missing Authorization header
2. Invalid token format
3. Token not from Keycloak

**Solutions:**
1. Include `Authorization: Bearer {token}` header
2. Verify token format (JWT)
3. Check Keycloak configuration

---

## References

- [Keycloak Integration Guide](../../../config-control-service/README-KEYCLOAK.md)
- [Security Configuration](../README.md#authentication-flow)
- [Authorization Model](./authorization.md)

