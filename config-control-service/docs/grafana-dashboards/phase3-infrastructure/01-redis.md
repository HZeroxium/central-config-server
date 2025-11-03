# Redis Dashboard (USE Method)

This dashboard monitors Redis performance using the USE method: Utilization, Saturation, and Errors.

## 📊 Overview

### Purpose
Monitor Redis health, memory usage, connection pool, command performance, and errors using the USE method.

### Target Audience
- **SRE teams** - Redis performance and capacity monitoring
- **Operations teams** - Redis troubleshooting and optimization
- **Developers** - Understanding Redis performance impact

### USE Method
- **Utilization**: Memory usage %, connection pool usage %
- **Saturation**: Memory pressure, connection pool exhaustion, eviction rate
- **Errors**: Command errors, connection errors, timeouts

### Prerequisites
- Redis running and accessible
- Redis exporter installed (if needed) or Spring Boot auto-instrumentation
- Prometheus scraping Redis metrics
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build USE method sections (Utilization, Saturation, Errors)
3. Adjust metric names based on available Redis metrics

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Set up variables if needed (environment, redis_instance)

### Step 2: Create Row 1 - Utilization

Utilization metrics show how much of Redis resources are being used.

#### Panel 1: Memory Usage %

1. Click **Add visualization**
2. **Query tab**:
   ```promql
   100 * (
     redis_memory_used_bytes{instance=~"$redis_instance"}
     /
     redis_memory_max_bytes{instance=~"$redis_instance"}
   )
   ```
   Or if using Spring Boot metrics:
   ```promql
   100 * (
     spring_data_redis_repository_health_indicator_used_memory_bytes
     /
     spring_data_redis_repository_health_indicator_max_memory_bytes
   )
   ```
3. **Visualization**: `Stat` or `Time series`
4. **Title**: `Redis Memory Usage %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-70%), Yellow (70-90%), Red (90-100%)
7. Click **Apply**

**Note**: Metric names vary by exporter. Check `/actuator/prometheus` or Redis exporter metrics.

#### Panel 2: Connection Pool Usage %

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     redis_connected_clients{instance=~"$redis_instance"}
     /
     redis_maxclients{instance=~"$redis_instance"}
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Connection Pool Usage %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-70%), Yellow (70-90%), Red (90-100%)
7. Click **Apply**

#### Panel 3: Memory Usage Over Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   redis_memory_used_bytes{instance=~"$redis_instance"}
   ```
3. **Visualization**: `Time series`
4. **Title**: `Redis Memory Usage`
5. **Unit**: `bytes` (or `decbytes` for MB/GB)
6. **Legend**: `{{instance}}`
7. Click **Apply**

#### Panel 4: Memory Max vs Used

1. **Add panel** → **Add visualization**
2. **Query A** (Used):
   ```promql
   redis_memory_used_bytes{instance=~"$redis_instance"}
   ```
3. **Query B** (Max):
   ```promql
   redis_memory_max_bytes{instance=~"$redis_instance"}
   ```
4. **Visualization**: `Time series`
5. **Title**: `Redis Memory Usage vs Max`
6. **Legend**: `Used`, `Max`
7. **Unit**: `bytes`
8. Click **Apply**

### Step 3: Create Row 2 - Saturation

Saturation metrics show when Redis resources are full or under pressure.

#### Panel 1: Eviction Rate

1. **Add row** → `Saturation`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   rate(redis_evicted_keys_total{instance=~"$redis_instance"}[5m])
   ```
3. **Visualization**: `Time series`
4. **Title**: `Redis Eviction Rate`
5. **Unit**: `keys/sec`
6. **Description**: `Rate at which keys are evicted due to memory pressure`
7. Click **Apply**

**Note**: High eviction rate indicates memory saturation.

#### Panel 2: Memory Pressure (If Available)

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   redis_memory_pressure_ratio{instance=~"$redis_instance"}
   ```
3. **Visualization**: `Time series`
4. **Title**: `Memory Pressure Ratio`
5. **Unit**: `percent (0-100)`
6. Click **Apply**

**Note**: This metric might not exist in all exporters.

#### Panel 3: Connection Pool Exhaustion

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   redis_maxclients{instance=~"$redis_instance"}
   -
   redis_connected_clients{instance=~"$redis_instance"}
   ```
3. **Visualization**: `Time series`
4. **Title**: `Available Connections`
5. **Unit**: `short`
6. **Description**: `Remaining available connections. Low values indicate saturation`
7. Click **Apply**

### Step 4: Create Row 3 - Errors

Error metrics show Redis failures and issues.

#### Panel 1: Command Errors

1. **Add row** → `Errors`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   rate(redis_commands_errors_total{instance=~"$redis_instance"}[5m])
   ```
   Or:
   ```promql
   rate(redis_commands_total{status="error",instance=~"$redis_instance"}[5m])
   ```
3. **Visualization**: `Time series`
4. **Title**: `Command Errors Rate`
5. **Unit**: `errors/sec`
6. Click **Apply**

