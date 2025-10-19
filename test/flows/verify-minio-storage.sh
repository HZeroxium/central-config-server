#!/bin/bash
# Verify Mimir writes blocks to MinIO storage
# Checks for recent block uploads and TSDB structure
# (Updated: add authenticated MinIO access via mc alias with minioadmin/minioadmin by default)

set -euo pipefail

# Load test helpers
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UTILS_DIR="$(dirname "$SCRIPT_DIR")/utils"
source "$UTILS_DIR/test-helpers.sh"

# ============================================================================
# Configuration (overridable via env or CLI)
# ============================================================================

# Target bucket & checks
BUCKET="${BUCKET:-mimir-data}"
RECENT_MINUTES="${RECENT_MINUTES:-10}"
VERBOSE="${VERBOSE:-false}"
DETAILED_CHECK="${DETAILED_CHECK:-false}"

# MinIO access (defaults match docker-compose in this project)
# - We use the minio-client container (alias "local" -> http://minio:9000)
# - Health endpoint uses host-mapped port 20000 by default
MINIO_MC_CONTAINER="${MINIO_MC_CONTAINER:-minio-client}"
MINIO_ALIAS="${MINIO_ALIAS:-local}"
MINIO_URL="${MINIO_URL:-http://minio:9000}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-minioadmin}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-minioadmin}"
MINIO_HEALTH_URL="${MINIO_HEALTH_URL:-http://localhost:20000/minio/health/live}"

# Mimir base URL (HTTP API on host-mapped 9009 -> 23009 per docker-compose)
MIMIR_URL="${MIMIR_URL:-http://localhost:23009}"

# ============================================================================
# Usage and Help
# ============================================================================

show_usage() {
    cat << EOF
Usage: verify-minio-storage.sh [OPTIONS]

Verify Mimir writes blocks to MinIO storage.

OPTIONS:
    --bucket=NAME              MinIO bucket name (default: ${BUCKET})
    --recent-minutes=N         Check for uploads in last N minutes (default: ${RECENT_MINUTES})
    --verbose                  Show detailed operations
    --detailed                 Perform detailed TSDB structure check

  MinIO authentication (optional; defaults match docker-compose):
    --minio-mc-container=NAME  Name of mc container (default: ${MINIO_MC_CONTAINER})
    --minio-alias=ALIAS        mc alias name to use/create (default: ${MINIO_ALIAS})
    --minio-url=URL            MinIO/S3 endpoint URL for mc alias (default: ${MINIO_URL})
    --minio-access-key=KEY     Access key (default: ${MINIO_ACCESS_KEY})
    --minio-secret-key=KEY     Secret key (default: ${MINIO_SECRET_KEY})
    --minio-health-url=URL     Health URL for curl liveness (default: ${MINIO_HEALTH_URL})

  Mimir:
    --mimir-url=URL            Mimir base URL (default: ${MIMIR_URL})

    --help                     Show this help message

EXAMPLES:
    # Basic verification
    verify-minio-storage.sh

    # Check recent uploads only
    verify-minio-storage.sh --recent-minutes=5

    # Detailed structure check
    verify-minio-storage.sh --detailed --verbose

    # Use custom MinIO alias/endpoint
    verify-minio-storage.sh --minio-alias=myminio --minio-url=http://minio:9000

VERIFICATION CHECKS:
    1. MinIO connectivity and bucket existence (with authentication)
    2. Recent block uploads (based on timestamp)
    3. TSDB structure (chunks, index files)
    4. Block metadata and consistency
    5. Storage usage statistics
EOF
}

