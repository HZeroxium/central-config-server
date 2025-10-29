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

# Source function library
. "$BASE_DIR/keycloak-functions.sh"

# Configure custom authentication flows
configure_custom_flows() {
  log_info "Configuring custom authentication flows..."

  # Create custom registration flow by copying the built-in 'registration' flow (idempotent)
  log_info "Creating custom registration flow by copying 'registration'..."
  curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/authentication/flows/registration/copy" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"newName": "custom-registration"}' >/dev/null || true

  # Bind as realm registration flow
  curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"registrationFlow":"custom-registration"}' >/dev/null || true
  log_success "Custom registration flow configured (copied from built-in)"

  # Create custom browser flow by copying the built-in 'browser' flow (idempotent)
  log_info "Creating custom browser flow by copying 'browser'..."
  curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/authentication/flows/browser/copy" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"newName": "custom-browser"}' >/dev/null || true

  # Bind as realm browser flow
  curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"browserFlow":"custom-browser"}' >/dev/null || true
  log_success "Custom browser flow configured (copied from built-in)"
}

# Configure User Profile with custom attributes
configure_user_profile() {
  log_info "Configuring User Profile attributes..."
  
  # Get current User Profile configuration
  USER_PROFILE=$(curl -s -X GET "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/profile" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json")
  
  # Load custom attributes from JSON file
  CUSTOM_ATTRIBUTES=$(load_json "$BASE_DIR/user-profile-attributes.json")
  
  # Merge custom attributes with existing profile
  UPDATED_PROFILE=$(echo "$USER_PROFILE" | jq --argjson custom "$CUSTOM_ATTRIBUTES" \
    '.attributes += $custom')
  
  # Update User Profile
  curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/profile" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$UPDATED_PROFILE"
  
  log_success "User Profile configured with custom attributes"
}

# Function to update manager_id attribute for a user
update_manager_id() {
  local USER_ID=$1
  local MANAGER_USERNAME=$2
  
  local MANAGER_USER_ID=$(get_user_id "$MANAGER_USERNAME")
  
  if [ -n "$MANAGER_USER_ID" ]; then
    log_info "Setting manager_id=$MANAGER_USER_ID for user $USER_ID"
    
    # Get current user data
    CURRENT_USER=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
      "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$USER_ID")
    
    # Merge manager_id with existing attributes
    UPDATED_ATTRIBUTES=$(echo "$CURRENT_USER" | jq ".attributes + {\"manager_id\": [\"$MANAGER_USER_ID\"]}")
    
    # Build complete user object by merging attributes
    UPDATED_USER=$(echo "$CURRENT_USER" | jq --argjson attrs "$UPDATED_ATTRIBUTES" '.attributes = $attrs')
    
    # Update user with complete object
    curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$USER_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "$UPDATED_USER"
  else
    log_warning "Manager user '$MANAGER_USERNAME' not found, skipping manager_id"
  fi
}

# Main initialization flow
echo ""
log_step 1 15 "Waiting for Keycloak to be ready"
wait_for_keycloak

echo ""
log_step 2 15 "Getting admin access token"
get_admin_token

# Check if realm exists, create if missing
echo ""
log_step 3 15 "Checking/creating realm"
REALM_EXISTS=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME" | jq -r '.realm // empty' || true)

if [ -n "$REALM_EXISTS" ]; then
    log_success "Realm $REALM_NAME already exists"
else
    log_info "Creating realm $REALM_NAME..."
    RESP_FILE=$(mktemp)
    HTTP_CODE=$(curl -s -o "$RESP_FILE" -w "%{http_code}" -X POST "$KEYCLOAK_API_URL/admin/realms" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d @"$BASE_DIR/keycloak-realm-export.json")
    
    if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "409" ]; then
        log_warning "Falling back to minimal realm creation..."
        MIN_REALM_PAYLOAD='{"realm":"config-control","enabled":true,"registrationAllowed":true,"loginWithEmailAllowed":true}'
        HTTP_CODE2=$(curl -s -o "$RESP_FILE" -w "%{http_code}" -X POST "$KEYCLOAK_API_URL/admin/realms" \
            -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
            -d "$MIN_REALM_PAYLOAD")
        if [ "$HTTP_CODE2" != "201" ] && [ "$HTTP_CODE2" != "409" ]; then
            log_error "Minimal realm creation failed. HTTP $HTTP_CODE2. Response: $(cat "$RESP_FILE")"
            exit 1
        fi
    fi
    rm -f "$RESP_FILE"

    # Wait for realm to become available
    for i in $(seq 1 30); do
      R=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME" | jq -r '.realm // empty') || true
      if [ -n "$R" ]; then 
        log_success "Realm created successfully"
        break
      fi
      sleep 2
    done
