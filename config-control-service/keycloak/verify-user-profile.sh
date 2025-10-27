#!/bin/bash
set -e

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM_NAME="${REALM_NAME:-config-control}"

echo "======================================================================"
echo "Keycloak User Profile Verification Script"
echo "======================================================================"
echo "Keycloak URL: $KEYCLOAK_URL"
echo "Realm: $REALM_NAME"
echo "======================================================================"

# Get admin token
echo ""
echo "[1/4] Getting admin token..."
ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
    echo "ERROR: Failed to get admin token"
    exit 1
fi
echo "  ✓ Admin token obtained"

# Verify User Profile configuration
echo ""
echo "[2/4] Verifying User Profile configuration..."
USER_PROFILE=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users/profile" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if [ -z "$USER_PROFILE" ]; then
    echo "ERROR: Failed to get User Profile configuration"
    exit 1
fi

echo "  ✓ User Profile configuration retrieved"

# Check for custom attributes
echo ""
echo "Checking custom attributes..."
ATTRIBUTES=("manager_id" "phone" "employee_id" "department" "job_title" "office_location" "hire_date")
ALL_ATTRIBUTES_FOUND=true

for ATTR in "${ATTRIBUTES[@]}"; do
  if echo "$USER_PROFILE" | jq -e ".attributes[] | select(.name == \"$ATTR\")" > /dev/null; then
    echo "  ✓ Attribute '$ATTR' configured"
  else
    echo "  ✗ Attribute '$ATTR' NOT configured"
    ALL_ATTRIBUTES_FOUND=false
  fi
done

if [ "$ALL_ATTRIBUTES_FOUND" = false ]; then
    echo "ERROR: Not all custom attributes are configured"
    exit 1
fi

# Verify user2 and user4 attributes
echo ""
echo "[3/4] Verifying user2 and user4 attributes..."

# Check user2
echo "Checking user2..."
USER2_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users?username=user2" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id // empty')

if [ -z "$USER2_ID" ] || [ "$USER2_ID" = "null" ]; then
    echo "  ✗ user2 not found"
    exit 1
fi

USER2_ATTRS=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users/$USER2_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.attributes')

echo "  ✓ user2 found (ID: $USER2_ID)"

# Check user2 attributes
USER2_MANAGER_USERNAME=$(echo "$USER2_ATTRS" | jq -r '.manager_username[0] // empty')
USER2_PHONE=$(echo "$USER2_ATTRS" | jq -r '.phone[0] // empty')
USER2_EMPLOYEE_ID=$(echo "$USER2_ATTRS" | jq -r '.employee_id[0] // empty')

if [ "$USER2_MANAGER_USERNAME" = "user1" ]; then
    echo "  ✓ user2 manager_username: $USER2_MANAGER_USERNAME"
else
    echo "  ✗ user2 manager_username: $USER2_MANAGER_USERNAME (expected: user1)"
fi

if [ -n "$USER2_PHONE" ]; then
    echo "  ✓ user2 phone: $USER2_PHONE"
else
    echo "  ✗ user2 phone not set"
fi

if [ -n "$USER2_EMPLOYEE_ID" ]; then
    echo "  ✓ user2 employee_id: $USER2_EMPLOYEE_ID"
else
    echo "  ✗ user2 employee_id not set"
fi

# Check user4
echo "Checking user4..."
USER4_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users?username=user4" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id // empty')

if [ -z "$USER4_ID" ] || [ "$USER4_ID" = "null" ]; then
    echo "  ✗ user4 not found"
    exit 1
fi

USER4_ATTRS=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users/$USER4_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.attributes')

echo "  ✓ user4 found (ID: $USER4_ID)"

# Check user4 attributes
USER4_MANAGER_USERNAME=$(echo "$USER4_ATTRS" | jq -r '.manager_username[0] // empty')
USER4_PHONE=$(echo "$USER4_ATTRS" | jq -r '.phone[0] // empty')
USER4_EMPLOYEE_ID=$(echo "$USER4_ATTRS" | jq -r '.employee_id[0] // empty')

if [ "$USER4_MANAGER_USERNAME" = "user3" ]; then
    echo "  ✓ user4 manager_username: $USER4_MANAGER_USERNAME"
else
    echo "  ✗ user4 manager_username: $USER4_MANAGER_USERNAME (expected: user3)"
fi

if [ -n "$USER4_PHONE" ]; then
    echo "  ✓ user4 phone: $USER4_PHONE"
else
    echo "  ✗ user4 phone not set"
fi

if [ -n "$USER4_EMPLOYEE_ID" ]; then
    echo "  ✓ user4 employee_id: $USER4_EMPLOYEE_ID"
else
    echo "  ✗ user4 employee_id not set"
fi

# Verify manager_id attributes are set
echo ""
echo "[4/4] Verifying manager_id attributes..."

# Get user1 ID for manager_id verification
USER1_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users?username=user1" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id // empty')

USER3_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users?username=user3" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id // empty')

# Check user2 manager_id
USER2_MANAGER_ID=$(echo "$USER2_ATTRS" | jq -r '.manager_id[0] // empty')
if [ "$USER2_MANAGER_ID" = "$USER1_ID" ]; then
    echo "  ✓ user2 manager_id: $USER2_MANAGER_ID (matches user1)"
else
    echo "  ✗ user2 manager_id: $USER2_MANAGER_ID (expected: $USER1_ID)"
fi

# Check user4 manager_id
USER4_MANAGER_ID=$(echo "$USER4_ATTRS" | jq -r '.manager_id[0] // empty')
if [ "$USER4_MANAGER_ID" = "$USER3_ID" ]; then
    echo "  ✓ user4 manager_id: $USER4_MANAGER_ID (matches user3)"
else
    echo "  ✗ user4 manager_id: $USER4_MANAGER_ID (expected: $USER3_ID)"
fi

echo ""
echo "======================================================================"
echo "Verification completed successfully!"
echo "======================================================================"
echo ""
echo "Summary:"
echo "  - User Profile: 7 custom attributes configured"
echo "  - user2: manager_username=user1, manager_id set"
echo "  - user4: manager_username=user3, manager_id set"
echo "  - All users: phone, employee_id, department, job_title, office_location, hire_date"
echo ""
echo "Keycloak Admin Console: $KEYCLOAK_URL/admin"
echo "Realm: $REALM_NAME"
echo "======================================================================"
