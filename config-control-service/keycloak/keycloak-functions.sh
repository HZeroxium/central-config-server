#!/bin/sh

# Keycloak Functions Library
# Reusable functions for Keycloak Admin API operations

# Configuration
KEYCLOAK_API_URL="${KEYCLOAK_API_URL:-http://keycloak:8080}"
KEYCLOAK_HEALTH_URL="${KEYCLOAK_HEALTH_URL:-http://keycloak:9000}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
REALM_NAME="config-control"

# Global admin token (set by get_admin_token)
ADMIN_TOKEN=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
  echo -e "${BLUE}$1${NC}"
}

log_success() {
  echo -e "${GREEN}  ✓ $1${NC}"
}

log_warning() {
  echo -e "${YELLOW}  ⚠ $1${NC}"
}

log_error() {
  echo -e "${RED}  ✗ ERROR: $1${NC}" >&2
}

log_step() {
  local step=$1
  local total=$2
  local message=$3
  echo ""
  echo "[$step/$total] $message..."
}

# Wait for Keycloak to be ready
wait_for_keycloak() {
  local max_attempts=60
  local attempt=1
  
  log_info "Waiting for Keycloak to be ready at $KEYCLOAK_HEALTH_URL..."
  
  while [ $attempt -le $max_attempts ]; do
    if curl -f -s "$KEYCLOAK_HEALTH_URL/health/ready" > /dev/null 2>&1; then
      log_success "Keycloak is ready"
      return 0
    fi
    
    echo "  Attempt $attempt/$max_attempts - Keycloak not ready yet..."
    sleep 5
    attempt=$((attempt + 1))
  done
  
  log_error "Keycloak did not become ready in time"
  return 1
}

