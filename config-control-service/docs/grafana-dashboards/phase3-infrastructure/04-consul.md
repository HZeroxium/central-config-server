# Consul Dashboard

This dashboard monitors Consul service discovery, key-value store operations, health checks, and API performance.

## 📊 Overview

### Purpose
Monitor Consul service registry health, service instance status, health check success/failure rates, and API performance.

### Target Audience
- **SRE teams** - Service discovery health monitoring
- **Operations teams** - Service registration troubleshooting
- **Developers** - Understanding service discovery status

### Key Metrics Covered
- Service registry health status
- Service instance count by status (healthy/unhealthy/passing)
- Health check success/failure rate
- Consul API performance (latency, error rate)
- Key-value store operations (if used)

### Prerequisites
- Consul running and accessible
- Consul exporter installed (if needed) or HTTP metrics from Consul API
- Prometheus scraping Consul metrics
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build service discovery, health checks, and API performance panels
3. Adjust metric names based on available Consul metrics

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Set up variables if needed (environment, consul_instance)

### Step 2: Create Row 1 - Service Registry Health

#### Panel 1: Consul Health Status

1. Click **Add visualization**
2. **Query**:
   ```promql
   up{job=~".*consul.*"}
   ```
3. **Visualization**: `Stat`
4. **Title**: `Consul Health Status`
5. **Thresholds**: Red (0-0.5), Green (0.5-1)
6. **Value mappings**: `1` → `UP`, `0` → `DOWN`
7. Click **Apply**

#### Panel 2: Services Registered Count

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   consul_catalog_services_total{instance=~"$consul_instance"}
   ```
   Or if using HTTP metrics:
   ```promql
   count(up{job=~".*-service.*"} == 1)
   ```
3. **Visualization**: `Stat`
4. **Title**: `Services Registered`
5. **Unit**: `short`
6. Click **Apply**

#### Panel 3: Total Service Instances

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   consul_catalog_service_instances_total{instance=~"$consul_instance"}
   ```
3. **Visualization**: `Stat`
4. **Title**: `Total Service Instances`
5. **Unit**: `short`
6. Click **Apply**

### Step 3: Create Row 2 - Service Instance Status

#### Panel 1: Service Instances by Status

1. **Add row** → `Service Instance Status`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(consul_health_service_status{instance=~"$consul_instance"}) by (status)
   ```
3. **Visualization**: `Time series` or `Stat`
4. **Title**: `Service Instances by Status`
5. **Legend**: `{{status}}`
6. **Unit**: `short`
7. Click **Apply**

#### Panel 2: Healthy vs Unhealthy Instances

1. **Add panel** → **Add visualization**
2. **Query A** (Healthy):
   ```promql
   sum(consul_health_service_status{status="passing",instance=~"$consul_instance"})
   ```
3. **Query B** (Unhealthy):
   ```promql
   sum(consul_health_service_status{status=~"critical|warning",instance=~"$consul_instance"})
   ```
4. **Visualization**: `Time series`
5. **Title**: `Healthy vs Unhealthy Instances`
6. **Legend**: `Healthy`, `Unhealthy`
7. **Unit**: `short`
8. Click **Apply**

#### Panel 3: Service Instances by Service Name

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(consul_health_service_status{status="passing",instance=~"$consul_instance"}) by (service)
   ```
3. **Visualization**: `Table` or `Time series`
4. **Title**: `Service Instances by Service`
5. **Legend**: `{{service}}`
6. **Unit**: `short`
7. Click **Apply**

### Step 4: Create Row 3 - Health Check Status

#### Panel 1: Health Check Success Rate

1. **Add row** → `Health Check Status`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(consul_health_checks_status{status="passing",instance=~"$consul_instance"})
     /
     sum(consul_health_checks_status{instance=~"$consul_instance"})
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Health Check Success Rate (%)`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (95-100%), Yellow (80-95%), Red (0-80%)
7. Click **Apply**

#### Panel 2: Health Check Failures Over Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(consul_health_checks_status{status=~"critical|warning",instance=~"$consul_instance"}) by (service)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Health Check Failures by Service`
5. **Legend**: `{{service}}`
6. **Unit**: `short`
7. Click **Apply**

