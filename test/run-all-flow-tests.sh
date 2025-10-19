#!/bin/bash
# Master control script for running all observability flow tests
# Provides high-level orchestration and reporting

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
FLOWS_DIR="$SCRIPT_DIR/flows"
UTILS_DIR="$SCRIPT_DIR/utils"

# Load test helpers
source "$UTILS_DIR/test-helpers.sh"

# Configuration
SERVICE_URL="${SERVICE_URL:-http://localhost:8081}"
CLEANUP_AFTER="${CLEANUP_AFTER:-true}"
VERBOSE="${VERBOSE:-false}"
RUN_INFRA_CHECK="${RUN_INFRA_CHECK:-true}"
RUN_MINIO_VERIFICATION="${RUN_MINIO_VERIFICATION:-true}"
OUTPUT_DIR="${OUTPUT_DIR:-$SCRIPT_DIR/results}"

# Test timing
MASTER_START_TIME=$(date +%s)

# ============================================================================
# Usage and Help
# ============================================================================

show_usage() {
    cat << EOF
Usage: run-all-flow-tests.sh [OPTIONS]

Master control script for observability flow testing.

OPTIONS:
    --service-url=URL     Target service URL (default: http://localhost:8081)
    --no-cleanup         Don't cleanup test data after tests
    --verbose            Show detailed test operations
    --skip-infra-check   Skip infrastructure health check
    --skip-minio-check   Skip MinIO storage verification
    --output-dir=DIR     Output directory for test results (default: test/results)
    --logs-only          Run only logs flow test
    --metrics-only       Run only metrics flow test
    --traces-only        Run only traces flow test
    --help               Show this help message

EXAMPLES:
    # Run all tests
    run-all-flow-tests.sh

    # Run with verbose output and keep data
    run-all-flow-tests.sh --verbose --no-cleanup

    # Run only logs test
    run-all-flow-tests.sh --logs-only

    # Run with custom output directory
    run-all-flow-tests.sh --output-dir=/tmp/test-results

DESCRIPTION:
    This script orchestrates the complete observability testing suite:
    
    1. Infrastructure Health Check
       - Validates all infrastructure services are running
       - Checks network connectivity and service availability
    
    2. MinIO Storage Verification
       - Verifies Mimir writes blocks to MinIO
       - Checks TSDB structure and recent uploads
    
    3. Logs Flow Test
       - Tests: services → Alloy → Loki → Grafana
       - Validates log collection and processing pipeline
    
    4. Metrics Flow Test
       - Tests: services → Prometheus/Alloy → Mimir → MinIO
       - Validates both Prometheus and Alloy metric paths
    
    5. Traces Flow Test
       - Tests: services → Alloy → Tempo → Grafana
       - Validates trace collection and processing pipeline
    
    6. Summary Report
       - Provides comprehensive test results
       - Includes performance metrics and recommendations

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
            --skip-infra-check)
                RUN_INFRA_CHECK=false
                shift
                ;;
            --skip-minio-check)
                RUN_MINIO_VERIFICATION=false
                shift
                ;;
            --output-dir=*)
                OUTPUT_DIR="${1#*=}"
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
# Setup and Validation
# ============================================================================

setup_output_directory() {
    log_step "setup-output" "Setup output directory for test results"
    
    if [ ! -d "$OUTPUT_DIR" ]; then
        mkdir -p "$OUTPUT_DIR"
        log_success "Created output directory: $OUTPUT_DIR"
    else
        log_info "Using existing output directory: $OUTPUT_DIR"
    fi
    
    # Create timestamped subdirectory
    local timestamp=$(date +%Y%m%d-%H%M%S)
    local session_dir="$OUTPUT_DIR/session-$timestamp"
    mkdir -p "$session_dir"
    
    log_info "Session directory: $session_dir"
    export SESSION_DIR="$session_dir"
    
    log_step_complete "setup-output" "true"
}

validate_environment() {
    log_step "validate-environment" "Validate testing environment"
    
    # Check if we're in the right directory
    if [ ! -f "$PROJECT_ROOT/docker-compose.infra.yml" ]; then
        log_error "docker-compose.infra.yml not found in project root"
        log_error "Please run this script from the project root directory"
        return 1
    fi
    
    # Check if flows directory exists
    if [ ! -d "$FLOWS_DIR" ]; then
        log_error "Flows directory not found: $FLOWS_DIR"
        return 1
    fi
    
    # Check if required test scripts exist
    local required_scripts=(
        "$FLOWS_DIR/test-logs-flow.sh"
        "$FLOWS_DIR/test-metrics-flow.sh"
        "$FLOWS_DIR/test-traces-flow.sh"
        "$FLOWS_DIR/verify-minio-storage.sh"
        "$FLOWS_DIR/test-all-flows.sh"
    )
    
    for script in "${required_scripts[@]}"; do
        if [ ! -f "$script" ]; then
            log_error "Required test script not found: $script"
            return 1
        fi
    done
    
    log_success "Environment validation passed"
    log_step_complete "validate-environment" "true"
}

