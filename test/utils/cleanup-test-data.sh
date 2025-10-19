#!/bin/bash
# Cleanup script for observability flow test data
# Supports selective cleanup with --keep flag for debugging

set -euo pipefail

# Load test helpers
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/test-helpers.sh"

# Default values
CLEANUP_ALL=false
KEEP_DATA=false
TEST_ID=""
VERBOSE=false

# Configuration
LOKI_URL="${LOKI_URL:-http://localhost:23100}"
TEMPO_URL="${TEMPO_URL:-http://localhost:24317}"
MIMIR_URL="${MIMIR_URL:-http://localhost:23009}"
MINIO_ENDPOINT="${MINIO_ENDPOINT:-localhost:20000}"

# ============================================================================
# Usage and Help
# ============================================================================

show_usage() {
    cat << EOF
Usage: cleanup-test-data.sh [OPTIONS]

Clean up test data from observability services.

OPTIONS:
    --all                 Clean all test data (not just specific test runs)
    --test-id=ID         Clean data for specific test ID only
    --keep               Skip cleanup (keep data for debugging)
    --verbose            Show detailed cleanup operations
    --help               Show this help message

EXAMPLES:
    # Clean specific test run
    cleanup-test-data.sh --test-id=test-20240101-120000-abc123

    # Clean all test data
    cleanup-test-data.sh --all

    # Skip cleanup (for debugging)
    cleanup-test-data.sh --keep

    # Clean all with verbose output
    cleanup-test-data.sh --all --verbose

NOTES:
    - Test data is identified by test_id labels/attributes
    - Cleanup operations are logged for audit purposes
    - Use --keep flag to preserve data for debugging
    - All operations are reversible (except MinIO data)

EOF
}

# ============================================================================
# Argument Parsing
# ============================================================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --all)
                CLEANUP_ALL=true
                shift
                ;;
            --test-id=*)
                TEST_ID="${1#*=}"
                shift
                ;;
            --keep)
                KEEP_DATA=true
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
# Cleanup Functions
# ============================================================================

cleanup_loki_data() {
    local test_id="$1"
    
    if [ "$KEEP_DATA" = "true" ]; then
        log_warning "Skipping Loki cleanup (--keep flag)"
        return 0
    fi
    
    log_step "cleanup-loki" "Clean up test logs from Loki"
    
    if [ -n "$test_id" ]; then
        # Delete specific test logs
        local query="{test_id=\"$test_id\"}"
        local response=$(curl -s -X DELETE \
            -G --data-urlencode "query=$query" \
            "$LOKI_URL/loki/api/v1/delete" 2>/dev/null)
        
        if [ $? -eq 0 ]; then
            log_success "Deleted Loki logs for test ID: $test_id"
        else
            log_warning "Failed to delete Loki logs for test ID: $test_id"
        fi
    elif [ "$CLEANUP_ALL" = "true" ]; then
        # Delete all test logs (those with test_id label)
        local query="{test_id=~\".*\"}"
        local response=$(curl -s -X DELETE \
            -G --data-urlencode "query=$query" \
            "$LOKI_URL/loki/api/v1/delete" 2>/dev/null)
        
        if [ $? -eq 0 ]; then
            log_success "Deleted all test logs from Loki"
        else
            log_warning "Failed to delete all test logs from Loki"
        fi
    fi
    
    log_step_complete "cleanup-loki" "true"
}

cleanup_tempo_data() {
    local test_id="$1"
    
    if [ "$KEEP_DATA" = "true" ]; then
        log_warning "Skipping Tempo cleanup (--keep flag)"
        return 0
    fi
    
    log_step "cleanup-tempo" "Clean up test traces from Tempo"
    
    # Note: Tempo doesn't have a direct delete API for traces by attributes
    # In practice, traces expire based on retention policies
    # This is more of a placeholder for future implementation
    
    log_warning "Tempo trace cleanup not implemented - traces will expire based on retention policy"
    log_step_complete "cleanup-tempo" "true"
}

