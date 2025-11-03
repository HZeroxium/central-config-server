# PromQL Basics

This guide teaches you PromQL (Prometheus Query Language) from the ground up. You'll learn to write queries that power your Grafana dashboards.

## 🎯 What is PromQL?

**PromQL** (Prometheus Query Language) is used to query metrics from Prometheus. It allows you to:

- Select specific metrics
- Filter by labels
- Calculate rates and aggregations
- Compute percentiles and distributions

## 📊 Basic Concepts

### Metrics

A metric is a measurement with a name and optional labels.

**Format**: `<metric_name>{<label1>="<value1>", <label2>="<value2>", ...}`

**Example**:
```
http_server_requests_seconds_count{method="GET", status="200", uri="/api/heartbeat"}
```

This metric means:
- **Name**: `http_server_requests_seconds_count`
- **Labels**: 
  - `method="GET"`
  - `status="200"`
  - `uri="/api/heartbeat"`
- **Meaning**: Total count of GET requests to `/api/heartbeat` that returned 200

### Time Series

A time series is a sequence of values collected over time.

```
http_server_requests_seconds_count{method="GET"} 
  → [1523, 1524, 1527, 1530, 1532, ...] at different timestamps
```

## 🔍 Query Types

### Instant Query

Returns current value:
```
http_server_requests_seconds_count
```

### Range Query

Returns values over a time range (used in Grafana):
```
http_server_requests_seconds_count[5m]
```
Returns values over the last 5 minutes.

**Range durations**: `5m`, `1h`, `30s`, `1d`

## 🎨 Label Matching

### Exact Match

Select metrics with specific label value:
```
http_server_requests_seconds_count{method="GET"}
```

### Not Equal

Exclude specific label value:
```
http_server_requests_seconds_count{method!="GET"}
```

### Regex Match

Match label using regular expression:
```
http_server_requests_seconds_count{uri=~"/api/.*"}
```
Matches all URIs starting with `/api/`.

### Regex Not Match

Exclude using regex:
```
http_server_requests_seconds_count{uri!~"/api/admin.*"}
```

### Multiple Labels

Combine multiple label filters:
```
http_server_requests_seconds_count{method="GET", status="200"}
```

## 📈 Common Functions

### rate()

Calculate per-second rate from a counter over time range.

**Syntax**: `rate(metric[time_range])`

**Example**:
```promql
rate(http_server_requests_seconds_count[5m])
```

**What it does**:
- Takes counter values over 5 minutes
- Calculates per-second rate
- Accounts for counter resets

**Use case**: Request rate (requests per second), error rate

### irate()

Instant rate (last two data points). More reactive but less smooth.

**Example**:
```promql
irate(http_server_requests_seconds_count[5m])
```

**When to use**: Need immediate feedback (alerting), rapid changes

### increase()

Total increase over time range.

**Example**:
```promql
increase(http_server_requests_seconds_count[1h])
```

**Use case**: Total requests in the last hour

### sum()

Sum values across series.

**Example**:
```promql
sum(http_server_requests_seconds_count)
```

**With labels**: Sum by specific label
```promql
sum(http_server_requests_seconds_count) by (method)
```

**Use case**: Total requests across all instances, grouped by HTTP method

### avg()

Average across series.

**Example**:
```promql
avg(jvm_memory_used_bytes)
```

### min() / max()

Minimum or maximum value.

**Example**:
```promql
max(jvm_memory_max_bytes)
```

### count()

Count number of series.

**Example**:
```promql
count(up{job="config-control-service"})
```
Returns number of healthy instances.

## 🎯 Percentiles with Histograms

### histogram_quantile()

Calculate percentiles from histogram buckets.

**Syntax**: `histogram_quantile(quantile, histogram_metric)`

**Example**:
```promql
histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
)
```

**What it does**:
1. `rate(http_server_requests_seconds_bucket[5m])` - Rate of requests in each bucket
2. `sum(...) by (le)` - Sum buckets by `le` (less than or equal) label
3. `histogram_quantile(0.95, ...)` - Calculate 95th percentile

**Common percentiles**:
- `0.50` - Median (p50)
- `0.90` - p90
- `0.95` - p95
- `0.99` - p99

**Use case**: Response time percentiles (p95 latency)

## 📊 Aggregation Examples

### Group By

Aggregate while preserving labels:

```promql
sum(rate(http_server_requests_seconds_count[5m])) by (method)
```

Result: Request rate grouped by HTTP method.

### Without Labels

Aggregate removing specific labels:

```promql
sum(rate(http_server_requests_seconds_count[5m])) without (instance)
```

### Combining Aggregations

```promql
sum(
  rate(http_server_requests_seconds_count[5m])
) by (method, status)
```

## 🧮 Mathematical Operations

### Arithmetic

Add, subtract, multiply, divide:

```promql
# Error rate percentage
(rate(http_server_requests_seconds_count{status=~"5..|4.."}[5m]) 
 / 
 rate(http_server_requests_seconds_count[5m])) * 100
```

### Percentage Calculation

```promql
# Memory usage percentage
(jvm_memory_used_bytes{area="heap"} 
 / 
 jvm_memory_max_bytes{area="heap"}) * 100
```

### Division with Error Handling

Handle division by zero:

```promql
(
  rate(http_server_requests_seconds_count{status=~"5.."}[5m])
  /
  (rate(http_server_requests_seconds_count[5m]) > 0)
) * 100
```

## 📝 Common Patterns

### Request Rate

```promql
sum(rate(http_server_requests_seconds_count[5m]))
```

### Request Rate by Method

```promql
sum(rate(http_server_requests_seconds_count[5m])) by (method)
```

### Error Rate (Percentage)

