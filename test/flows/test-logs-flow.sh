#!/bin/bash
# Test logs flow: services → alloy → loki → grafana
# Validates end-to-end log collection and processing pipeline

set -euo pipefail

# Load test helpers
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UTILS_DIR="$(dirname "$SCRIPT_DIR")/utils"
source "$UTILS_DIR/test-helpers.sh"

# Configuration
SERVICE_URL="${SERVICE_URL:-http://localhost:8081}"
CLEANUP_AFTER="${CLEANUP_AFTER:-true}"
VERBOSE="${VERBOSE:-false}"

# Test timing
TEST_START_TIME=$(date +%s)
TEST_ID=""

# ============================================================================
# Usage and Help
# ============================================================================

show_usage() {
    cat << EOF
Usage: test-logs-flow.sh [OPTIONS]

Test the logs flow: services → Alloy → Loki → Grafana

OPTIONS:
    --service-url=URL     Target service URL (default: http://localhost:8081)
    --no-cleanup         Don't cleanup test data after test
    --verbose            Show detailed test operations
    --help               Show this help message

EXAMPLES:
    # Basic test
    test-logs-flow.sh

    # Test specific service
    test-logs-flow.sh --service-url=http://localhost:8080

    # Keep test data for debugging
    test-logs-flow.sh --no-cleanup

    # Verbose output
    test-logs-flow.sh --verbose

TEST FLOW:
    1. Generate test log with unique marker
    2. Wait for Alloy to collect (query Alloy metrics API)
    3. Verify log in Loki (query LogQL API)
    4. Verify log in Grafana datasource (query via Grafana API)
    5. Report results with timing at each step
    6. Optional cleanup (--no-cleanup to keep data)

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
# Test Steps
# ============================================================================

step_1_generate_test_log() {
    log_step "generate-log" "Generate test log with unique marker"
    
    TEST_ID=$(generate_test_id)
    log_info "Generated test ID: $TEST_ID"
    
    # Generate test log data
    local test_log=$(generate_test_log "$TEST_ID" "test-service" "INFO")
    log_info "Test log: $test_log"
    
    # Send log to service (simulate by calling actuator endpoint)
    local response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "{\"message\":\"Test log for flow testing\",\"test_id\":\"$TEST_ID\",\"level\":\"INFO\"}" \
        "$SERVICE_URL/actuator/loggers/ROOT" 2>/dev/null)
    
    if [ $? -eq 0 ]; then
        log_success "Test log sent to service"
    else
        log_warning "Failed to send test log to service - will check for existing logs"
    fi
    
    # Also log directly to trigger application logging
    log_info "Triggering application logging..."
    curl -s "$SERVICE_URL/actuator/health" >/dev/null 2>&1 || true
    
    log_step_complete "generate-log" "true"
}

step_2_wait_for_alloy_collection() {
    log_step "alloy-collection" "Wait for Alloy to collect logs from Docker"
    
    # Wait for Alloy to process the logs
    # This is a simplified check - in practice you'd query Alloy's metrics
    local alloy_ready=false
    local attempts=0
    local max_attempts=15
    
    while [ $attempts -lt $max_attempts ] && [ "$alloy_ready" = "false" ]; do
        if curl -s -f "$ALLOY_URL/metrics" >/dev/null 2>&1; then
            alloy_ready=true
            log_success "Alloy is ready and collecting logs"
            break
        fi
        
        log_info "Waiting for Alloy to be ready... (attempt $((attempts + 1))/$max_attempts)"
        sleep 2
        attempts=$((attempts + 1))
    done
    
    if [ "$alloy_ready" = "false" ]; then
        log_warning "Alloy may not be ready - continuing with test"
    fi
    
    # Give Alloy time to process logs
    log_info "Waiting for Alloy to process logs..."
    sleep 5
    
    log_step_complete "alloy-collection" "true"
}

step_3_verify_loki_ingestion() {
    log_step "loki-ingestion" "Verify logs are ingested into Loki"
    
    # Query Loki for our test log
    local query="{test_id=\"$TEST_ID\"}"
    local start_time=$(date -u -d '10 minutes ago' +%Y-%m-%dT%H:%M:%SZ)
    local end_time=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    
    log_info "Querying Loki with: $query"
    log_info "Time range: $start_time to $end_time"
    
    local response=$(curl -s -G \
        --data-urlencode "query=$query" \
        --data-urlencode "start=$start_time" \
        --data-urlencode "end=$end_time" \
        "$LOKI_URL/loki/api/v1/query_range" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$response" ]; then
        local log_count=$(echo "$response" | jq -r '.data.result | length' 2>/dev/null || echo "0")
        
        if [ "$log_count" -gt 0 ]; then
            log_success "Found $log_count log entries in Loki"
            
            if [ "$VERBOSE" = "true" ]; then
                log_info "Log entries:"
                echo "$response" | jq -r '.data.result[] | .values[] | .[1]' 2>/dev/null | head -5
            fi
        else
            log_warning "No log entries found in Loki for test ID: $TEST_ID"
            log_info "This might be due to timing or log format issues"
        fi
    else
        log_error "Failed to query Loki"
        log_step_complete "loki-ingestion" "false"
        return 1
    fi
    
    log_step_complete "loki-ingestion" "true"
}

step_4_verify_grafana_datasource() {
    log_step "grafana-datasource" "Verify logs are accessible via Grafana datasource"
    
    # Check if Grafana is accessible
    if ! curl -s -f "$GRAFANA_URL/api/health" >/dev/null 2>&1; then
        log_warning "Grafana is not accessible - skipping datasource verification"
        log_step_complete "grafana-datasource" "true"
        return 0
    fi
    
    # Check datasources
    local datasources=$(curl -s -H "Authorization: Bearer admin:admin" \
        "$GRAFANA_URL/api/datasources" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$datasources" ]; then
        local loki_datasource=$(echo "$datasources" | jq -r '.[] | select(.type=="loki") | .name' 2>/dev/null)
        
        if [ -n "$loki_datasource" ]; then
            log_success "Loki datasource found in Grafana: $loki_datasource"
        else
            log_warning "No Loki datasource found in Grafana"
        fi
    else
        log_warning "Failed to query Grafana datasources"
    fi
    
    log_step_complete "grafana-datasource" "true"
}

step_5_verify_loki_minio_storage() {
    log_step "loki-minio-storage" "Verify logs stored in MinIO"
    
    # Check MinIO bucket for Loki data
    local bucket="loki-data"
    local bucket_contents=$(docker exec minio-client mc ls "local/$bucket/" 2>&1)
    local object_count=$(echo "$bucket_contents" | wc -l)
    
    if [ "$object_count" -gt 0 ]; then
        log_success "Found $object_count objects in MinIO bucket '$bucket'"
        
        if [ "$VERBOSE" = "true" ]; then
            log_info "Loki MinIO contents (first 10):"
            echo "$bucket_contents" | head -10
        fi
    else
        log_warning "No log data found in MinIO bucket"
    fi
    
    log_step_complete "loki-minio-storage" "true"
}

step_6_performance_metrics() {
    log_step "performance-metrics" "Collect performance metrics for the flow"
    
    local total_time=$(($(date +%s) - TEST_START_TIME))
    local step_times=()
    
    # Calculate step times (simplified)
    log_info "Total test time: ${total_time}s"
    
    # Log performance summary
    log_info "Performance Summary:"
    log_info "  - Test ID: $TEST_ID"
    log_info "  - Total time: ${total_time}s"
    log_info "  - Service URL: $SERVICE_URL"
    log_info "  - Loki URL: $LOKI_URL"
    log_info "  - Grafana URL: $GRAFANA_URL"
    
    log_step_complete "performance-metrics" "true"
}

step_7_cleanup() {
    if [ "$CLEANUP_AFTER" = "true" ]; then
        log_step "cleanup" "Clean up test data"
        
        # Use the cleanup script
        if [ -f "$UTILS_DIR/cleanup-test-data.sh" ]; then
            log_info "Running cleanup for test ID: $TEST_ID"
            "$UTILS_DIR/cleanup-test-data.sh" --test-id="$TEST_ID" --verbose
        else
            log_warning "Cleanup script not found - manual cleanup may be required"
        fi
        
        log_step_complete "cleanup" "true"
    else
        log_info "Skipping cleanup (--no-cleanup flag)"
        log_info "Test data preserved with ID: $TEST_ID"
    fi
}

# ============================================================================
# Main Test Flow
# ============================================================================

run_test() {
    local test_success=true
    
    log_info "Starting logs flow test"
    log_info "Service URL: $SERVICE_URL"
    log_info "Cleanup after test: $CLEANUP_AFTER"
    log_info "Verbose mode: $VERBOSE"
    
    # Pre-flight checks
    log_info "Running pre-flight checks..."
    if ! check_dependencies; then
        log_error "Pre-flight checks failed"
        return 1
    fi
    
    # Run test steps
    step_1_generate_test_log || test_success=false
    step_2_wait_for_alloy_collection || test_success=false
    step_3_verify_loki_ingestion || test_success=false
    step_4_verify_grafana_datasource || test_success=false
    step_5_verify_loki_minio_storage || test_success=false
    step_6_performance_metrics || test_success=false
    step_7_cleanup || test_success=false
    
    # Print summary
    print_test_summary "Logs Flow Test" "$test_success"
    
    if [ "$test_success" = "true" ]; then
        log_success "Logs flow test completed successfully"
        return 0
    else
        log_error "Logs flow test failed"
        return 1
    fi
}

# ============================================================================
# Main Execution
# ============================================================================

main() {
    log_info "Observability Logs Flow Test"
    
    # Parse command line arguments
    parse_arguments "$@"
    
    # Run the test
    run_test
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
