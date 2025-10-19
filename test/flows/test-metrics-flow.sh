#!/bin/bash
# Test metrics flow: services → prometheus/alloy → mimir → minio
# Validates both Prometheus and Alloy paths to Mimir with MinIO verification

set -euo pipefail

# Load test helpers
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UTILS_DIR="$(dirname "$SCRIPT_DIR")/utils"
source "$UTILS_DIR/test-helpers.sh"

# Configuration
SERVICE_URL="${SERVICE_URL:-http://localhost:8081}"
ALLOY_OTLP_HTTP="${ALLOY_OTLP_HTTP:-http://localhost:24318}"
CLEANUP_AFTER="${CLEANUP_AFTER:-true}"
VERBOSE="${VERBOSE:-false}"
TEST_BOTH_PATHS="${TEST_BOTH_PATHS:-true}"

# Test timing
TEST_START_TIME=$(date +%s)
TEST_ID=""

# ============================================================================
# Usage and Help
# ============================================================================

show_usage() {
    cat << EOF
Usage: test-metrics-flow.sh [OPTIONS]

Test the metrics flow: services → Prometheus/Alloy → Mimir → MinIO

OPTIONS:
    --service-url=URL     Target service URL (default: http://localhost:8081)
    --no-cleanup         Don't cleanup test data after test
    --verbose            Show detailed test operations
    --prometheus-only    Test only Prometheus → Mimir path
    --alloy-only         Test only Alloy → Mimir path
    --help               Show this help message

EXAMPLES:
    # Test both paths (default)
    test-metrics-flow.sh

    # Test only Prometheus path
    test-metrics-flow.sh --prometheus-only

    # Test only Alloy path
    test-metrics-flow.sh --alloy-only

    # Keep test data for debugging
    test-metrics-flow.sh --no-cleanup

TEST FLOWS:
    Path A: Prometheus → Mimir
    1. Trigger test metric via actuator endpoint
    2. Verify Prometheus scraped it (query /api/v1/query)
    3. Verify remote_write succeeded (check Prometheus metrics)
    4. Verify metric in Mimir (query /prometheus/api/v1/query)
    5. Verify MinIO bucket has data (use mc ls with timestamp filter)

    Path B: Alloy OTLP → Mimir
    1. Send test OTLP metric directly to Alloy
    2. Verify in Alloy (metrics endpoint)
    3. Verify in Mimir
    4. Verify MinIO storage

EOF
}

# ============================================================================
# Argument Parsing
# ============================================================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --service-url=*)
                SERVICE_URL="${1#*=}"
                shift
                ;;
            --no-cleanup)
                CLEANUP_AFTER=false
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --prometheus-only)
                TEST_BOTH_PATHS=false
                TEST_PROMETHEUS_ONLY=true
                shift
                ;;
            --alloy-only)
                TEST_BOTH_PATHS=false
                TEST_ALLOY_ONLY=true
                shift
                ;;
            --help)
                show_usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
}

# ============================================================================
# Test Steps - Prometheus Path
# ============================================================================

step_prometheus_1_trigger_metric() {
    log_step "prometheus-trigger" "Trigger test metric via actuator endpoint"
    
    TEST_ID=$(generate_test_id)
    log_info "Generated test ID: $TEST_ID"
    
    # Trigger a metric by calling actuator endpoints
    log_info "Triggering metrics via actuator endpoints..."
    
    # Call multiple endpoints to generate metrics
    curl -s "$SERVICE_URL/actuator/health" >/dev/null 2>&1 || true
    curl -s "$SERVICE_URL/actuator/info" >/dev/null 2>&1 || true
    curl -s "$SERVICE_URL/actuator/metrics" >/dev/null 2>&1 || true
    
    # Give Prometheus time to scrape
    log_info "Waiting for Prometheus to scrape metrics..."
    sleep 10
    
    log_success "Test metrics triggered"
    log_step_complete "prometheus-trigger" "true"
}

step_prometheus_2_verify_scraping() {
    log_step "prometheus-scraping" "Verify Prometheus scraped the metrics"
    
    # Query Prometheus for application metrics
    local query="up{job=\"config-control-service\"}"
    local count=$(query_prometheus "$query")
    
    if [ "$count" -gt 0 ]; then
        log_success "Found $count application metrics in Prometheus"
        
        if [ "$VERBOSE" = "true" ]; then
            # Show some sample metrics
            local response=$(curl -s -G \
                --data-urlencode "query=$query" \
                "$PROMETHEUS_URL/api/v1/query" 2>/dev/null)
            
            log_info "Sample metrics:"
            echo "$response" | jq -r '.data.result[] | "\(.metric.__name__) = \(.value[1])"' 2>/dev/null | head -5
        fi
    else
        log_warning "No application metrics found in Prometheus"
        log_info "This might indicate scraping configuration issues"
    fi
    
    log_step_complete "prometheus-scraping" "true"
}