# Get admin access token
get_admin_token() {
  log_info "Getting admin access token..."
  
  local token_url="$KEYCLOAK_API_URL/realms/master/protocol/openid-connect/token"
  local response=$(curl -s -X POST "$token_url" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=$ADMIN_USER" \
    -d "password=$ADMIN_PASSWORD" \
    -d "grant_type=password" \
    -d "client_id=admin-cli")
  
  ADMIN_TOKEN=$(echo "$response" | jq -r 'select(type=="object") | .access_token // empty') || true
  export ADMIN_TOKEN
  
  if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
    log_error "Failed to get admin token"
    echo "Response: $response" >&2
    exit 1
  fi
  
  log_success "Admin token obtained"
}

# Get user ID by username
get_user_id() {
  local username=$1
  curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users?username=$username" | \
    jq -r '.[0].id // empty' || true
}

# Check if user exists
user_exists() {
  local username=$1
  local user_id=$(get_user_id "$username")
  [ -n "$user_id" ] && [ "$user_id" != "null" ]
}

# Get role ID by name
get_role_id() {
  local role_name=$1
  curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/roles/$role_name" | \
    jq -r '.id // empty' || true
}

# Check if role exists
role_exists() {
  local role_name=$1
  local role_id=$(get_role_id "$role_name")
  [ -n "$role_id" ] && [ "$role_id" != "null" ]
}

# Get group ID by name
get_group_id() {
  local group_name=$1
  local result=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/groups?search=$group_name")
  
  # First try to find in top-level
  local id=$(echo "$result" | jq -r ".[] | select(.name==\"$group_name\") | .id // empty")
  if [ -n "$id" ]; then
    echo "$id"
    return 0
  fi
  
  # Then check in subGroups recursively
  id=$(echo "$result" | jq -r ".[] | .subGroups[]? | select(.name==\"$group_name\") | .id // empty")
  if [ -n "$id" ]; then
    echo "$id"
  fi
}

# Check if group exists
group_exists() {
  local group_name=$1
  local group_id=$(get_group_id "$group_name")
  [ -n "$group_id" ] && [ "$group_id" != "null" ]
}

# Get client ID by clientId
get_client_id() {
  local client_id=$1
  curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/clients?clientId=$client_id" | \
    jq -r '.[0].id // empty' || true
}

# Check if client exists
client_exists() {
  local client_id=$1
  local client_uuid=$(get_client_id "$client_id")
  [ -n "$client_uuid" ] && [ "$client_uuid" != "null" ]
}

# Get client scope ID by name
get_client_scope_id() {
  local scope_name=$1
  curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/client-scopes?search=$scope_name" | \
    jq -r ".[] | select(.name==\"$scope_name\") | .id // empty" || true
}

# Check if client scope exists
client_scope_exists() {
  local scope_name=$1
  local scope_id=$(get_client_scope_id "$scope_name")
  [ -n "$scope_id" ] && [ "$scope_id" != "null" ]
}

# Create or update realm role
create_or_update_role() {
  local role_name=$1
  local role_description=$2
  
  if role_exists "$role_name"; then
    log_success "Role '$role_name' already exists"
    return 0
  fi
  
  log_info "Creating role: $role_name"
  local role_json="{\"name\":\"$role_name\",\"description\":\"$role_description\",\"composite\":false,\"clientRole\":false}"
  
  local response=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$role_json")
  
  if [ "$response" = "201" ]; then
    log_success "Created role: $role_name"
  else
    log_warning "Role '$role_name' creation returned HTTP $response"
  fi
}

# Assign role to user
assign_role_to_user() {
  local username=$1
  local role_name=$2
  
  local user_id=$(get_user_id "$username")
  if [ -z "$user_id" ]; then
    log_warning "User '$username' not found, skipping role assignment"
    return 1
  fi
  
  local role_id=$(get_role_id "$role_name")
  if [ -z "$role_id" ]; then
    log_warning "Role '$role_name' not found, skipping role assignment"
    return 1
  fi
  
  curl -s -o /dev/null -X POST \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$user_id/role-mappings/realm" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "[{\"id\":\"$role_id\",\"name\":\"$role_name\"}]" || true
  
  log_success "Assigned role '$role_name' to user '$username'"
}

# Add user to group
add_user_to_group() {
  local username=$1
  local group_name=$2
  
  local user_id=$(get_user_id "$username")
  if [ -z "$user_id" ]; then
    log_warning "User '$username' not found, skipping group membership"
    return 1
  fi
  
  local group_id=$(get_group_id "$group_name")
  if [ -z "$group_id" ]; then
    log_warning "Group '$group_name' not found, skipping group membership"
    return 1
  fi
  
  curl -s -o /dev/null -X PUT \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$user_id/groups/$group_id" \
    -H "Authorization: Bearer $ADMIN_TOKEN" || true
  
  log_success "Added user '$username' to group '$group_name'"
}

# Get manager ID for a user by resolving username
get_manager_id() {
  local manager_username=$1
  local manager_id=$(get_user_id "$manager_username")
  echo "$manager_id"
}

# Update manager_id attribute for a user
update_manager_id_attribute() {
  local username=$1
  local manager_username=$2
  
  local user_id=$(get_user_id "$username")
  if [ -z "$user_id" ]; then
    log_warning "User '$username' not found, skipping manager_id update"
    return 1
  fi
  
  local manager_id=$(get_manager_id "$manager_username")
  if [ -z "$manager_id" ]; then
    log_warning "Manager user '$manager_username' not found, skipping manager_id update for user '$username'"
    return 1
  fi
  
  log_info "Setting manager_id=$manager_id for user $username"
  
  curl -s -o /dev/null -X PUT \
    "$KEYCLOAK_API_URL/admin/realms/$REALM_NAME/users/$user_id" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"attributes\": {\"manager_id\": [\"$manager_id\"]}}"
  
  log_success "Updated manager_id for user '$username'"
}

# Load JSON file
load_json() {
  local filepath=$1
  if [ ! -f "$filepath" ]; then
    log_error "JSON file not found: $filepath"
    return 1
  fi
  cat "$filepath"
}
