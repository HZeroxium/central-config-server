# Config Control Service - Infrastructure Dependencies Dashboard

This dashboard monitors the health and performance of infrastructure dependencies used by Config Control Service: MongoDB, Redis, Kafka, Consul, and Config Server.

## 📊 Overview

### Purpose
Monitor infrastructure dependency health, connection pools, operation metrics, and identify dependency-related performance bottlenecks.

### Target Audience
- **SRE teams** - Dependency health monitoring and capacity planning
- **Developers** - Understanding dependency performance impact
- **Operations teams** - Dependency troubleshooting and optimization

### Key Metrics Covered
- **MongoDB**: Connection pool, operation rates, latency, errors
- **Redis Cache**: Hit ratios, operation rates, memory usage, errors
- **Kafka**: Message publish rates, consumer lag, operation latency
- **Consul**: API call rates, operation latency, health check status
- **Config Server**: Request rates, response times, error rates

### Prerequisites
- Prometheus scraping Config Control Service metrics
- Infrastructure components instrumented (Spring Boot auto-instrumentation)
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))
- Understanding of dependency connection patterns

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build each dependency section step-by-step
3. Adjust metric names based on Spring Boot's auto-instrumentation

### Step 2: Verify Dashboard Works

After creating all panels:
1. Check panels show data from dependencies
2. Verify connection pool metrics are available
3. Test operation rate calculations
4. Verify error rates are captured

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Set up variables: `service`, `instance` (similar to previous dashboards)

### Step 2: Create Row 1 - MongoDB

MongoDB metrics from Spring Boot's auto-instrumentation.

#### Panel 1: MongoDB Connection Pool Status

1. Click **Add visualization**
2. **Query**:
   ```promql
   # Active connections
   spring_data_mongodb_repository_health_indicator_active_connections{
     service="$service",
     instance=~"$instance"
   }
   ```
   Or if using different metric:
   ```promql
   # Connection pool active
   mongodb_pool_active_connections{
     service="$service",
     instance=~"$instance"
   }
   ```
3. **Visualization**: `Stat` or `Time series`
4. **Title**: `MongoDB Active Connections`
5. **Unit**: `short`
6. Click **Apply**

**Note**: Metric names may vary. Check `/actuator/prometheus` for actual names. Spring Boot may expose these as health indicators.

#### Panel 2: MongoDB Connection Pool Available

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   spring_data_mongodb_repository_health_indicator_available_connections{
     service="$service",
     instance=~"$instance"
   }
   ```
3. **Visualization**: `Stat`
4. **Title**: `MongoDB Available Connections`
5. **Unit**: `short`
6. Click **Apply**

#### Panel 3: MongoDB Operations Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(mongo_commands_total{
     service="$service",
     instance=~"$instance"
   }[5m])) by (command)
   ```
   Or if using Spring Data metrics:
   ```promql
   sum(rate(spring_data_mongodb_repository_operations_total{
     service="$service",
     instance=~"$instance"
   }[5m])) by (operation)
   ```
3. **Visualization**: `Time series`
4. **Title**: `MongoDB Operations Rate`
5. **Legend**: `{{command}}` or `{{operation}}`
6. **Unit**: `ops/sec`
7. Click **Apply**

**Note**: Check available MongoDB metrics in Prometheus. Spring Boot auto-instrumentation names may differ.

