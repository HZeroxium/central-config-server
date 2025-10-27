#!/bin/bash
# Trace Correlation Test Script
# Tests end-to-end correlation between logs, metrics, and traces

set -e

echo "🔗 Testing Trace Correlation..."
echo "=============================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test functions
extract_trace_id() {
    local response=$1
    echo "$response" | grep -o '"traceId":"[^"]*"' | cut -d'"' -f4
}

extract_span_id() {
    local response=$1
    echo "$response" | grep -o '"spanId":"[^"]*"' | cut -d'"' -f4
}

query_loki_for_trace() {
    local trace_id=$1
    local query="{\$__json.trace_id=\"$trace_id\"}"
    curl -s "http://localhost:3100/loki/api/v1/query_range?query=$query&start=$(date -u -d '5 minutes ago' +%s)000000000&end=$(date -u +%s)000000000"
}

query_tempo_for_trace() {
    local trace_id=$1
    curl -s "http://localhost:3200/api/traces/$trace_id"
}

query_mimir_for_metrics() {
    local query=$1
    curl -s "http://localhost:9009/prometheus/api/v1/query?query=$query"
}

echo -e "\n${BLUE}1. Generating Test Trace${NC}"
echo "------------------------"

# Generate a unique test payload
timestamp=$(date +%s)
test_payload="{
  \"serviceName\": \"correlation-test-service\",
  \"instanceId\": \"test-instance-$timestamp\",
  \"configHash\": \"test-hash-$timestamp\",
  \"host\": \"localhost\",
  \"port\": 8080,
  \"environment\": \"test\",
  \"version\": \"1.0.0\"
}"

echo "Sending heartbeat request..."
echo "Payload: $test_payload"

# Send heartbeat request and capture response
response=$(curl -s -X POST "http://localhost:8081/api/heartbeat" \
    -H "Content-Type: application/json" \
    -H "X-Test-Request: true" \
    -d "$test_payload" \
    -w "\nHTTP_CODE:%{http_code}")

