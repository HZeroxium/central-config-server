#!/bin/sh

# Keycloak initialization script for config-control realm
# This script orchestrates initialization from normalized JSON configuration files
# with smart merge logic (create if missing, update if exists)

set -e
set -o pipefail

# Configuration
KEYCLOAK_API_URL="${KEYCLOAK_API_URL:-http://keycloak:8080}"
KEYCLOAK_HEALTH_URL="${KEYCLOAK_HEALTH_URL:-http://keycloak:9000}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
REALM_NAME="config-control"
BASE_DIR="/tmp"

echo "======================================================================"
echo "Keycloak Initialization Script (JSON-driven with Smart Merge)"
echo "======================================================================"
echo "Keycloak API URL: $KEYCLOAK_API_URL"
echo "Keycloak Health URL: $KEYCLOAK_HEALTH_URL"
echo "Admin User: $ADMIN_USER"
echo "Realm: $REALM_NAME"
echo "Base Directory: $BASE_DIR"
echo "======================================================================"

# Helper: Load JSON file
load_json() {
  local filepath="$1"
  if [ ! -f "$filepath" ]; then
    echo "ERROR: JSON file not found: $filepath" >&2
    return 1
  fi
  cat "$filepath"
}

# Wait for Keycloak to be ready
echo ""
echo "[1/12] Waiting for Keycloak to be ready..."
until curl -f -s "$KEYCLOAK_HEALTH_URL/health/ready" > /dev/null; do
    echo "  Keycloak not ready yet, waiting 5 seconds..."
    sleep 5
done
echo "  ✓ Keycloak is ready"

# Get admin access token
echo ""
echo "[2/12] Getting admin access token..."
TOKEN_RESP=$(curl -s -X POST "$KEYCLOAK_API_URL/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=$ADMIN_USER" \
    -d "password=$ADMIN_PASSWORD" \
    -d "grant_type=password" \
    -d "client_id=admin-cli")

ADMIN_TOKEN=$(echo "$TOKEN_RESP" | jq -r 'select(type=="object") | .access_token // empty') || true

if [ -z "$ADMIN_TOKEN" ]; then
    echo "ERROR: Failed to get admin token; raw response: $TOKEN_RESP" >&2
    exit 1
fi
echo "  ✓ Admin token obtained successfully"

# Check if realm exists, create if missing
echo ""
echo "[3/12] Checking/creating realm..."
REALM_EXISTS=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME" | jq -r '.realm // empty' || true)

if [ -n "$REALM_EXISTS" ]; then
    echo "  ✓ Realm $REALM_NAME already exists"
else
    echo "  Creating realm $REALM_NAME..."
    RESP_FILE=$(mktemp)
    HTTP_CODE=$(curl -s -o "$RESP_FILE" -w "%{http_code}" -X POST "$KEYCLOAK_API_URL/admin/realms" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d @"$BASE_DIR/keycloak-realm-export.json")
    
    if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "409" ]; then
        echo "  Falling back to minimal realm creation..."
        MIN_REALM_PAYLOAD='{"realm":"config-control","enabled":true,"registrationAllowed":true,"loginWithEmailAllowed":true}'
        HTTP_CODE2=$(curl -s -o "$RESP_FILE" -w "%{http_code}" -X POST "$KEYCLOAK_API_URL/admin/realms" \
            -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
            -d "$MIN_REALM_PAYLOAD")
        if [ "$HTTP_CODE2" != "201" ] && [ "$HTTP_CODE2" != "409" ]; then
            echo "ERROR: Minimal realm creation failed. HTTP $HTTP_CODE2. Response: $(cat "$RESP_FILE")" >&2
            exit 1
        fi
    fi
    rm -f "$RESP_FILE"

    # Wait for realm to become available
    for i in $(seq 1 30); do
      R=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME" | jq -r '.realm // empty') || true
      if [ -n "$R" ]; then 
        echo "  ✓ Realm created successfully"
        break
      fi
      sleep 2
    done
fi

# Process roles.json
echo ""
echo "[4/12] Processing roles..."
ROLES_JSON=$(load_json "$BASE_DIR/roles.json")
echo "$ROLES_JSON" | jq -c '.[]' | while IFS= read -r role; do
  name=$(echo "$role" | jq -r '.name')
  description=$(echo "$role" | jq -r '.description')
  
  EXISTING=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/roles/$name" | jq -r '.name // empty') || true
  
  if [ -z "$EXISTING" ]; then
    curl -s -o /dev/null -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/roles" \
      -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
      -d "$role" || true
    echo "  ✓ Created role: $name"
  else
    echo "  - Role already exists: $name"
  fi
