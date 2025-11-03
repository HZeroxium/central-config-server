# Prerequisites and Setup

This guide helps you verify that your environment is ready for creating Grafana dashboards.

## ✅ Checklist

Before creating dashboards, verify:

- [ ] Prometheus is running and scraping metrics
- [ ] Grafana is running and accessible
- [ ] Prometheus data source is configured in Grafana
- [ ] Config Control Service is running and exposing metrics
- [ ] You can query metrics in Prometheus
- [ ] You can query metrics in Grafana Explore

## 🔍 Verify Prometheus

### 1. Check Prometheus is Running

Access Prometheus UI:
- **URL**: `http://localhost:9090` (or your Prometheus URL)
- **Remote Server**: `http://10.40.30.161:23090` (as per your config)

You should see the Prometheus UI.

### 2. Verify Metrics are Being Scraped

1. Go to **Status → Targets** in Prometheus
2. Look for `config-control-service` target
3. Status should be **UP** (green)

If status is **DOWN**:
- Check service is running
- Verify network connectivity
- Check scrape configuration in `prometheus.yml`

### 3. Query Metrics in Prometheus

1. Go to **Graph** tab in Prometheus
2. Enter query: `up{job="config-control-service-local"}`
3. Click **Execute**
4. You should see value `1` (meaning target is up)

Try more queries:
```
http_server_requests_seconds_count
jvm_memory_used_bytes
heartbeat.process
```

## 🔍 Verify Grafana

### 1. Check Grafana is Running

Access Grafana UI:
- **URL**: `http://localhost:3000` (or your Grafana URL)
- **Remote Server**: `http://10.40.30.161:23000`

Default credentials:
- **Username**: `admin`
- **Password**: `admin` (change on first login)

### 2. Configure Prometheus Data Source

#### Method 1: Via UI

1. Go to **Configuration → Data Sources**
2. Click **Add data source**
3. Select **Prometheus**
4. Configure:
   - **Name**: `Prometheus`
   - **URL**: `http://prometheus:9090` (if same network) or `http://10.40.30.161:23090` (remote)
   - **Access**: `Server (default)`
5. Click **Save & Test**
6. You should see "Data source is working"

#### Method 2: Via Provisioning (Recommended)

Data source can be provisioned via configuration file. Check if `config/grafana/grafana-datasources.yml` exists:

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    jsonData:
      httpMethod: POST
      timeInterval: 15s
```

If using remote Prometheus:
```yaml
url: http://10.40.30.161:23090
```

### 3. Test Query in Grafana Explore

1. Go to **Explore** in Grafana
2. Select **Prometheus** as data source
3. Enter query: `up{job="config-control-service-local"}`
4. Click **Run query**
5. You should see graph/data

Try these test queries:
```
# Total requests
http_server_requests_seconds_count

# Request rate
rate(http_server_requests_seconds_count[5m])

# Memory usage
jvm_memory_used_bytes{area="heap"}

# Custom metric
heartbeat.process
```

## 🔍 Verify Config Control Service

### 1. Check Service is Running

Service should be accessible at:
- **HTTP**: `http://localhost:8081` (port mapped from 8080)
- **Actuator**: `http://localhost:8081/actuator`

### 2. Verify Metrics Endpoint

Check Prometheus metrics endpoint:
```bash
curl http://localhost:8081/actuator/prometheus
```

You should see metrics in Prometheus format:
```
# HELP http_server_requests_seconds Duration of HTTP server request handling
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",status="200",uri="/api/heartbeat"} 1523
...
```

### 3. Check Custom Metrics

Verify custom metrics are present:
```bash
curl http://localhost:8081/actuator/prometheus | grep -E "(heartbeat|config_control)"
```

Expected metrics:
- `heartbeat.process` - Heartbeat processing
- `config_control.thrift.heartbeat` - Thrift heartbeat
- `config_control.cleanup.*` - Cleanup metrics
- `config_control.application_service.*` - Application service metrics
- `config_control.drift_event.*` - Drift event metrics

### 4. Verify Metrics in Prometheus

Query Prometheus for application metrics:
```
up{job="config-control-service-local"}
http_server_requests_seconds_count{service="config-control-service"}
heartbeat.process_count
```

## 📊 Verify Scrape Configuration

### Check Prometheus Configuration

Verify `config/prometheus/prometheus.yml` has correct scrape config:

```yaml
scrape_configs:
  - job_name: 'config-control-service-local'
    static_configs:
      - targets: ['config-control-service:8080']  # Or IP:port for remote
    scrape_interval: 15s
    metrics_path: /actuator/prometheus
```

### Check Prometheus Agent Configuration (If Used)

If using Prometheus Agent (local collector):
- Verify `config/prometheus/prometheus-agent.yml` is configured
- Check agent is running: `docker ps | grep prometheus-agent`
- Verify remote write is working

## 🧪 Test End-to-End Flow

### Step 1: Generate Some Traffic

Make some requests to your service:
```bash
# Health check
curl http://localhost:8081/actuator/health

# Heartbeat endpoint
curl -X POST http://localhost:8081/api/heartbeat \
  -H "Content-Type: application/json" \
  -d '{"serviceName":"test-service","instanceId":"test-1"}'
```

### Step 2: Wait for Scrape

Wait 15-30 seconds for Prometheus to scrape metrics.

### Step 3: Query in Prometheus

In Prometheus UI, query:
```
rate(http_server_requests_seconds_count[5m])
```

You should see data points.

### Step 4: Query in Grafana

In Grafana Explore, same query should show data.

## 🔧 Troubleshooting

### Problem: "No data" in Prometheus

**Possible causes:**
1. Service not running
2. Network connectivity issue
3. Wrong scrape target URL
4. Metrics endpoint not accessible

**Solutions:**
```bash
# Check service is running
curl http://localhost:8081/actuator/health

# Check metrics endpoint
curl http://localhost:8081/actuator/prometheus

# Check Prometheus can reach service
# If service is in Docker, ensure same network
docker network ls
docker inspect <network_name>
```

### Problem: "Data source is not working" in Grafana

**Possible causes:**
1. Prometheus URL incorrect
2. Network connectivity
3. Prometheus not running

**Solutions:**
- Test Prometheus URL directly in browser
- Check Prometheus is accessible from Grafana container
- Verify URL format: `http://host:port` (no trailing slash)

### Problem: Metrics not appearing

**Possible causes:**
1. Metrics not being emitted
2. Wrong metric names in query
3. Labels mismatch

**Solutions:**
```bash
# Check what metrics are available
curl http://localhost:8081/actuator/prometheus | grep -E "^[^#]"

# Query all metrics in Prometheus
# Go to Prometheus UI → Graph → Type metric name prefix
```

### Problem: Timeout errors

**Possible causes:**
1. Scrape timeout too short
2. Service responding slowly
3. Network latency

**Solutions:**
- Increase `scrape_timeout` in `prometheus.yml`
- Check service performance
- Verify network latency

## ✅ Verification Script

Quick verification commands:

```bash
#!/bin/bash

echo "=== Verifying Prerequisites ==="

# Check Prometheus
echo "1. Checking Prometheus..."
curl -s http://localhost:9090/-/healthy && echo "✓ Prometheus is running" || echo "✗ Prometheus not accessible"

# Check Grafana
echo "2. Checking Grafana..."
curl -s http://localhost:3000/api/health && echo "✓ Grafana is running" || echo "✗ Grafana not accessible"

# Check Config Control Service
echo "3. Checking Config Control Service..."
curl -s http://localhost:8081/actuator/health && echo "✓ Service is running" || echo "✗ Service not accessible"

# Check Metrics Endpoint
echo "4. Checking metrics endpoint..."
curl -s http://localhost:8081/actuator/prometheus | head -5 && echo "✓ Metrics endpoint working" || echo "✗ Metrics endpoint not accessible"

# Check Custom Metrics
echo "5. Checking custom metrics..."
curl -s http://localhost:8081/actuator/prometheus | grep -q "heartbeat.process" && echo "✓ Custom metrics present" || echo "✗ Custom metrics missing"
```

Save as `verify-prerequisites.sh`, make executable, and run.

## 📚 Next Steps

Once prerequisites are verified:

1. ✅ **Prometheus is working** - Metrics are being scraped
2. ✅ **Grafana is configured** - Data source connected
3. ✅ **Service is running** - Metrics are available
4. ✅ **Queries work** - You can query in Grafana Explore

You're ready to create dashboards!

**Next**: 
- [PromQL Basics](03-promql-basics.md) - Learn to write queries
- [Platform Overview Dashboard](phase1-foundation/01-platform-overview.md) - Create your first dashboard

---

**Previous**: [Getting Started](01-getting-started.md)  
**Next**: [PromQL Basics](03-promql-basics.md)

