#!/bin/bash
# Test utilities for observability flow testing
# Provides common functions for querying services, waiting for data, and logging

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration - load from environment or use defaults
LOKI_URL="${LOKI_URL:-http://localhost:23100}"
TEMPO_URL="${TEMPO_URL:-http://localhost:24317}"
MIMIR_URL="${MIMIR_URL:-http://localhost:23009}"
GRAFANA_URL="${GRAFANA_URL:-http://localhost:23000}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:23090}"
ALLOY_URL="${ALLOY_URL:-http://localhost:22345}"
MINIO_ENDPOINT="${MINIO_ENDPOINT:-localhost:20000}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-minioadmin}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-minioadmin}"

# Test timing
TEST_START_TIME=$(date +%s)
STEP_TIMES=()

# ============================================================================
# Logging Functions
# ============================================================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    local step="$1"
    local description="$2"
    echo -e "\n${BLUE}=== STEP: $step ===${NC}"
    echo -e "${BLUE}Description:${NC} $description"
    local step_start=$(date +%s)
    STEP_TIMES+=("$step_start:$step")
}

log_step_complete() {
    local step="$1"
    local success="$2"
    local step_start=$(date +%s)
    local duration=$((step_start - $(echo "$step_start" | head -c10)))
    
    if [ "$success" = "true" ]; then
        log_success "Step '$step' completed in ${duration}s"
    else
        log_error "Step '$step' failed after ${duration}s"
    fi
}

# ============================================================================
# Test Data Generation
# ============================================================================

generate_test_id() {
    # Generate a unique test ID for tagging test data
    echo "test-$(date +%Y%m%d-%H%M%S)-$(uuidgen | cut -d'-' -f1)"
}

generate_test_log() {
    local test_id="$1"
    local service_name="${2:-test-service}"
    local level="${3:-INFO}"
    
    echo "{\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\",\"level\":\"$level\",\"service\":\"$service_name\",\"message\":\"Test log message for flow testing\",\"test_id\":\"$test_id\",\"trace_id\":\"$(uuidgen)\",\"span_id\":\"$(uuidgen | cut -d'-' -f1)\"}"
}

send_otlp_metric() {
    local endpoint="$1"
    local test_id="$2"
    
    local payload=$(cat <<EOF
{
  "resourceMetrics": [{
    "resource": {
      "attributes": [{
        "key": "service.name",
        "value": {"stringValue": "test-service"}
      }]
    },
    "scopeMetrics": [{
      "scope": {"name": "test-scope"},
      "metrics": [{
        "name": "test_counter",
        "unit": "1",
        "sum": {
          "dataPoints": [{
            "asInt": "42",
            "timeUnixNano": "$(date +%s)000000000",
            "attributes": [{
              "key": "test_id",
              "value": {"stringValue": "$test_id"}
            }]
          }],
          "aggregationTemporality": 2,
          "isMonotonic": true
        }
      }]
    }]
  }]
}
EOF
)
    
    local response=$(curl -s -X POST "$endpoint/v1/metrics" \
        -H "Content-Type: application/json" \
        -d "$payload" \
        -w "\n%{http_code}" 2>&1)
    
    local status_code=$(echo "$response" | tail -n1)
    
    if [ "$status_code" = "200" ] || [ "$status_code" = "202" ]; then
        log_success "OTLP metric sent to $endpoint"
        return 0
    else
        log_error "Failed to send OTLP metric (HTTP $status_code)"
        return 1
    fi
}

# ============================================================================
# Wait Functions
# ============================================================================

wait_for_data() {
    local query_func="$1"
    local expected_count="$2"
    local timeout="${3:-60}"
    local interval="${4:-2}"
    
    log_info "Waiting for data (expected: $expected_count, timeout: ${timeout}s)"
    
    local elapsed=0
    while [ $elapsed -lt $timeout ]; do
        local count=$($query_func)
        if [ "$count" -ge "$expected_count" ]; then
            log_success "Data found: $count items (expected: $expected_count)"
            return 0
        fi
        
        log_info "Found $count items, waiting... (${elapsed}s elapsed)"
        sleep $interval
        elapsed=$((elapsed + interval))
    done
    
    log_error "Timeout waiting for data (expected: $expected_count, found: $count)"
    return 1
}