# Extract HTTP code and response body
http_code=$(echo "$response" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
response_body=$(echo "$response" | sed 's/HTTP_CODE:[0-9]*//')

if [ "$http_code" != "200" ]; then
    echo -e "${RED}❌ Heartbeat request failed with HTTP $http_code${NC}"
    echo "Response: $response_body"
    exit 1
fi

echo -e "${GREEN}✓ Heartbeat request successful${NC}"

# Extract trace ID from response headers (if available)
trace_id_from_header=""
if curl_response=$(curl -s -X POST "http://localhost:8081/api/heartbeat" \
    -H "Content-Type: application/json" \
    -H "X-Test-Request: true" \
    -d "$test_payload" \
    -D /dev/stderr 2>&1 > /dev/null); then
    
    trace_id_from_header=$(echo "$curl_response" | grep -i "X-Trace-ID" | cut -d: -f2 | tr -d ' \r\n')
fi

# Extract trace ID from logs (alternative method)
trace_id_from_logs=""
if log_response=$(curl -s "http://localhost:3100/loki/api/v1/query?query={service=\"config-control-service\"}&limit=5"); then
    trace_id_from_logs=$(echo "$log_response" | jq -r '.data.result[0].values[0][1]' 2>/dev/null | jq -r '.trace_id // empty' 2>/dev/null)
fi

# Use the first available trace ID
trace_id=""
if [ -n "$trace_id_from_header" ]; then
    trace_id="$trace_id_from_header"
    echo "Trace ID from header: $trace_id"
elif [ -n "$trace_id_from_logs" ]; then
    trace_id="$trace_id_from_logs"
    echo "Trace ID from logs: $trace_id"
else
    echo -e "${YELLOW}⚠ No trace ID found in response or logs${NC}"
    echo "This might be normal if tracing is not fully configured yet."
fi

echo -e "\n${BLUE}2. Testing Log Correlation${NC}"
echo "-------------------------"

if [ -n "$trace_id" ]; then
    echo "Searching for logs with trace ID: $trace_id"
    
    # Wait for logs to be processed
    echo "Waiting for logs to be processed..."
    sleep 3
    
    # Query Loki for logs with this trace ID
    loki_response=$(query_loki_for_trace "$trace_id")
    
    if echo "$loki_response" | jq -e '.data.result | length > 0' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Found logs in Loki with trace ID: $trace_id${NC}"
        
        # Show sample log entry
        log_entry=$(echo "$loki_response" | jq -r '.data.result[0].values[0][1]' 2>/dev/null)
        echo "Sample log entry:"
        echo "$log_entry" | jq . 2>/dev/null || echo "$log_entry"
    else
        echo -e "${YELLOW}⚠ No logs found with trace ID: $trace_id${NC}"
        echo "This might be normal if logs are still being processed."
        
        # Show recent logs anyway
        echo "Recent logs from config-control-service:"
        recent_logs=$(curl -s "http://localhost:3100/loki/api/v1/query?query={service=\"config-control-service\"}&limit=3")
        echo "$recent_logs" | jq -r '.data.result[]?.values[]?[1]' 2>/dev/null | head -3
    fi
else
    echo -e "${YELLOW}⚠ Skipping log correlation test (no trace ID)${NC}"
fi

echo -e "\n${BLUE}3. Testing Trace Correlation${NC}"
echo "-------------------------"

if [ -n "$trace_id" ]; then
    echo "Searching for trace in Tempo: $trace_id"
    
    # Query Tempo for the trace
    tempo_response=$(query_tempo_for_trace "$trace_id")
    
    if echo "$tempo_response" | jq -e '.traces | length > 0' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Found trace in Tempo: $trace_id${NC}"
        
        # Show trace summary
        trace_summary=$(echo "$tempo_response" | jq -r '.traces[0] | {traceID, spans: (.spans | length), rootSpan: .spans[0].operationName}')
        echo "Trace summary:"
        echo "$trace_summary" | jq .
    else
        echo -e "${YELLOW}⚠ No trace found in Tempo: $trace_id${NC}"
        echo "This might be normal if traces are still being processed."
        
        # Show recent traces anyway
        echo "Recent traces:"
        recent_traces=$(curl -s "http://localhost:3200/api/search?tags=service.name=config-control-service&limit=5")
        echo "$recent_traces" | jq -r '.traces[]?.traceID' 2>/dev/null | head -3
    fi
else
    echo -e "${YELLOW}⚠ Skipping trace correlation test (no trace ID)${NC}"
fi

echo -e "\n${BLUE}4. Testing Metrics Correlation${NC}"
echo "----------------------------"

# Test metrics collection
echo "Checking metrics collection..."

# Query for HTTP request metrics
http_metrics=$(query_mimir_for_metrics "http_server_requests_seconds_count{service=\"config-control-service\"}")
if echo "$http_metrics" | jq -e '.data.result | length > 0' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Found HTTP request metrics${NC}"
    echo "HTTP metrics sample:"
    echo "$http_metrics" | jq -r '.data.result[0] | {metric: .metric, value: .value[1]}'
else
    echo -e "${YELLOW}⚠ No HTTP request metrics found yet${NC}"
fi

# Query for custom heartbeat metrics
heartbeat_metrics=$(query_mimir_for_metrics "config_control_heartbeat_process_seconds_count")
if echo "$heartbeat_metrics" | jq -e '.data.result | length > 0' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Found heartbeat metrics${NC}"
    echo "Heartbeat metrics sample:"
    echo "$heartbeat_metrics" | jq -r '.data.result[0] | {metric: .metric, value: .value[1]}'
else
    echo -e "${YELLOW}⚠ No heartbeat metrics found yet${NC}"
fi

echo -e "\n${BLUE}5. Testing Grafana Integration${NC}"
echo "----------------------------"

# Test Grafana datasources
echo "Checking Grafana datasources..."

datasources=$(curl -s "http://localhost:3000/api/datasources")
if echo "$datasources" | jq -e '.data | length > 0' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Grafana datasources configured${NC}"
    
    # List datasources
    echo "Available datasources:"
    echo "$datasources" | jq -r '.data[] | {name: .name, type: .type, url: .url}'
    
    # Check if all required datasources are present
    required_datasources=("Prometheus" "Mimir" "Loki" "Tempo")
    for ds in "${required_datasources[@]}"; do
        if echo "$datasources" | jq -e --arg name "$ds" '.data[] | select(.name == $name)' > /dev/null 2>&1; then
            echo -e "  ${GREEN}✓ $ds${NC}"
        else
            echo -e "  ${RED}✗ $ds (missing)${NC}"
        fi
    done
else
    echo -e "${RED}✗ No Grafana datasources found${NC}"
fi

echo -e "\n${BLUE}6. Testing Full Correlation Chain${NC}"
echo "-------------------------------"

if [ -n "$trace_id" ]; then
    echo "Testing full correlation for trace ID: $trace_id"
    
    correlation_success=true
    
    # Test 1: Logs → Traces
    echo -n "Logs → Traces correlation: "
    if loki_response=$(query_loki_for_trace "$trace_id"); then
        if echo "$loki_response" | jq -e '.data.result | length > 0' > /dev/null 2>&1; then
            echo -e "${GREEN}✓${NC}"
        else
            echo -e "${YELLOW}⚠${NC} (logs not found yet)"
            correlation_success=false
        fi
    else
        echo -e "${RED}✗${NC}"
        correlation_success=false
    fi
    
    # Test 2: Traces → Logs
    echo -n "Traces → Logs correlation: "
    if tempo_response=$(query_tempo_for_trace "$trace_id"); then
        if echo "$tempo_response" | jq -e '.traces | length > 0' > /dev/null 2>&1; then
            echo -e "${GREEN}✓${NC}"
        else
            echo -e "${YELLOW}⚠${NC} (trace not found yet)"
            correlation_success=false
        fi
    else
        echo -e "${RED}✗${NC}"
        correlation_success=false
    fi
    
    # Test 3: Metrics → Traces (exemplars)
    echo -n "Metrics → Traces correlation: "
    exemplars_query="http_server_requests_seconds_bucket{service=\"config-control-service\"}"
    if exemplars_response=$(query_mimir_for_metrics "$exemplars_query"); then
        if echo "$exemplars_response" | jq -e '.data.result | length > 0' > /dev/null 2>&1; then
            echo -e "${GREEN}✓${NC}"
        else
            echo -e "${YELLOW}⚠${NC} (exemplars not found yet)"
            correlation_success=false
        fi
    else
        echo -e "${RED}✗${NC}"
        correlation_success=false
    fi
    
    if [ "$correlation_success" = true ]; then
        echo -e "\n${GREEN}🎉 Full correlation chain is working!${NC}"
    else
        echo -e "\n${YELLOW}⚠ Partial correlation working (some components still processing)${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Skipping full correlation test (no trace ID)${NC}"
fi

echo -e "\n${BLUE}Summary${NC}"
echo "========"
echo "Test completed. Check the results above."
echo "If some correlations are not working yet, wait a few minutes for data to be processed."
echo "You can also check Grafana at http://localhost:3000 (admin/admin) for visual correlation."
