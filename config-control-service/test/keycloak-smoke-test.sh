#!/bin/bash

# Keycloak Smoke Test Script
# This script tests the basic functionality of the Keycloak integration

set -e

# Configuration
KEYCLOAK_URL="http://localhost:8080"
SERVICE_URL="http://localhost:8081"
REALM="config-control"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Test functions
test_keycloak_health() {
    log_info "Testing Keycloak health..."
    
    if curl -f -s "http://localhost:9000/health/ready" > /dev/null; then
        log_info "✓ Keycloak is healthy"
    else
        log_error "✗ Keycloak is not responding"
        exit 1
    fi
}

test_service_health() {
    log_info "Testing Config Control Service health..."
    
    if curl -f -s "$SERVICE_URL/actuator/health" > /dev/null; then
        log_info "✓ Service is healthy"
    else
        log_error "✗ Service is not responding"
        exit 1
    fi
}

test_realm_configuration() {
    log_info "Testing realm configuration..."
    
    if curl -f -s "$KEYCLOAK_URL/realms/$REALM" > /dev/null; then
        log_info "✓ Realm is accessible"
    else
        log_error "✗ Realm is not accessible"
        exit 1
    fi
}

get_token() {
    local username=$1
    local password=$2
    local client_id=${3:-"admin-dashboard"}
    
    log_info "Getting token for user: $username"
    
    local response=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
        -d "client_id=$client_id" \
        -d "grant_type=password" \
        -d "username=$username" \
        -d "password=$password")
    
    local token=$(echo "$response" | jq -r '.access_token // empty')
    
    if [ -n "$token" ] && [ "$token" != "null" ]; then
        log_info "✓ Token obtained successfully"
        echo "$token"
    else
        log_error "✗ Failed to get token"
        log_error "Response: $response"
        exit 1
    fi
}

test_user_info() {
    local token=$1
    
    log_info "Testing user info endpoint..."
    
    local response=$(curl -s -H "Authorization: Bearer $token" \
        "$SERVICE_URL/api/users/me")
    
    local userId=$(echo "$response" | jq -r '.userId // empty')
    
    if [ -n "$userId" ] && [ "$userId" != "null" ]; then
        log_info "✓ User info retrieved successfully"
        log_info "User ID: $userId"
    else
        log_error "✗ Failed to get user info"
        log_error "Response: $response"
        exit 1
    fi
}

test_public_endpoint() {
    log_info "Testing public endpoint (application services)..."
    
    local response=$(curl -s "$SERVICE_URL/api/application-services")
    
    if echo "$response" | jq -e '.content' > /dev/null 2>&1; then
        log_info "✓ Public endpoint accessible"
    else
        log_error "✗ Public endpoint not accessible"
        log_error "Response: $response"
        exit 1
    fi
}

test_protected_endpoint() {
    local token=$1
    
    log_info "Testing protected endpoint without token..."
    
    local response=$(curl -s -w "%{http_code}" -o /dev/null "$SERVICE_URL/api/users/me")
    
    if [ "$response" = "401" ]; then
        log_info "✓ Protected endpoint correctly returns 401 without token"
    else
        log_error "✗ Protected endpoint should return 401 without token, got: $response"
        exit 1
    fi
}

create_test_service() {
    local token=$1
    
    log_info "Creating test application service..."
    
    local response=$(curl -s -X POST "$SERVICE_URL/api/application-services" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d '{
            "id": "test-service",
            "displayName": "Test Service",
            "ownerTeamId": "team_core",
            "environments": ["dev", "prod"],
            "tags": ["test", "smoke"]
        }')
    
    local serviceId=$(echo "$response" | jq -r '.id // empty')
    
    if [ -n "$serviceId" ] && [ "$serviceId" != "null" ]; then
        log_info "✓ Test service created successfully"
        log_info "Service ID: $serviceId"
        echo "$serviceId"
    else
        log_error "✗ Failed to create test service"
        log_error "Response: $response"
        exit 1
    fi
}

