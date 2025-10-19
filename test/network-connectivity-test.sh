#!/bin/bash

# ============================================================================
# Network Connectivity Test Script
# ============================================================================
# Verifies network connectivity between local services and infrastructure
# Tests DNS resolution and port connectivity within infra-network
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

# ============================================================================
# Main Tests
# ============================================================================

print_header "Network Connectivity Test"

# Check if infra-network exists
print_header "Network Existence Check"

print_test "infra-network existence"
if docker network ls | grep -q "infra-network"; then
    print_success "infra-network exists"
else
    print_failure "infra-network does not exist"
    echo -e "${RED}Please run: docker compose -f docker-compose.infra.yml up -d${NC}"
    exit 1
fi

# Get network details
print_test "infra-network details"
if network_info=$(docker network inspect infra-network 2>/dev/null); then
    subnet=$(echo "$network_info" | grep -o '"Subnet": "[^"]*"' | head -1 | cut -d'"' -f4)
    gateway=$(echo "$network_info" | grep -o '"Gateway": "[^"]*"' | head -1 | cut -d'"' -f4)
    print_success "Network subnet: $subnet, Gateway: $gateway"
else
    print_failure "Could not inspect infra-network"
fi

# DNS Resolution Tests
print_header "DNS Resolution Tests (from test container)"

# Services to test
services=(
    "mongodb:27017"
    "redis:6379"
    "kafka:9092"
    "consul:8500"
    "minio:9000"
    "loki:3100"
    "grafana:3000"
    "tempo:3200"
    "mimir:9009"
    "alloy:12345"
    "prometheus:9090"
    "cadvisor:8080"
)

# Run a temporary container on infra-network to test DNS resolution
print_test "Starting test container on infra-network"

for service in "${services[@]}"; do
    IFS=':' read -r host port <<< "$service"
    
    print_test "DNS resolution and connectivity: $host:$port"
    
    # Test DNS resolution and TCP connectivity
    if docker run --rm --network infra-network alpine:latest \
        sh -c "nc -zv -w 5 $host $port" 2>&1 | grep -q "open"; then
        print_success "$host:$port is resolvable and reachable"
    else
        print_failure "$host:$port is not resolvable or not reachable"
    fi
done

# Port Accessibility from Host
print_header "Port Accessibility from Host"

host_ports=(
    "20017:MongoDB"
    "20379:Redis"
    "20092:Kafka"
    "20500:Consul"
    "20000:MinIO API"
    "23100:Loki"
    "23000:Grafana"
    "23200:Tempo"
    "23009:Mimir"
    "22345:Alloy"
    "23090:Prometheus"
    "20082:cAdvisor"
)

for port_service in "${host_ports[@]}"; do
    IFS=':' read -r port service <<< "$port_service"
    
    print_test "Host port $port ($service)"
    
    if timeout 5 bash -c "cat < /dev/null > /dev/tcp/localhost/$port" 2>/dev/null; then
        print_success "Port $port ($service) is accessible from host"
    else
        print_failure "Port $port ($service) is not accessible from host"
    fi
done

# Container Network Connectivity
print_header "Container Network Connectivity"

# Check if containers are on the correct network
print_test "Containers on infra-network"

expected_containers=(
    "mongodb"
    "redis"
    "kafka"
    "consul"
    "minio"
    "loki"
    "grafana"
    "tempo"
    "mimir"
    "alloy"
    "prometheus"
    "cadvisor"
)

for container in "${expected_containers[@]}"; do
    if docker ps --filter "name=$container" --format "{{.Names}}" | grep -q "^$container$"; then
        if docker inspect "$container" 2>/dev/null | grep -q "infra-network"; then
            print_success "$container is on infra-network"
        else
            print_failure "$container is not on infra-network"
        fi
    else
        print_failure "$container is not running"
    fi
done

# Inter-service Communication Test
print_header "Inter-service Communication Test"

# Test if Prometheus can scrape metrics from other services
print_test "Prometheus -> Mimir connectivity"
if docker exec prometheus wget -q -O- http://mimir:9009/ready 2>/dev/null | grep -q "ready"; then
    print_success "Prometheus can reach Mimir"
else
    print_failure "Prometheus cannot reach Mimir"
fi

print_test "Alloy -> Loki connectivity"
if docker exec alloy wget -q -O- http://loki:3100/ready 2>/dev/null | grep -q "ready"; then
    print_success "Alloy can reach Loki"
else
    print_failure "Alloy cannot reach Loki"
fi

print_test "Grafana -> Prometheus connectivity"
if docker exec grafana wget -q -O- http://prometheus:9090/-/healthy 2>/dev/null | grep -q "Prometheus"; then
    print_success "Grafana can reach Prometheus"
else
    print_failure "Grafana cannot reach Prometheus"
fi

# Summary
print_header "Network Connectivity Summary"

echo -e "Total tests: ${BLUE}$TOTAL${NC}"
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"

if [ $FAILED -eq 0 ]; then
    echo -e "\n${GREEN}✓ All network connectivity tests passed!${NC}\n"
    exit 0
else
    echo -e "\n${RED}✗ Some network connectivity tests failed.${NC}\n"
    echo -e "${YELLOW}Troubleshooting tips:${NC}"
    echo "  1. Verify infra-network exists: docker network ls"
    echo "  2. Check container network: docker inspect <container-name> | grep NetworkMode"
    echo "  3. Restart infrastructure: docker compose -f docker-compose.infra.yml restart"
    echo "  4. Check firewall rules: sudo iptables -L"
    echo "  5. Verify DNS resolution: docker run --rm --network infra-network alpine nslookup <service>"
    echo ""
    exit 1
fi

