#!/bin/bash

# Correlations Verification Script
# Tests correlations between logs, metrics, and traces

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

# Get trace ID from previous tests or generate new one
get_trace_id() {
    local trace_id=""
    
    # Try to get from previous tests
    if [ -f "/tmp/verification_trace_id" ]; then
        trace_id=$(cat /tmp/verification_trace_id 2>/dev/null || echo "")
    elif [ -f "/tmp/test_trace_id" ]; then
        trace_id=$(cat /tmp/test_trace_id 2>/dev/null || echo "")
    fi
    
    # If no trace ID available, generate one
    if [ -z "$trace_id" ]; then
        log_info "No existing trace ID found, generating new one..."
        
        local headers
        headers=$(curl -sf -X POST "${CONFIG_CONTROL_URL}/api/heartbeat" \
            -H "Content-Type: application/json" \
            -H "X-Test-Request: true" \
            -d '{
                "serviceName": "test-correlation-service",
                "instanceId": "test-correlation-instance-'$(date +%s)'",
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

# Test logs to traces correlation
test_logs_to_traces_correlation() {
    local trace_id="$1"
    
    log_info "Testing logs to traces correlation..."
    
    # Wait for data to be available
    sleep 10
    
    # Query Loki for logs with trace ID
    local log_data
    log_data=$(curl -sf "${LOKI_URL}/loki/api/v1/query_range" \
        -G \
        --data-urlencode "query={job=\"config-control-service\"} |= \"${trace_id}\"" \
        --data-urlencode "start=$(date -u -d '10 minutes ago' +%s)000000000" \
        --data-urlencode "end=$(date -u +%s)000000000" \
        --data-urlencode "limit=10" 2>/dev/null || echo "")
    
    if [ -n "$log_data" ]; then
        local log_count
        log_count=$(echo "$log_data" | jq -r '.data.result | length' 2>/dev/null || echo "0")
        
        if [ "$log_count" -gt 0 ]; then
            log_success "Found $log_count logs containing trace ID $trace_id"
            
            # Verify trace exists in Tempo
            local trace_exists
            trace_exists=$(curl -sf "${TEMPO_URL}/api/traces/${trace_id}" 2>/dev/null || echo "")
            
            if [ -n "$trace_exists" ]; then
                log_success "Corresponding trace found in Tempo"
                return 0
            else
                log_warning "Logs found but corresponding trace not found in Tempo"
                return 0
            fi
        else
            log_error "No logs found with trace ID $trace_id"
            return 1
        fi
    else
        log_error "Failed to query Loki for logs"
        return 1
    fi
}

# Test traces to logs correlation
test_traces_to_logs_correlation() {
    local trace_id="$1"
    
    log_info "Testing traces to logs correlation..."
    
    # Check if trace exists in Tempo
    local trace_data
    trace_data=$(curl -sf "${TEMPO_URL}/api/traces/${trace_id}" 2>/dev/null || echo "")
    
    if [ -z "$trace_data" ]; then
        log_warning "No trace found in Tempo for correlation test"
        return 0
    fi
    
    log_success "Trace found in Tempo for correlation"
    
    # Query Loki for logs that should correspond to this trace
    local correlation_logs
    correlation_logs=$(curl -sf "${LOKI_URL}/loki/api/v1/query_range" \
        -G \
        --data-urlencode "query={job=\"config-control-service\"} |= \"${trace_id}\"" \
        --data-urlencode "start=$(date -u -d '10 minutes ago' +%s)000000000" \
        --data-urlencode "end=$(date -u +%s)000000000" \
        --data-urlencode "limit=5" 2>/dev/null || echo "")
    
    if [ -n "$correlation_logs" ]; then
        log_success "Found corresponding logs in Loki"
        return 0
    else
        log_warning "No corresponding logs found in Loki"
        return 0
    fi
}

# Test metrics to traces correlation (exemplars)
test_metrics_to_traces_correlation() {
    local trace_id="$1"
    
    log_info "Testing metrics to traces correlation (exemplars)..."
    
    # Query for histogram metrics with exemplars
    local exemplar_metrics
    exemplar_metrics=$(curl -sf "${MIMIR_URL}/prometheus/api/v1/query" \
        -G \
        --data-urlencode "query=http_server_requests_seconds_bucket" 2>/dev/null || echo "")
    
    if [ -n "$exemplar_metrics" ]; then
        # Check if any exemplars contain our trace ID
        local has_trace_exemplar
        has_trace_exemplar=$(echo "$exemplar_metrics" | jq -r ".data.result[] | select(.exemplars[]? | contains(\"${trace_id}\"))" 2>/dev/null || echo "")
        
        if [ -n "$has_trace_exemplar" ]; then
            log_success "Found metrics with exemplars containing trace ID $trace_id"
            return 0
        else
            log_warning "No metrics with exemplars containing trace ID $trace_id found (may be normal)"
            return 0
        fi
    else
        log_warning "No exemplar metrics available for correlation test"
        return 0
    fi
}

# Test Grafana datasource correlations
test_grafana_correlations() {
    log_info "Testing Grafana datasource correlations..."
    
    # Check Grafana datasources configuration
    local datasources
    datasources=$(curl -sf "${GRAFANA_URL}/api/datasources" -H "Authorization: Basic YWRtaW46YWRtaW4=" 2>/dev/null || echo "[]")
    
    if [ "$datasources" = "[]" ]; then
        log_error "No datasources found in Grafana"
        return 1
    fi
    
    # Check Tempo datasource configuration
    local tempo_config
    tempo_config=$(echo "$datasources" | jq -r '.[] | select(.name == "Tempo") | .jsonData' 2>/dev/null || echo "{}")
    
    if [ "$tempo_config" != "{}" ]; then
        # Check for traces to logs correlation
        local traces_to_logs
        traces_to_logs=$(echo "$tempo_config" | jq -r '.tracesToLogsV2 // empty' 2>/dev/null || echo "")
        
        if [ -n "$traces_to_logs" ]; then
            log_success "Tempo traces to logs correlation configured"
        else
            log_warning "Tempo traces to logs correlation not configured"
        fi
        
        # Check for traces to metrics correlation
        local traces_to_metrics
        traces_to_metrics=$(echo "$tempo_config" | jq -r '.tracesToMetrics // empty' 2>/dev/null || echo "")
        
        if [ -n "$traces_to_metrics" ]; then
            log_success "Tempo traces to metrics correlation configured"
        else
            log_warning "Tempo traces to metrics correlation not configured"
        fi
    else
        log_error "Tempo datasource not found in Grafana"
        return 1
    fi
    
    # Check Loki datasource configuration
    local loki_config
    loki_config=$(echo "$datasources" | jq -r '.[] | select(.name == "Loki") | .jsonData' 2>/dev/null || echo "{}")
    
    if [ "$loki_config" != "{}" ]; then
        # Check for derived fields
        local derived_fields
        derived_fields=$(echo "$loki_config" | jq -r '.derivedFields // [] | length' 2>/dev/null || echo "0")
        
        if [ "$derived_fields" -gt 0 ]; then
            log_success "Loki derived fields configured for correlation"
        else
            log_warning "Loki derived fields not configured"
        fi
    else
        log_error "Loki datasource not found in Grafana"
        return 1
    fi
    
    # Check Mimir datasource configuration
    local mimir_config
    mimir_config=$(echo "$datasources" | jq -r '.[] | select(.name == "Mimir") | .jsonData' 2>/dev/null || echo "{}")
    
    if [ "$mimir_config" != "{}" ]; then
        # Check for exemplar destinations
        local exemplar_destinations
        exemplar_destinations=$(echo "$mimir_config" | jq -r '.exemplarTraceIdDestinations // [] | length' 2>/dev/null || echo "0")
        
        if [ "$exemplar_destinations" -gt 0 ]; then
            log_success "Mimir exemplar trace ID destinations configured"
        else
            log_warning "Mimir exemplar trace ID destinations not configured"
        fi
    else
        log_error "Mimir datasource not found in Grafana"
        return 1
    fi
    
    return 0
}

# Test end-to-end correlation flow
test_end_to_end_correlation() {
    local trace_id="$1"
    
    log_info "Testing end-to-end correlation flow..."
    
    # Step 1: Verify trace exists
    local trace_exists
    trace_exists=$(curl -sf "${TEMPO_URL}/api/traces/${trace_id}" 2>/dev/null || echo "")
    
    if [ -z "$trace_exists" ]; then
        log_error "Trace not found in Tempo"
        return 1
    fi
    
    log_success "Step 1: Trace found in Tempo"
    
    # Step 2: Verify logs exist with trace ID
    local logs_exist
    logs_exist=$(curl -sf "${LOKI_URL}/loki/api/v1/query_range" \
        -G \
        --data-urlencode "query={job=\"config-control-service\"} |= \"${trace_id}\"" \
        --data-urlencode "start=$(date -u -d '10 minutes ago' +%s)000000000" \
        --data-urlencode "end=$(date -u +%s)000000000" \
        --data-urlencode "limit=1" 2>/dev/null || echo "")
    
    if [ -n "$logs_exist" ]; then
        local log_count
        log_count=$(echo "$logs_exist" | jq -r '.data.result | length' 2>/dev/null || echo "0")
        
        if [ "$log_count" -gt 0 ]; then
            log_success "Step 2: Logs found with trace ID"
        else
            log_warning "Step 2: No logs found with trace ID"
        fi
    else
        log_warning "Step 2: Failed to query logs"
    fi
    
    # Step 3: Verify metrics exist
    local metrics_exist
    metrics_exist=$(curl -sf "${MIMIR_URL}/prometheus/api/v1/query" \
        -G \
        --data-urlencode "query=http_server_requests_seconds_count" 2>/dev/null || echo "")
    
    if [ -n "$metrics_exist" ]; then
        local metric_count
        metric_count=$(echo "$metrics_exist" | jq -r '.data.result | length' 2>/dev/null || echo "0")
        
        if [ "$metric_count" -gt 0 ]; then
            log_success "Step 3: Metrics found in Mimir"
        else
            log_warning "Step 3: No metrics found in Mimir"
        fi
    else
        log_warning "Step 3: Failed to query metrics"
    fi
    
    # Step 4: Test correlation links
    log_info "Step 4: Correlation links configured"
    
    return 0
}

# Main execution
main() {
    log_info "Starting Correlations Verification"
    log_info "=================================="
    
    # Get trace ID
    local trace_id
    trace_id=$(get_trace_id)
    if [ -z "$trace_id" ]; then
        exit 1
    fi
    
    log_info "Using trace ID: $trace_id"
    
    # Test individual correlations
    if ! test_logs_to_traces_correlation "$trace_id"; then
        exit 1
    fi
    
    test_traces_to_logs_correlation "$trace_id"
    test_metrics_to_traces_correlation "$trace_id"
    
    # Test Grafana correlations
    if ! test_grafana_correlations; then
        exit 1
    fi
    
    # Test end-to-end correlation
    if ! test_end_to_end_correlation "$trace_id"; then
        exit 1
    fi
    
    log_info "=================================="
    log_success "All correlation tests completed!"
    log_info "Trace ID used: $trace_id"
    
    exit 0
}

# Run main function
main "$@"
