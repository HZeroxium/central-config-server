# MongoDB Dashboard (USE Method)

This dashboard monitors MongoDB performance using the USE method: Utilization, Saturation, and Errors.

## 📊 Overview

### Purpose
Monitor MongoDB health, connection pool, operation performance, and errors using the USE method.

### Target Audience
- **SRE teams** - MongoDB performance and capacity monitoring
- **Operations teams** - MongoDB troubleshooting and optimization
- **Database administrators** - Database performance monitoring

### USE Method
- **Utilization**: Connection pool usage %, CPU usage %
- **Saturation**: Queue length, lock wait time, connection wait time
- **Errors**: Operation errors, connection errors, replication lag

### Prerequisites
- MongoDB running and accessible
- MongoDB exporter installed (if needed) or Spring Boot auto-instrumentation
- Prometheus scraping MongoDB metrics
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build USE method sections (Utilization, Saturation, Errors)
3. Adjust metric names based on available MongoDB metrics

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Set up variables if needed (environment, mongodb_instance)

### Step 2: Create Row 1 - Utilization

#### Panel 1: Connection Pool Usage %

1. Click **Add visualization**
2. **Query**:
   ```promql
   100 * (
     mongodb_connections_active{instance=~"$mongodb_instance"}
     /
     mongodb_connections_available{instance=~"$mongodb_instance"}
   )
   ```
   Or if using Spring Boot:
   ```promql
   100 * (
     spring_data_mongodb_repository_health_indicator_active_connections
     /
     spring_data_mongodb_repository_health_indicator_max_connections
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `MongoDB Connection Pool Usage %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-70%), Yellow (70-90%), Red (90-100%)
7. Click **Apply**

#### Panel 2: CPU Usage %

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   mongodb_cpu_usage_percent{instance=~"$mongodb_instance"}
   ```
3. **Visualization**: `Stat` or `Time series`
4. **Title**: `MongoDB CPU Usage %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-70%), Yellow (70-90%), Red (90-100%)
7. Click **Apply**

#### Panel 3: Connection Pool Active vs Available

1. **Add panel** → **Add visualization**
2. **Query A** (Active):
   ```promql
   mongodb_connections_active{instance=~"$mongodb_instance"}
   ```
3. **Query B** (Available):
   ```promql
   mongodb_connections_available{instance=~"$mongodb_instance"}
   ```
4. **Visualization**: `Time series`
5. **Title**: `Connection Pool Status`
6. **Legend**: `Active`, `Available`
7. **Unit**: `short`
8. Click **Apply**

### Step 3: Create Row 2 - Saturation

#### Panel 1: Queue Length

1. **Add row** → `Saturation`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   mongodb_global_lock_current_queue_total{instance=~"$mongodb_instance"}
   ```
3. **Visualization**: `Time series`
4. **Title**: `MongoDB Queue Length`
5. **Unit**: `short`
6. **Description**: `Number of operations waiting in queue. High values indicate saturation`
7. Click **Apply**

#### Panel 2: Lock Wait Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   rate(mongodb_global_lock_total_lock_time_milliseconds{instance=~"$mongodb_instance"}[5m])
   ```
3. **Visualization**: `Time series`
4. **Title**: `Lock Wait Time (per second)`
5. **Unit**: `milliseconds`
6. Click **Apply**

#### Panel 3: Connection Wait Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(mongodb_connections_duration_seconds_bucket{
       instance=~"$mongodb_instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Connection Wait Time (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

### Step 4: Create Row 3 - Errors

#### Panel 1: Operation Errors

1. **Add row** → `Errors`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   rate(mongodb_operation_errors_total{instance=~"$mongodb_instance"}[5m])
   ```
   Or:
   ```promql
   sum(rate(mongodb_commands_total{status="error",instance=~"$mongodb_instance"}[5m])) by (command)
   ```
3. **Visualization**: `Time series`
4. **Title**: `MongoDB Operation Errors Rate`
5. **Legend**: `{{command}}` (if grouped)
6. **Unit**: `errors/sec`
7. Click **Apply**

#### Panel 2: Connection Errors

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   rate(mongodb_connections_errors_total{instance=~"$mongodb_instance"}[5m])
   ```
3. **Visualization**: `Time series`
4. **Title**: `Connection Errors Rate`
5. **Unit**: `errors/sec`
6. Click **Apply**

#### Panel 3: Replication Lag (If Replica Set)

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   mongodb_replication_lag_seconds{instance=~"$mongodb_instance"}
   ```
3. **Visualization**: `Time series`
4. **Title**: `Replication Lag`
5. **Unit**: `seconds`
6. **Thresholds**: Y-axis at 10s (warning), 30s (critical)
7. Click **Apply**

**Note**: Only applicable for replica set configurations.

### Step 5: Create Row 4 - Performance

#### Panel 1: Operations Rate

1. **Add row** → `Performance`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(mongodb_commands_total{instance=~"$mongodb_instance"}[5m])) by (command)
   ```
3. **Visualization**: `Time series`
4. **Title**: `MongoDB Operations Rate`
5. **Legend**: `{{command}}`
6. **Unit**: `ops/sec`
7. Click **Apply**

#### Panel 2: Operation Latency

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(mongodb_commands_duration_seconds_bucket{
       instance=~"$mongodb_instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `MongoDB Operation Latency (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

#### Panel 3: Read vs Write Operations

1. **Add panel** → **Add visualization**
2. **Query A** (Read):
   ```promql
   sum(rate(mongodb_commands_total{command=~"find|count|aggregate",instance=~"$mongodb_instance"}[5m]))
   ```
3. **Query B** (Write):
   ```promql
   sum(rate(mongodb_commands_total{command=~"insert|update|delete",instance=~"$mongodb_instance"}[5m]))
   ```
4. **Visualization**: `Time series`
5. **Title**: `Read vs Write Operations`
6. **Legend**: `Read`, `Write`
7. **Unit**: `ops/sec`
8. Click **Apply**

### Step 6: Save Dashboard

1. Click **Save dashboard**
2. **Name**: `MongoDB - USE Method`
3. **Folder**: `2-Infrastructure`
4. **Tags**: `mongodb`, `infrastructure`, `use-method`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### MongoDB Metrics

MongoDB metrics vary by exporter:
- **mongodb_exporter**: `mongodb_*`
- **Spring Boot**: `spring_data_mongodb_*`
- **Custom**: Check your instrumentation

### Metric Naming Variations

Query available metrics:
```promql
{__name__=~".*mongo.*"}
```

---

## ✅ Best Practices

### MongoDB Monitoring

1. **Monitor connection pool** - Prevent exhaustion
2. **Watch queue length** - High queues indicate saturation
3. **Track operation latency** - Slow operations impact application
4. **Monitor replication lag** - For replica sets

---

## 🐛 Troubleshooting

### "No data" for MongoDB metrics

**Check**:
1. MongoDB exporter is installed and running
2. Prometheus is scraping MongoDB metrics
3. Metric names match actual exporter

**Debug**:
```promql
{__name__=~".*mongo.*"}
```

---

## 📚 References

### Related Dashboards
- [Config Control Service - Infrastructure Dependencies](../phase2-service-deep-dive/02-config-control-infrastructure-dependencies.md)

### External Documentation
- [MongoDB Monitoring](https://www.mongodb.com/docs/manual/administration/monitoring/)
- [MongoDB Exporter](https://github.com/percona/mongodb_exporter)

---

**Next**: [Kafka Dashboard](03-kafka.md)  
**Previous**: [Redis Dashboard](01-redis.md)

