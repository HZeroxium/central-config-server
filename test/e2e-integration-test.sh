#!/bin/bash

# ============================================================================
# End-to-End Integration Test Script
# ============================================================================
# Complete integration test for infrastructure + local services
# Tests the full stack deployment and critical endpoints
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

# Test mode
CLEANUP=${CLEANUP:-true}

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

wait_for_health() {
    local service=$1
    local url=$2
    local max_wait=${3:-120}
    
    echo -n "  Waiting for $service to be healthy (max ${max_wait}s)... "
    
    for i in $(seq 1 $max_wait); do
        if curl -s -f "$url" &>/dev/null; then
            echo -e "${GREEN}ready after ${i}s${NC}"
            return 0
        fi
        sleep 1
        if [ $((i % 10)) -eq 0 ]; then
            echo -n "${i}s..."
        fi
    done
    
    echo -e "${RED}timeout${NC}"
    return 1
}

cleanup() {
    if [ "$CLEANUP" = "true" ]; then
        print_header "Cleanup"
        echo "Stopping local services..."
        docker compose -f docker-compose.config-server-min.yml down -v 2>/dev/null || true
        echo "Stopping infrastructure..."
        docker compose -f docker-compose.infra.yml down -v 2>/dev/null || true
        echo "Cleanup completed."
    else
        echo "Skipping cleanup (CLEANUP=false)"
    fi
}

# Trap to ensure cleanup on exit
trap cleanup EXIT INT TERM

# ============================================================================
# Main E2E Test Flow
# ============================================================================

print_header "End-to-End Integration Test"

# Step 1: Deploy Infrastructure
print_header "Step 1: Deploy Infrastructure"

print_test "Stopping any existing infrastructure"
docker compose -f docker-compose.infra.yml down -v 2>/dev/null || true
print_success "Cleaned up existing infrastructure"

print_test "Starting infrastructure services"
if docker compose -f docker-compose.infra.yml up -d; then
    print_success "Infrastructure services started"
else
    print_failure "Failed to start infrastructure services"
    exit 1
fi

# Wait for critical infrastructure services
print_test "Waiting for infrastructure services to be healthy"

services_health=(
    "MongoDB:http://localhost:20017"
    "Redis:http://localhost:20379"
    "Consul:http://localhost:20500/v1/status/leader"
    "Kafka:http://localhost:20092"
    "Loki:http://localhost:23100/ready"
    "Tempo:http://localhost:23200/ready"
    "Mimir:http://localhost:23009/ready"
    "Prometheus:http://localhost:23090/-/healthy"
    "Grafana:http://localhost:23000/api/health"
)

all_healthy=true
for service_url in "${services_health[@]}"; do
    IFS=':' read -r service url <<< "$service_url"
    if wait_for_health "$service" "$url" 120; then
        print_success "$service is healthy"
    else
        print_failure "$service failed to become healthy"
        all_healthy=false
    fi
done

if [ "$all_healthy" = "false" ]; then
    echo -e "${RED}Some infrastructure services failed to start. Check logs:${NC}"
    echo "  docker compose -f docker-compose.infra.yml logs"
    exit 1
fi

# Step 2: Run Infrastructure Health Check
print_header "Step 2: Infrastructure Health Check"

if ./test/infra-health-check.sh; then
    print_success "Infrastructure health check passed"
else
    print_failure "Infrastructure health check failed"
    exit 1
fi

# Step 3: Run Network Connectivity Test
print_header "Step 3: Network Connectivity Test"

if ./test/network-connectivity-test.sh; then
    print_success "Network connectivity test passed"
else
    print_failure "Network connectivity test failed"
    exit 1
fi

# Step 4: Deploy Local Services
print_header "Step 4: Deploy Local Services"

print_test "Stopping any existing local services"
docker compose -f docker-compose.config-server-min.yml down 2>/dev/null || true
print_success "Cleaned up existing local services"

print_test "Starting local services"
if docker compose -f docker-compose.config-server-min.yml up -d; then
    print_success "Local services started"
else
    print_failure "Failed to start local services"
    exit 1
fi

# Wait for local services
print_test "Waiting for local services to be healthy"

local_services=(
    "config-server:http://localhost:8888/actuator/health"
    "sample-service:http://localhost:8080/actuator/health"
    "config-control-service:http://localhost:8081/actuator/health"
)

for service_url in "${local_services[@]}"; do
    IFS=':' read -r service url <<< "$service_url"
    if wait_for_health "$service" "$url" 120; then
        print_success "$service is healthy"
    else
        print_failure "$service failed to become healthy"
        all_healthy=false
    fi
done

# Step 5: Test Critical Endpoints
print_header "Step 5: Test Critical Endpoints"

# Config Server endpoints
print_test "Config Server - Health"
if curl -s -f "http://localhost:8888/actuator/health" | grep -q "UP"; then
    print_success "Config Server health endpoint working"
else
    print_failure "Config Server health endpoint failed"
fi

# Config Control Service endpoints
print_test "Config Control Service - Health"
if curl -s -f "http://localhost:8081/actuator/health" | grep -q "UP"; then
    print_success "Config Control Service health endpoint working"
else
    print_failure "Config Control Service health endpoint failed"
fi

print_test "Config Control Service - Registry Services"
if curl -s -f "http://localhost:8081/api/registry/services"; then
    print_success "Registry services endpoint working"
