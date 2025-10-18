#!/bin/sh

# Keycloak initialization script for config-control realm
# This script sets up the realm, clients, roles, groups, and sample users

set -e
set -o pipefail

# Separate API and health endpoints to avoid port confusion
# - KEYCLOAK_API_URL: OIDC/Admin API base (usually 8080)
# - KEYCLOAK_HEALTH_URL: Health/Metrics base (usually 9000)
KEYCLOAK_API_URL="${KEYCLOAK_API_URL:-http://keycloak:8080}"
KEYCLOAK_HEALTH_URL="${KEYCLOAK_HEALTH_URL:-http://keycloak:9000}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
REALM_NAME="config-control"
REALM_FILE="/tmp/keycloak-realm-export.json"

echo "Starting Keycloak initialization..."
echo "Keycloak API URL: $KEYCLOAK_API_URL"
echo "Keycloak HEALTH URL: $KEYCLOAK_HEALTH_URL"
echo "Admin User: $ADMIN_USER"

# Wait for Keycloak to be ready
echo "Waiting for Keycloak to be ready..."
until curl -f -s "$KEYCLOAK_HEALTH_URL/health/ready" > /dev/null; do
    echo "Keycloak not ready yet, waiting 5 seconds..."
    sleep 5
done

echo "Keycloak is ready, proceeding with initialization..."

# Get admin access token
echo "Getting admin access token..."
TOKEN_RESP=$(curl -s -X POST "$KEYCLOAK_API_URL/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=$ADMIN_USER" \
    -d "password=$ADMIN_PASSWORD" \
    -d "grant_type=password" \
    -d "client_id=admin-cli")

# Extract access_token safely
ADMIN_TOKEN=$(echo "$TOKEN_RESP" | jq -r 'select(type=="object") | .access_token // empty') || true

if [ -z "$ADMIN_TOKEN" ]; then
    echo "Failed to get admin token; raw response: $TOKEN_RESP"
    exit 1
fi

echo "Admin token obtained successfully"

# Check if realm already exists
echo "Checking if realm $REALM_NAME already exists..."
REALM_EXISTS=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME" | jq -r '.realm // empty' || true)

if [ -n "$REALM_EXISTS" ]; then
    echo "Realm $REALM_NAME already exists, skipping creation"
else
    echo "Creating realm $REALM_NAME..."
    RESP_FILE=$(mktemp)
    HTTP_CODE=$(curl -s -o "$RESP_FILE" -w "%{http_code}" -X POST "$KEYCLOAK_API_URL/admin/realms" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d @"$REALM_FILE")
    if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "409" ]; then
        echo "Failed to create realm. HTTP $HTTP_CODE. Response: $(cat "$RESP_FILE")"
        echo "Falling back to minimal realm creation..."
        MIN_REALM_PAYLOAD='{"realm":"config-control","enabled":true,"registrationAllowed":true,"loginWithEmailAllowed":true}'
        HTTP_CODE2=$(curl -s -o "$RESP_FILE" -w "%{http_code}" -X POST "$KEYCLOAK_API_URL/admin/realms" \
            -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
            -d "$MIN_REALM_PAYLOAD")
        if [ "$HTTP_CODE2" != "201" ] && [ "$HTTP_CODE2" != "409" ]; then
            echo "Minimal realm creation failed. HTTP $HTTP_CODE2. Response: $(cat "$RESP_FILE")"
            exit 1
        fi
        echo "Minimal realm create returned HTTP $HTTP_CODE2"
    else
        echo "Realm create returned HTTP $HTTP_CODE"
    fi
    rm -f "$RESP_FILE"

    # Wait until realm is available
    echo "Waiting for realm to become available..."
    for i in $(seq 1 30); do
      R=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME" | jq -r '.realm // empty') || true
      if [ -n "$R" ]; then echo "Realm created successfully"; break; fi
      echo "realm not ready yet..."
      sleep 2
    done
    if [ -z "$R" ]; then
      echo "Realm did not appear in time"
      exit 1
    fi
fi

# Ensure base roles exist (SYS_ADMIN, USER)
ROLE_SYS=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/roles/SYS_ADMIN" | jq -r '.name // empty') || true
if [ -z "$ROLE_SYS" ]; then
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"name":"SYS_ADMIN","description":"System administrator with full access"}' >/dev/null || true
fi
ROLE_USER=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/roles/USER" | jq -r '.name // empty') || true
if [ -z "$ROLE_USER" ]; then
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"name":"USER","description":"Regular user with basic access"}' >/dev/null || true
fi

# Ensure groups
for g in teams team_core team_analytics team_infrastructure; do
  GID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/groups?search=$g" | jq -r '.[0].id // empty') || true
  if [ -z "$GID" ]; then
    curl -s -o /dev/null -w "%{http_code}\n" -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/groups" \
      -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
      -d '{"name":"'"$g"'"}' >/dev/null || true
  fi
done

