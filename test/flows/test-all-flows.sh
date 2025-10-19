#!/bin/bash
# Master orchestrator for all observability flow tests
# Runs logs, metrics, and traces flow tests sequentially

set -euo pipefail

# Load test helpers
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UTILS_DIR="$(dirname "$SCRIPT_DIR")/utils"
source "$UTILS_DIR/test-helpers.sh"

# Configuration
SERVICE_URL="${SERVICE_URL:-http://localhost:8081}"
CLEANUP_AFTER="${CLEANUP_AFTER:-true}"
VERBOSE="${VERBOSE:-false}"
RUN_INFRA_CHECK="${RUN_INFRA_CHECK:-true}"
RUN_MINIO_VERIFICATION="${RUN_MINIO_VERIFICATION:-true}"

# Test results tracking
TEST_RESULTS=()
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# ============================================================================
# Usage and Help
# ============================================================================

show_usage() {
    cat << EOF
Usage: test-all-flows.sh [OPTIONS]

Run all observability flow tests: logs, metrics, traces, and MinIO verification.

OPTIONS:
    --service-url=URL     Target service URL (default: http://localhost:8081)
    --no-cleanup         Don't cleanup test data after tests
    --verbose            Show detailed test operations
    --skip-infra-check   Skip infrastructure health check
    --skip-minio-check   Skip MinIO storage verification
    --logs-only          Run only logs flow test
    --metrics-only       Run only metrics flow test
    --traces-only        Run only traces flow test
    --help               Show this help message

EXAMPLES:
    # Run all tests
    test-all-flows.sh

    # Run with verbose output
    test-all-flows.sh --verbose

    # Run specific test only
    test-all-flows.sh --logs-only

    # Skip cleanup for debugging
    test-all-flows.sh --no-cleanup

TEST SEQUENCE:
    1. Infrastructure health check (optional)
    2. MinIO storage verification (optional)
    3. Logs flow test
    4. Metrics flow test (both Prometheus and Alloy paths)
    5. Traces flow test
    6. Summary report

EOF
}

# ============================================================================
# Argument Parsing
# ============================================================================

parse_arguments() {
    RUN_LOGS=true
    RUN_METRICS=true
    RUN_TRACES=true
    
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
            --skip-infra-check)
                RUN_INFRA_CHECK=false
                shift
                ;;
            --skip-minio-check)
                RUN_MINIO_VERIFICATION=false
                shift
                ;;
            --logs-only)
                RUN_METRICS=false
                RUN_TRACES=false
                RUN_MINIO_VERIFICATION=false
                shift
                ;;
            --metrics-only)
                RUN_LOGS=false
                RUN_TRACES=false
                shift
                ;;
            --traces-only)
                RUN_LOGS=false
                RUN_METRICS=false
                RUN_MINIO_VERIFICATION=false
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
# Test Execution Functions
# ============================================================================

run_test() {
    local test_name="$1"
    local test_script="$2"
    local test_args="$3"
    
    log_info "Starting $test_name"
    
    local start_time=$(date +%s)
    local test_success=false
    
    if [ -f "$test_script" ]; then
        if bash "$test_script" $test_args; then
            test_success=true
            ((PASSED_TESTS++))
            log_success "$test_name PASSED"
        else
            ((FAILED_TESTS++))
            log_error "$test_name FAILED"
        fi
    else
        log_error "Test script not found: $test_script"
        ((FAILED_TESTS++))
    fi
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    TEST_RESULTS+=("$test_name:$test_success:$duration")
    ((TOTAL_TESTS++))
    
    log_info "$test_name completed in ${duration}s"
}

run_infrastructure_check() {
    if [ "$RUN_INFRA_CHECK" = "false" ]; then
        log_info "Skipping infrastructure health check"
        return 0
    fi
    
    log_step "infra-check" "Run infrastructure health check"
    
    local infra_script="../infra-health-check.sh"
    if [ -f "$infra_script" ]; then
        if bash "$infra_script"; then
            log_success "Infrastructure health check passed"
        else
            log_warning "Infrastructure health check failed - continuing with tests"
        fi
    else
        log_warning "Infrastructure health check script not found - skipping"
    fi
    
    log_step_complete "infra-check" "true"
}

run_minio_verification() {
    if [ "$RUN_MINIO_VERIFICATION" = "false" ]; then
        log_info "Skipping MinIO storage verification"
        return 0
    fi
    
    run_test "MinIO Storage Verification" \
             "verify-minio-storage.sh" \
             "--verbose"
}

