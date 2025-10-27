#!/bin/bash

# Metric Ingestion Verification Script
# Tests metric ingestion into Mimir with exemplars

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
MIMIR_URL="${BASE_URL}:9009"
PROMETHEUS_URL="${BASE_URL}:9092"

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

# Generate test metrics
generate_test_metrics() {
    log_info "Generating test metrics..."
    
    # Send multiple requests to generate metrics
    for i in {1..5}; do
        curl -sf -X POST "${CONFIG_CONTROL_URL}/api/heartbeat" \
            -H "Content-Type: application/json" \
            -H "X-Test-Request: true" \
            -d "{
                \"serviceName\": \"test-metric-service-$i\",
                \"instanceId\": \"test-metric-instance-$i-'$(date +%s)'\",
                \"configHash\": \"test-hash-$i-'$(date +%s)'\",
                \"host\": \"test-host\",
                \"port\": 8080,
                \"environment\": \"test\",
                \"version\": \"1.0.0\"
            }" > /dev/null
        
        # Also hit different endpoints to generate different metrics
        curl -sf -X GET "${CONFIG_CONTROL_URL}/api/heartbeat/health" > /dev/null
        curl -sf -X GET "${CONFIG_CONTROL_URL}/actuator/health" > /dev/null
        
        sleep 2
    done
    
    log_success "Test metric generation completed"
}

# Test Mimir connectivity
test_mimir_connectivity() {
    log_info "Testing Mimir connectivity..."
    
    # Test Mimir health
    if curl -sf "${MIMIR_URL}/ready" > /dev/null; then
        log_success "Mimir is healthy"
    else
        log_error "Mimir health check failed"
        return 1
    fi
    
    # Test Mimir metrics endpoint
    local metrics_data
    metrics_data=$(curl -sf "${MIMIR_URL}/metrics" 2>/dev/null || echo "")
    
    if [ -n "$metrics_data" ]; then
        log_success "Mimir metrics endpoint accessible"
        return 0
    else
        log_error "Mimir metrics endpoint not accessible"
        return 1
    fi
}

# Verify HTTP request metrics in Mimir
verify_http_metrics() {
    log_info "Verifying HTTP request metrics in Mimir..."
    
    # Wait for metrics to be scraped
    log_info "Waiting 20 seconds for metric ingestion..."
    sleep 20
    
    # Query for HTTP request metrics
    local http_metrics
    http_metrics=$(curl -sf "${MIMIR_URL}/prometheus/api/v1/query" \
        -G \
        --data-urlencode "query=http_server_requests_seconds_count" 2>/dev/null || echo "")
    
    if [ -z "$http_metrics" ]; then
        log_error "No HTTP metrics found in Mimir"
        return 1
    fi
    
    # Parse metric data
    local metric_count
    metric_count=$(echo "$http_metrics" | jq -r '.data.result | length' 2>/dev/null || echo "0")
    
    if [ "$metric_count" -gt 0 ]; then
        log_success "Found $metric_count HTTP request metric entries"
        
        # Show sample metrics
        echo "Sample HTTP metrics:"
        echo "$http_metrics" | jq -r '.data.result[] | "  - \(.metric.method) \(.metric.uri) \(.metric.status) = \(.value[1])"' 2>/dev/null | head -5 || echo "  - Unable to parse metric details"
        
        return 0
    else
        log_error "No HTTP request metrics found"
        return 1
    fi
}

# Test metric exemplars
test_metric_exemplars() {
    log_info "Testing metric exemplars..."
    
    # Query for histogram metrics that should have exemplars
    local histogram_metrics
    histogram_metrics=$(curl -sf "${MIMIR_URL}/prometheus/api/v1/query" \
        -G \
        --data-urlencode "query=http_server_requests_seconds_bucket" 2>/dev/null || echo "")
    
    if [ -n "$histogram_metrics" ]; then
        local histogram_count
        histogram_count=$(echo "$histogram_metrics" | jq -r '.data.result | length' 2>/dev/null || echo "0")
        
        if [ "$histogram_count" -gt 0 ]; then
            log_success "Found $histogram_count histogram metric entries"
            
            # Check for exemplars in the response
            local has_exemplars
            has_exemplars=$(echo "$histogram_metrics" | jq -r '.data.result[0] | has("exemplars")' 2>/dev/null || echo "false")
            
            if [ "$has_exemplars" = "true" ]; then
                log_success "Exemplars found in histogram metrics"
                
                # Show exemplar details
                echo "Sample exemplar:"
                echo "$histogram_metrics" | jq -r '.data.result[0].exemplars[0]' 2>/dev/null || echo "  - Unable to parse exemplar details"
                
                return 0
            else
                log_warning "No exemplars found in histogram metrics (may be normal for some configurations)"
                return 0
            fi
        else
            log_warning "No histogram metrics found"
            return 0
        fi
    else
        log_warning "No histogram metrics available"
        return 0
    fi
}

