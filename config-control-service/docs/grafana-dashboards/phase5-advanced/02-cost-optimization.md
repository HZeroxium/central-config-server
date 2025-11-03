# Cost Optimization Dashboard

This dashboard monitors resource utilization and identifies cost optimization opportunities.

## 📊 Overview

### Purpose
Monitor resource utilization, identify underutilized resources, and optimize infrastructure costs.

### Target Audience
- **SRE teams** - Resource optimization and cost management
- **Operations teams** - Infrastructure cost optimization
- **Finance teams** - Cost tracking and optimization

### Key Metrics Covered
- Resource utilization (CPU, memory, connections)
- Instance count optimization opportunities
- Cache efficiency metrics
- Infrastructure component utilization

### Prerequisites
- Resource utilization metrics available
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))
- Understanding of resource allocation patterns

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build resource utilization panels step-by-step
3. Identify optimization opportunities

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Set up variables: `service`, `environment`

### Step 2: Create Row 1 - Resource Utilization

#### Panel 1: CPU Utilization %

1. Click **Add visualization**
2. **Query tab**:
   ```promql
   100 - (avg(rate(process_cpu_seconds_total{
     service="$service"
   }[5m])) * 100)
   ```
   Or if using container metrics:
   ```promql
   100 * (
     sum(rate(container_cpu_usage_seconds_total{
       name=~".*$service.*"
     }[5m]))
     /
     sum(container_spec_cpu_quota{name=~".*$service.*"} / container_spec_cpu_period{name=~".*$service.*"})
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `CPU Utilization %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-50%), Yellow (50-80%), Red (80-100%)
7. Click **Apply**

**Note**: Adjust based on available CPU metrics. Container metrics might differ.

#### Panel 2: Memory Utilization %

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(jvm_memory_used_bytes{
       service="$service",
       area="heap"
     })
     /
     sum(jvm_memory_max_bytes{
       service="$service",
       area="heap"
     })
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Memory Utilization %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-60%), Yellow (60-80%), Red (80-100%)
7. Click **Apply**

#### Panel 3: Connection Pool Utilization %

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(spring_data_mongodb_repository_health_indicator_active_connections{
       service="$service"
     })
     /
     sum(spring_data_mongodb_repository_health_indicator_max_connections{
       service="$service"
     })
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Connection Pool Utilization %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-50%), Yellow (50-80%), Red (80-100%)
7. Click **Apply**

**Note**: Repeat for MongoDB, Redis, and other connection pools.

### Step 3: Create Row 2 - Instance Optimization

#### Panel 1: Instance Count vs Traffic

1. **Add row** → `Instance Optimization`
2. **Add panel** → **Add visualization**
2. **Query A** (Instances):
   ```promql
   count(up{service="$service"} == 1)
   ```
3. **Query B** (Request Rate per Instance):
   ```promql
   sum(rate(http_server_requests_seconds_count{
     service="$service"
   }[5m]))
   /
   count(up{service="$service"} == 1)
   ```
4. **Visualization**: `Time series` or `Stat`
5. **Title**: `Request Rate per Instance`
6. **Legend**: `Requests/sec per instance`
7. **Unit**: `reqps`
8. Click **Apply**