#### Panel 2: Connection Errors

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   rate(redis_connections_errors_total{instance=~"$redis_instance"}[5m])
   ```
3. **Visualization**: `Time series`
4. **Title**: `Connection Errors Rate`
5. **Unit**: `errors/sec`
6. Click **Apply**

#### Panel 3: Timeouts

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   rate(redis_commands_timeout_total{instance=~"$redis_instance"}[5m])
   ```
   Or if tracked via client:
   ```promql
   sum(rate(http_client_requests_seconds_count{
     uri=~".*redis.*",
     status=~"5.."
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Redis Timeouts`
5. **Unit**: `timeouts/sec`
6. Click **Apply**

### Step 5: Create Row 4 - Performance

Performance metrics for Redis operations.

#### Panel 1: Operations Rate

1. **Add row** → `Performance`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   rate(redis_commands_total{instance=~"$redis_instance"}[5m])
   ```
   Or by command type:
   ```promql
   sum(rate(redis_commands_total{instance=~"$redis_instance"}[5m])) by (command)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Redis Operations Rate`
5. **Legend**: `{{command}}` (if grouped)
6. **Unit**: `ops/sec`
7. Click **Apply**

#### Panel 2: Operation Latency

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(redis_commands_duration_seconds_bucket{
       instance=~"$redis_instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Redis Operation Latency (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

#### Panel 3: Cache Hit Ratio (If Applicable)

If Redis is used as cache:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(rate(redis_commands_total{command="GET",result="hit",instance=~"$redis_instance"}[5m]))
     /
     sum(rate(redis_commands_total{command="GET",instance=~"$redis_instance"}[5m]))
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Redis Cache Hit Ratio (%)`
5. **Unit**: `percent (0-100)`
6. Click **Apply**

### Step 6: Save Dashboard

1. Click **Save dashboard**
2. **Name**: `Redis - USE Method`
3. **Folder**: `2-Infrastructure`
4. **Tags**: `redis`, `infrastructure`, `use-method`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### USE Method Metrics

#### Utilization Metrics

- **Memory usage**: `redis_memory_used_bytes / redis_memory_max_bytes * 100`
- **Connection pool**: `redis_connected_clients / redis_maxclients * 100`
- **CPU usage**: `redis_cpu_usage_percent` (if available)

#### Saturation Metrics

- **Eviction rate**: `rate(redis_evicted_keys_total[5m])`
- **Memory pressure**: Various indicators (eviction rate, used memory %)
- **Queue length**: `redis_blocked_clients` (clients waiting)

#### Error Metrics

- **Command errors**: `rate(redis_commands_errors_total[5m])`
- **Connection errors**: `rate(redis_connections_errors_total[5m])`
- **Rejected connections**: `rate(redis_rejected_connections_total[5m])`

### Metric Naming Variations

Redis metrics vary by exporter:
- **redis_exporter**: `redis_*`
- **Spring Boot**: `spring_data_redis_*`
- **Custom**: Check your instrumentation

Query available metrics:
```promql
{__name__=~".*redis.*"}
```

---

## ⚙️ Panel Configuration (Advanced)

### Gauge Visualization

For utilization metrics:
1. Use **Gauge** visualization
2. Set min (0%) and max (100%)
3. Configure thresholds
4. Show percentage and absolute values

### Threshold Configuration

For memory usage:
- **Green**: 0-70% (Normal)
- **Yellow**: 70-90% (Warning)
- **Red**: 90-100% (Critical)

---

## ✅ Best Practices

### Redis Monitoring

1. **Monitor memory usage** - Prevent OOM errors
2. **Watch eviction rate** - High rate indicates memory saturation
3. **Track connection pool** - Prevent connection exhaustion
4. **Monitor errors** - Command/connection errors indicate issues

### Performance Optimization

1. **Memory configuration** - Set appropriate maxmemory
2. **Eviction policy** - Choose based on use case
3. **Connection pooling** - Optimize pool size
4. **Error tracking** - Address root causes

---

## 🐛 Troubleshooting

### "No data" for Redis metrics

**Check**:
1. Redis exporter is installed and running
2. Prometheus is scraping Redis metrics
3. Metric names match actual exporter

**Debug**:
```promql
# List all Redis metrics
{__name__=~".*redis.*"}

# Check specific metric
redis_memory_used_bytes
```

### High eviction rate

**Possible causes**:
1. Memory limit too low
2. Too many keys in Redis
3. Large values stored

**Solutions**:
1. Increase maxmemory
2. Review data stored in Redis
3. Optimize data structures

---

## 📚 References

### Related Dashboards
- [Config Control Service - Infrastructure Dependencies](../phase2-service-deep-dive/02-config-control-infrastructure-dependencies.md)

### External Documentation
- [Redis Monitoring](https://redis.io/docs/management/monitoring/)
- [Redis Exporter](https://github.com/oliver006/redis_exporter)
- [USE Method](https://www.brendangregg.com/usemethod.html)

---

**Next**: [MongoDB Dashboard](02-mongodb.md)  
**Previous**: [Config Server Dashboard](../phase2-service-deep-dive/03-config-server.md)