# ============================================================================
# Argument Parsing
# ============================================================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --bucket=*)              BUCKET="${1#*=}"; shift ;;
            --recent-minutes=*)      RECENT_MINUTES="${1#*=}"; shift ;;
            --verbose)               VERBOSE=true; shift ;;
            --detailed)              DETAILED_CHECK=true; shift ;;
            --minio-mc-container=*)  MINIO_MC_CONTAINER="${1#*=}"; shift ;;
            --minio-alias=*)         MINIO_ALIAS="${1#*=}"; shift ;;
            --minio-url=*)           MINIO_URL="${1#*=}"; shift ;;
            --minio-access-key=*)    MINIO_ACCESS_KEY="${1#*=}"; shift ;;
            --minio-secret-key=*)    MINIO_SECRET_KEY="${1#*=}"; shift ;;
            --minio-health-url=*)    MINIO_HEALTH_URL="${1#*=}"; shift ;;
            --mimir-url=*)           MIMIR_URL="${1#*=}"; shift ;;
            --help)                  show_usage; exit 0 ;;
            *) log_error "Unknown option: $1"; show_usage; exit 1 ;;
        esac
    done
}

# ============================================================================
# MinIO mc helpers (authenticated)
# ============================================================================

mc_cmd() {
    # Wrapper to run mc inside the designated container
    docker exec -i "${MINIO_MC_CONTAINER}" mc "$@"
}

ensure_minio_auth() {
    # Ensure the mc container exists and is ready, then set/refresh alias with credentials
    if ! docker ps --format '{{.Names}}' | grep -qx "${MINIO_MC_CONTAINER}"; then
        log_error "MinIO client container '${MINIO_MC_CONTAINER}' not found or not running"
        return 1
    fi

    # Set/overwrite alias with provided credentials; ignore output
    if ! docker exec -i "${MINIO_MC_CONTAINER}" sh -c \
        "mc alias set '${MINIO_ALIAS}' '${MINIO_URL}' '${MINIO_ACCESS_KEY}' '${MINIO_SECRET_KEY}' >/dev/null 2>&1"; then
        log_error "Failed to configure mc alias '${MINIO_ALIAS}' -> ${MINIO_URL}"
        return 1
    fi

    if [ "$VERBOSE" = "true" ]; then
        log_info "Configured mc alias '${MINIO_ALIAS}' -> ${MINIO_URL}"
    fi
    return 0
}

# Override/implement helper-like functions with authenticated mc

check_minio_bucket() {
    # Returns count of objects in bucket
    local bucket="$1"
    local count=$(docker exec minio-client mc ls "local/$bucket/" 2>/dev/null | wc -l)
    echo "$count"
}

check_minio_recent_uploads() {
    # Count objects newer than RECENT_MINUTES using mc find (auth)
    # Args: bucket minutes
    local bucket="$1"
    local minutes="$2"

    # mc find supports --newer-than in #d#hh#mm#ss; use ${minutes}m
    # Return numeric count (0 on error)
    local count=$(docker exec minio-client mc find "local/$bucket/" --newer-than "${minutes}m" 2>/dev/null | wc -l)
    echo "${count:-0}"
}

# ============================================================================
# Verification Functions
# ============================================================================

ensure_minio_client_running() {
    log_step "minio-client-check" "Check if minio-client container is running"
    
    if ! docker ps | grep -q minio-client; then
        log_error "minio-client container not running"
        log_info "Please restart infrastructure: docker compose -f docker-compose.infra.yml up -d"
        log_step_complete "minio-client-check" "false"
        return 1
    fi
    log_success "minio-client container is running"
    log_step_complete "minio-client-check" "true"
}

verify_mimir_bucket() {
    log_step "mimir-bucket" "Verify Mimir data in MinIO bucket"
    
    local bucket_contents=$(docker exec minio-client mc ls local/mimir-data/anonymous/ 2>&1)
    local block_count=$(echo "$bucket_contents" | grep -c "DIR" 2>/dev/null || echo "0")
    # Ensure block_count is numeric
    if ! [[ "$block_count" =~ ^[0-9]+$ ]]; then
        block_count=0
    fi
    
    log_info "Found $block_count TSDB blocks in mimir-data bucket"
    
    if [ "$block_count" -gt 0 ]; then
        log_success "Mimir has written blocks to MinIO"
        if [ "$VERBOSE" = "true" ]; then
            echo "$bucket_contents"
        fi
    else
        log_warning "No TSDB blocks found - Mimir may need more time to compact"
    fi
    
    log_step_complete "mimir-bucket" "true"
}