fi

# Configure User Profile (must be before user creation)
echo ""
log_step 4 15 "Configuring User Profile"
configure_user_profile

# Configure custom authentication flows
echo ""
log_step 4.5 15 "Configuring Custom Authentication Flows"
configure_custom_flows

# Process roles.json
echo ""
log_step 5 15 "Processing roles"
ROLES_JSON=$(load_json "$BASE_DIR/roles.json")
echo "$ROLES_JSON" | jq -c '.[]' | while IFS= read -r role; do
  name=$(echo "$role" | jq -r '.name')
  description=$(echo "$role" | jq -r '.description')
  
  if role_exists "$name"; then
    log_success "Role already exists: $name"
  else
    curl -s -o /dev/null -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/roles" \
      -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
      -d "$role" || true
    log_success "Created role: $name"
  fi
done

# Process groups.json
echo ""
log_step 6 15 "Processing groups"
GROUPS_JSON=$(load_json "$BASE_DIR/groups.json")
GROUP_PARENT_COUNT=$(echo "$GROUPS_JSON" | jq 'length')

# Process parent group first
i=0
while [ $i -lt $GROUP_PARENT_COUNT ]; do
  parent_group=$(echo "$GROUPS_JSON" | jq -c ".[$i]")
  parent_name=$(echo "$parent_group" | jq -r '.name')
  parent_path=$(echo "$parent_group" | jq -r '.path')
  
  PARENT_GID=$(get_group_id "$parent_name")
  
  if [ -z "$PARENT_GID" ]; then
    PARENT_PAYLOAD=$(echo "$parent_group" | jq 'del(.subGroups) | del(.id)')
    curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/groups" \
      -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
      -d "$PARENT_PAYLOAD" >/dev/null || true
    PARENT_GID=$(get_group_id "$parent_name")
    log_success "Created parent group: $parent_name (ID: $PARENT_GID)"
  else
    log_success "Parent group already exists: $parent_name (ID: $PARENT_GID)"
  fi
  
  # Process child groups as children of parent
  if [ -n "$PARENT_GID" ]; then
    CHILD_COUNT=$(echo "$parent_group" | jq '.subGroups | length // 0')
    j=0
    while [ $j -lt $CHILD_COUNT ]; do
      child_group=$(echo "$parent_group" | jq -c ".subGroups[$j]")
      child_name=$(echo "$child_group" | jq -r '.name')
      
      CHILD_GID=$(get_group_id "$child_name")
      
      if [ -z "$CHILD_GID" ]; then
        CHILD_PAYLOAD=$(echo "$child_group" | jq 'del(.id)')
        curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/groups/$PARENT_GID/children" \
          -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
          -d "$CHILD_PAYLOAD" >/dev/null || true
        CHILD_GID=$(get_group_id "$child_name")
        log_success "Created child group: $child_name (ID: $CHILD_GID)"
      else
        log_success "Child group already exists: $child_name (ID: $CHILD_GID)"
      fi
      j=$((j+1))
    done
  fi
  i=$((i+1))
done