test_approval_workflow() {
    local token=$1
    local serviceId=$2
    
    log_info "Testing approval workflow..."
    
    # Create approval request
    local request_response=$(curl -s -X POST "$SERVICE_URL/api/approval-requests" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d '{
            "serviceId": "'$serviceId'",
            "targetTeamId": "team_analytics",
            "note": "Test approval request"
        }')
    
    local requestId=$(echo "$request_response" | jq -r '.id // empty')
    
    if [ -n "$requestId" ] && [ "$requestId" != "null" ]; then
        log_info "✓ Approval request created successfully"
        log_info "Request ID: $requestId"
    else
        log_error "✗ Failed to create approval request"
        log_error "Response: $request_response"
        exit 1
    fi
    
    # Get admin token for approval
    local admin_token=$(get_token "admin@example.com" "admin123")
    
    # Submit approval decision
    local decision_response=$(curl -s -X POST "$SERVICE_URL/api/approval-requests/$requestId/decisions" \
        -H "Authorization: Bearer $admin_token" \
        -H "Content-Type: application/json" \
        -d '{
            "decision": "APPROVE",
            "gate": "SYS_ADMIN",
            "note": "Approved for smoke test"
        }')
    
    if echo "$decision_response" | jq -e '.id' > /dev/null 2>&1; then
        log_info "✓ Approval decision submitted successfully"
    else
        log_error "✗ Failed to submit approval decision"
        log_error "Response: $decision_response"
        exit 1
    fi
}

test_service_sharing() {
    local token=$1
    local serviceId=$2
    
    log_info "Testing service sharing..."
    
    local share_response=$(curl -s -X POST "$SERVICE_URL/api/service-shares" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d '{
            "serviceId": "'$serviceId'",
            "grantToType": "TEAM",
            "grantToId": "team_analytics",
            "permissions": ["VIEW_INSTANCE"],
            "environments": ["dev"]
        }')
    
    local shareId=$(echo "$share_response" | jq -r '.id // empty')
    
    if [ -n "$shareId" ] && [ "$shareId" != "null" ]; then
        log_info "✓ Service share created successfully"
        log_info "Share ID: $shareId"
    else
        log_error "✗ Failed to create service share"
        log_error "Response: $share_response"
        exit 1
    fi
}

test_service_instances() {
    local token=$1
    
    log_info "Testing service instances with team filtering..."
    
    local response=$(curl -s -H "Authorization: Bearer $token" \
        "$SERVICE_URL/api/service-instances")
    
    if echo "$response" | jq -e '.content' > /dev/null 2>&1; then
        log_info "✓ Service instances endpoint accessible with team filtering"
    else
        log_error "✗ Service instances endpoint not accessible"
        log_error "Response: $response"
        exit 1
    fi
}

test_drift_events() {
    local token=$1
    
    log_info "Testing drift events with team filtering..."
    
    local response=$(curl -s -H "Authorization: Bearer $token" \
        "$SERVICE_URL/api/drift-events")
    
    if echo "$response" | jq -e '.content' > /dev/null 2>&1; then
        log_info "✓ Drift events endpoint accessible with team filtering"
    else
        log_error "✗ Drift events endpoint not accessible"
        log_error "Response: $response"
        exit 1
    fi
}

cleanup_test_data() {
    local token=$1
    local serviceId=$2
    
    log_info "Cleaning up test data..."
    
    # Delete test service (admin only)
    local admin_token=$(get_token "admin@example.com" "admin123")
    
    local delete_response=$(curl -s -X DELETE "$SERVICE_URL/api/application-services/$serviceId" \
        -H "Authorization: Bearer $admin_token")
    
    log_info "✓ Test data cleaned up"
}

# Main test execution
main() {
    log_info "Starting Keycloak smoke tests..."
    
    # Basic health checks
    test_keycloak_health
    test_service_health
    test_realm_configuration
    
    # Test public endpoints
    test_public_endpoint
    
    # Test authentication
    local user_token=$(get_token "user1@example.com" "user123")
    test_user_info "$user_token"
    test_protected_endpoint "$user_token"
    
    # Test application service creation
    local service_id=$(create_test_service "$user_token")
    
    # Test approval workflow
    test_approval_workflow "$user_token" "$service_id"
    
    # Test service sharing
    test_service_sharing "$user_token" "$service_id"
    
    # Test team-based filtering
    test_service_instances "$user_token"
    test_drift_events "$user_token"
    
    # Cleanup
    cleanup_test_data "$user_token" "$service_id"
    
    log_info "All smoke tests passed! ✓"
}

# Check dependencies
check_dependencies() {
    if ! command -v curl &> /dev/null; then
        log_error "curl is required but not installed"
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        log_error "jq is required but not installed"
        exit 1
    fi
}

# Run main function
check_dependencies
main