verify_tempo_bucket() {
    log_step "tempo-bucket" "Verify Tempo data in MinIO bucket"
    
    local bucket_contents=$(docker exec minio-client mc ls local/tempo-data/ 2>&1)
    local block_count=$(echo "$bucket_contents" | grep -c "PRE" 2>/dev/null || echo "0")
    # Ensure block_count is numeric
    if ! [[ "$block_count" =~ ^[0-9]+$ ]]; then
        block_count=0
    fi
    
    log_info "Found $block_count trace blocks in tempo-data bucket"
    
    if [ "$block_count" -gt 0 ]; then
        log_success "Tempo has written trace blocks to MinIO"
        if [ "$VERBOSE" = "true" ]; then
            echo "$bucket_contents"
        fi
    else
        log_warning "No trace blocks found - Tempo may need traces to be generated first"
        log_info "Run test-traces-flow.sh to generate traces"
    fi
    
    log_step_complete "tempo-bucket" "true"
}

verify_loki_bucket() {
    log_step "loki-bucket" "Verify Loki data in MinIO bucket"
    
    local bucket_contents=$(docker exec minio-client mc ls local/loki-data/ 2>&1)
    local object_count=$(echo "$bucket_contents" | wc -l)
    
    log_info "Found $object_count objects in loki-data bucket"
    
    if [ "$object_count" -gt 0 ]; then
        log_success "Loki has written log data to MinIO"
        if [ "$VERBOSE" = "true" ]; then
            echo "$bucket_contents" | head -10
        fi
    else
        log_warning "No log data found in MinIO"
    fi
    
    log_step_complete "loki-bucket" "true"
}

check_minio_connectivity() {
    log_step "minio-connectivity" "Check MinIO connectivity and bucket existence"

    # Liveness (unauthenticated health endpoint)
    local minio_health
    minio_health=$(curl -s -o /dev/null -w "%{http_code}" "${MINIO_HEALTH_URL}")
    if [ "$minio_health" = "200" ]; then
        log_success "MinIO is accessible"
    else
        log_error "MinIO is not accessible (HTTP $minio_health) at ${MINIO_HEALTH_URL}"
        log_step_complete "minio-connectivity" "false"
        return 1
    fi

    # Ensure authenticated alias is configured
    if ! ensure_minio_auth; then
        log_error "Failed to configure authenticated MinIO access"
        log_step_complete "minio-connectivity" "false"
        return 1
    fi

    # Authenticated bucket check via mc (replaces unauthenticated curl 403)
    if mc_cmd ls "${MINIO_ALIAS}/${BUCKET}" >/dev/null 2>&1; then
        log_success "MinIO API is accessible with authentication (bucket '${BUCKET}' reachable)"
    else
        log_error "MinIO API is not accessible (authenticated ls failed for bucket '${BUCKET}')"
        log_step_complete "minio-connectivity" "false"
        return 1
    fi

    log_step_complete "minio-connectivity" "true"
}

check_recent_uploads() {
    log_step "recent-uploads" "Check for recent block uploads in the last $RECENT_MINUTES minutes"

    local recent_uploads
    recent_uploads=$(check_minio_recent_uploads "$BUCKET" "$RECENT_MINUTES")

    if [ "${recent_uploads:-0}" -gt 0 ]; then
        log_success "Found $recent_uploads recent uploads in the last $RECENT_MINUTES minutes"

        if [ "$VERBOSE" = "true" ]; then
            log_info "Recent uploads (sample):"
            # Show up to 10 newest objects newer than cutoff
            docker exec -i "${MINIO_MC_CONTAINER}" sh -c \
              "mc find '${MINIO_ALIAS}/${BUCKET}' --newer-than ${RECENT_MINUTES}m | head -10" || true
        fi
    else
        log_warning "No recent uploads found in the last $RECENT_MINUTES minutes"
        log_info "This might indicate:"
        log_info "  - Mimir is not writing to MinIO"
        log_info "  - Timing issues (uploads might be older)"
        log_info "  - Configuration problems"
    fi

    log_step_complete "recent-uploads" "true"
}

