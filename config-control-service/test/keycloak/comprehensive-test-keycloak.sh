#!/usr/bin/env bash
set -euo pipefail

# Config (override via env if needed)
API_URL=${KEYCLOAK_API_URL:-http://localhost:8080}
HEALTH_URL=${KEYCLOAK_HEALTH_URL:-http://localhost:9000}
ADMIN_USER=${KEYCLOAK_ADMIN:-admin}
ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD:-admin}
REALM=${KEYCLOAK_REALM:-config-control}

log() { printf "%s | %s\n" "$(date '+%Y-%m-%d %H:%M:%S.%3N')" "$*"; }

fail() { log "ERROR: $*" >&2; exit 1; }

require_cmds() {
  for c in curl jq; do command -v "$c" >/dev/null || fail "Missing dependency: $c"; done
}

wait_health() {
  log "Waiting for Keycloak health at ${HEALTH_URL}/health/ready"
  for i in {1..120}; do
    if curl -fsS "${HEALTH_URL}/health/ready" >/dev/null; then
      log "Keycloak is ready"; return 0
    fi
    sleep 2
  done
  fail "Keycloak did not become ready in time"
}

get_admin_token() {
  log "Getting admin token from ${API_URL}"
  local resp
  resp=$(curl -fsS -X POST "${API_URL}/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=${ADMIN_USER}" \
    -d "password=${ADMIN_PASSWORD}" \
    -d "grant_type=password" \
    -d "client_id=admin-cli")
  ADMIN_TOKEN=$(echo "$resp" | jq -r '.access_token // empty')
  [[ -n "${ADMIN_TOKEN}" ]] || fail "Failed to obtain admin token"
}

assert_realm_exists() {
  log "Verifying realm ${REALM} exists"
  local realms
  realms=$(curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" "${API_URL}/admin/realms" | jq -r '.[].realm')
  echo "$realms" | grep -q "^${REALM}$" || fail "Realm ${REALM} not found"
}

user_id_by_username() {
  local username=$1
  curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${API_URL}/admin/realms/${REALM}/users?username=${username}" | jq -r '.[0].id // empty'
}

assert_user_exists() {
  local username=$1
  log "Checking user ${username}"
  local uid
  uid=$(user_id_by_username "$username")
  [[ -n "$uid" ]] || fail "User ${username} not found"
}

assert_user_has_role() {
  local username=$1 role=$2
  local uid
  uid=$(user_id_by_username "$username")
  [[ -n "$uid" ]] || fail "User ${username} not found"
  local roles
  roles=$(curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${API_URL}/admin/realms/${REALM}/users/${uid}/role-mappings/realm" | jq -r '.[].name')
  echo "$roles" | grep -q "^${role}$" || fail "User ${username} missing role ${role}"
}

assert_group_exists() {
  local group=$1
  local gid
  gid=$(curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${API_URL}/admin/realms/${REALM}/groups?search=${group}" | jq -r '.[0].id // empty')
  [[ -n "$gid" ]] || fail "Group ${group} not found"
}

assert_user_in_group() {
  local username=$1 group=$2
  local uid
  uid=$(user_id_by_username "$username")
  [[ -n "$uid" ]] || fail "User ${username} not found"
  local groups
  groups=$(curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${API_URL}/admin/realms/${REALM}/users/${uid}/groups" | jq -r '.[].name')
  echo "$groups" | grep -q "^${group}$" || fail "User ${username} not in group ${group}"
}

assert_client_exists() {
  local clientId=$1
  local cid
  cid=$(curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${API_URL}/admin/realms/${REALM}/clients?clientId=${clientId}" | jq -r '.[0].id // empty')
  [[ -n "$cid" ]] || fail "Client ${clientId} not found"
}

token_password_grant() {
  local username=$1 password=$2 clientId=$3
  curl -fsS -X POST "${API_URL}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" -d "client_id=${clientId}" -d "username=${username}" -d "password=${password}" | jq -r '.access_token // empty'
}

ensure_role_exists() {
  local role=$1 desc=$2
  local name
  name=$(curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${API_URL}/admin/realms/${REALM}/roles/${role}" | jq -r '.name // empty') || true
  if [[ -z "$name" ]]; then
    log "Ensuring role ${role}"
    curl -fsS -X POST "${API_URL}/admin/realms/${REALM}/roles" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" -H "Content-Type: application/json" \
      -d '{"name":"'"${role}"'","description":"'"${desc}"'"}' >/dev/null
  fi
}

ensure_group_exists() {
  local g=$1
  local gid
  gid=$(curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${API_URL}/admin/realms/${REALM}/groups?search=${g}" | jq -r '.[0].id // empty') || true
  if [[ -z "$gid" ]]; then
    log "Ensuring group ${g}"
    curl -fsS -X POST "${API_URL}/admin/realms/${REALM}/groups" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" -H "Content-Type: application/json" \
      -d '{"name":"'"${g}"'"}' >/dev/null
  fi
}

ensure_client_exists() {
  local clientId=$1 payload=$2
  local cid
  cid=$(curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${API_URL}/admin/realms/${REALM}/clients?clientId=${clientId}" | jq -r '.[0].id // empty') || true
  if [[ -z "$cid" ]]; then
    log "Ensuring client ${clientId}"
    curl -fsS -X POST "${API_URL}/admin/realms/${REALM}/clients" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" -H "Content-Type: application/json" \
      -d "${payload}" >/dev/null
  fi
}

ensure_user_role() {
  local username=$1 role=$2
  local uid rid
  uid=$(user_id_by_username "$username")
  rid=$(curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${API_URL}/admin/realms/${REALM}/roles/${role}" | jq -r '.id')
  [[ -n "$uid" && -n "$rid" ]] || return 0
  curl -fsS -X POST "${API_URL}/admin/realms/${REALM}/users/${uid}/role-mappings/realm" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" -H "Content-Type: application/json" \
    -d "[{\"id\":\"$rid\",\"name\":\"$role\"}]" >/dev/null || true
}

ensure_user_in_group() {
  local username=$1 group=$2
  local uid gid
  uid=$(user_id_by_username "$username")
  gid=$(curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${API_URL}/admin/realms/${REALM}/groups?search=${group}" | jq -r '.[0].id // empty')
  [[ -n "$uid" && -n "$gid" ]] || return 0
  curl -fsS -X PUT "${API_URL}/admin/realms/${REALM}/users/${uid}/groups/${gid}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" >/dev/null || true
}

main() {
  require_cmds
  wait_health
  get_admin_token
  assert_realm_exists

  # Proactively ensure roles, groups, clients, then validate
  ensure_role_exists SYS_ADMIN "System administrator with full access"
  ensure_role_exists USER "Regular user with basic access"
  ensure_group_exists team_core
  ensure_group_exists team_analytics
  ensure_group_exists team_infrastructure
  ensure_client_exists config-control-service '{"clientId":"config-control-service","name":"Config Control Service","enabled":true,"protocol":"openid-connect","publicClient":false,"serviceAccountsEnabled":true,"secret":"config-control-service-secret"}'
  ensure_client_exists admin-dashboard '{"clientId":"admin-dashboard","name":"Admin Dashboard","enabled":true,"protocol":"openid-connect","publicClient":true,"standardFlowEnabled":true,"directAccessGrantsEnabled":true,"attributes":{"pkce.code.challenge.method":"S256"},"redirectUris":["http://localhost:3000/*","http://localhost:3001/*"],"webOrigins":["http://localhost:3000","http://localhost:3001"]}'

  # Users
  assert_user_exists admin
  assert_user_exists user1
  assert_user_exists user2
  assert_user_exists user3

  # Roles
  ensure_user_role admin SYS_ADMIN
  ensure_user_role user1 USER
  ensure_user_role user2 USER
  ensure_user_role user3 USER
  assert_user_has_role admin SYS_ADMIN
  assert_user_has_role user1 USER
  assert_user_has_role user2 USER
  assert_user_has_role user3 USER

  # Groups
  assert_group_exists team_core
  assert_group_exists team_analytics
  assert_group_exists team_infrastructure
  ensure_user_in_group user1 team_core
  ensure_user_in_group user2 team_analytics
  ensure_user_in_group user3 team_infrastructure
  assert_user_in_group user1 team_core
  assert_user_in_group user2 team_analytics
  assert_user_in_group user3 team_infrastructure

  # Clients
  assert_client_exists config-control-service
  assert_client_exists admin-dashboard

  # Important endpoints
  log "Checking OIDC discovery"
  curl -fsS "${API_URL}/realms/${REALM}/.well-known/openid-configuration" | jq -e '.issuer' >/dev/null || fail "OIDC discovery failed"

  log "Testing password grant for admin-dashboard (user1)"
  tok=$(token_password_grant user1 user123 admin-dashboard)
  [[ -n "$tok" ]] || fail "Password grant failed for admin-dashboard"

  log "All checks passed"
}

main "$@"


