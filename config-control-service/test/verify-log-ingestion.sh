#!/bin/bash

# Log Ingestion Verification Script
# Tests log ingestion into Loki with trace correlation

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
LOKI_URL="${BASE_URL}:3100"

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

# Get trace ID from previous test or generate new one
get_trace_id() {
    local trace_id=""
    
    # Try to get from previous test
    if [ -f "/tmp/verification_trace_id" ]; then
        trace_id=$(cat /tmp/verification_trace_id 2>/dev/null || echo "")
    fi
    
    # If no trace ID available, generate one
    if [ -z "$trace_id" ]; then
        log_info "No existing trace ID found, generating new one..."
        
        local headers
        headers=$(curl -sf -X POST "${CONFIG_CONTROL_URL}/api/heartbeat" \
            -H "Content-Type: application/json" \
            -H "X-Test-Request: true" \
            -d '{
                "serviceName": "test-log-service",
                "instanceId": "test-log-instance-'$(date +%s)'",
                "configHash": "test-hash-'$(date +%s)'",
                "host": "test-host",
                "port": 8080,
                "environment": "test",
                "version": "1.0.0"
            }' \
            -I 2>/dev/null || echo "")
        
        trace_id=$(echo "$headers" | grep -i "x-trace-id" | cut -d' ' -f2 | tr -d '\r\n' || echo "")
    fi
    
    if [ -z "$trace_id" ]; then
        log_error "Failed to obtain trace ID"
        return 1
    fi
    
    echo "$trace_id"
    return 0
}

# Generate test logs
generate_test_logs() {
    log_info "Generating test logs..."
    
    # Send multiple requests to generate logs
    for i in {1..3}; do
        curl -sf -X POST "${CONFIG_CONTROL_URL}/api/heartbeat" \
            -H "Content-Type: application/json" \
            -H "X-Test-Request: true" \
            -d "{
                \"serviceName\": \"test-log-service-$i\",
                \"instanceId\": \"test-log-instance-$i-'$(date +%s)'\",
                \"configHash\": \"test-hash-$i-'$(date +%s)'\",
                \"host\": \"test-host\",
                \"port\": 8080,
                \"environment\": \"test\",
                \"version\": \"1.0.0\"
            }" > /dev/null
        
        sleep 1
    done
    
    log_success "Test log generation completed"
}

# Verify logs in Loki
verify_logs_in_loki() {
    local trace_id="$1"
    
    log_info "Verifying logs in Loki..."
    
    # Wait for logs to be ingested
    log_info "Waiting 15 seconds for log ingestion..."
    sleep 15
    
    # Query Loki for logs
    local start_time
    start_time=$(date -u -d '10 minutes ago' +%s)000000000
    
    local end_time
    end_time=$(date -u +%s)000000000
    
    # Search for logs with trace ID
    local log_data
    log_data=$(curl -sf "${LOKI_URL}/loki/api/v1/query_range" \
        -G \
        --data-urlencode "query={job=\"config-control-service\"} |= \"${trace_id}\"" \
        --data-urlencode "start=${start_time}" \
        --data-urlencode "end=${end_time}" \
        --data-urlencode "limit=100" 2>/dev/null || echo "")
    
    if [ -z "$log_data" ]; then
        log_error "No logs found in Loki"
        return 1
    fi
    
    # Parse log data
    local log_count
    log_count=$(echo "$log_data" | jq -r '.data.result | length' 2>/dev/null || echo "0")
    
    if [ "$log_count" -gt 0 ]; then
        log_success "Found $log_count log entries with trace ID $trace_id"
        
        # Show sample log entries
        echo "Sample log entries:"
        echo "$log_data" | jq -r '.data.result[0].values[0][1]' 2>/dev/null | head -3 || echo "  - Unable to parse log entries"
        
        return 0
    else
        log_error "No log entries found with trace ID $trace_id"
        return 1
    fi
}