check_docker_environment() {
    log_step "docker-environment" "Check Docker environment"
    
    # Check if Docker is running
    if ! docker info >/dev/null 2>&1; then
        log_error "Docker is not running or not accessible"
        return 1
    fi
    
    # Check if docker-compose is available
    if ! command -v docker-compose >/dev/null 2>&1; then
        log_error "docker-compose is not available"
        return 1
    fi
    
    # Check if infrastructure is running
    local infra_services=$(docker-compose -f "$PROJECT_ROOT/docker-compose.infra.yml" ps --services --filter "status=running" 2>/dev/null | wc -l)
    
    if [ "$infra_services" -gt 0 ]; then
        log_success "Found $infra_services running infrastructure services"
    else
        log_warning "No infrastructure services appear to be running"
        log_info "You may need to start infrastructure with:"
        log_info "  docker-compose -f docker-compose.infra.yml up -d"
    fi
    
    log_step_complete "docker-environment" "true"
}

# ============================================================================
# Test Execution
# ============================================================================

run_flow_tests() {
    log_step "flow-tests" "Run all observability flow tests"
    
    # Change to flows directory
    cd "$FLOWS_DIR"
    
    # Prepare test arguments
    local test_args="--service-url=$SERVICE_URL"
    if [ "$CLEANUP_AFTER" = "false" ]; then
        test_args="$test_args --no-cleanup"
    fi
    if [ "$VERBOSE" = "true" ]; then
        test_args="$test_args --verbose"
    fi
    if [ "$RUN_INFRA_CHECK" = "false" ]; then
        test_args="$test_args --skip-infra-check"
    fi
    if [ "$RUN_MINIO_VERIFICATION" = "false" ]; then
        test_args="$test_args --skip-minio-check"
    fi
    
    # Add specific test filters if provided
    if [ "${RUN_LOGS:-true}" = "false" ]; then
        test_args="$test_args --logs-only"
    elif [ "${RUN_METRICS:-true}" = "false" ]; then
        test_args="$test_args --metrics-only"
    elif [ "${RUN_TRACES:-true}" = "false" ]; then
        test_args="$test_args --traces-only"
    fi
    
    # Run the test orchestrator
    log_info "Running flow tests with args: $test_args"
    
    if bash "test-all-flows.sh" $test_args; then
        log_success "All flow tests completed successfully"
        log_step_complete "flow-tests" "true"
        return 0
    else
        log_error "Some flow tests failed"
        log_step_complete "flow-tests" "false"
        return 1
    fi
}

# ============================================================================
# Results and Reporting
# ============================================================================

generate_test_report() {
    log_step "test-report" "Generate comprehensive test report"
    
    local report_file="$SESSION_DIR/test-report.md"
    local total_time=$(($(date +%s) - MASTER_START_TIME))
    
    cat > "$report_file" << EOF
# Observability Flow Test Report

**Generated:** $(date)
**Session:** $(basename "$SESSION_DIR")
**Total Time:** ${total_time}s

## Configuration

- **Service URL:** $SERVICE_URL
- **Cleanup After Tests:** $CLEANUP_AFTER
- **Verbose Mode:** $VERBOSE
- **Infrastructure Check:** $RUN_INFRA_CHECK
- **MinIO Verification:** $RUN_MINIO_VERIFICATION

## Test Results

EOF

    # Add test results if available
    if [ -f "$FLOWS_DIR/test-results.log" ]; then
        echo "### Detailed Results" >> "$report_file"
        echo '```' >> "$report_file"
        cat "$FLOWS_DIR/test-results.log" >> "$report_file"
        echo '```' >> "$report_file"
    fi
    
    # Add recommendations
    cat >> "$report_file" << EOF

## Recommendations

1. **Monitor Test Data:** Check that test data is properly cleaned up
2. **Verify MinIO Storage:** Ensure Mimir is writing blocks to MinIO
3. **Check Service Connectivity:** Verify all services can communicate
4. **Review Logs:** Check application logs for any errors or warnings

## Troubleshooting

If tests fail, check:

1. Infrastructure services are running: \`docker-compose -f docker-compose.infra.yml ps\`
2. Service endpoints are accessible: \`curl \$SERVICE_URL/actuator/health\`
3. Observability stack is ready: \`curl http://localhost:23000/api/health\`
4. MinIO is accessible: \`curl http://localhost:20000/minio/health/live\`

## Next Steps

1. Review test results and address any failures
2. Check observability dashboards in Grafana
3. Verify data persistence in MinIO
4. Run individual flow tests for detailed debugging

EOF

    log_success "Test report generated: $report_file"
    log_step_complete "test-report" "true"
}

# ============================================================================
# Main Execution
# ============================================================================

main() {
    log_info "Observability Flow Tests Master Controller"
    
    # Parse command line arguments
    parse_arguments "$@"
    
    # Setup and validation
    setup_output_directory
    validate_environment
    check_docker_environment
    
    # Run tests
    local test_success=true
    run_flow_tests || test_success=false
    
    # Generate report
    generate_test_report
    
    # Print final summary
    local total_time=$(($(date +%s) - MASTER_START_TIME))
    print_test_summary "Master Flow Tests" "$test_success"
    
    if [ "$test_success" = "true" ]; then
        log_success "All observability flow tests completed successfully"
        log_info "Test results saved to: $SESSION_DIR"
        return 0
    else
        log_error "Some observability flow tests failed"
        log_info "Check test results in: $SESSION_DIR"
        return 1
    fi
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
