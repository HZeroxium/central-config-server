#!/bin/bash
# Full observability stack validation
# Tests logs, metrics, traces, and correlations

echo "========================================="
echo "Full Observability Stack Test"
echo "========================================="

# Test 1: Check all services healthy
echo ""
echo "1. Checking service health..."
docker compose -f docker-compose.config-server-min.yml ps

# Test 2: Test Prometheus
echo ""
echo "2. Testing Prometheus..."
echo "Querying Prometheus for 'up' metric..."
curl -s "http://localhost:9092/api/v1/query?query=up" | jq -r '.data.result[] | "\(.metric.job): \(.value[1])"'

# Test 3: Test Mimir
echo ""
echo "3. Testing Mimir..."
echo "Querying Mimir for 'up' metric..."
curl -s "http://localhost:9009/prometheus/api/v1/query?query=up" | jq -r '.data.result[] | "\(.metric.job): \(.value[1])"'

# Test 4: Test Loki
echo ""
echo "4. Testing Loki..."
curl -s http://localhost:3100/ready && echo "✅ Loki is ready"

# Test 5: Test Tempo
echo ""
echo "5. Testing Tempo..."
curl -s http://localhost:3200/ready && echo "✅ Tempo is ready"

# Test 6: Test Grafana
echo ""
echo "6. Testing Grafana..."
curl -s http://localhost:3000/api/health | jq .

# Test 7: Test Grafana datasources
echo ""
echo "7. Testing Grafana datasources..."
curl -s http://admin:admin@localhost:3000/api/datasources | jq -r '.[] | "\(.name): \(.type)"'

# Test 8: Test config-control-service metrics
echo ""
echo "8. Testing config-control-service metrics..."
curl -s http://localhost:8081/actuator/prometheus | head -30

# Test 9: Test heartbeat with trace correlation
echo ""
echo "9. Testing heartbeat endpoint with trace correlation..."
RESPONSE=$(curl -s -D - -X POST http://localhost:8081/api/heartbeat \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "test-service",
    "instanceId": "test-instance-obs-1",
    "configHash": "abc123def456",
    "host": "localhost",
    "port": 8080,
    "environment": "dev",
    "version": "1.0.0",
    "metadata": {
      "region": "us-east-1",
      "zone": "zone-a"
    }
  }')

echo "$RESPONSE"

TRACE_ID=$(echo "$RESPONSE" | grep -i "X-Trace-ID" | awk '{print $2}' | tr -d '\r')
echo ""
echo "Extracted Trace ID: $TRACE_ID"

# Test 10: Query Loki for trace
echo ""
echo "10. Querying Loki for logs with trace_id..."
sleep 3
LOKI_QUERY='{ container=~"config-control-service.*" } | json | trace_id != ""'
curl -s "http://localhost:3100/loki/api/v1/query_range?query=$(echo $LOKI_QUERY | jq -sRr @uri)&limit=10&start=$(date -u -d '5 minutes ago' +%s)000000000&end=$(date -u +%s)000000000" | jq '.data.result[] | .values[] | fromjson | {timestamp: .[0], message: .[1].message, trace_id: .[1].trace_id, level: .[1].level}'

# Test 11: Query Tempo for trace
if [ ! -z "$TRACE_ID" ]; then
  echo ""
  echo "11. Querying Tempo for trace $TRACE_ID..."
  curl -s "http://localhost:3200/api/traces/$TRACE_ID" | jq -r '.batches[]?.scopeSpans[]?.spans[]? | "Span: \(.name), Duration: \(.endTimeUnixNano - .startTimeUnixNano)ns"'
else
  echo ""
  echo "11. ⚠️  No trace ID found, skipping Tempo query"
fi

# Test 12: Query Prometheus for exemplars
echo ""
echo "12. Querying Prometheus for metrics with exemplars..."
curl -s "http://localhost:9092/api/v1/query?query=http_server_requests_seconds_count" | jq -r '.data.result[0] | .metric'

# Test 13: Verify metrics in Mimir
echo ""
echo "13. Verifying metrics in Mimir (remote storage)..."
curl -s "http://localhost:9009/prometheus/api/v1/query?query=up{job='config-control-service'}" | jq '.data.result[]'

echo ""
echo "========================================="
echo "✅ Full Observability Test Complete"
echo "========================================="
echo ""
echo "Next steps:"
echo "1. Open Grafana: http://localhost:3000 (admin/admin)"
echo "2. Explore > Tempo > Search for trace: $TRACE_ID"
echo "3. Explore > Loki > Query: {container=~\"config-control-service.*\"} | json"
echo "4. Explore > Prometheus > Query: http_server_requests_seconds_count"
echo "5. Click on trace_id in logs to jump to Tempo"
echo "6. Click on exemplars in metrics to jump to traces"