done

# Process groups.json
echo ""
echo "[5/12] Processing groups..."
GROUPS_JSON=$(load_json "$BASE_DIR/groups.json")
GROUP_PARENT_COUNT=$(echo "$GROUPS_JSON" | jq 'length')

# Process parent group first
i=0
while [ $i -lt $GROUP_PARENT_COUNT ]; do
  parent_group=$(echo "$GROUPS_JSON" | jq -c ".[$i]")
  parent_name=$(echo "$parent_group" | jq -r '.name')
  parent_path=$(echo "$parent_group" | jq -r '.path')
  
  PARENT_GID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/groups?search=$parent_name" | jq -r '.[0].id // empty') || true
  
  if [ -z "$PARENT_GID" ]; then
    PARENT_PAYLOAD=$(echo "$parent_group" | jq 'del(.subGroups) | del(.id)')
    curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/groups" \
      -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
      -d "$PARENT_PAYLOAD" >/dev/null || true
    # Get all top-level groups and find ours by name
    PARENT_GID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
      "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/groups" | jq -r ".[] | select(.name==\"$parent_name\") | .id // empty") || true
    echo "  ✓ Created parent group: $parent_name (ID: $PARENT_GID)"
  else
    echo "  - Parent group already exists: $parent_name (ID: $PARENT_GID)"
  fi
  
  # Process child groups as children of parent
  if [ -n "$PARENT_GID" ]; then
    CHILD_COUNT=$(echo "$parent_group" | jq '.subGroups | length // 0')
    j=0
    while [ $j -lt $CHILD_COUNT ]; do
      child_group=$(echo "$parent_group" | jq -c ".subGroups[$j]")
      child_name=$(echo "$child_group" | jq -r '.name')
      
      # Search for child group
      CHILD_GID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
        "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/groups?search=$child_name" | jq -r '.[0].id // empty') || true
      
      if [ -z "$CHILD_GID" ]; then
        # Create as child of parent using the children endpoint (remove id field)
        CHILD_PAYLOAD=$(echo "$child_group" | jq 'del(.id)')
        curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/groups/$PARENT_GID/children" \
          -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
          -d "$CHILD_PAYLOAD" >/dev/null || true
        # Search for the newly created child
        CHILD_GID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
          "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/groups/$PARENT_GID/children" | jq -r ".[] | select(.name==\"$child_name\") | .id // empty") || true
        echo "  ✓ Created child group: $child_name (ID: $CHILD_GID)"
      else
        echo "  - Child group already exists: $child_name (ID: $CHILD_GID)"
      fi
      j=$((j+1))
    done
  fi
  i=$((i+1))
done

