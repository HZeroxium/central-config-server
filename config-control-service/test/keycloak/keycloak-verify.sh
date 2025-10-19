#!/usr/bin/env bash
set -euo pipefail

# Config
API_URL=${KEYCLOAK_API_URL:-http://localhost:8080}
HEALTH_URL=${KEYCLOAK_HEALTH_URL:-http://localhost:9000}
ADMIN_USER=${KEYCLOAK_ADMIN:-admin}
ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD:-admin}
REALM=${KEYCLOAK_REALM:-config-control}

echo "[verify] Health check on ${HEALTH_URL}"
for i in {1..60}; do
  if curl -fsS "${HEALTH_URL}/health/ready" >/dev/null; then
    echo "[verify] Keycloak ready"
    break
  fi
  echo "[verify] waiting..."; sleep 2
done

echo "[verify] Getting admin token"
ADMIN_TOKEN=$(curl -fsS -X POST "${API_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')
if [[ -z "${ADMIN_TOKEN}" || "${ADMIN_TOKEN}" == "null" ]]; then
  echo "[verify] Failed to get admin token" >&2
  exit 2
fi

echo "[verify] Listing realms"
REALMS=$(curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" "${API_URL}/admin/realms" | jq -r '.[].realm')
echo "[verify] realms: ${REALMS}"
echo "${REALMS}" | grep -q "^${REALM}$" || { echo "[verify] realm ${REALM} not found" >&2; exit 3; }

echo "[verify] Checking users in realm ${REALM}"
curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${API_URL}/admin/realms/${REALM}/users?username=admin" | jq -e '.[0].username == "admin"' >/dev/null || {
  echo "[verify] admin user not found in realm ${REALM}" >&2; exit 4; }

echo "[verify] Checking clients"
curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${API_URL}/admin/realms/${REALM}/clients?clientId=config-control-service" | jq -e '.[0].clientId == "config-control-service"' >/dev/null || {
  echo "[verify] client config-control-service missing" >&2; exit 5; }

curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${API_URL}/admin/realms/${REALM}/clients?clientId=admin-dashboard" | jq -e '.[0].clientId == "admin-dashboard"' >/dev/null || {
  echo "[verify] client admin-dashboard missing" >&2; exit 6; }

echo "[verify] OK — realm, users, and clients verified"

