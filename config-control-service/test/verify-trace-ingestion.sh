#!/bin/bash

# Trace Ingestion Verification Script
# Tests trace generation and ingestion into Tempo

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

# Generate test trace
generate_test_trace() {
    log_info "Generating test trace..."
    
    # Send test heartbeat request
    local response
    response=$(curl -sf -X POST "${CONFIG_CONTROL_URL}/api/heartbeat" \
        -H "Content-Type: application/json" \
        -H "X-Test-Request: true" \
        -d '{
            "serviceName": "test-trace-service",
            "instanceId": "test-trace-instance-'$(date +%s)'",
            "configHash": "test-hash-'$(date +%s)'",
            "host": "test-host",
            "port": 8080,
            "environment": "test",
            "version": "1.0.0",
            "metadata": {
                "test": "true",
                "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
            }
        }' \
        -w "%{http_code}")
    
    local http_code="${response: -3}"
    local body="${response%???}"
    
    if [ "$http_code" != "200" ]; then
        log_error "Heartbeat request failed with HTTP $http_code"
        echo "Response: $body"
        return 1
    fi
    
    log_success "Test heartbeat request sent successfully"
    echo "Response: $body"
    return 0
}

# Extract trace ID from response headers
extract_trace_id() {
    log_info "Extracting trace ID from response headers..."
    
    local headers
    headers=$(curl -sf -X POST "${CONFIG_CONTROL_URL}/api/heartbeat" \
        -H "Content-Type: application/json" \
        -H "X-Test-Request: true" \
        -d '{
            "serviceName": "test-trace-service",
            "instanceId": "test-trace-instance-'$(date +%s)'",
            "configHash": "test-hash-'$(date +%s)'",
            "host": "test-host",
            "port": 8080,
            "environment": "test",
            "version": "1.0.0"
        }' \
        -I 2>/dev/null || echo "")
    
    local trace_id
    trace_id=$(echo "$headers" | grep -i "x-trace-id" | cut -d' ' -f2 | tr -d '\r\n' || echo "")
    
    if [ -z "$trace_id" ]; then
        log_error "No trace ID found in response headers"
        echo "Headers: $headers"
        return 1
    fi
    
    # Validate trace ID format (should be 32 hex characters)
    if [[ $trace_id =~ ^[a-f0-9]{32}$ ]]; then
        log_success "Trace ID extracted: $trace_id"
        echo "$trace_id"
        return 0
    else
        log_error "Invalid trace ID format: $trace_id"
        return 1
    fi
}

# Verify trace in Tempo
verify_trace_in_tempo() {
    local trace_id="$1"
    
    log_info "Verifying trace in Tempo..."
    
    # Wait for trace to be ingested
    log_info "Waiting 10 seconds for trace ingestion..."
    sleep 10
    
    # Query Tempo for the trace
    local trace_data
    trace_data=$(curl -sf "${TEMPO_URL}/api/traces/${trace_id}" 2>/dev/null || echo "")
    
    if [ -z "$trace_data" ]; then
        log_error "Trace not found in Tempo"
        log_info "Attempting alternative query methods..."
        
        # Try search API
        local search_data
        search_data=$(curl -sf "${TEMPO_URL}/api/search?tags=service.name%3Dtest-trace-service" 2>/dev/null || echo "")
        
        if [ -n "$search_data" ]; then
            log_warning "Found traces for service but not specific trace ID"
            echo "Search results: $search_data"
        else
            log_error "No traces found for service in Tempo"
        fi
        
        return 1
    fi
    
    # Parse trace data
    local span_count
    span_count=$(echo "$trace_data" | jq -r '.spans | length' 2>/dev/null || echo "0")
    
    if [ "$span_count" -gt 0 ]; then
        log_success "Trace found in Tempo with $span_count spans"
        
        # Show trace details
        echo "Trace details:"
        echo "$trace_data" | jq -r '.spans[] | "  - \(.operationName) (\(.duration)ns)"' 2>/dev/null || echo "  - Unable to parse span details"
        
        return 0
    else
        log_error "Trace found but no spans in Tempo"
        return 1
    fi
}

# Test trace search functionality
test_trace_search() {
    log_info "Testing Tempo search functionality..."
    
    # Search for traces by service name
    local search_data
    search_data=$(curl -sf "${TEMPO_URL}/api/search?tags=service.name%3Dtest-trace-service" 2>/dev/null || echo "")
    
    if [ -n "$search_data" ]; then
        log_success "Tempo search functionality working"
        echo "Search results: $search_data"
        return 0
    else
        log_warning "No search results found (may be normal for new traces)"
        return 0
    fi
}

# Test trace correlation headers
test_correlation_headers() {
    log_info "Testing trace correlation headers..."
    
    local headers
    headers=$(curl -sf -X GET "${CONFIG_CONTROL_URL}/api/heartbeat/health" \
        -H "X-Test-Request: true" \
        -I 2>/dev/null || echo "")
    
    local trace_id
    trace_id=$(echo "$headers" | grep -i "x-trace-id" | cut -d' ' -f2 | tr -d '\r\n' || echo "")
    
    if [ -n "$trace_id" ]; then
        log_success "Trace correlation headers working"
        echo "Trace ID in response: $trace_id"
        return 0
    else
        log_error "No trace correlation headers found"
        return 1
    fi
}

# Main execution
main() {
    log_info "Starting Trace Ingestion Verification"
    log_info "====================================="
    
    # Generate test trace
    if ! generate_test_trace; then
        exit 1
    fi
    
    # Extract trace ID
    local trace_id
    trace_id=$(extract_trace_id)
    if [ -z "$trace_id" ]; then
        exit 1
    fi
    
    # Verify trace in Tempo
    if ! verify_trace_in_tempo "$trace_id"; then
        exit 1
    fi
    
    # Test search functionality
    test_trace_search
    
    # Test correlation headers
    if ! test_correlation_headers; then
        exit 1
    fi
    
    log_info "====================================="
    log_success "All trace ingestion tests passed!"
    log_info "Trace ID for correlation tests: $trace_id"
    
    # Store trace ID for other tests
    echo "$trace_id" > /tmp/verification_trace_id
    
    exit 0
}

# Run main function
main "$@"