# Process client-scopes
echo ""
echo "[6/12] Processing client scopes..."
for scope_file in "$BASE_DIR"/client-scopes/*.json; do
  if [ ! -f "$scope_file" ]; then continue; fi
  
  SCOPE_JSON=$(load_json "$scope_file")
  scope_name=$(echo "$SCOPE_JSON" | jq -r '.name')
  
  SCOPE_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/client-scopes?search=$scope_name" | \
    jq -r ".[] | select(.name==\"$scope_name\") | .id // empty") || true
  
  if [ -z "$SCOPE_ID" ]; then
    RESP=$(curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/client-scopes" \
      -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
      -d "$SCOPE_JSON")
    SCOPE_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
      "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/client-scopes?search=$scope_name" | \
      jq -r ".[] | select(.name==\"$scope_name\") | .id // empty") || true
    echo "  ✓ Created client scope: $scope_name (ID: $SCOPE_ID)"
  else
    echo "  - Client scope already exists: $scope_name (ID: $SCOPE_ID)"
  fi
done

# Process mappers (associate with scopes)
echo ""
echo "[7/12] Processing mappers..."
for mapper_file in "$BASE_DIR"/mappers/*.json; do
  if [ ! -f "$mapper_file" ]; then continue; fi
  
  MAPPER_JSON=$(load_json "$mapper_file")
  mapper_name=$(echo "$MAPPER_JSON" | jq -r '.name')
  parent_scope_name=$(echo "$MAPPER_JSON" | jq -r '.parentScope')
  
  # Get scope ID
  SCOPE_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/client-scopes?search=$parent_scope_name" | \
    jq -r ".[] | select(.name==\"$parent_scope_name\") | .id // empty") || true
  
  if [ -n "$SCOPE_ID" ]; then
    # Remove parentScope from payload
    MAPPER_PAYLOAD=$(echo "$MAPPER_JSON" | jq 'del(.parentScope)')
    
    # Check if mapper already exists
    EXISTING_MAPPER=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
      "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/client-scopes/$SCOPE_ID/protocol-mappers/models" | \
      jq -r ".[] | select(.name==\"$mapper_name\") | .id // empty") || true
    
    if [ -z "$EXISTING_MAPPER" ]; then
      curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/client-scopes/$SCOPE_ID/protocol-mappers/models" \
        -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
        -d "$MAPPER_PAYLOAD" >/dev/null || true
      echo "  ✓ Created mapper: $mapper_name in scope: $parent_scope_name"
    else
      echo "  - Mapper already exists: $mapper_name in scope: $parent_scope_name"
    fi
  fi
done

# Process clients
echo ""
echo "[8/12] Processing clients..."
for client_file in "$BASE_DIR"/clients/*.json; do
  if [ ! -f "$client_file" ]; then continue; fi
  
  CLIENT_JSON=$(load_json "$client_file")
  client_id=$(echo "$CLIENT_JSON" | jq -r '.clientId')
  
  CLIENT_IEID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients?clientId=$client_id" | \
    jq -r '.[0].id // empty') || true
  
  if [ -z "$CLIENT_IEID" ]; then
    curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
      -d "$CLIENT_JSON" >/dev/null || true
    CLIENT_IEID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
      "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients?clientId=$client_id" | \
      jq -r '.[0].id // empty') || true
    echo "  ✓ Created client: $client_id (IE ID: $CLIENT_IEID)"
  else
    echo "  - Client already exists: $client_id (IE ID: $CLIENT_IEID)"
  fi
done

# Process scope assignments
echo ""
echo "[9/12] Processing scope assignments..."
SCOPE_ASSIGNMENTS=$(load_json "$BASE_DIR/scope-assignments.json")

echo "$SCOPE_ASSIGNMENTS" | jq -c '.[]' | while IFS= read -r assignment; do
  client_id=$(echo "$assignment" | jq -r '.clientId')
  
  CLIENT_IEID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients?clientId=$client_id" | \
    jq -r '.[0].id // empty') || true
  
  if [ -n "$CLIENT_IEID" ]; then
    # Process default scopes
    echo "$assignment" | jq -r '.defaultScopes[]' 2>/dev/null | while IFS= read -r scope_name; do
      SCOPE_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
        "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/client-scopes?search=$scope_name" | \
        jq -r ".[] | select(.name==\"$scope_name\") | .id // empty") || true
      
      if [ -n "$SCOPE_ID" ]; then
        curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients/$CLIENT_IEID/default-client-scopes/$SCOPE_ID" \
          -H "Authorization: Bearer $ADMIN_TOKEN" >/dev/null || true
        echo "  ✓ Assigned default scope '$scope_name' to client '$client_id'"
      fi
    done
    
    # Process optional scopes
    echo "$assignment" | jq -r '.optionalScopes[]' 2>/dev/null | while IFS= read -r scope_name; do
      SCOPE_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
        "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/client-scopes?search=$scope_name" | \
        jq -r ".[] | select(.name==\"$scope_name\") | .id // empty") || true
      
      if [ -n "$SCOPE_ID" ]; then
        curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients/$CLIENT_IEID/optional-client-scopes/$SCOPE_ID" \
          -H "Authorization: Bearer $ADMIN_TOKEN" >/dev/null || true
        echo "  ✓ Assigned optional scope '$scope_name' to client '$client_id'"
      fi
    done
  fi
done

# Process users
echo ""
echo "[10/12] Processing users..."
USERS_JSON=$(load_json "$BASE_DIR/users.json")
USER_COUNT=$(echo "$USERS_JSON" | jq 'length')

# First pass: Create users
i=0
while [ $i -lt $USER_COUNT ]; do
  user=$(echo "$USERS_JSON" | jq -c ".[$i]")
  username=$(echo "$user" | jq -r '.username')
  
  USER_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users?username=$username" | \
    jq -r '.[0].id // empty') || true
  
  if [ -z "$USER_ID" ]; then
    # Create user payload without credentials
    USER_PAYLOAD=$(echo "$user" | jq 'del(.credentials) | del(.realmRoles) | del(.groups)')
    
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users" \
      -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
      -d "$USER_PAYLOAD")
    
    # Get the newly created user
    USER_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
      "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users?username=$username" | \
      jq -r '.[0].id // empty') || true
    
    if [ -n "$USER_ID" ]; then
      # Set password
      PASSWORD=$(echo "$user" | jq -r '.credentials[0].value')
      curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$USER_ID/reset-password" \
        -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
        -d "{\"type\":\"password\",\"value\":\"$PASSWORD\",\"temporary\":false}" >/dev/null || true
      
      echo "  ✓ Created user: $username (ID: $USER_ID)"
    fi
  else
    echo "  - User already exists: $username (ID: $USER_ID)"
  fi
  
  i=$((i+1))
done

# Second pass: Update manager_id attributes (after all users exist)
echo ""
echo "[11/12] Processing user relationships..."
i=0
while [ $i -lt $USER_COUNT ]; do
  user=$(echo "$USERS_JSON" | jq -c ".[$i]")
  username=$(echo "$user" | jq -r '.username')
  manager_username=$(echo "$user" | jq -r '.attributes.manager_username[0] // empty')
  
  if [ -n "$manager_username" ]; then
    USER_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
      "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users?username=$username" | \
      jq -r '.[0].id // empty') || true
    
    MANAGER_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
      "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users?username=$manager_username" | \
      jq -r '.[0].id // empty') || true
    
    if [ -n "$USER_ID" ] && [ -n "$MANAGER_ID" ]; then
      curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$USER_ID" \
        -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
        -d "{\"attributes\":{\"manager_id\":[\"$MANAGER_ID\"]}}" >/dev/null || true
      echo "  ✓ Set manager for $username → $manager_username (ID: $MANAGER_ID)"
    fi
  fi
  i=$((i+1))
done

# Assign roles to users
i=0
while [ $i -lt $USER_COUNT ]; do
  user=$(echo "$USERS_JSON" | jq -c ".[$i]")
  username=$(echo "$user" | jq -r '.username')
  
  USER_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users?username=$username" | \
    jq -r '.[0].id // empty') || true
  
  if [ -n "$USER_ID" ]; then
    ROLE_COUNT=$(echo "$user" | jq '.realmRoles | length // 0')
    j=0
    while [ $j -lt $ROLE_COUNT ]; do
      role_name=$(echo "$user" | jq -r ".realmRoles[$j]")
      ROLE_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
        "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/roles/$role_name" | jq -r '.id // empty') || true
      
      if [ -n "$ROLE_ID" ]; then
        curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$USER_ID/role-mappings/realm" \
          -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
          -d "[{\"id\":\"$ROLE_ID\",\"name\":\"$role_name\"}]" >/dev/null || true
        echo "  ✓ Assigned role '$role_name' to user '$username'"
      fi
      j=$((j+1))
    done
  fi
  i=$((i+1))
done

# Assign groups to users
echo ""
echo "[12/12] Processing group memberships..."
i=0
while [ $i -lt $USER_COUNT ]; do
  user=$(echo "$USERS_JSON" | jq -c ".[$i]")
  username=$(echo "$user" | jq -r '.username')
  
  USER_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users?username=$username" | \
    jq -r '.[0].id // empty') || true
  
  if [ -n "$USER_ID" ]; then
    GROUP_COUNT=$(echo "$user" | jq '.groups | length // 0')
    j=0
    while [ $j -lt $GROUP_COUNT ]; do
      group_name=$(echo "$user" | jq -r ".groups[$j]")
      GROUP_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
        "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/groups?search=$group_name" | \
        jq -r '.[0].id // empty') || true
      
      if [ -n "$GROUP_ID" ]; then
        curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$USER_ID/groups/$GROUP_ID" \
          -H "Authorization: Bearer $ADMIN_TOKEN" >/dev/null || true
        echo "  ✓ Added user '$username' to group '$group_name'"
      fi
      j=$((j+1))
    done
  fi
  i=$((i+1))
done

echo ""
echo "======================================================================"
echo "Keycloak initialization completed successfully!"
echo "======================================================================"
echo ""
echo "Summary:"
echo "  - Realm: $REALM_NAME"
echo "  - Roles: SYS_ADMIN, USER, TEAM_LEADER"
echo "  - Groups: teams/team1, teams/team2"
echo "  - Users: admin, admin2, user1, user2, user3, user4, user5"
echo "  - Clients: config-control-service, admin-dashboard"
echo "  - Client Scopes: groups, manager_id, audience"
echo ""
echo "Keycloak Admin Console: $KEYCLOAK_API_URL/admin"
echo "Realm: $REALM_NAME"
echo "======================================================================"
