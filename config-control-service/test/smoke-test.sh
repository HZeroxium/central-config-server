#!/bin/bash
# Smoke test for observability stack
# Quick health check for all services

set -e

echo "========================================="
echo "Smoke Testing Observability Stack"
echo "========================================="

# Wait for services to stabilize
echo ""
echo "Waiting 15 seconds for services to stabilize..."
sleep 15

# Test each service
echo ""
echo "Testing Prometheus..."
curl -f http://localhost:9092/-/healthy || { echo "❌ Prometheus failed"; exit 1; }
echo "✅ Prometheus healthy"

echo ""
echo "Testing Mimir..."
curl -f http://localhost:9009/ready || { echo "❌ Mimir failed"; exit 1; }
echo "✅ Mimir healthy"

echo ""
echo "Testing Loki..."
curl -f http://localhost:3100/ready || { echo "❌ Loki failed"; exit 1; }
echo "✅ Loki healthy"

echo ""
echo "Testing Tempo..."
curl -f http://localhost:3200/ready || { echo "❌ Tempo failed"; exit 1; }
echo "✅ Tempo healthy"

echo ""
echo "Testing Alloy..."
curl -f http://localhost:12345/-/healthy || { echo "❌ Alloy failed"; exit 1; }
echo "✅ Alloy healthy"

echo ""
echo "Testing Grafana..."
curl -f http://localhost:3000/api/health || { echo "❌ Grafana failed"; exit 1; }
echo "✅ Grafana healthy"

echo ""
echo "Testing config-control-service..."
curl -f http://localhost:8081/actuator/health || { echo "❌ config-control-service failed"; exit 1; }
echo "✅ config-control-service healthy"

echo ""
echo "========================================="
echo "✅ All services are healthy!"
echo "========================================="