```promql
100 * (
  sum(rate(http_server_requests_seconds_count{status=~"5..|4.."}[5m]))
  /
  sum(rate(http_server_requests_seconds_count[5m]))
)
```

### Latency p95

```promql
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
)
```

### Latency p50, p95, p99 (Multiple Percentiles)

Create separate queries for each:
```promql
# p50
histogram_quantile(0.50, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))

# p95
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))

# p99
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))
```

### Memory Usage Percentage

```promql
100 * (
  sum(jvm_memory_used_bytes{area="heap"})
  /
  sum(jvm_memory_max_bytes{area="heap"})
)
```

### Active Instances Count

```promql
count(up{job="config-control-service"} == 1)
```

### Top N Queries

```promql
topk(10, sum(rate(http_server_requests_seconds_count[5m])) by (uri))
```

Returns top 10 URIs by request rate.

### Conditional Queries

```promql
# Only show if requests exist
rate(http_server_requests_seconds_count[5m]) > 0

# Filter by threshold
histogram_quantile(0.95, ...) > 0.5
```

## 🎯 Using Variables in Grafana

In Grafana, variables are referenced with `$variable_name`:

```promql
# Use $service variable
rate(http_server_requests_seconds_count{service="$service"}[5m])

# Use $instance variable (can be multi-value)
rate(http_server_requests_seconds_count{instance=~"$instance"}[5m])

# Chain variables
rate(http_server_requests_seconds_count{service="$service", instance=~"$instance"}[5m])
```

**Variable syntax**:
- `$service` - Single value
- `$instance` - Multi-value (use with `=~` for regex match)

## 🔍 Query Debugging Tips

### 1. Start Simple

Start with basic metric name:
```promql
http_server_requests_seconds_count
```

### 2. Add Filters Gradually

```promql
# Step 1
http_server_requests_seconds_count

# Step 2
http_server_requests_seconds_count{method="GET"}

# Step 3
http_server_requests_seconds_count{method="GET", status="200"}
```

### 3. Test in Grafana Explore

Use Grafana Explore to test queries before adding to panels:
1. Go to **Explore**
2. Select **Prometheus** data source
3. Enter query
4. Verify results look correct

### 4. Check for Data

If query returns no data:
```promql
# First check if metric exists
http_server_requests_seconds_count

# Check without filters
http_server_requests_seconds_count

# Check label values
label_values(http_server_requests_seconds_count, method)
```

### 5. Validate Syntax

Common syntax errors:
- Missing closing parenthesis
- Missing quotes around label values
- Wrong range duration format

## 💡 Best Practices

### 1. Use rate() for Counters

Always use `rate()` or `irate()` with counters:
```promql
# ✅ Good
rate(http_server_requests_seconds_count[5m])

# ❌ Bad (absolute value, not useful)
http_server_requests_seconds_count
```

### 2. Choose Appropriate Time Range

- Short range (`[1m]`): More reactive, more noise
- Long range (`[5m]`): Smoother, less reactive

**Recommendation**: Use `[5m]` for most dashboards.

### 3. Aggregate Before Calculating Percentiles

```promql
# ✅ Good (aggregate first)
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
)

# ❌ Bad (percentile per series, then aggregate)
avg(histogram_quantile(0.95, rate(...)))
```

### 4. Use by() for Grouping

```promql
# ✅ Good (preserve labels for grouping)
sum(rate(...)) by (method, status)

# ❌ Avoid (loses grouping)
sum(rate(...))
```

### 5. Filter Early

Apply filters early in query:
```promql
# ✅ Good (filter first)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))

# Less efficient
sum(rate(http_server_requests_seconds_count[5m])) and on() (status=~"5..")
```

## 📚 Common Metrics in Config Control Service

### HTTP Metrics

```promql
# Total requests
http_server_requests_seconds_count

# Request rate
rate(http_server_requests_seconds_count[5m])

# Request duration buckets (for percentiles)
http_server_requests_seconds_bucket

# Request duration sum
http_server_requests_seconds_sum
```

### JVM Metrics

```promql
# Memory usage
jvm_memory_used_bytes{area="heap"}

# Memory max
jvm_memory_max_bytes{area="heap"}

# GC pause time
jvm_gc_pause_seconds_sum

# GC collections
jvm_gc_pause_seconds_count
```

### Custom Metrics

```promql
# Heartbeat processing
heartbeat.process_count
heartbeat.process_sum

# Drift events
config_control.drift_event.save_count

# Service operations
config_control.service_instance.save_count
```

## 🎓 Practice Queries

Try these queries in Grafana Explore:

1. **Total request rate across all services**
   ```promql
   sum(rate(http_server_requests_seconds_count[5m]))
   ```

2. **Request rate by HTTP method**
   ```promql
   sum(rate(http_server_requests_seconds_count[5m])) by (method)
   ```

3. **Error rate percentage**
   ```promql
   100 * (
     sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
     /
     sum(rate(http_server_requests_seconds_count[5m]))
   )
   ```

4. **p95 latency**
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
   )
   ```

5. **Memory usage percentage**
   ```promql
   100 * (
     sum(jvm_memory_used_bytes{area="heap"})
     /
     sum(jvm_memory_max_bytes{area="heap"})
   )
   ```

## 📖 Next Steps

Now that you understand PromQL:

1. ✅ **You can write basic queries**
2. ✅ **You understand functions like rate() and histogram_quantile()**
3. ✅ **You know how to filter and aggregate**

**Next**: Create your first dashboard:
- [Platform Overview Dashboard](phase1-foundation/01-platform-overview.md)

---

**Previous**: [Prerequisites](02-prerequisites.md)  
**Next**: [Platform Overview Dashboard](phase1-foundation/01-platform-overview.md)