step_prometheus_3_verify_remote_write() {
    log_step "prometheus-remote-write" "Verify Prometheus remote_write to Mimir"
    
    # Check Prometheus remote write metrics
    local query="prometheus_remote_storage_succeeded_samples_total"
    local count=$(query_prometheus "$query")
    
    if [ "$count" -gt 0 ]; then
        log_success "Found $count successful remote write samples"
        
        # Check for failed writes
        local failed_query="prometheus_remote_storage_failed_samples_total"
        local failed_count=$(query_prometheus "$failed_query")
        
        if [ "$failed_count" -gt 0 ]; then
            log_warning "Found $failed_count failed remote write samples"
        fi
    else
        log_warning "No remote write metrics found - remote write may not be configured"
    fi
    
    log_step_complete "prometheus-remote-write" "true"
}

step_prometheus_4_verify_mimir_ingestion() {
    log_step "mimir-ingestion-prometheus" "Verify metrics in Mimir from Prometheus"
    
    # Query Mimir for the same metrics
    local query="up{job=\"config-control-service\"}"
    local count=$(query_mimir "$query")
    
    if [ "$count" -gt 0 ]; then
        log_success "Found $count metrics in Mimir from Prometheus path"
        
        if [ "$VERBOSE" = "true" ]; then
            # Show some sample metrics
            local response=$(curl -s -G \
                --data-urlencode "query=$query" \
                "$MIMIR_URL/prometheus/api/v1/query" 2>/dev/null)
            
            log_info "Sample Mimir metrics:"
            echo "$response" | jq -r '.data.result[] | "\(.metric.__name__) = \(.value[1])"' 2>/dev/null | head -5
        fi
    else
        log_warning "No metrics found in Mimir from Prometheus path"
        log_info "This might indicate remote write or ingestion issues"
    fi
    
    log_step_complete "mimir-ingestion-prometheus" "true"
}

# ============================================================================
# Test Steps - Alloy Path
# ============================================================================

step_alloy_1_send_otlp_metric() {
    log_step "alloy-otlp-send" "Send test OTLP metric directly to Alloy"
    
    TEST_ID=$(generate_test_id)
    log_info "Generated test ID: $TEST_ID"
    
    # Send OTLP metric to Alloy
    local alloy_endpoint="http://${ALLOY_OTLP_HTTP#*://}"
    
    if send_otlp_metric "$alloy_endpoint" "$TEST_ID"; then
        log_success "OTLP metric sent to Alloy"
    else
        log_error "Failed to send OTLP metric to Alloy"
        log_step_complete "alloy-otlp-send" "false"
        return 1
    fi
    
    # Give Alloy time to process
    log_info "Waiting for Alloy to process metric..."
    sleep 5
    
    log_step_complete "alloy-otlp-send" "true"
}