cleanup_mimir_data() {
    local test_id="$1"
    
    if [ "$KEEP_DATA" = "true" ]; then
        log_warning "Skipping Mimir cleanup (--keep flag)"
        return 0
    fi
    
    log_step "cleanup-mimir" "Clean up test metrics from Mimir"
    
    # Note: Mimir doesn't have a direct delete API for specific metrics
    # Metrics are typically cleaned up by retention policies
    # This is more of a placeholder for future implementation
    
    log_warning "Mimir metric cleanup not implemented - metrics will expire based on retention policy"
    log_step_complete "cleanup-mimir" "true"
}

cleanup_minio_test_data() {
    local test_id="$1"
    
    if [ "$KEEP_DATA" = "true" ]; then
        log_warning "Skipping MinIO cleanup (--keep flag)"
        return 0
    fi
    
    log_step "cleanup-minio" "Clean up test data from MinIO buckets"
    
    # This is a destructive operation - be very careful
    if [ "$VERBOSE" = "true" ]; then
        log_info "MinIO cleanup would remove test-related blocks (not implemented for safety)"
    fi
    
    log_warning "MinIO cleanup not implemented - manual cleanup may be required"
    log_step_complete "cleanup-minio" "true"
}

cleanup_grafana_data() {
    local test_id="$1"
    
    if [ "$KEEP_DATA" = "true" ]; then
        log_warning "Skipping Grafana cleanup (--keep flag)"
        return 0
    fi
    
    log_step "cleanup-grafana" "Clean up test data from Grafana"
    
    # Note: Grafana cleanup would require authentication and specific API calls
    # This is more of a placeholder for future implementation
    
    log_warning "Grafana cleanup not implemented - test data will remain in dashboards/queries"
    log_step_complete "cleanup-grafana" "true"
}

# ============================================================================
# Main Cleanup Logic
# ============================================================================

perform_cleanup() {
    local test_id="$1"
    
    log_info "Starting test data cleanup"
    log_info "Test ID: ${test_id:-"ALL"}"
    log_info "Keep data: $KEEP_DATA"
    log_info "Cleanup all: $CLEANUP_ALL"
    log_info "Verbose: $VERBOSE"
    
    # Clean up each service
    cleanup_loki_data "$test_id"
    cleanup_tempo_data "$test_id"
    cleanup_mimir_data "$test_id"
    cleanup_minio_test_data "$test_id"
    cleanup_grafana_data "$test_id"
    
    log_success "Test data cleanup completed"
}

# ============================================================================
# Validation and Safety Checks
# ============================================================================

validate_cleanup_request() {
    if [ -z "$TEST_ID" ] && [ "$CLEANUP_ALL" = "false" ]; then
        log_error "No cleanup target specified. Use --test-id or --all"
        show_usage
        exit 1
    fi
    
    if [ -n "$TEST_ID" ] && [ "$CLEANUP_ALL" = "true" ]; then
        log_error "Cannot specify both --test-id and --all"
        show_usage
        exit 1
    fi
    
    if [ "$KEEP_DATA" = "true" ]; then
        log_warning "Cleanup disabled by --keep flag"
        log_info "This is useful for debugging - data will be preserved"
    fi
}

check_service_availability() {
    local services=("$LOKI_URL" "$TEMPO_URL" "$MIMIR_URL")
    local failed=()
    
    for service in "${services[@]}"; do
        if ! curl -s -f "$service" >/dev/null 2>&1; then
            failed+=("$service")
        fi
    done
    
    if [ ${#failed[@]} -gt 0 ]; then
        log_warning "Some services are not available: ${failed[*]}"
        log_warning "Cleanup will continue for available services"
    fi
}

# ============================================================================
# Main Execution
# ============================================================================

main() {
    log_info "Observability Test Data Cleanup Tool"
    
    # Parse command line arguments
    parse_arguments "$@"
    
    # Validate request
    validate_cleanup_request
    
    # Check service availability
    check_service_availability
    
    # Perform cleanup
    perform_cleanup "$TEST_ID"
    
    log_success "Cleanup process completed"
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