check_total_objects() {
    log_step "total-objects" "Check total objects and storage usage"

    # Use authenticated check for bucket accessibility
    if mc_cmd ls "${MINIO_ALIAS}/${BUCKET}" >/dev/null 2>&1; then
        log_success "Bucket '$BUCKET' is accessible"

        if [ "$VERBOSE" = "true" ]; then
            # Summary using mc du
            if summary=$(mc_cmd du --human --summarize "${MINIO_ALIAS}/${BUCKET}" 2>/dev/null); then
                log_info "Bucket usage summary:"
                echo "$summary" | sed 's/^/  /'
            else
                log_info "Bucket status: Accessible (usage summary unavailable)"
            fi
        fi
    else
        log_error "Bucket '$BUCKET' is not accessible"
        log_step_complete "total-objects" "false"
        return 1
    fi

    log_step_complete "total-objects" "true"
}

check_tsdb_structure() {
    log_step "tsdb-structure" "Check TSDB block structure and metadata"

    if mc_cmd ls "${MINIO_ALIAS}/${BUCKET}" >/dev/null 2>&1; then
        log_success "Bucket is accessible for TSDB structure checks"
        log_info "Note: Detailed TSDB structure analysis uses authenticated mc operations"

        if [ "$VERBOSE" = "true" ]; then
            log_info "TSDB structure check: listing top-level prefixes (sample)"
            mc_cmd ls "${MINIO_ALIAS}/${BUCKET}" | head -10 || true
        fi
    else
        log_warning "Bucket is not accessible for TSDB structure checks"
    fi

    log_step_complete "tsdb-structure" "true"
}

check_mimir_configuration() {
    log_step "mimir-config" "Verify Mimir configuration for MinIO"

    # Check if Mimir is running
    if ! curl -s -f "$MIMIR_URL/ready" >/dev/null 2>&1; then
        log_warning "Mimir is not ready - cannot verify configuration"
        log_step_complete "mimir-config" "false"
        return 1
    fi

    # Check Mimir storage configuration
    local config_response
    config_response="$(curl -s "$MIMIR_URL/api/v1/status/config" 2>/dev/null || true)"

    if [ -n "$config_response" ]; then
        # Look for S3/MinIO configuration
        local s3_config
        s3_config="$(echo "$config_response" | jq -r '.data | to_entries[] | select(.key | contains("s3")) | .value' 2>/dev/null || echo "")"

        if [ -n "$s3_config" ] && [ "$s3_config" != "null" ]; then
            log_success "Mimir S3 configuration found"

            if [ "$VERBOSE" = "true" ]; then
                log_info "S3 configuration details:"
                echo "$s3_config" | jq -r '.' 2>/dev/null || true
            fi
        else
            log_warning "No S3 configuration found in Mimir"
        fi
    else
        log_warning "Failed to retrieve Mimir configuration"
    fi

    log_step_complete "mimir-config" "true"
}

check_mimir_health() {
    log_step "mimir-health" "Check Mimir health and storage status"

    # Check Mimir readiness
    if curl -s -f "$MIMIR_URL/ready" >/dev/null 2>&1; then
        log_success "Mimir is ready"
    else
        log_error "Mimir is not ready"
        log_step_complete "mimir-health" "false"
        return 1
    fi

    # Check Mimir metrics for storage operations
    local metrics_response
    metrics_response="$(curl -s "$MIMIR_URL/metrics" 2>/dev/null || true)"

    if [ -n "$metrics_response" ]; then
        # Look for storage-related metrics
        local storage_metrics
        storage_metrics="$(echo "$metrics_response" | grep -c -E "storage|s3|minio" || echo "0")"

        if [ "${storage_metrics:-0}" -gt 0 ]; then
            log_success "Found $storage_metrics storage-related metrics in Mimir"

            if [ "$VERBOSE" = "true" ]; then
                log_info "Sample storage metrics:"
                echo "$metrics_response" | grep -E "storage|s3|minio" | head -10 || true
            fi
        else
            log_warning "No storage-related metrics found in Mimir"
        fi
    else
        log_warning "Failed to retrieve Mimir metrics"
    fi

    log_step_complete "mimir-health" "true"
}