#### Panel 2: Underutilized Instances

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Instances with low request rate (<10 req/s)
   count(
     sum(rate(http_server_requests_seconds_count{
       service="$service"
     }[5m])) by (instance)
     < 10
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Underutilized Instances (<10 req/s)`
5. **Unit**: `short`
6. **Description**: `Instances with request rate below threshold (potential for consolidation)`
7. Click **Apply**

#### Panel 3: Instance Count Over Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   count(up{service="$service"} == 1)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Instance Count Over Time`
5. **Unit**: `short`
6. Click **Apply**

### Step 4: Create Row 3 - Cache Efficiency

#### Panel 1: Cache Hit Ratio

1. **Add row** → `Cache Efficiency`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(rate(cache.gets{result="hit",service="$service"}[5m]))
     /
     sum(rate(cache.gets{service="$service"}[5m]))
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Cache Hit Ratio (%)`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (80-100%), Yellow (60-80%), Red (0-60%)
7. Click **Apply**

#### Panel 2: Cache Memory Usage

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(cache.size{service="$service"}) by (cache_name)
   ```
   Or if memory tracked:
   ```promql
   sum(cache.memory_bytes{service="$service"}) by (cache_name)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Cache Memory Usage`
5. **Legend**: `{{cache_name}}`
6. **Unit**: `bytes`
7. Click **Apply**

#### Panel 3: Cache Eviction Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(cache.evictions{service="$service"}[5m])) by (cache_name)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Cache Eviction Rate`
5. **Legend**: `{{cache_name}}`
6. **Unit**: `evictions/sec`
7. **Description**: `High eviction rate indicates cache size might be too small`
8. Click **Apply**

### Step 5: Create Row 4 - Infrastructure Utilization

#### Panel 1: MongoDB Utilization

1. **Add row** → `Infrastructure Utilization`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(spring_data_mongodb_repository_health_indicator_active_connections)
     /
     sum(spring_data_mongodb_repository_health_indicator_max_connections)
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `MongoDB Connection Pool Utilization %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-60%), Yellow (60-80%), Red (80-100%)
7. Click **Apply**

#### Panel 2: Redis Utilization

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     redis_memory_used_bytes
     /
     redis_memory_max_bytes
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Redis Memory Utilization %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-70%), Yellow (70-90%), Red (90-100%)
7. Click **Apply**

#### Panel 3: Kafka Utilization

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(kafka_server_log_dir_size_bytes)
     /
     sum(node_filesystem_size_bytes{mountpoint="/var/lib/kafka"})
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Kafka Disk Utilization %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-70%), Yellow (70-85%), Red (85-100%)
7. Click **Apply**

### Step 6: Create Row 5 - Cost Optimization Opportunities

#### Panel 1: Low Utilization Instances

1. **Add row** → `Optimization Opportunities`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Instances with CPU < 30% and memory < 50%
   count(
     (100 * (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) < 50)
     and
     (process_cpu_seconds_total rate < 0.3)
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Low Utilization Instances`
5. **Unit**: `short`
6. **Description**: `Instances with low CPU and memory usage (candidates for consolidation)`
7. Click **Apply**

**Note**: Adjust thresholds based on your optimization criteria.

#### Panel 2: High Cache Miss Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   count(
     (100 * (sum(rate(cache.gets{result="miss"}[5m])) / sum(rate(cache.gets[5m]))) > 30)
     by (cache_name)
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Caches with High Miss Rate (>30%)`
5. **Unit**: `short`
6. **Description**: `Caches with miss rate above threshold (consider size increase)`
7. Click **Apply**

#### Panel 3: Over-Provisioned Resources

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Services with consistently low utilization
   count(
     avg_over_time((jvm_memory_used_bytes / jvm_memory_max_bytes)[1h:5m]) < 0.3
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Over-Provisioned Services`
5. **Unit**: `short`
6. **Description**: `Services with consistently low resource utilization`
7. Click **Apply**

### Step 7: Save Dashboard

1. Click **Save dashboard**
2. **Name**: `Cost Optimization Dashboard`
3. **Folder**: `5-Advanced` or `3-Business`
4. **Tags**: `cost-optimization`, `resource-utilization`, `efficiency`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### Resource Utilization Calculation

**CPU Utilization**:
```promql
100 * (
  sum(rate(container_cpu_usage_seconds_total[5m]))
  /
  sum(container_spec_cpu_quota / container_spec_cpu_period)
)
```

**Memory Utilization**:
```promql
100 * (
  sum(jvm_memory_used_bytes{area="heap"})
  /
  sum(jvm_memory_max_bytes{area="heap"})
)
```

### Optimization Opportunities

#### Low Utilization Detection

```promql
# Instances with utilization below threshold
count(
  (cpu_util < 30) and (memory_util < 50)
)
```

#### Cache Optimization

```promql
# Caches with high miss rate
cache_miss_rate > 30
```

#### Connection Pool Optimization

```promql
# Pools with low utilization
connection_pool_util < 30
```

---

## ⚙️ Panel Configuration (Advanced)

### Utilization Gauge

For resource utilization:
1. **Visualization**: `Gauge`
2. **Options** → **Min**: `0`, **Max**: `100`
3. **Thresholds**: Green (0-60%), Yellow (60-80%), Red (80-100%)
4. Show percentage and absolute values

### Table for Opportunities

List optimization opportunities:
1. **Visualization**: `Table`
2. **Query**: List resources with low utilization
3. **Transform**: Organize fields, sort by utilization
4. **Options**: Add action links to detail pages

---

## ✅ Best Practices

### Cost Optimization

1. **Monitor utilization** - Track resource usage over time
2. **Identify opportunities** - Find underutilized resources
3. **Set thresholds** - Alert on low/high utilization
4. **Track trends** - Watch for utilization changes

### Resource Management

1. **Right-size resources** - Match capacity to demand
2. **Optimize cache** - Improve hit ratios to reduce backend load
3. **Consolidate instances** - Reduce instance count if possible
4. **Review regularly** - Periodically review utilization

---

## 🐛 Troubleshooting

### "No data" for utilization metrics

**Check**:
1. Container/JVM metrics are available
2. Resource metrics are being scraped
3. Metric names match actual instrumentation

**Debug**:
```promql
# Check CPU metrics
{__name__=~".*cpu.*"}

# Check memory metrics
{__name__=~".*memory.*"}
```

---

## 📚 References

### Related Dashboards
- [Platform Overview](../phase1-foundation/01-platform-overview.md)
- [Config Control Service - Golden Signals](../phase1-foundation/02-config-control-service-golden-signals.md)

### External Documentation
- [Cost Optimization Best Practices](https://aws.amazon.com/cost-optimization/)
- [Resource Utilization Monitoring](https://prometheus.io/docs/guides/container-resources/)

---

**Next**: [Capacity Planning Dashboard](03-capacity-planning.md)  
**Previous**: [SLO Dashboards](01-slo-dashboards.md)