# Process client-scopes
echo ""
log_step 7 15 "Processing client scopes"
for scope_file in "$BASE_DIR"/client-scopes/*.json; do
  if [ ! -f "$scope_file" ]; then continue; fi
  
  SCOPE_JSON=$(load_json "$scope_file")
  scope_name=$(echo "$SCOPE_JSON" | jq -r '.name')
  
  if client_scope_exists "$scope_name"; then
    log_success "Client scope already exists: $scope_name"
  else
    curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/client-scopes" \
      -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
      -d "$SCOPE_JSON"
    log_success "Created client scope: $scope_name"
  fi
done

# Process mappers (associate with scopes)
echo ""
log_step 8 15 "Processing mappers"
for mapper_file in "$BASE_DIR"/mappers/*.json; do
  if [ ! -f "$mapper_file" ]; then continue; fi
  
  MAPPER_JSON=$(load_json "$mapper_file")
  mapper_name=$(echo "$MAPPER_JSON" | jq -r '.name')
  parent_scope_name=$(echo "$MAPPER_JSON" | jq -r '.parentScope')
  
  SCOPE_ID=$(get_client_scope_id "$parent_scope_name")
  
  if [ -n "$SCOPE_ID" ]; then
    MAPPER_PAYLOAD=$(echo "$MAPPER_JSON" | jq 'del(.parentScope)')
    
    EXISTING_MAPPER=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
      "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/client-scopes/$SCOPE_ID/protocol-mappers/models" | \
      jq -r ".[] | select(.name==\"$mapper_name\") | .id // empty") || true
    
    if [ -z "$EXISTING_MAPPER" ]; then
      curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/client-scopes/$SCOPE_ID/protocol-mappers/models" \
        -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
        -d "$MAPPER_PAYLOAD" >/dev/null || true
      log_success "Created mapper: $mapper_name in scope: $parent_scope_name"
    else
      log_success "Mapper already exists: $mapper_name in scope: $parent_scope_name"
    fi
  fi
done

# Process clients
echo ""
log_step 9 15 "Processing clients"
for client_file in "$BASE_DIR"/clients/*.json; do
  if [ ! -f "$client_file" ]; then continue; fi
  
  CLIENT_JSON=$(load_json "$client_file")
  client_id=$(echo "$CLIENT_JSON" | jq -r '.clientId')
  
  if client_exists "$client_id"; then
    log_success "Client already exists: $client_id"
  else
    curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients" \
      -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
      -d "$CLIENT_JSON" >/dev/null || true
    log_success "Created client: $client_id"
  fi
done

# Process scope assignments
echo ""
log_step 10 15 "Processing scope assignments"
SCOPE_ASSIGNMENTS=$(load_json "$BASE_DIR/scope-assignments.json")

echo "$SCOPE_ASSIGNMENTS" | jq -c '.[]' | while IFS= read -r assignment; do
  client_id=$(echo "$assignment" | jq -r '.clientId')
  CLIENT_IEID=$(get_client_id "$client_id")
  
  if [ -n "$CLIENT_IEID" ]; then
    echo "$assignment" | jq -r '.defaultScopes[]' 2>/dev/null | while IFS= read -r scope_name; do
      SCOPE_ID=$(get_client_scope_id "$scope_name")
      
      if [ -n "$SCOPE_ID" ]; then
        curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients/$CLIENT_IEID/default-client-scopes/$SCOPE_ID" \
          -H "Authorization: Bearer $ADMIN_TOKEN" >/dev/null || true
        log_success "Assigned default scope '$scope_name' to client '$client_id'"
      fi
    done
    
    echo "$assignment" | jq -r '.optionalScopes[]' 2>/dev/null | while IFS= read -r scope_name; do
      SCOPE_ID=$(get_client_scope_id "$scope_name")
      
      if [ -n "$SCOPE_ID" ]; then
        curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients/$CLIENT_IEID/optional-client-scopes/$SCOPE_ID" \
          -H "Authorization: Bearer $ADMIN_TOKEN" >/dev/null || true
        log_success "Assigned optional scope '$scope_name' to client '$client_id'"
      fi
    done
  fi
done

# Process users
echo ""
log_step 11 15 "Processing users"
USERS_JSON=$(load_json "$BASE_DIR/users.json")
USER_COUNT=$(echo "$USERS_JSON" | jq 'length')

# First pass: Create users
i=0
while [ $i -lt $USER_COUNT ]; do
  user=$(echo "$USERS_JSON" | jq -c ".[$i]")
  username=$(echo "$user" | jq -r '.username')
  
  USER_ID=$(get_user_id "$username")
  
  if [ -z "$USER_ID" ]; then
    # Remove manager_username as it will be resolved to manager_id in second pass
    # Keep all other fields including enabled, emailVerified
    USER_PAYLOAD=$(echo "$user" | jq 'del(.credentials) | del(.realmRoles) | del(.groups) | del(.attributes.manager_username)')
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users" \
      -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
      -d "$USER_PAYLOAD")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    RESPONSE_BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "409" ]; then
      log_error "Failed to create user $username: HTTP $HTTP_CODE - $RESPONSE_BODY"
    fi
    
    USER_ID=$(get_user_id "$username")
    
    if [ -n "$USER_ID" ]; then
      # Set password
      PASSWORD=$(echo "$user" | jq -r '.credentials[0].value')
      curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$USER_ID/reset-password" \
        -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
        -d "{\"type\":\"password\",\"value\":\"$PASSWORD\",\"temporary\":false}" >/dev/null || true
      
      log_success "Created user: $username (ID: $USER_ID)"
    fi
  else
    # User exists - update basic info if needed
    # Get current user to preserve existing fields
    CURRENT_USER=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
      "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$USER_ID")
    
    # Extract desired values and update current user object
    UPDATE_PAYLOAD=$(echo "$CURRENT_USER" | jq --argjson new_user "$user" \
      '.firstName = $new_user.firstName | .lastName = $new_user.lastName | .email = $new_user.email | .enabled = $new_user.enabled | .emailVerified = $new_user.emailVerified')
    
    curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$USER_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
      -d "$UPDATE_PAYLOAD" >/dev/null || true
    
    log_success "User already exists: $username (ID: $USER_ID)"
  fi
  
  i=$((i+1))
done

# Second pass: Update user attributes
echo ""
log_step 12 15 "Updating user attributes"
i=0
while [ $i -lt $USER_COUNT ]; do
  user=$(echo "$USERS_JSON" | jq -c ".[$i]")
  username=$(echo "$user" | jq -r '.username')
  
  USER_ID=$(get_user_id "$username")
  
  if [ -n "$USER_ID" ]; then
    # Get current user data
    CURRENT_USER=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
      "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$USER_ID")
    
    # Merge attributes (excluding manager_username which will be set as manager_id)
    NEW_ATTRIBUTES=$(echo "$user" | jq 'del(.attributes.manager_username) | .attributes')
    CURRENT_ATTRIBUTES=$(echo "$CURRENT_USER" | jq '.attributes')
    MERGED_ATTRIBUTES=$(echo "$CURRENT_ATTRIBUTES" | jq --argjson new "$NEW_ATTRIBUTES" '. + $new')
    
    if [ "$MERGED_ATTRIBUTES" != "null" ] && [ "$MERGED_ATTRIBUTES" != "{}" ]; then
      # Build complete user object by merging attributes into current user
      UPDATED_USER=$(echo "$CURRENT_USER" | jq --argjson attrs "$MERGED_ATTRIBUTES" '.attributes = $attrs')
      
      curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$USER_ID" \
        -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
        -d "$UPDATED_USER"
      log_success "Updated attributes for user: $username"
    fi
  fi
  
  i=$((i+1))
done

# Third pass: Update manager relationships
echo ""
log_step 13 15 "Processing user relationships"
echo "$USERS_JSON" | jq -c '.[]' | while IFS= read -r USER_DATA; do
  USERNAME=$(echo "$USER_DATA" | jq -r '.username')
  MANAGER_USERNAME=$(echo "$USER_DATA" | jq -r '.attributes.manager_username // empty')
  
  if [ -n "$MANAGER_USERNAME" ]; then
    USER_ID=$(get_user_id "$USERNAME")
    if [ -n "$USER_ID" ]; then
      update_manager_id "$USER_ID" "$MANAGER_USERNAME"
    fi
  fi
done

# Assign roles to users
echo ""
log_step 14 15 "Assigning roles to users"
i=0
while [ $i -lt $USER_COUNT ]; do
  user=$(echo "$USERS_JSON" | jq -c ".[$i]")
  username=$(echo "$user" | jq -r '.username')
  
  USER_ID=$(get_user_id "$username")
  
  if [ -n "$USER_ID" ]; then
    ROLE_COUNT=$(echo "$user" | jq '.realmRoles | length // 0')
    j=0
    while [ $j -lt $ROLE_COUNT ]; do
      role_name=$(echo "$user" | jq -r ".realmRoles[$j]")
      ROLE_ID=$(get_role_id "$role_name")
      
      if [ -n "$ROLE_ID" ]; then
        curl -s -X POST "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$USER_ID/role-mappings/realm" \
          -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
          -d "[{\"id\":\"$ROLE_ID\",\"name\":\"$role_name\"}]" >/dev/null || true
        log_success "Assigned role '$role_name' to user '$username'"
      fi
      j=$((j+1))
    done
  fi
  i=$((i+1))
done

# Assign groups to users
echo ""
log_step 15 15 "Processing group memberships"
i=0
while [ $i -lt $USER_COUNT ]; do
  user=$(echo "$USERS_JSON" | jq -c ".[$i]")
  username=$(echo "$user" | jq -r '.username')
  
  USER_ID=$(get_user_id "$username")
  
  if [ -n "$USER_ID" ]; then
    GROUP_COUNT=$(echo "$user" | jq '.groups | length // 0')
    j=0
    while [ $j -lt $GROUP_COUNT ]; do
      group_name=$(echo "$user" | jq -r ".groups[$j]")
      GROUP_ID=$(get_group_id "$group_name")
      
      if [ -n "$GROUP_ID" ]; then
        curl -s -X PUT "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$USER_ID/groups/$GROUP_ID" \
          -H "Authorization: Bearer $ADMIN_TOKEN" >/dev/null || true
        log_success "Added user '$username' to group '$group_name'"
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
echo "  - User Profile: Configured with 7 custom attributes"
echo "  - Roles: SYS_ADMIN, USER, TEAM_LEADER"
echo "  - Groups: teams/team1, teams/team2"
echo "  - Users: admin, admin2, user1, user2, user3, user4, user5"
echo "  - Clients: config-control-service, admin-dashboard"
echo "  - Client Scopes: groups, manager_id, audience"
echo ""
echo "Keycloak Admin Console: $KEYCLOAK_API_URL/admin"
echo "Realm: $REALM_NAME"
echo "======================================================================"