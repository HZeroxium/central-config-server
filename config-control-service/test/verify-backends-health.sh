#!/bin/bash

# Backend Health Verification Script
# Checks health of all observability backends

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost"
CONFIG_CONTROL_URL="${BASE_URL}:8081"
TEMPO_URL="${BASE_URL}:3200"
LOKI_URL="${BASE_URL}:3100"
MIMIR_URL="${BASE_URL}:9009"
GRAFANA_URL="${BASE_URL}:3000"
ALLOY_URL="${BASE_URL}:12345"

# Utility functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

# Check service health
check_service() {
    local service_name="$1"
    local url="$2"
    local health_path="$3"
    
    log_info "Checking $service_name health..."
    
    if curl -sf "${url}${health_path}" > /dev/null 2>&1; then
        log_success "$service_name is healthy"
        return 0
    else
        log_error "$service_name health check failed"
        return 1
    fi
}

# Check service with detailed response
check_service_detailed() {
    local service_name="$1"
    local url="$2"
    local health_path="$3"
    
    log_info "Checking $service_name health (detailed)..."
    
    local response
    response=$(curl -sf "${url}${health_path}" 2>/dev/null || echo "")
    
    if [ -n "$response" ]; then
        log_success "$service_name is healthy"
        echo "Response: $response"
        return 0
    else
        log_error "$service_name health check failed"
        return 1
    fi
}

# Check Grafana datasources
check_grafana_datasources() {
    log_info "Checking Grafana datasources..."
    
    # Wait for Grafana to be ready
    local attempt=1
    local max_attempts=30
    
    while [ $attempt -le $max_attempts ]; do
        if curl -sf "${GRAFANA_URL}/api/health" > /dev/null 2>&1; then
            break
        fi
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    if [ $attempt -gt $max_attempts ]; then
        log_error "Grafana failed to become ready"
        return 1
    fi
    
    # Get datasources
    local datasources
    datasources=$(curl -sf "${GRAFANA_URL}/api/datasources" -H "Authorization: Basic YWRtaW46YWRtaW4=" 2>/dev/null || echo "[]")
    
    if [ "$datasources" = "[]" ]; then
        log_error "No datasources found in Grafana"
        return 1
    fi
    
    # Check for required datasources
    local required_ds=("Tempo" "Loki" "Mimir" "Prometheus")
    local found_ds=0
    
    for ds in "${required_ds[@]}"; do
        if echo "$datasources" | jq -e ".[] | select(.name == \"$ds\")" > /dev/null; then
            log_success "Datasource $ds found"
            ((found_ds++))
        else
            log_warning "Datasource $ds not found"
        fi
    done
    
    if [ $found_ds -gt 0 ]; then
        log_success "Found $found_ds datasources in Grafana"
        return 0
    else
        log_error "No required datasources found"
        return 1
    fi
}

# Main execution
main() {
    log_info "Starting Backend Health Verification"
    log_info "===================================="
    
    local failed_checks=0
    
    # Check individual services
    check_service "Tempo" "$TEMPO_URL" "/ready" || ((failed_checks++))
    check_service "Loki" "$LOKI_URL" "/ready" || ((failed_checks++))
    check_service "Mimir" "$MIMIR_URL" "/ready" || ((failed_checks++))
    check_service "Alloy" "$ALLOY_URL" "/-/healthy" || ((failed_checks++))
    check_service_detailed "Config Control Service" "$CONFIG_CONTROL_URL" "/actuator/health" || ((failed_checks++))
    
    # Check Grafana datasources
    check_grafana_datasources || ((failed_checks++))
    
    # Summary
    log_info "===================================="
    if [ $failed_checks -eq 0 ]; then
        log_success "All backend health checks passed!"
        exit 0
    else
        log_error "$failed_checks health check(s) failed"
        exit 1
    fi
}

# Run main function
main "$@"
