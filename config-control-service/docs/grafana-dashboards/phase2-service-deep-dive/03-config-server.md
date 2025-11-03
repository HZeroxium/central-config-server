# Config Server Dashboard

This dashboard monitors Spring Cloud Config Server health, configuration operations, Git backend performance, and overall service health.

## 📊 Overview

### Purpose
Monitor Config Server performance, track configuration fetch operations, monitor Git repository sync status, and ensure reliable configuration delivery.

### Target Audience
- **SRE teams** - Config Server performance and availability
- **Operations teams** - Configuration delivery monitoring
- **Developers** - Understanding config fetch performance

### Key Metrics Covered
- Config Server health status
- Configuration fetch operations (by application/profile)
- Config Server response times and error rates
- Git backend operations and sync status
- JVM and connection pool metrics

### Prerequisites
- Config Server running and exposing metrics
- Prometheus scraping Config Server
- Grafana configured with Prometheus data source
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build each panel step-by-step
3. Adjust queries based on Config Server's actual metric names

### Step 2: Verify Dashboard Works

After creating all panels:
1. Check all panels show Config Server data
2. Verify configuration fetch metrics are tracked
3. Test Git backend sync status

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Set up variables: `service`, `instance` (optional)

### Step 2: Create Row 1 - KPIs

#### Panel 1: Config Server Health Status

1. Click **Add visualization**
2. **Query**:
   ```promql
   up{job=~"config-server.*"}
   ```
3. **Visualization**: `Stat`
4. **Title**: `Config Server Health`
5. **Thresholds**: Red (0-0.5), Green (0.5-1)
6. **Value mappings**: `1` → `UP`, `0` → `DOWN`
7. Click **Apply**

#### Panel 2: Request Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_count{
     service=~"config-server.*",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Stat`
4. **Title**: `Request Rate`
5. **Unit**: `reqps`
6. Click **Apply**

#### Panel 3: Error Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(rate(http_server_requests_seconds_count{
       service=~"config-server.*",
       status=~"5..|4..",
       instance=~"$instance"
     }[5m]))
     /
     sum(rate(http_server_requests_seconds_count{
       service=~"config-server.*",
       instance=~"$instance"
     }[5m]))
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Error Rate (%)`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-1%), Yellow (1-5%), Red (5-100%)
7. Click **Apply**

