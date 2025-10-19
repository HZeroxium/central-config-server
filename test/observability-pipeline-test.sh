#!/bin/bash

# ============================================================================
# Observability Pipeline Test Script
# ============================================================================
# Tests the complete observability pipeline: Logs, Metrics, Traces
# Verifies data flows from services -> Alloy/Prometheus -> Loki/Mimir/Tempo -> Grafana
# ============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
TOTAL=0

# Helper functions
print_header() {
    echo -e "\n${BLUE}============================================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}============================================================================${NC}\n"
}

print_test() {
    echo -e "${YELLOW}Testing:${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASSED++))
    ((TOTAL++))
}

print_failure() {
    echo -e "${RED}✗${NC} $1"
    ((FAILED++))
    ((TOTAL++))
}

wait_for_data() {
    local max_wait=$1
    local check_command=$2
    local description=$3
    
    echo -n "  Waiting for $description (max ${max_wait}s)... "
    
    for i in $(seq 1 $max_wait); do
        if eval "$check_command" &>/dev/null; then
            echo -e "${GREEN}found after ${i}s${NC}"
            return 0
        fi
        sleep 1
        echo -n "."
    done
    
    echo -e "${RED}timeout${NC}"
    return 1
}

# ============================================================================
# Main Tests
# ============================================================================

print_header "Observability Pipeline Test"

# Prerequisites check
print_header "Prerequisites Check"

print_test "Loki is ready"
if curl -s "http://localhost:23100/ready" | grep -q "ready"; then
    print_success "Loki is ready"
else
    print_failure "Loki is not ready"
    exit 1
fi

print_test "Tempo is ready"
if curl -s "http://localhost:23200/ready" | grep -q "ready"; then
    print_success "Tempo is ready"
else
    print_failure "Tempo is not ready"
    exit 1
fi

print_test "Mimir is ready"
if curl -s "http://localhost:23009/ready" | grep -q "ready"; then
    print_success "Mimir is ready"
else
    print_failure "Mimir is not ready"
    exit 1
fi

print_test "Prometheus is healthy"
if curl -s "http://localhost:23090/-/healthy" | grep -q "Prometheus"; then
    print_success "Prometheus is healthy"
else
    print_failure "Prometheus is not healthy"
    exit 1
fi

print_test "Grafana is healthy"
if curl -s "http://localhost:23000/api/health" | grep -q "ok"; then
    print_success "Grafana is healthy"
else
    print_failure "Grafana is not healthy"
    exit 1
fi

# Logs Pipeline Test
print_header "Logs Pipeline Test (Docker -> Alloy -> Loki)"

print_test "Generate test log entry"
docker run --rm --network infra-network alpine:latest echo "TEST_LOG_ENTRY_$(date +%s)" > /dev/null 2>&1
print_success "Test log generated"

print_test "Query logs from Loki"
if wait_for_data 30 "curl -s 'http://localhost:23100/loki/api/v1/query?query={job=\"docker\"}' | grep -q 'values'" "logs in Loki"; then
    print_success "Logs are being ingested into Loki"
else
    print_failure "Logs are not appearing in Loki"
fi

# Metrics Pipeline Test
print_header "Metrics Pipeline Test (Services -> Prometheus -> Mimir)"

print_test "Check Prometheus targets"
if targets=$(curl -s "http://localhost:23090/api/v1/targets"); then
    up_targets=$(echo "$targets" | grep -o '"health":"up"' | wc -l)
    if [ "$up_targets" -gt 0 ]; then
        print_success "Prometheus has $up_targets active targets"
    else
        print_failure "Prometheus has no active targets"
    fi
else
    print_failure "Could not query Prometheus targets"
fi

print_test "Query metrics from Prometheus"
if metrics=$(curl -s "http://localhost:23090/api/v1/query?query=up"); then
    if echo "$metrics" | grep -q '"status":"success"'; then
        print_success "Metrics are available in Prometheus"
    else
        print_failure "No metrics found in Prometheus"
    fi
else
    print_failure "Could not query Prometheus metrics"
fi

print_test "Check Prometheus remote write to Mimir"
if curl -s "http://localhost:23090/api/v1/status/config" | grep -q "mimir:9009"; then
    print_success "Prometheus is configured to remote write to Mimir"
else
    print_failure "Prometheus remote write to Mimir not configured"
fi

print_test "Query metrics from Mimir"
if wait_for_data 30 "curl -s 'http://localhost:23009/prometheus/api/v1/query?query=up' | grep -q 'success'" "metrics in Mimir"; then
    print_success "Metrics are being written to Mimir"
else
    print_failure "Metrics are not appearing in Mimir"
fi

# Traces Pipeline Test (Simulated)
print_header "Traces Pipeline Test (OTLP -> Alloy -> Tempo)"

