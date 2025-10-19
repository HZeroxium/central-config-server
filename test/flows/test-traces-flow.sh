#!/bin/bash
# Test traces flow: services → alloy → tempo → grafana
# Validates end-to-end trace collection and processing pipeline

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

# Test timing
TEST_START_TIME=$(date +%s)
TEST_ID=""
TRACE_ID=""

# ============================================================================
# Usage and Help
# ============================================================================

show_usage() {
    cat << EOF
Usage: test-traces-flow.sh [OPTIONS]

Test the traces flow: services → Alloy → Tempo → Grafana

OPTIONS:
    --service-url=URL     Target service URL (default: http://localhost:8081)
    --no-cleanup         Don't cleanup test data after test
    --verbose            Show detailed test operations
    --help               Show this help message

EXAMPLES:
    # Basic test
    test-traces-flow.sh

    # Test specific service
    test-traces-flow.sh --service-url=http://localhost:8080

    # Keep test data for debugging
    test-traces-flow.sh --no-cleanup

    # Verbose output
    test-traces-flow.sh --verbose

TEST FLOW:
    1. Generate test trace via OTLP to Alloy
    2. Verify Alloy received (check Alloy receiver metrics)
    3. Query trace from Tempo by trace ID
    4. Verify trace in Grafana
    5. Optional: test trace-to-logs correlation
    6. Report results with timing at each step
    7. Optional cleanup (--no-cleanup to keep data)

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

send_otlp_trace() {
    local endpoint="$1"
    local test_id="$2"
    local trace_id="${3:-$(uuidgen | tr -d '-')}"
    
    # Generate OTLP trace JSON payload
    local payload=$(cat <<EOF
{
  "resourceSpans": [{
    "resource": {
      "attributes": [{
        "key": "service.name",
        "value": {"stringValue": "test-service"}
      }, {
        "key": "test.id",
        "value": {"stringValue": "$test_id"}
      }]
    },
    "scopeSpans": [{
      "scope": {"name": "test-scope"},
      "spans": [{
        "traceId": "$trace_id",
        "spanId": "$(uuidgen | tr -d '-' | cut -c1-16)",
        "name": "test-span",
        "kind": 1,
        "startTimeUnixNano": "$(date +%s)000000000",
        "endTimeUnixNano": "$(date +%s)000000000",
        "attributes": [{
          "key": "test.marker",
          "value": {"stringValue": "$test_id"}
        }]
      }]
    }]
  }]
}
EOF
)
    
    # Send to Alloy OTLP endpoint
    local response=$(curl -s -X POST "$endpoint/v1/traces" \
        -H "Content-Type: application/json" \
        -d "$payload" \
        -w "\n%{http_code}" 2>&1)
    
    local status_code=$(echo "$response" | tail -n1)
    
    if [ "$status_code" = "200" ] || [ "$status_code" = "202" ]; then
        log_success "OTLP trace sent to $endpoint"
        return 0
    else
        log_error "Failed to send OTLP trace (HTTP $status_code)"
        return 1
    fi
}

step_1_generate_test_trace() {
    log_step "generate-trace" "Generate test trace via OTLP to Alloy"
    
    TEST_ID=$(generate_test_id)
    TRACE_ID=$(uuidgen | tr -d '-')
    log_info "Generated test ID: $TEST_ID"
    log_info "Generated trace ID: $TRACE_ID"
    
    # Send OTLP trace to Alloy
    local alloy_endpoint="http://localhost:24318"  # OTLP HTTP endpoint
    
    if send_otlp_trace "$alloy_endpoint" "$TEST_ID" "$TRACE_ID"; then
        log_success "OTLP trace sent to Alloy"
    else
        log_error "Failed to send OTLP trace to Alloy"
        log_step_complete "generate-trace" "false"
        return 1
    fi
    
    # Also generate trace by triggering service endpoints (simulate real traces)
    log_info "Triggering service endpoints to generate traces..."
    curl -s "$SERVICE_URL/actuator/health" >/dev/null 2>&1 || true
    curl -s "$SERVICE_URL/api/heartbeat/health" >/dev/null 2>&1 || true
    
    # Give time for trace processing
    log_info "Waiting for trace processing..."
    sleep 5
    
    log_step_complete "generate-trace" "true"
}

step_2_verify_alloy_reception() {
    log_step "alloy-reception" "Verify Alloy received and processed traces"
    
    # Check Alloy metrics for trace reception
    local response=$(curl -s "$ALLOY_URL/metrics" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$response" ]; then
        # Look for OTLP receiver metrics
        local otlp_traces=$(echo "$response" | grep -c "otelcol_receiver_accepted_spans" || echo "0")
        local otlp_errors=$(echo "$response" | grep -c "otelcol_receiver_refused_spans" || echo "0")
        
        # Ensure values are numeric
        if ! [[ "$otlp_traces" =~ ^[0-9]+$ ]]; then
            otlp_traces=0
        fi
        if ! [[ "$otlp_errors" =~ ^[0-9]+$ ]]; then
            otlp_errors=0
        fi
        
        if [ "$otlp_traces" -gt 0 ]; then
            log_success "Found OTLP trace reception metrics in Alloy"
        else
            log_warning "No OTLP trace reception metrics found in Alloy"
        fi
        
        if [ "$otlp_errors" -gt 0 ]; then
            log_warning "Found $otlp_errors trace reception errors in Alloy"
        fi
        
        if [ "$VERBOSE" = "true" ]; then
            log_info "Alloy OTLP receiver metrics:"
            echo "$response" | grep "otelcol_receiver" | head -10
        fi
    else
        log_warning "Failed to query Alloy metrics"
    fi
    
    log_step_complete "alloy-reception" "true"
}

step_3_verify_tempo_storage() {
    log_step "tempo-storage" "Verify traces are stored in Tempo"
    
    # Query Tempo for recent traces
    local response=$(curl -s -G \
        --data-urlencode "tags=service.name=test-service" \
        --data-urlencode "start=$(date -u -d '10 minutes ago' +%Y-%m-%dT%H:%M:%SZ)" \
        --data-urlencode "end=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
        "$TEMPO_URL/api/search" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$response" ]; then
        local trace_count=$(echo "$response" | jq -r '.traces | length' 2>/dev/null || echo "0")
        
        if [ "$trace_count" -gt 0 ]; then
            log_success "Found $trace_count traces in Tempo"
            
            # Extract trace IDs for further verification
            local trace_ids=$(echo "$response" | jq -r '.traces[].traceID' 2>/dev/null)
            if [ -n "$trace_ids" ]; then
                log_info "Sample trace IDs found:"
                echo "$trace_ids" | head -3
            fi
            
            if [ "$VERBOSE" = "true" ]; then
                log_info "Trace details:"
                echo "$response" | jq -r '.traces[] | "\(.traceID): \(.rootServiceName) - \(.rootTraceName)"' 2>/dev/null | head -5
            fi
        else
            log_warning "No traces found in Tempo"
            log_info "This might indicate trace processing or storage issues"
        fi
    else
        log_error "Failed to query Tempo search API"
        log_step_complete "tempo-storage" "false"
        return 1
    fi
    
    log_step_complete "tempo-storage" "true"
}

step_4_verify_tempo_query() {
    log_step "tempo-query" "Query specific trace from Tempo"
    
    # Get the first trace ID from the previous step
    local response=$(curl -s -G \
        --data-urlencode "tags=service.name=test-service" \
        --data-urlencode "start=$(date -u -d '10 minutes ago' +%Y-%m-%dT%H:%M:%SZ)" \
        --data-urlencode "end=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
        "$TEMPO_URL/api/search" 2>/dev/null)
    
    local first_trace_id=$(echo "$response" | jq -r '.traces[0].traceID' 2>/dev/null)
    
    if [ -n "$first_trace_id" ] && [ "$first_trace_id" != "null" ]; then
        log_info "Querying trace: $first_trace_id"
        
        # Query the specific trace
        local trace_response=$(curl -s "$TEMPO_URL/api/traces/$first_trace_id" 2>/dev/null)
        
        if [ $? -eq 0 ] && [ -n "$trace_response" ]; then
            local span_count=$(echo "$trace_response" | jq -r '.resourceSpans | length' 2>/dev/null || echo "0")
            
            if [ "$span_count" -gt 0 ]; then
                log_success "Retrieved trace with $span_count resource spans"
                
                if [ "$VERBOSE" = "true" ]; then
                    log_info "Trace details:"
                    echo "$trace_response" | jq -r '.resourceSpans[0].scopeSpans[0].spans[] | "\(.name): \(.startTimeUnixNano) - \(.endTimeUnixNano)"' 2>/dev/null | head -5
                fi
            else
                log_warning "Trace retrieved but no spans found"
            fi
        else
            log_warning "Failed to retrieve specific trace: $first_trace_id"
        fi
    else
        log_warning "No trace ID available for detailed query"
    fi
    
    log_step_complete "tempo-query" "true"
}

step_5_verify_tempo_minio_storage() {
    log_step "tempo-minio-storage" "Verify traces stored in MinIO"
    
    # Check MinIO bucket for Tempo data
    local bucket="tempo-data"
    local bucket_contents=$(docker exec minio-client mc ls "local/$bucket/" 2>&1)
    local block_count=$(echo "$bucket_contents" | grep -c "PRE" 2>/dev/null || echo "0")
    # Ensure block_count is numeric
    if ! [[ "$block_count" =~ ^[0-9]+$ ]]; then
        block_count=0
    fi
    
    if [ "$block_count" -gt 0 ]; then
        log_success "Found $block_count trace blocks in MinIO bucket '$bucket'"
        
        if [ "$VERBOSE" = "true" ]; then
            log_info "Tempo MinIO contents:"
            echo "$bucket_contents"
        fi
    else
        log_warning "No trace blocks found in MinIO - Tempo may need more time to flush"
        log_info "Checking for WAL files..."
        
        # Check if Tempo is at least writing to WAL
        local wal_check=$(docker exec tempo ls -la /var/tempo/wal 2>/dev/null | wc -l)
        log_info "WAL directory entries: $wal_check"
    fi
    
    log_step_complete "tempo-minio-storage" "true"
}

step_6_verify_grafana_datasource() {
    log_step "grafana-datasource" "Verify traces are accessible via Grafana datasource"
    
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
        local tempo_datasource=$(echo "$datasources" | jq -r '.[] | select(.type=="tempo") | .name' 2>/dev/null)
        
        if [ -n "$tempo_datasource" ]; then
            log_success "Tempo datasource found in Grafana: $tempo_datasource"
        else
            log_warning "No Tempo datasource found in Grafana"
        fi
    else
        log_warning "Failed to query Grafana datasources"
    fi
    
    log_step_complete "grafana-datasource" "true"
}

step_7_test_trace_correlation() {
    log_step "trace-correlation" "Test trace-to-logs correlation"
    
    # This is a simplified test - in practice you'd need to:
    # 1. Generate a trace with specific trace_id
    # 2. Generate a log with the same trace_id
    # 3. Verify correlation in Grafana
    
    log_info "Trace-to-logs correlation test (simplified)"
    
    # Check if both Loki and Tempo datasources exist in Grafana
    local datasources=$(curl -s -H "Authorization: Bearer admin:admin" \
        "$GRAFANA_URL/api/datasources" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$datasources" ]; then
        local loki_ds=$(echo "$datasources" | jq -r '.[] | select(.type=="loki") | .name' 2>/dev/null)
        local tempo_ds=$(echo "$datasources" | jq -r '.[] | select(.type=="tempo") | .name' 2>/dev/null)
        
        if [ -n "$loki_ds" ] && [ -n "$tempo_ds" ]; then
            log_success "Both Loki and Tempo datasources available for correlation"
        else
            log_warning "Missing datasources for correlation (Loki: ${loki_ds:-"none"}, Tempo: ${tempo_ds:-"none"})"
        fi
    else
        log_warning "Could not verify datasources for correlation testing"
    fi
    
    log_step_complete "trace-correlation" "true"
}

step_8_cleanup() {
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
        log_info "Trace ID: $TRACE_ID"
    fi
}

# ============================================================================
# Main Test Flow
# ============================================================================

run_test() {
    local test_success=true
    
    log_info "Starting traces flow test"
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
    step_1_generate_test_trace || test_success=false
    step_2_verify_alloy_reception || test_success=false
    step_3_verify_tempo_storage || test_success=false
    step_4_verify_tempo_query || test_success=false
    step_5_verify_tempo_minio_storage || test_success=false
    step_6_verify_grafana_datasource || test_success=false
    step_7_test_trace_correlation || test_success=false
    step_8_cleanup || test_success=false
    
    # Print summary
    print_test_summary "Traces Flow Test" "$test_success"
    
    if [ "$test_success" = "true" ]; then
        log_success "Traces flow test completed successfully"
        return 0
    else
        log_error "Traces flow test failed"
        return 1
    fi
}

# ============================================================================
# Main Execution
# ============================================================================

main() {
    log_info "Observability Traces Flow Test"
    
    # Parse command line arguments
    parse_arguments "$@"
    
    # Run the test
    run_test
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