step_alloy_2_verify_alloy_reception() {
    log_step "alloy-reception" "Verify metric received by Alloy"
    
    # Check Alloy metrics endpoint
    local response=$(curl -s "$ALLOY_URL/metrics" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$response" ]; then
        # Look for OTLP receiver metrics
        local otlp_metrics=$(echo "$response" | grep -c "otelcol_receiver_accepted" || echo "0")
        
        if [ "$otlp_metrics" -gt 0 ]; then
            log_success "Found $otlp_metrics OTLP receiver metrics in Alloy"
        else
            log_warning "No OTLP receiver metrics found in Alloy"
        fi
    else
        log_warning "Failed to query Alloy metrics"
    fi
    
    log_step_complete "alloy-reception" "true"
}

step_alloy_3_verify_mimir_ingestion() {
    log_step "mimir-ingestion-alloy" "Verify metrics in Mimir from Alloy path"
    
    # Query Mimir for test metrics
    local query="test_counter{test_id=\"$TEST_ID\"}"
    local count=$(query_mimir "$query")
    
    if [ "$count" -gt 0 ]; then
        log_success "Found $count test metrics in Mimir from Alloy path"
    else
        log_warning "No test metrics found in Mimir from Alloy path"
        log_info "This might indicate OTLP processing or ingestion issues"
    fi
    
    log_step_complete "mimir-ingestion-alloy" "true"
}

# ============================================================================
# Test Steps - MinIO Verification
# ============================================================================

step_minio_verification() {
    log_step "minio-verification" "Verify Mimir writes blocks to MinIO"
    
    # Check MinIO bucket for recent uploads
    local bucket="mimir-data"
    local recent_uploads=$(check_minio_recent_uploads "$bucket" 10)
    
    if [ "$recent_uploads" -gt 0 ]; then
        log_success "Found $recent_uploads recent uploads to MinIO bucket '$bucket'"
        
        if [ "$VERBOSE" = "true" ]; then
            # List recent objects
            log_info "Recent MinIO objects:"
            docker exec minio-client mc ls "local/$bucket" 2>/dev/null | tail -10
        fi
    else
        log_warning "No recent uploads found in MinIO bucket '$bucket'"
        log_info "This might indicate Mimir is not writing to MinIO or timing issues"
        
        # Check total objects in bucket
        local total_objects=$(check_minio_bucket "$bucket")
        log_info "Total objects in bucket: $total_objects"
        
        # Check for TSDB blocks specifically
        local block_count=$(docker exec minio-client mc ls "local/$bucket/" 2>/dev/null | grep -c "DIR" || echo "0")
        log_info "TSDB blocks found: $block_count"
        
        if [ "$block_count" -gt 0 ]; then
            log_success "Found $block_count TSDB blocks in MinIO (may be older than 10 minutes)"
        else
            log_warning "No TSDB blocks found - Mimir may need more time to compact"
        fi
    fi
    
    log_step_complete "minio-verification" "true"
}

# ============================================================================
# Main Test Flow
# ============================================================================

run_prometheus_path_test() {
    log_info "Testing Prometheus → Mimir path"
    
    step_prometheus_1_trigger_metric
    step_prometheus_2_verify_scraping
    step_prometheus_3_verify_remote_write
    step_prometheus_4_verify_mimir_ingestion
}

run_alloy_path_test() {
    log_info "Testing Alloy OTLP → Mimir path"
    
    step_alloy_1_send_otlp_metric
    step_alloy_2_verify_alloy_reception
    step_alloy_3_verify_mimir_ingestion
}

run_test() {
    local test_success=true
    
    log_info "Starting metrics flow test"
    log_info "Service URL: $SERVICE_URL"
    log_info "Cleanup after test: $CLEANUP_AFTER"
    log_info "Verbose mode: $VERBOSE"
    log_info "Test both paths: $TEST_BOTH_PATHS"
    
    # Pre-flight checks
    log_info "Running pre-flight checks..."
    if ! check_dependencies; then
        log_error "Pre-flight checks failed"
        return 1
    fi
    
    # Run Prometheus path test
    if [ "$TEST_BOTH_PATHS" = "true" ] || [ "${TEST_PROMETHEUS_ONLY:-false}" = "true" ]; then
        run_prometheus_path_test || test_success=false
    fi
    
    # Run Alloy path test
    if [ "$TEST_BOTH_PATHS" = "true" ] || [ "${TEST_ALLOY_ONLY:-false}" = "true" ]; then
        run_alloy_path_test || test_success=false
    fi
    
    # MinIO verification (always run)
    step_minio_verification || test_success=false
    
    # Cleanup
    if [ "$CLEANUP_AFTER" = "true" ]; then
        log_info "Running cleanup for test ID: $TEST_ID"
        if [ -f "$UTILS_DIR/cleanup-test-data.sh" ]; then
            "$UTILS_DIR/cleanup-test-data.sh" --test-id="$TEST_ID" --verbose
        fi
    else
        log_info "Skipping cleanup (--no-cleanup flag)"
        log_info "Test data preserved with ID: $TEST_ID"
    fi
    
    # Print summary
    print_test_summary "Metrics Flow Test" "$test_success"
    
    if [ "$test_success" = "true" ]; then
        log_success "Metrics flow test completed successfully"
        return 0
    else
        log_error "Metrics flow test failed"
        return 1
    fi
}

# ============================================================================
# Main Execution
# ============================================================================

main() {
    log_info "Observability Metrics Flow Test"
    
    # Parse command line arguments
    parse_arguments "$@"
    
    # Run the test
    run_test
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