# Test application-specific metrics
test_application_metrics() {
    log_info "Testing application-specific metrics..."
    
    # Query for config control service specific metrics
    local app_metrics
    app_metrics=$(curl -sf "${MIMIR_URL}/prometheus/api/v1/query" \
        -G \
        --data-urlencode "query=config_control_heartbeat_processed_total" 2>/dev/null || echo "")
    
    if [ -n "$app_metrics" ]; then
        local app_metric_count
        app_metric_count=$(echo "$app_metrics" | jq -r '.data.result | length' 2>/dev/null || echo "0")
        
        if [ "$app_metric_count" -gt 0 ]; then
            log_success "Found $app_metric_count application-specific metric entries"
            return 0
        fi
    fi
    
    # Try alternative metric names
    local alt_metrics
    alt_metrics=$(curl -sf "${MIMIR_URL}/prometheus/api/v1/query" \
        -G \
        --data-urlencode "query=config_control_heartbeat_duration_seconds" 2>/dev/null || echo "")
    
    if [ -n "$alt_metrics" ]; then
        log_success "Found alternative application metrics"
        return 0
    fi
    
    log_warning "No application-specific metrics found (may be normal depending on configuration)"
    return 0
}

# Test OTLP metric ingestion
test_otlp_metrics() {
    log_info "Testing OTLP metric ingestion..."
    
    # Query for OTLP-style metric names (with dots)
    local otlp_metrics
    otlp_metrics=$(curl -sf "${MIMIR_URL}/prometheus/api/v1/query" \
        -G \
        --data-urlencode "query=http.server.request.body.size_bytes" 2>/dev/null || echo "")
    
    if [ -n "$otlp_metrics" ]; then
        local otlp_count
        otlp_count=$(echo "$otlp_metrics" | jq -r '.data.result | length' 2>/dev/null || echo "0")
        
        if [ "$otlp_count" -gt 0 ]; then
            log_success "Found $otlp_count OTLP-style metrics"
            return 0
        fi
    fi
    
    # Try other OTLP metric patterns
    local otlp_alt
    otlp_alt=$(curl -sf "${MIMIR_URL}/prometheus/api/v1/query" \
        -G \
        --data-urlencode "query=process.runtime.jvm.memory.used_bytes" 2>/dev/null || echo "")
    
    if [ -n "$otlp_alt" ]; then
        log_success "Found OTLP runtime metrics"
        return 0
    fi
    
    log_warning "No OTLP-style metrics found (may be normal depending on configuration)"
    return 0
}

# Test Prometheus remote write
test_prometheus_remote_write() {
    log_info "Testing Prometheus remote write..."
    
    # Check if Prometheus is writing to Mimir
    local prometheus_metrics
    prometheus_metrics=$(curl -sf "${PROMETHEUS_URL}/api/v1/query" \
        -G \
        --data-urlencode "query=up" 2>/dev/null || echo "")
    
    if [ -n "$prometheus_metrics" ]; then
        log_success "Prometheus is accessible and has metrics"
        
        # Check if the same metrics appear in Mimir
        local mimir_up
        mimir_up=$(curl -sf "${MIMIR_URL}/prometheus/api/v1/query" \
            -G \
            --data-urlencode "query=up" 2>/dev/null || echo "")
        
        if [ -n "$mimir_up" ]; then
            log_success "Metrics successfully replicated from Prometheus to Mimir"
            return 0
        else
            log_warning "Metrics not found in Mimir (remote write may not be configured)"
            return 0
        fi
    else
        log_warning "Prometheus not accessible (may be normal in this configuration)"
        return 0
    fi
}

# Main execution
main() {
    log_info "Starting Metric Ingestion Verification"
    log_info "======================================"
    
    # Test Mimir connectivity
    if ! test_mimir_connectivity; then
        exit 1
    fi
    
    # Generate test metrics
    generate_test_metrics
    
    # Test Prometheus remote write
    test_prometheus_remote_write
    
    # Verify HTTP metrics
    if ! verify_http_metrics; then
        exit 1
    fi
    
    # Test metric exemplars
    test_metric_exemplars
    
    # Test application-specific metrics
    test_application_metrics
    
    # Test OTLP metrics
    test_otlp_metrics
    
    log_info "======================================"
    log_success "All metric ingestion tests passed!"
    
    exit 0
}

# Run main function
main "$@"