wait_for_service() {
    local service_url="$1"
    local endpoint="${2:-/health}"
    local timeout="${3:-30}"
    
    log_info "Waiting for service at $service_url"
    
    local elapsed=0
    while [ $elapsed -lt $timeout ]; do
        if curl -s -f "$service_url$endpoint" >/dev/null 2>&1; then
            log_success "Service is ready at $service_url"
            return 0
        fi
        
        log_info "Service not ready, waiting... (${elapsed}s elapsed)"
        sleep 2
        elapsed=$((elapsed + 2))
    done
    
    log_error "Timeout waiting for service at $service_url"
    return 1
}

# ============================================================================
# Query Functions
# ============================================================================

query_loki() {
    local query="$1"
    local start_time="${2:-$(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%SZ)}"
    local end_time="${3:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"
    
    local response=$(curl -s -G \
        --data-urlencode "query=$query" \
        --data-urlencode "start=$start_time" \
        --data-urlencode "end=$end_time" \
        "$LOKI_URL/loki/api/v1/query_range" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$response" ]; then
        echo "$response" | jq -r '.data.result | length' 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

query_mimir() {
    local query="$1"
    local time="${2:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"
    
    local response=$(curl -s -G \
        --data-urlencode "query=$query" \
        --data-urlencode "time=$time" \
        "$MIMIR_URL/prometheus/api/v1/query" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$response" ]; then
        echo "$response" | jq -r '.data.result | length' 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

query_prometheus() {
    local query="$1"
    local time="${2:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"
    
    local response=$(curl -s -G \
        --data-urlencode "query=$query" \
        --data-urlencode "time=$time" \
        "$PROMETHEUS_URL/api/v1/query" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$response" ]; then
        echo "$response" | jq -r '.data.result | length' 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

query_tempo() {
    local trace_id="$1"
    
    local response=$(curl -s \
        "$TEMPO_URL/api/traces/$trace_id" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$response" ]; then
        echo "$response" | jq -r '.resourceSpans | length' 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

query_grafana() {
    local datasource="$1"
    local query="$2"
    
    # This is a simplified version - in practice you'd need to authenticate with Grafana
    log_warning "Grafana query not implemented - requires authentication"
    echo "0"
}

# ============================================================================
# MinIO Functions
# ============================================================================

check_minio_bucket() {
    local bucket="$1"
    local pattern="${2:-.*}"
    
    # Use minio-client container with pre-configured credentials
    local count=$(docker exec minio-client mc ls "local/$bucket/" 2>/dev/null | grep -E "$pattern" | wc -l)
    echo "$count"
}

check_minio_recent_uploads() {
    local bucket="$1"
    local minutes="${2:-5}"
    
    # Use mc find with --newer-than to check recent uploads
    local count=$(docker exec minio-client mc find "local/$bucket/" --newer-than "${minutes}m" 2>/dev/null | wc -l)
    echo "$count"
}

# ============================================================================
# OTLP Functions
# ============================================================================

send_otlp_metric() {
    local alloy_endpoint="$1"
    local test_id="$2"
    
    # Create a simple counter metric in OTLP format
    local metric_data='{
        "resourceMetrics": [{
            "resource": {
                "attributes": [{
                    "key": "service.name",
                    "value": {"stringValue": "test-service"}
                }]
            },
            "scopeMetrics": [{
                "scope": {"name": "test-scope"},
                "metrics": [{
                    "name": "test_counter",
                    "description": "Test counter metric",
                    "unit": "1",
                    "sum": {
                        "dataPoints": [{
                            "attributes": [{
                                "key": "test_id",
                                "value": {"stringValue": "'$test_id'"}
                            }],
                            "timeUnixNano": "'$(date +%s%9N)'",
                            "asInt": "1"
                        }],
                        "aggregationTemporality": "AGGREGATION_TEMPORALITY_CUMULATIVE",
                        "isMonotonic": true
                    }
                }]
            }]
        }]
    }'
    
    local response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$metric_data" \
        "$alloy_endpoint/v1/metrics" 2>/dev/null)
    
    if [ $? -eq 0 ]; then
        log_success "OTLP metric sent to $alloy_endpoint"
        return 0
    else
        log_error "Failed to send OTLP metric to $alloy_endpoint"
        return 1
    fi
}

send_otlp_trace() {
    local alloy_endpoint="$1"
    local test_id="$2"
    
    # Create a simple trace in OTLP format
    local trace_data='{
        "resourceSpans": [{
            "resource": {
                "attributes": [{
                    "key": "service.name",
                    "value": {"stringValue": "test-service"}
                }]
            },
            "scopeSpans": [{
                "scope": {"name": "test-scope"},
                "spans": [{
                    "traceId": "'$(uuidgen | tr -d '-')'",
                    "spanId": "'$(uuidgen | tr -d '-' | cut -c1-16)'",
                    "name": "test-span",
                    "startTimeUnixNano": "'$(date +%s%9N)'",
                    "endTimeUnixNano": "'$(date +%s%9N)'",
                    "attributes": [{
                        "key": "test_id",
                        "value": {"stringValue": "'$test_id'"}
                    }]
                }]
            }]
        }]
    }'
    
    local response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$trace_data" \
        "$alloy_endpoint/v1/traces" 2>/dev/null)
    
    if [ $? -eq 0 ]; then
        log_success "OTLP trace sent to $alloy_endpoint"
        return 0
    else
        log_error "Failed to send OTLP trace to $alloy_endpoint"
        return 1
    fi
}

# ============================================================================
# Test Summary Functions
# ============================================================================

print_test_summary() {
    local test_name="$1"
    local success="$2"
    local total_time=$(($(date +%s) - TEST_START_TIME))
    
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}  Test Summary: $test_name${NC}"
    echo -e "${BLUE}========================================${NC}"
    
    if [ "$success" = "true" ]; then
        log_success "Test PASSED in ${total_time}s"
    else
        log_error "Test FAILED in ${total_time}s"
    fi
    
    echo -e "${BLUE}Total time:${NC} ${total_time}s"
    echo -e "${BLUE}End time:${NC} $(date)"
}

# ============================================================================
# Utility Functions
# ============================================================================

check_dependencies() {
    local missing_deps=()
    
    command -v curl >/dev/null 2>&1 || missing_deps+=("curl")
    command -v jq >/dev/null 2>&1 || missing_deps+=("jq")
    command -v uuidgen >/dev/null 2>&1 || missing_deps+=("uuidgen")
    
    if [ ${#missing_deps[@]} -gt 0 ]; then
        log_error "Missing dependencies: ${missing_deps[*]}"
        log_error "Please install missing dependencies and try again"
        return 1
    fi
    
    log_success "All dependencies available"
    return 0
}

validate_service_urls() {
    local urls=("$LOKI_URL" "$TEMPO_URL" "$MIMIR_URL" "$GRAFANA_URL" "$PROMETHEUS_URL")
    local failed=()
    
    for url in "${urls[@]}"; do
        if ! curl -s -f "$url" >/dev/null 2>&1; then
            failed+=("$url")
        fi
    done
    
    if [ ${#failed[@]} -gt 0 ]; then
        log_error "Unreachable services: ${failed[*]}"
        return 1
    fi
    
    log_success "All services reachable"
    return 0
}

# Export functions for use in other scripts
export -f log_info log_success log_warning log_error log_step log_step_complete
export -f generate_test_id generate_test_log send_otlp_metric
export -f wait_for_data wait_for_service
export -f query_loki query_mimir query_prometheus query_tempo query_grafana
export -f check_minio_bucket check_minio_recent_uploads
export -f send_otlp_metric send_otlp_trace
export -f print_test_summary check_dependencies validate_service_urls