#### Panel 4: MongoDB Operation Latency p95

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(mongo_commands_duration_seconds_bucket{
       service="$service",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `MongoDB Operation Latency (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

#### Panel 5: MongoDB Error Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(mongo_commands_total{
     service="$service",
     instance=~"$instance",
     status="error"
   }[5m]))
   ```
   Or use health indicator:
   ```promql
   1 - spring_data_mongodb_repository_health_indicator_health{
     service="$service",
     instance=~"$instance",
     status="UP"
   }
   ```
3. **Visualization**: `Time series`
4. **Title**: `MongoDB Error Rate`
5. **Unit**: `ops/sec`
6. Click **Apply**

### Step 3: Create Row 2 - Redis Cache

Redis cache metrics from Spring Boot's auto-instrumentation.

#### Panel 1: Cache Hit Ratio (Overall)

1. **Add row** → `Redis Cache`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   sum(cache_hit_ratio{
     service="$service",
     instance=~"$instance"
   }) by (cache)
   ```
   Or calculate from counters:
   ```promql
   sum(rate(cache.gets{result="hit",service="$service",instance=~"$instance"}[5m]))
   /
   sum(rate(cache.gets{service="$service",instance=~"$instance"}[5m]))
   ```
4. **Visualization**: `Time series`
5. **Title**: `Cache Hit Ratio`
6. **Unit**: `percent (0-1)` or multiply by 100 for percentage
7. **Thresholds**: Add Y-axis threshold at 0.7 (70% hit ratio)
8. Click **Apply**

**Note**: Spring Boot auto-instruments cache metrics. Check `/actuator/prometheus` for exact names.

#### Panel 2: Cache Hit Ratio by Cache Name

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(cache.gets{result="hit",service="$service",instance=~"$instance"}[5m])) by (cache_name)
   /
   sum(rate(cache.gets{service="$service",instance=~"$instance"}[5m])) by (cache_name)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Cache Hit Ratio by Cache Name`
5. **Legend**: `{{cache_name}}`
6. **Unit**: `percent (0-1)`
7. Click **Apply**

#### Panel 3: Cache Operations Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(cache.gets{service="$service",instance=~"$instance"}[5m])) by (result)
   ```
   Separate queries:
   - **Query A**: `cache.gets{result="hit"}` → "Hits"
   - **Query B**: `cache.gets{result="miss"}` → "Misses"
3. **Visualization**: `Time series`
4. **Title**: `Cache Operations Rate`
5. **Legend**: `{{result}}`
6. **Unit**: `ops/sec`
7. Click **Apply**

#### Panel 4: Cache Size / Memory Usage

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(cache.size{
     service="$service",
     instance=~"$instance"
   }) by (cache_name)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Cache Size`
5. **Legend**: `{{cache_name}}`
6. **Unit**: `short`
7. Click **Apply**

#### Panel 5: Cache Error Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(cache.errors{
     service="$service",
     instance=~"$instance"
   }[5m])) by (cache_name, error_type)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Cache Error Rate`
5. **Legend**: `{{cache_name}} - {{error_type}}`
6. **Unit**: `errors/sec`
7. Click **Apply**

**Note**: If custom cache error metrics exist (from CacheMetrics.java), use those instead.

### Step 4: Create Row 3 - Kafka

Kafka messaging metrics.

#### Panel 1: Kafka Message Publish Rate

1. **Add row** → `Kafka`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(spring_kafka_producer_records_total{
     service="$service",
     instance=~"$instance"
   }[5m])) by (topic)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Kafka Message Publish Rate`
5. **Legend**: `{{topic}}`
6. **Unit**: `messages/sec`
7. Click **Apply**

**Note**: Spring Kafka auto-instruments producer/consumer metrics.

#### Panel 2: Kafka Producer Errors

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(spring_kafka_producer_errors_total{
     service="$service",
     instance=~"$instance"
   }[5m])) by (topic)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Kafka Producer Errors`
5. **Legend**: `{{topic}}`
6. **Unit**: `errors/sec`
7. Click **Apply**

#### Panel 3: Kafka Operation Latency

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(spring_kafka_producer_records_total_bucket{
       service="$service",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Kafka Operation Latency (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

#### Panel 4: Kafka Consumer Lag (If Applicable)

If your service consumes from Kafka:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(spring_kafka_consumer_lag_sum{
     service="$service",
     instance=~"$instance"
   }) by (topic, partition)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Kafka Consumer Lag`
5. **Legend**: `{{topic}} - partition {{partition}}`
6. **Unit**: `messages`
7. Click **Apply**

**Note**: Consumer lag metrics might not be available if service only produces.

### Step 5: Create Row 4 - Consul Service Discovery

Consul service discovery metrics.

#### Panel 1: Consul API Calls Rate

