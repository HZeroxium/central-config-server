#!/bin/bash

# ============================================================================
# Infrastructure Health Check Script
# ============================================================================
# Validates all infrastructure services are running and healthy
# Port range: 20000-25000
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

check_http_endpoint() {
    local name=$1
    local url=$2
    local expected_code=${3:-200}
    
    print_test "$name - $url"
    
    if response=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$url" 2>/dev/null); then
        if [ "$response" -eq "$expected_code" ]; then
            print_success "$name is healthy (HTTP $response)"
            return 0
        else
            print_failure "$name returned HTTP $response (expected $expected_code)"
            return 1
        fi
    else
        print_failure "$name is unreachable"
        return 1
    fi
}

check_tcp_port() {
    local name=$1
    local host=$2
    local port=$3
    
    print_test "$name - $host:$port"
    
    if timeout 5 bash -c "cat < /dev/null > /dev/tcp/$host/$port" 2>/dev/null; then
        print_success "$name port $port is open"
        return 0
    else
        print_failure "$name port $port is not accessible"
        return 1
    fi
}

# ============================================================================
# Main Health Checks
# ============================================================================

print_header "Infrastructure Health Check"

# Data Stores
print_header "Data Stores"

check_tcp_port "MongoDB" "localhost" "20017"
check_http_endpoint "MongoExpress" "http://localhost:20081" "401"  # Basic auth required
check_tcp_port "Redis" "localhost" "20379"
check_http_endpoint "RedisInsight" "http://localhost:20001"

# Messaging
print_header "Messaging"

check_tcp_port "Kafka" "localhost" "20092"
check_http_endpoint "KafbatUI" "http://localhost:20084"

# Service Discovery
print_header "Service Discovery"

check_http_endpoint "Consul" "http://localhost:20500/v1/status/leader"
check_http_endpoint "Consul UI" "http://localhost:20500/ui/"

# Object Storage
print_header "Object Storage"

check_http_endpoint "MinIO Health" "http://localhost:20000/minio/health/live"
check_http_endpoint "MinIO Console" "http://localhost:20002"

# Observability - LGTM Stack
print_header "Observability Stack"

check_http_endpoint "Loki Ready" "http://localhost:23100/ready"
check_http_endpoint "Grafana Health" "http://localhost:23000/api/health"
check_http_endpoint "Tempo Ready" "http://localhost:23200/ready"
check_http_endpoint "Mimir Ready" "http://localhost:23009/ready"
check_http_endpoint "Alloy Metrics" "http://localhost:22345/metrics"
check_http_endpoint "Prometheus Healthy" "http://localhost:23090/-/healthy"
check_http_endpoint "cAdvisor Health" "http://localhost:20082/healthz"

# Additional Checks
print_header "Additional Checks"

# Check Docker containers
print_test "Docker containers status"
if docker compose -f docker-compose.infra.yml ps | grep -q "Up"; then
    print_success "Infrastructure containers are running"
else
    print_failure "Some infrastructure containers are not running"
fi

# Check network
print_test "Docker network existence"
if docker network ls | grep -q "infra-network"; then
    print_success "infra-network exists"
else
    print_failure "infra-network does not exist"
fi

# Check Grafana datasources
print_test "Grafana datasources"
if datasources=$(curl -s -u admin:admin "http://localhost:23000/api/datasources" 2>/dev/null); then
    datasource_count=$(echo "$datasources" | grep -o '"name"' | wc -l)
    if [ "$datasource_count" -ge 4 ]; then
        print_success "Grafana has $datasource_count datasources configured"
    else
        print_failure "Grafana has only $datasource_count datasources (expected >= 4)"
    fi
else
    print_failure "Could not retrieve Grafana datasources"
fi

# Check Prometheus targets
print_test "Prometheus targets"
if targets=$(curl -s "http://localhost:23090/api/v1/targets" 2>/dev/null); then
    active_targets=$(echo "$targets" | grep -o '"health":"up"' | wc -l)
    print_success "Prometheus has $active_targets active targets"
else
    print_failure "Could not retrieve Prometheus targets"
fi

# Summary
print_header "Health Check Summary"

echo -e "Total tests: ${BLUE}$TOTAL${NC}"
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"

if [ $FAILED -eq 0 ]; then
    echo -e "\n${GREEN}✓ All infrastructure services are healthy!${NC}\n"
    exit 0
else
    echo -e "\n${RED}✗ Some infrastructure services have issues. Please check the logs.${NC}\n"
    echo -e "${YELLOW}Troubleshooting tips:${NC}"
    echo "  1. Check container logs: docker compose -f docker-compose.infra.yml logs <service-name>"
    echo "  2. Restart failed services: docker compose -f docker-compose.infra.yml restart <service-name>"
    echo "  3. Check port availability: netstat -tuln | grep <port>"
    echo "  4. View container status: docker compose -f docker-compose.infra.yml ps"
    echo ""
    exit 1
fi