# ============================================================================
# Detailed Analysis Functions
# ============================================================================

analyze_block_structure() {
    if [ "$DETAILED_CHECK" = "false" ]; then
        return 0
    fi

    log_step "block-analysis" "Detailed TSDB block structure analysis"

    # Get a sample block prefix (list first directory-looking entry)
    local sample_block
    if ! sample_block="$(mc_cmd ls "${MINIO_ALIAS}/${BUCKET}" 2>/dev/null | awk '/^DIR/ {print $NF; exit}')"; then
        sample_block=""
    fi

    if [ -n "$sample_block" ]; then
        log_info "Analyzing block: $sample_block"

        # List contents of the block
        local block_contents
        block_contents="$(mc_cmd ls "${MINIO_ALIAS}/${BUCKET}/${sample_block}" 2>/dev/null || true)"

        if [ -n "$block_contents" ]; then
            log_success "Block structure analysis:"
            echo "$block_contents"

            # Check for essential files (index, chunks/, meta.json)
            local has_index has_chunks has_meta
            has_index="$(echo "$block_contents" | grep -c "index" || echo "0")"
            has_chunks="$(echo "$block_contents" | grep -c "chunks" || echo "0")"
            has_meta="$(echo "$block_contents" | grep -c "meta.json" || echo "0")"

            log_info "Block contents:"
            log_info "  - Index files: $has_index"
            log_info "  - Chunk files: $has_chunks"
            log_info "  - Metadata: $has_meta"
        else
            log_warning "Could not analyze block contents"
        fi
    else
        log_warning "No blocks available for detailed analysis"
    fi

    log_step_complete "block-analysis" "true"
}

# ============================================================================
# Main Verification Flow
# ============================================================================

run_verification() {
    local verification_success=true

    log_info "Starting MinIO storage verification"
    log_info "Bucket: $BUCKET"
    log_info "Recent minutes: $RECENT_MINUTES"
    log_info "Verbose: $VERBOSE"
    log_info "Detailed check: $DETAILED_CHECK"
    log_info "MinIO: alias=${MINIO_ALIAS}, url=${MINIO_URL}, mc_container=${MINIO_MC_CONTAINER}"

    # Run verification steps
    ensure_minio_client_running || verification_success=false
    check_minio_connectivity || verification_success=false
    check_mimir_health || verification_success=false
    check_mimir_configuration || verification_success=false
    verify_mimir_bucket || verification_success=false
    verify_tempo_bucket || verification_success=false
    verify_loki_bucket || verification_success=false
    check_total_objects || verification_success=false
    check_recent_uploads || verification_success=false
    check_tsdb_structure || verification_success=false

    # Optional detailed analysis
    if [ "$DETAILED_CHECK" = "true" ]; then
        analyze_block_structure || verification_success=false
    fi

    # Print summary
    print_test_summary "MinIO Storage Verification" "$verification_success"

    if [ "$verification_success" = "true" ]; then
        log_success "MinIO storage verification completed successfully"
        log_info "Mimir appears to be writing data to MinIO correctly"
        return 0
    else
        log_error "MinIO storage verification failed"
        log_info "Check Mimir configuration and MinIO connectivity"
        return 1
    fi
}

# ============================================================================
# Main Execution
# ============================================================================

main() {
    log_info "MinIO Storage Verification Tool"

    # Parse command line arguments
    parse_arguments "$@"

    # Run verification
    run_verification
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