run_logs_flow_test() {
    if [ "$RUN_LOGS" = "false" ]; then
        log_info "Skipping logs flow test"
        return 0
    fi
    
    local test_args="--service-url=$SERVICE_URL"
    if [ "$CLEANUP_AFTER" = "false" ]; then
        test_args="$test_args --no-cleanup"
    fi
    if [ "$VERBOSE" = "true" ]; then
        test_args="$test_args --verbose"
    fi
    
    run_test "Logs Flow Test" \
             "test-logs-flow.sh" \
             "$test_args"
}

run_metrics_flow_test() {
    if [ "$RUN_METRICS" = "false" ]; then
        log_info "Skipping metrics flow test"
        return 0
    fi
    
    local test_args="--service-url=$SERVICE_URL"
    if [ "$CLEANUP_AFTER" = "false" ]; then
        test_args="$test_args --no-cleanup"
    fi
    if [ "$VERBOSE" = "true" ]; then
        test_args="$test_args --verbose"
    fi
    
    run_test "Metrics Flow Test" \
             "test-metrics-flow.sh" \
             "$test_args"
}

run_traces_flow_test() {
    if [ "$RUN_TRACES" = "false" ]; then
        log_info "Skipping traces flow test"
        return 0
    fi
    
    local test_args="--service-url=$SERVICE_URL"
    if [ "$CLEANUP_AFTER" = "false" ]; then
        test_args="$test_args --no-cleanup"
    fi
    if [ "$VERBOSE" = "true" ]; then
        test_args="$test_args --verbose"
    fi
    
    run_test "Traces Flow Test" \
             "test-traces-flow.sh" \
             "$test_args"
}

# ============================================================================
# Summary and Reporting
# ============================================================================

print_test_summary() {
    local total_time=$(($(date +%s) - TEST_START_TIME))
    
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}  All Flow Tests Summary${NC}"
    echo -e "${BLUE}========================================${NC}"
    
    echo -e "${BLUE}Total Tests:${NC} $TOTAL_TESTS"
    echo -e "${GREEN}Passed:${NC} $PASSED_TESTS"
    echo -e "${RED}Failed:${NC} $FAILED_TESTS"
    echo -e "${BLUE}Total Time:${NC} ${total_time}s"
    
    if [ $FAILED_TESTS -eq 0 ]; then
        log_success "All tests passed!"
        echo -e "${GREEN}Overall Result: SUCCESS${NC}"
    else
        log_error "Some tests failed"
        echo -e "${RED}Overall Result: FAILURE${NC}"
    fi
    
    echo -e "\n${BLUE}Detailed Results:${NC}"
    for result in "${TEST_RESULTS[@]}"; do
        IFS=':' read -r name success duration <<< "$result"
        local status_color
        local status_text
        
        if [ "$success" = "true" ]; then
            status_color="$GREEN"
            status_text="PASS"
        else
            status_color="$RED"
            status_text="FAIL"
        fi
        
        echo -e "  ${BLUE}$name:${NC} ${status_color}$status_text${NC} (${duration}s)"
    done
    
    echo -e "\n${BLUE}Configuration:${NC}"
    echo -e "  Service URL: $SERVICE_URL"
    echo -e "  Cleanup: $CLEANUP_AFTER"
    echo -e "  Verbose: $VERBOSE"
    echo -e "  Infrastructure Check: $RUN_INFRA_CHECK"
    echo -e "  MinIO Verification: $RUN_MINIO_VERIFICATION"
    echo -e "  Tests Enabled:"
    echo -e "    - Logs: $RUN_LOGS"
    echo -e "    - Metrics: $RUN_METRICS"
    echo -e "    - Traces: $RUN_TRACES"
}

# ============================================================================
# Main Test Orchestration
# ============================================================================

run_all_tests() {
    log_info "Starting all observability flow tests"
    log_info "Service URL: $SERVICE_URL"
    log_info "Cleanup after tests: $CLEANUP_AFTER"
    log_info "Verbose mode: $VERBOSE"
    
    # Pre-flight checks
    log_info "Running pre-flight checks..."
    if ! check_dependencies; then
        log_error "Pre-flight checks failed"
        return 1
    fi
    
    # Run tests in sequence
    run_infrastructure_check
    run_minio_verification
    run_logs_flow_test
    run_metrics_flow_test
    run_traces_flow_test
    
    # Print summary
    print_test_summary
    
    # Return appropriate exit code
    if [ $FAILED_TESTS -eq 0 ]; then
        log_success "All flow tests completed successfully"
        return 0
    else
        log_error "Some flow tests failed"
        return 1
    fi
}

# ============================================================================
# Main Execution
# ============================================================================

main() {
    log_info "Observability All Flow Tests Orchestrator"
    
    # Parse command line arguments
    parse_arguments "$@"
    
    # Run all tests
    run_all_tests
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