1. **Add row** → `Consul`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_client_requests_seconds_count{
     service="$service",
     instance=~"$instance",
     uri=~".*consul.*"
   }[5m]))
   ```
   Or if Consul client has specific metrics:
   ```promql
   sum(rate(spring_cloud_consul_discovery_client_requests_total{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Consul API Calls Rate`
5. **Unit**: `reqps`
6. Click **Apply**

#### Panel 2: Consul Operation Latency

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_client_requests_seconds_bucket{
       service="$service",
       instance=~"$instance",
       uri=~".*consul.*"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Consul Operation Latency (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

#### Panel 3: Consul Health Check Failures

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_client_requests_seconds_count{
     service="$service",
     instance=~"$instance",
     uri=~".*consul.*",
     status=~"5..|4.."
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Consul Health Check Failures`
5. **Unit**: `failures/sec`
6. Click **Apply**

### Step 6: Create Row 5 - Config Server Proxy

Config Server proxy metrics from ConfigProxyService.

#### Panel 1: Config Server Request Rate

1. **Add row** → `Config Server Proxy`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_client_requests_seconds_count{
     service="$service",
     instance=~"$instance",
     uri=~".*config-server.*"
   }[5m]))
   ```
   Or if custom metric exists:
   ```promql
   sum(rate(config_server_client_requests_total{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Config Server Request Rate`
5. **Unit**: `reqps`
6. Click **Apply**

#### Panel 2: Config Server Response Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_client_requests_seconds_bucket{
       service="$service",
       instance=~"$instance",
       uri=~".*config-server.*"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Config Server Response Time (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

#### Panel 3: Config Server Error Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(rate(http_client_requests_seconds_count{
       service="$service",
       instance=~"$instance",
       uri=~".*config-server.*",
       status=~"5..|4.."
     }[5m]))
     /
     sum(rate(http_client_requests_seconds_count{
       service="$service",
       instance=~"$instance",
       uri=~".*config-server.*"
     }[5m]))
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Config Server Error Rate (%)`
5. **Unit**: `percent (0-100)`
6. Click **Apply**

### Step 7: Save Dashboard

1. Click **Save dashboard**
2. **Name**: `Config Control Service - Infrastructure Dependencies`
3. **Folder**: `1-Services`
4. **Tags**: `infrastructure`, `dependencies`, `config-control-service`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### Spring Boot Auto-Instrumentation Metrics

Spring Boot automatically instruments many dependencies:

#### MongoDB Metrics

- `spring_data_mongodb_repository_health_indicator_*` - Health metrics
- `mongo_commands_total` - Command counters (if MongoDB driver exposes)
- `mongo_commands_duration_seconds` - Command duration histograms

#### Redis/Cache Metrics

- `cache.gets` - Cache get operations (with `result="hit"` or `result="miss"`)
- `cache.puts` - Cache put operations
- `cache.evictions` - Cache evictions
- `cache.size` - Current cache size
- `cache.hit.ratio` - Hit ratio gauge (if available)

#### Kafka Metrics

- `spring_kafka_producer_records_total` - Producer record counts
- `spring_kafka_producer_errors_total` - Producer errors
- `spring_kafka_consumer_records_total` - Consumer record counts
- `spring_kafka_consumer_lag_sum` - Consumer lag

### Finding Actual Metric Names

Query Prometheus for available metrics:

```promql
# MongoDB metrics
{__name__=~".*mongo.*"}

# Cache metrics
{__name__=~"cache.*"}

# Kafka metrics
{__name__=~".*kafka.*"}

# Consul metrics
{__name__=~".*consul.*"}
```

Or check `/actuator/prometheus` endpoint directly.

---

## ⚙️ Panel Configuration (Advanced)

### Connection Pool Visualization

For connection pools:
1. Show **Active** and **Available** as two series
2. Use **Gauge** visualization for current state
3. Add **Max connections** as threshold line
4. Color code based on utilization percentage

### Cache Hit Ratio Formatting

For percentages:
1. **Unit**: `percent (0-1)` or multiply query by 100
2. **Decimals**: 2-3 for precision
3. **Thresholds**: Y-axis at 0.7 (70%) as warning, 0.5 (50%) as critical

### Dependency Error Correlation

Show error patterns:
1. Overlay dependency errors with service errors
2. Use same time scale for comparison
3. Add annotations for deployment times

---

## ✅ Best Practices

### Dependency Monitoring

1. **Monitor connection pools** - Exhaustion indicates capacity issues
2. **Track operation rates** - Sudden drops indicate connectivity problems
3. **Watch error rates** - Dependency errors cascade to service errors
4. **Set capacity thresholds** - Alert before resource exhaustion

### Performance Optimization

1. **Cache hit ratios** - Low hit ratios indicate cache configuration issues
2. **Connection pool utilization** - High utilization needs capacity increase
3. **Operation latency** - Slow dependencies impact service performance
4. **Error correlation** - Link dependency errors to service errors

---

## 🐛 Troubleshooting

### "No data" for dependency metrics

**Check**:
1. Dependencies are actually being used (check logs/requests)
2. Spring Boot auto-instrumentation is enabled
3. Metrics names match actual instrumentation

**Debug**:
```promql
# Check all cache metrics
{__name__=~"cache.*"}

# Check MongoDB metrics
{__name__=~".*mongo.*"}

# Check Kafka metrics
{__name__=~".*kafka.*"}
```

### Connection pool metrics not available

**Possible causes**:
1. Health indicators not enabled
2. Connection pool metrics not exposed
3. Different metric names

**Solution**: Use HTTP client metrics as proxy:
```promql
# Use HTTP client metrics for dependency calls
http_client_requests_seconds_count{uri=~".*mongodb.*"}
```

---

## 📚 References

### Related Dashboards
- [Config Control Service - Golden Signals](../phase1-foundation/02-config-control-service-golden-signals.md)
- [Config Control Service - Business Operations](01-config-control-business-operations.md)
- [MongoDB Dashboard](../phase3-infrastructure/02-mongodb.md)
- [Redis Dashboard](../phase3-infrastructure/01-redis.md)

### External Documentation
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)
- [Spring Data MongoDB Metrics](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/#metrics)
- [Spring Cache Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics.supported-metrics.cache)

---

**Next**: [Config Server Dashboard](03-config-server.md)  
**Previous**: [Config Control Service - Business Operations](01-config-control-business-operations.md)