else
    print_failure "Registry services endpoint failed"
fi

print_test "Config Control Service - Config Server Health"
if curl -s -f "http://localhost:8081/api/config-server/health"; then
    print_success "Config server proxy endpoint working"
else
    print_failure "Config server proxy endpoint failed"
fi

print_test "Config Control Service - Cache Health"
if curl -s -f "http://localhost:8081/api/cache/health"; then
    print_success "Cache health endpoint working"
else
    print_failure "Cache health endpoint failed"
fi

# Test heartbeat endpoint (POST)
print_test "Config Control Service - Heartbeat (POST)"
heartbeat_payload='{
  "serviceName": "test-service",
  "instanceId": "test-instance-1",
  "configHash": "test-hash-123",
  "host": "localhost",
  "port": 8080,
  "environment": "test",
  "version": "1.0.0"
}'

if curl -s -X POST "http://localhost:8081/api/heartbeat" \
    -H "Content-Type: application/json" \
    -d "$heartbeat_payload" | grep -q "status"; then
    print_success "Heartbeat endpoint working"
else
    print_failure "Heartbeat endpoint failed"
fi

# Sample Service endpoints
print_test "Sample Service - Health"
if curl -s -f "http://localhost:8080/actuator/health" | grep -q "UP"; then
    print_success "Sample Service health endpoint working"
else
    print_failure "Sample Service health endpoint failed"
fi

# Step 6: Test Observability Pipeline
print_header "Step 6: Test Observability Pipeline"

# Give some time for metrics/logs to be collected
echo "Waiting 30s for observability data collection..."
sleep 30

if ./test/observability-pipeline-test.sh; then
    print_success "Observability pipeline test passed"
else
    print_failure "Observability pipeline test failed"
fi

# Step 7: Verify Service Registration in Consul
print_header "Step 7: Verify Service Registration"

print_test "Check services registered in Consul"
if services=$(curl -s "http://localhost:20500/v1/catalog/services"); then
    service_count=$(echo "$services" | grep -o '"[^"]*":' | wc -l)
    print_success "Found $service_count services registered in Consul"
    
    # Check for specific services
    if echo "$services" | grep -q "sample-service"; then
        print_success "sample-service is registered in Consul"
    else
        print_failure "sample-service is not registered in Consul"
    fi
else
    print_failure "Could not query Consul services"
fi

# Step 8: Verify Metrics in Prometheus
print_header "Step 8: Verify Metrics Collection"

print_test "Query application metrics from Prometheus"
if metrics=$(curl -s "http://localhost:23090/api/v1/query?query=up{job=~\".*service.*\"}"); then
    if echo "$metrics" | grep -q '"status":"success"'; then
        metric_count=$(echo "$metrics" | grep -o '"metric"' | wc -l)
        print_success "Found $metric_count service metrics in Prometheus"
    else
        print_failure "No service metrics found in Prometheus"
    fi
else
    print_failure "Could not query Prometheus for service metrics"
fi

# Step 9: Verify Logs in Loki
print_header "Step 9: Verify Logs Collection"

print_test "Query application logs from Loki"
if logs=$(curl -s "http://localhost:23100/loki/api/v1/query?query={job=\"docker\"}"); then
    if echo "$logs" | grep -q '"status":"success"'; then
        print_success "Application logs are being collected in Loki"
    else
        print_failure "No application logs found in Loki"
    fi
else
    print_failure "Could not query Loki for application logs"
fi

# Summary
print_header "E2E Integration Test Summary"

echo -e "Total tests: ${BLUE}$TOTAL${NC}"
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"

if [ $FAILED -eq 0 ]; then
    echo -e "\n${GREEN}✓ All E2E integration tests passed!${NC}\n"
    echo -e "${BLUE}System is fully operational. Access URLs:${NC}"
    echo ""
    echo "Infrastructure:"
    echo "  Consul:        http://localhost:20500"
    echo "  MongoExpress:  http://localhost:20081 (admin/admin123)"
    echo "  RedisInsight:  http://localhost:20001"
    echo "  KafbatUI:      http://localhost:20084"
    echo "  MinIO Console: http://localhost:20002 (minioadmin/minioadmin)"
    echo ""
    echo "Observability:"
    echo "  Grafana:       http://localhost:23000 (admin/admin)"
    echo "  Prometheus:    http://localhost:23090"
    echo "  Loki:          http://localhost:23100"
    echo "  Tempo:         http://localhost:23200"
    echo "  Mimir:         http://localhost:23009"
    echo ""
    echo "Services:"
    echo "  Config Server:         http://localhost:8888"
    echo "  Sample Service:        http://localhost:8080"
    echo "  Config Control Service: http://localhost:8081"
    echo ""
    exit 0
else
    echo -e "\n${RED}✗ Some E2E integration tests failed.${NC}\n"
    echo -e "${YELLOW}Troubleshooting:${NC}"
    echo "  1. Check infrastructure logs: docker compose -f docker-compose.infra.yml logs"
    echo "  2. Check local service logs: docker compose -f docker-compose.config-server-min.yml logs"
    echo "  3. Verify network: docker network inspect infra-network"
    echo "  4. Check container status: docker ps -a"
    echo ""
    exit 1
fi