# Ensure clients
CLIENT_PAYLOAD_SVC='{"clientId":"config-control-service","name":"Config Control Service","enabled":true,"protocol":"openid-connect","publicClient":false,"serviceAccountsEnabled":true,"secret":"config-control-service-secret"}'
CLIENT_PAYLOAD_UI='{"clientId":"admin-dashboard","name":"Admin Dashboard","enabled":true,"protocol":"openid-connect","publicClient":true,"standardFlowEnabled":true,"directAccessGrantsEnabled":true,"attributes":{"pkce.code.challenge.method":"S256"},"redirectUris":["http://localhost:3000/*","http://localhost:3001/*"],"webOrigins":["http://localhost:3000","http://localhost:3001"]}'

# create service client if missing
EXISTS=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients?clientId=config-control-service" | jq -r '.[0].clientId // empty') || true
if [ -z "$EXISTS" ]; then
  curl -s -o /dev/null -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d "$CLIENT_PAYLOAD_SVC" >/dev/null || true
fi

# create ui client if missing
EXISTS=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients?clientId=admin-dashboard" | jq -r '.[0].clientId // empty') || true
if [ -z "$EXISTS" ]; then
  curl -s -o /dev/null -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d "$CLIENT_PAYLOAD_UI" >/dev/null || true
fi

# Helper: ensure user exists and return id
ensure_user() {
  local username="$1" email="$2" password="$3" first="$4" last="$5"
  local uid
  uid=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users?username=$username" | jq -r '.[0].id // empty')
  if [ -z "$uid" ]; then
    uid=$(curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users" \
      -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
      -d '{
        "username": "'$username'",
        "email": "'$email'",
        "firstName": "'$first'",
        "lastName": "'$last'",
        "enabled": true,
        "emailVerified": true
      }' -i | awk -v FS='[ /\r\n]' '/^Location:/ {print $NF}' | awk -F'/' '{print $NF}')
    # set password
    if [ -n "$uid" ]; then
      curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$uid/reset-password" \
        -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
        -d '{"type":"password","value":"'$password'","temporary":false}' >/dev/null
    fi
  fi
  echo "$uid"
}

ensure_role_mapping() {
  local uid="$1" roleName="$2"
  local rid
  rid=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/roles/$roleName" | jq -r '.id')
  [ -n "$rid" ] || return 0
  curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$uid/role-mappings/realm" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d "[{\"id\":\"$rid\",\"name\":\"$roleName\"}]" >/dev/null || true
}

ensure_group_membership() {
  local uid="$1" gname="$2"
  local gid
  gid=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/groups?search=$gname" | jq -r '.[0].id // empty')
  [ -n "$gid" ] || return 0
  curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$uid/groups/$gid" \
    -H "Authorization: Bearer $ADMIN_TOKEN" >/dev/null || true
}

# Create/ensure users and assignments
echo "Creating sample users..."

echo "Ensuring admin user and SYS_ADMIN role..."
ADMIN_USER_ID=$(ensure_user admin admin@example.com admin123 System Administrator)
[ -n "$ADMIN_USER_ID" ] && ensure_role_mapping "$ADMIN_USER_ID" SYS_ADMIN

# Create user1 (team lead)
echo "Creating user1 (team lead)..."
USER1_ID=$(ensure_user user1 user1@example.com user123 John Doe)

if [ -n "$USER1_ID" ]; then
    echo "User1 created with ID: $USER1_ID"
    
    ensure_role_mapping "$USER1_ID" USER
    
    # Add user1 to team_core
    ensure_group_membership "$USER1_ID" team_core && echo "User1 added to team_core"
fi

# Create user2 (regular user)
echo "Creating user2 (regular user)..."
USER2_ID=$(ensure_user user2 user2@example.com user123 Jane Smith)

if [ -n "$USER2_ID" ]; then
    echo "User2 created with ID: $USER2_ID"
    
    ensure_role_mapping "$USER2_ID" USER
    
    # Add user2 to team_analytics
    ensure_group_membership "$USER2_ID" team_analytics && echo "User2 added to team_analytics"
fi

# Create user3 (infrastructure team)
echo "Creating user3 (infrastructure team)..."
USER3_ID=$(ensure_user user3 user3@example.com user123 Bob Johnson)

if [ -n "$USER3_ID" ]; then
    echo "User3 created with ID: $USER3_ID"
    
    ensure_role_mapping "$USER3_ID" USER
    
    # Add user3 to team_infrastructure
    ensure_group_membership "$USER3_ID" team_infrastructure && echo "User3 added to team_infrastructure"
fi
echo "Keycloak initialization completed successfully!"
echo ""
echo "Sample users created:"
echo "  - admin@example.com (password: admin123) - SYS_ADMIN role"
echo "  - user1@example.com (password: user123) - USER role, team_core member"
echo "  - user2@example.com (password: user123) - USER role, team_analytics member, reports to user1"
echo "  - user3@example.com (password: user123) - USER role, team_infrastructure member"
echo ""
echo "Keycloak Admin Console: $KEYCLOAK_API_URL/admin"
echo "Realm: $REALM_NAME"
