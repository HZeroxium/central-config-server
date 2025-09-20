#!/bin/bash

# E2E Test Runner Script for User REST Spring Service
# This script runs end-to-end tests against the running system

set -e

echo "🚀 Starting E2E Test Runner for User REST Spring Service"
echo "=================================================="

# Check if Docker Compose is running
echo "📋 Checking if system is running..."
if ! docker-compose ps | grep -q "user-rest-spring-service.*Up"; then
    echo "❌ System is not running. Please start it first:"
    echo "   docker-compose up -d"
    echo "   Wait for all services to be healthy, then run this script again."
    exit 1
fi

echo "✅ System is running"

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check service health
echo "🏥 Checking service health..."
if ! curl -f http://localhost:8083/actuator/health > /dev/null 2>&1; then
    echo "❌ user-rest-spring-service is not healthy"
    exit 1
fi

echo "✅ All services are healthy"

# Run E2E tests
echo "🧪 Running E2E tests..."
cd "$(dirname "$0")"

# Run the E2E tests
./gradlew e2eTest --info

echo "✅ E2E tests completed successfully!"
echo "=================================================="