print_test "Check Tempo API"
if curl -s "http://localhost:23200/api/search" | grep -q "traces"; then
    print_success "Tempo API is responding"
else
    # Tempo might return empty result, which is OK
    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:23200/api/search" | grep -q "200"; then
        print_success "Tempo API is responding (no traces yet)"
    else
        print_failure "Tempo API is not responding"
    fi
fi

print_test "Check Alloy OTLP receivers"
if docker exec alloy wget -q -O- http://localhost:12345/metrics 2>/dev/null | grep -q "otelcol_receiver"; then
    print_success "Alloy OTLP receivers are configured"
else
    print_failure "Alloy OTLP receivers not found"
fi

# Grafana Datasources Test
print_header "Grafana Datasources Test"

print_test "Check Prometheus datasource in Grafana"
if curl -s -u admin:admin "http://localhost:23000/api/datasources/name/Prometheus" | grep -q '"type":"prometheus"'; then
    print_success "Prometheus datasource configured in Grafana"
else
    print_failure "Prometheus datasource not found in Grafana"
fi

print_test "Check Mimir datasource in Grafana"
if curl -s -u admin:admin "http://localhost:23000/api/datasources/name/Mimir" | grep -q '"type":"prometheus"'; then
    print_success "Mimir datasource configured in Grafana"
else
    print_failure "Mimir datasource not found in Grafana"
fi

print_test "Check Loki datasource in Grafana"
if curl -s -u admin:admin "http://localhost:23000/api/datasources/name/Loki" | grep -q '"type":"loki"'; then
    print_success "Loki datasource configured in Grafana"
else
    print_failure "Loki datasource not found in Grafana"
fi

print_test "Check Tempo datasource in Grafana"
if curl -s -u admin:admin "http://localhost:23000/api/datasources/name/Tempo" | grep -q '"type":"tempo"'; then
    print_success "Tempo datasource configured in Grafana"
else
    print_failure "Tempo datasource not found in Grafana"
fi

# Test Grafana datasource health
print_header "Grafana Datasource Health Test"

print_test "Test Prometheus datasource health"
if curl -s -u admin:admin "http://localhost:23000/api/datasources/proxy/1/api/v1/query?query=up" | grep -q "success"; then
    print_success "Prometheus datasource is healthy"
else
    print_failure "Prometheus datasource health check failed"
fi

print_test "Test Loki datasource health"
if curl -s -u admin:admin "http://localhost:23000/api/datasources/proxy/uid/loki/ready" | grep -q "ready"; then
    print_success "Loki datasource is healthy"
else
    print_failure "Loki datasource health check failed"
fi

# Correlation Test
print_header "Observability Correlation Test"

print_test "Check trace-to-logs correlation in Tempo datasource"
if curl -s -u admin:admin "http://localhost:23000/api/datasources/name/Tempo" | grep -q "tracesToLogsV2"; then
    print_success "Trace-to-logs correlation configured"
else
    print_failure "Trace-to-logs correlation not configured"
fi

print_test "Check trace-to-metrics correlation in Tempo datasource"
if curl -s -u admin:admin "http://localhost:23000/api/datasources/name/Tempo" | grep -q "tracesToMetrics"; then
    print_success "Trace-to-metrics correlation configured"
else
    print_failure "Trace-to-metrics correlation not configured"
fi

# Summary
print_header "Observability Pipeline Summary"

echo -e "Total tests: ${BLUE}$TOTAL${NC}"
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"

if [ $FAILED -eq 0 ]; then
    echo -e "\n${GREEN}✓ Observability pipeline is fully functional!${NC}\n"
    echo -e "${BLUE}Access URLs:${NC}"
    echo "  Grafana:    http://localhost:23000 (admin/admin)"
    echo "  Prometheus: http://localhost:23090"
    echo "  Loki:       http://localhost:23100"
    echo "  Tempo:      http://localhost:23200"
    echo "  Mimir:      http://localhost:23009"
    echo "  Alloy:      http://localhost:22345"
    echo ""
    exit 0
else
    echo -e "\n${RED}✗ Some observability pipeline tests failed.${NC}\n"
    echo -e "${YELLOW}Troubleshooting tips:${NC}"
    echo "  1. Check Alloy logs: docker logs alloy"
    echo "  2. Check Prometheus config: curl http://localhost:23090/api/v1/status/config"
    echo "  3. Check Grafana datasources: curl -u admin:admin http://localhost:23000/api/datasources"
    echo "  4. Verify network connectivity: ./test/network-connectivity-test.sh"
    echo "  5. Check service logs: docker compose -f docker-compose.infra.yml logs <service>"
    echo ""
    exit 1
fi