# Test log correlation with trace ID
test_log_correlation() {
    local trace_id="$1"
    
    log_info "Testing log correlation with trace ID..."
    
    # Query for logs containing trace_id in the message
    local correlation_data
    correlation_data=$(curl -sf "${LOKI_URL}/loki/api/v1/query_range" \
        -G \
        --data-urlencode "query={job=\"config-control-service\"} |= \"trace_id\"" \
        --data-urlencode "start=$(date -u -d '10 minutes ago' +%s)000000000" \
        --data-urlencode "end=$(date -u +%s)000000000" \
        --data-urlencode "limit=50" 2>/dev/null || echo "")
    
    if [ -n "$correlation_data" ]; then
        local correlation_count
        correlation_count=$(echo "$correlation_data" | jq -r '.data.result | length' 2>/dev/null || echo "0")
        
        if [ "$correlation_count" -gt 0 ]; then
            log_success "Found $correlation_count log entries with trace_id field"
            
            # Check for specific trace ID
            local specific_correlation
            specific_correlation=$(echo "$correlation_data" | jq -r ".data.result[] | select(.values[][1] | contains(\"${trace_id}\")) | .values | length" 2>/dev/null || echo "0")
            
            if [ "$specific_correlation" -gt 0 ]; then
                log_success "Found logs specifically containing trace ID $trace_id"
            else
                log_warning "No logs found specifically containing trace ID $trace_id"
            fi
            
            return 0
        fi
    fi
    
    log_error "No log correlation found"
    return 1
}

# Test log labels and metadata
test_log_labels() {
    log_info "Testing log labels and metadata..."
    
    # Query for logs with specific labels
    local label_data
    label_data=$(curl -sf "${LOKI_URL}/loki/api/v1/query_range" \
        -G \
        --data-urlencode "query={job=\"config-control-service\", level=\"INFO\"}" \
        --data-urlencode "start=$(date -u -d '10 minutes ago' +%s)000000000" \
        --data-urlencode "end=$(date -u +%s)000000000" \
        --data-urlencode "limit=10" 2>/dev/null || echo "")
    
    if [ -n "$label_data" ]; then
        local label_count
        label_count=$(echo "$label_data" | jq -r '.data.result | length' 2>/dev/null || echo "0")
        
        if [ "$label_count" -gt 0 ]; then
            log_success "Found $label_count log entries with proper labels"
            
            # Show label structure
            echo "Label structure:"
            echo "$label_data" | jq -r '.data.result[0].stream' 2>/dev/null || echo "  - Unable to parse labels"
            
            return 0
        fi
    fi
    
    log_warning "No logs found with expected labels (may be normal)"
    return 0
}

# Test Loki API functionality
test_loki_api() {
    log_info "Testing Loki API functionality..."
    
    # Test labels API
    local labels_data
    labels_data=$(curl -sf "${LOKI_URL}/loki/api/v1/labels" 2>/dev/null || echo "")
    
    if [ -n "$labels_data" ]; then
        log_success "Loki labels API working"
        
        local label_count
        label_count=$(echo "$labels_data" | jq -r '.data | length' 2>/dev/null || echo "0")
        echo "Available labels: $label_count"
        
        # Show available labels
        echo "$labels_data" | jq -r '.data[]' 2>/dev/null | head -5 || echo "  - Unable to parse labels"
        
        return 0
    else
        log_error "Loki labels API not working"
        return 1
    fi
}

# Main execution
main() {
    log_info "Starting Log Ingestion Verification"
    log_info "===================================="
    
    # Get trace ID
    local trace_id
    trace_id=$(get_trace_id)
    if [ -z "$trace_id" ]; then
        exit 1
    fi
    
    log_info "Using trace ID: $trace_id"
    
    # Generate test logs
    generate_test_logs
    
    # Test Loki API
    if ! test_loki_api; then
        exit 1
    fi
    
    # Test log labels
    test_log_labels
    
    # Verify logs in Loki
    if ! verify_logs_in_loki "$trace_id"; then
        exit 1
    fi
    
    # Test log correlation
    if ! test_log_correlation "$trace_id"; then
        exit 1
    fi
    
    log_info "===================================="
    log_success "All log ingestion tests passed!"
    log_info "Trace ID used: $trace_id"
    
    exit 0
}

# Run main function
main "$@"