#### Panel 3: Health Check Status Breakdown

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(consul_health_checks_status{instance=~"$consul_instance"}) by (status)
   ```
3. **Visualization**: `Time series` or `Stat`
4. **Title**: `Health Check Status Breakdown`
5. **Legend**: `{{status}}`
6. **Unit**: `short`
7. Click **Apply**

### Step 5: Create Row 4 - Consul API Performance

#### Panel 1: Consul API Request Rate

1. **Add row** → `API Performance`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_count{
     service=~".*consul.*",
     instance=~"$consul_instance"
   }[5m])) by (method, uri)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Consul API Request Rate`
5. **Legend**: `{{method}} {{uri}}`
6. **Unit**: `reqps`
7. Click **Apply**

#### Panel 2: Consul API Response Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket{
       service=~".*consul.*",
       instance=~"$consul_instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Consul API Response Time (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

#### Panel 3: Consul API Error Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(rate(http_server_requests_seconds_count{
       service=~".*consul.*",
       status=~"5..|4..",
       instance=~"$consul_instance"
     }[5m]))
     /
     sum(rate(http_server_requests_seconds_count{
       service=~".*consul.*",
       instance=~"$consul_instance"
     }[5m]))
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Consul API Error Rate (%)`
5. **Unit**: `percent (0-100)`
6. Click **Apply**

### Step 6: Create Row 5 - Key-Value Store (If Used)

If Consul KV store is used:

#### Panel 1: KV Operations Rate

1. **Add row** → `Key-Value Store`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(consul_kv_operations_total{instance=~"$consul_instance"}[5m])) by (operation)
   ```
3. **Visualization**: `Time series`
4. **Title**: `KV Operations Rate`
5. **Legend**: `{{operation}}`
6. **Unit**: `ops/sec`
7. Click **Apply**

**Note**: KV metrics might not be available. Adjust based on actual metrics.

### Step 7: Save Dashboard

1. Click **Save dashboard**
2. **Name**: `Consul`
3. **Folder**: `2-Infrastructure`
4. **Tags**: `consul`, `infrastructure`, `service-discovery`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### Consul Metrics

Consul metrics vary by exporter:
- **Consul Exporter**: `consul_*`
- **HTTP Metrics**: `http_server_requests_seconds_*` (if Consul exposes metrics)
- **Custom**: Check your instrumentation

Query available metrics:
```promql
{__name__=~".*consul.*"}
```

### Service Discovery Metrics

Service discovery typically tracks:
- **Service count**: `consul_catalog_services_total`
- **Instance count**: `consul_catalog_service_instances_total`
- **Health status**: `consul_health_service_status`

---

## ✅ Best Practices

### Consul Monitoring

1. **Monitor service registration** - Ensure services are properly registered
2. **Watch health check failures** - Failures indicate service issues
3. **Track API performance** - Slow API impacts service discovery
4. **Monitor instance counts** - Sudden drops indicate service issues

---

## 🐛 Troubleshooting

### "No data" for Consul metrics

**Check**:
1. Consul exporter is installed and running
2. Prometheus is scraping Consul metrics
3. Consul API is accessible

**Debug**:
```promql
{__name__=~".*consul.*"}
```

### High health check failures

**Possible causes**:
1. Services are unhealthy
2. Network connectivity issues
3. Health check configuration issues

**Solutions**:
1. Check service health directly
2. Verify network connectivity
3. Review health check configuration

---

## 📚 References

### Related Dashboards
- [System Health](../phase1-foundation/03-system-health.md)
- [Config Control Service - Infrastructure Dependencies](../phase2-service-deep-dive/02-config-control-infrastructure-dependencies.md)

### External Documentation
- [Consul Monitoring](https://developer.hashicorp.com/consul/docs/agent/telemetry)
- [Consul Exporter](https://github.com/prometheus/consul_exporter)

---

**Next**: [Phase 4: Business Intelligence](../phase4-business-intelligence/README.md)  
**Previous**: [Kafka Dashboard](03-kafka.md)