#### Panel 4: Response Time p95

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket{
       service=~"config-server.*",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Response Time p95`
5. **Unit**: `seconds`
6. **Thresholds**: Green (< 200ms), Yellow (200-500ms), Red (> 500ms)
7. Click **Apply**

### Step 3: Create Row 2 - Config Operations

#### Panel 1: Configuration Fetch Rate by Application

1. **Add row** → `Config Operations`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_count{
     service=~"config-server.*",
     uri=~"/.*/.*/(default|dev|prod|test).*",
     instance=~"$instance"
   }[5m])) by (uri)
   ```
   Or if application/profile in labels:
   ```promql
   sum(rate(http_server_requests_seconds_count{
     service=~"config-server.*",
     instance=~"$instance"
   }[5m])) by (application, profile)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Config Fetch Rate by Application`
5. **Legend**: `{{application}}/{{profile}}` or `{{uri}}`
6. **Unit**: `reqps`
7. Click **Apply**

**Note**: Adjust URI pattern or label names based on Config Server endpoint structure.

#### Panel 2: Config Server Response Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket{
       service=~"config-server.*",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Config Server Response Time (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

#### Panel 3: Config Refresh Operations

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_count{
     service=~"config-server.*",
     uri=~".*/actuator/busrefresh.*",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Config Refresh Operations Rate`
5. **Unit**: `reqps`
6. Click **Apply**

### Step 4: Create Row 3 - Git Backend

#### Panel 1: Git Operations Latency

1. **Add row** → `Git Backend`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(git_operations_duration_seconds_bucket{
       service=~"config-server.*",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
   Or if tracked via HTTP metrics:
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket{
       service=~"config-server.*",
       uri=~".*/(environment|.*config).*",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Git Operations Latency (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

**Note**: Git operation metrics might not be directly available. Use HTTP metrics as proxy.

#### Panel 2: Git Error Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(rate(http_server_requests_seconds_count{
       service=~"config-server.*",
       uri=~".*/(environment|.*config).*",
       status=~"5..|4..",
       instance=~"$instance"
     }[5m]))
     /
     sum(rate(http_server_requests_seconds_count{
       service=~"config-server.*",
       uri=~".*/(environment|.*config).*",
       instance=~"$instance"
     }[5m]))
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Git Error Rate (%)`
5. **Unit**: `percent (0-100)`
6. Click **Apply**

#### Panel 3: Repository Sync Status

If Config Server exposes sync status:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   config_server_git_repository_sync_status{
     service=~"config-server.*",
     instance=~"$instance"
   }
   ```
3. **Visualization**: `Stat`
4. **Title**: `Repository Sync Status`
5. **Value mappings**: Map status values to text
6. Click **Apply**

**Note**: This metric might not exist. Skip if not available.

### Step 5: Create Row 4 - Health Metrics

#### Panel 1: JVM Heap Usage

1. **Add row** → `Health Metrics`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(jvm_memory_used_bytes{
       service=~"config-server.*",
       area="heap",
       instance=~"$instance"
     })
     /
     sum(jvm_memory_max_bytes{
       service=~"config-server.*",
       area="heap",
       instance=~"$instance"
     })
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `JVM Heap Usage %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Y-axis at 80% (warning), 90% (critical)
7. Click **Apply**

#### Panel 2: Connection Pool Metrics

If Config Server uses database or connection pool:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   spring_data_source_active_connections{
     service=~"config-server.*",
     instance=~"$instance"
   }
   ```
3. **Visualization**: `Time series`
4. **Title**: `Connection Pool Active`
5. **Unit**: `short`
6. Click **Apply**

**Note**: Adjust metric name based on actual connection pool metrics.

### Step 6: Save Dashboard

1. Click **Save dashboard**
2. **Name**: `Config Server`
3. **Folder**: `1-Services`
4. **Tags**: `config-server`, `infrastructure`, `monitoring`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### Config Server Metrics

Spring Cloud Config Server typically exposes:
- Standard HTTP metrics: `http_server_requests_seconds_*`
- JVM metrics: `jvm_*`
- Spring Boot actuator health: `spring_boot_health_*`

Config Server-specific metrics might be limited unless custom instrumentation exists.

### Git Operations Monitoring

Git operations are typically tracked via:
1. **HTTP endpoint metrics** - Proxy for Git operations
2. **Custom metrics** - If Config Server exposes Git-specific metrics
3. **Health indicators** - Git repository health status

### Alternative Queries

#### Configuration Fetch by Profile

```promql
sum(rate(http_server_requests_seconds_count{
  uri=~".*/(default|dev|prod|test).*"
}[5m])) by (profile)
```

#### Configuration Delivery Success Rate

```promql
100 * (
  sum(rate(http_server_requests_seconds_count{status="200",uri=~".*/.*config.*"}[5m]))
  /
  sum(rate(http_server_requests_seconds_count{uri=~".*/.*config.*"}[5m]))
)
```

---

## ✅ Best Practices

### Config Server Monitoring

1. **Monitor fetch rates** - High rates indicate many clients
2. **Track error rates** - Git errors affect configuration delivery
3. **Watch response times** - Slow responses delay client startup
4. **Monitor Git operations** - Repository sync issues affect availability

### Performance Optimization

1. **Cache configuration** - Reduce Git repository access
2. **Monitor connection pool** - Prevent connection exhaustion
3. **Track JVM metrics** - Ensure adequate resources

---

## 🐛 Troubleshooting

### "No data" for Config Server

**Check**:
1. Config Server is running: `up{job=~"config-server.*"}`
2. Metrics endpoint is accessible: `curl http://config-server:8888/actuator/prometheus`
3. Prometheus is scraping Config Server

---

## 📚 References

### Related Dashboards
- [Platform Overview](../phase1-foundation/01-platform-overview.md)
- [Config Control Service - Golden Signals](../phase1-foundation/02-config-control-service-golden-signals.md)

### External Documentation
- [Spring Cloud Config Server](https://spring.io/projects/spring-cloud-config)
- [Config Server Health](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#_health_indicator)

---

**Next**: [Phase 3: Infrastructure Dashboards](../phase3-infrastructure/README.md)  
**Previous**: [Config Control Service - Infrastructure Dependencies](02-config-control-infrastructure-dependencies.md)

