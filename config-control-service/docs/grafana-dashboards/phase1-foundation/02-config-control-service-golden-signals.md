# Config Control Service - Golden Signals Dashboard

This dashboard provides detailed service-level monitoring following the Golden Signals methodology (Latency, Traffic, Errors, Saturation). It's essential for understanding service performance and troubleshooting issues.

## 📊 Overview

### Purpose
Monitor Config Control Service performance at the service level using Golden Signals methodology. Track latency, traffic patterns, error rates, and resource saturation.

### Target Audience
- **Developers** - Debugging performance issues, understanding service behavior
- **SRE teams** - Capacity planning, performance optimization
- **On-call engineers** - Service health monitoring during incidents
- **Performance engineers** - Latency and throughput analysis

### Key Metrics Covered
- **Latency**: p50, p95, p99 response times
- **Traffic**: Request rate by method, endpoint, and category
- **Errors**: Error rate over time, by status code, and by endpoint
- **Saturation**: JVM heap usage, GC metrics, thread pool utilization

### Golden Signals Methodology
1. **Latency** - How long requests take (p50/p95/p99)
2. **Traffic** - How much demand (requests per second)
3. **Errors** - How many requests fail (error rate percentage)
4. **Saturation** - How full resources are (heap usage, CPU, threads)

### Prerequisites
- Prometheus scraping metrics from Config Control Service
- Grafana configured with Prometheus data source
- Config Control Service running and exposing metrics
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))
- Completed [Platform Overview Dashboard](01-platform-overview.md) (recommended)

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build each panel step-by-step
3. Customize queries and thresholds for your environment

### Step 2: Verify Dashboard Works

After creating all panels:
1. Check all panels show data (not "No data")
2. Verify time picker shows appropriate time range
3. Test variables (service, instance) if configured
4. Verify thresholds are meaningful

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

This tutorial walks you through creating the complete Golden Signals dashboard.

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Click **Add visualization** → **Add panel**
3. Click **Dashboard settings** (gear icon) to configure

### Step 2: Set Up Variables

Variables make your dashboard reusable across services and instances.

#### Variable 1: Service

1. Dashboard settings → **Variables** → **Add variable**
2. Configure:
   - **Name**: `service`
   - **Type**: `Query`
   - **Data source**: `Prometheus`
   - **Query**: `label_values(http_server_requests_seconds_count, service)`
   - **Label**: `Service`
   - **Multi-value**: `Yes`
   - **Include All**: `Yes`
   - **Default value**: `All` or `config-control-service`
3. Click **Apply**

#### Variable 2: Instance

1. Add another variable:
   - **Name**: `instance`
   - **Type**: `Query`
   - **Query**: `label_values(http_server_requests_seconds_count{service="$service"}, instance)`
   - **Label**: `Instance`
   - **Multi-value**: `Yes`
   - **Include All**: `Yes`
   - **Regex**: (optional filter, e.g., `/.+-.*/`)
2. Click **Apply**

#### Variable 3: Environment (Optional)

1. Add variable:
   - **Name**: `environment`
   - **Type**: `Query`
   - **Query**: `label_values(http_server_requests_seconds_count, environment)`
   - **Label**: `Environment`
   - **Default value**: `development` or `All`
2. Click **Apply**

### Step 3: Create Row 1 - KPIs (Stat Panels)

KPI panels provide at-a-glance metrics with color-coded thresholds.

#### Panel 1: Error Rate (%)

1. Click **Add visualization**
2. **Query tab**:
   ```promql
   100 * (
     sum(rate(http_server_requests_seconds_count{
       service="$service", 
       status=~"5..|4..", 
       instance=~"$instance"
     }[5m]))
     /
     (sum(rate(http_server_requests_seconds_count{
       service="$service", 
       instance=~"$instance"
     }[5m])) > 0)
   )
   ```
3. **Visualization tab**:
   - **Visualization**: `Stat`
   - **Title**: `Error Rate`
   - **Options** → **Text**: `Value and name`
   - **Field** → **Unit**: `percent (0-100)`
   - **Field** → **Decimals**: `2`
4. **Field** → **Thresholds**:
   - **Mode**: `Absolute`
   - **Steps**:
     - **Green**: `null` to `1` (Error rate < 1%)
     - **Yellow**: `1` to `5` (Error rate 1-5%)
     - **Red**: `5` to `100` (Error rate > 5%)
5. **Panel options** → **Description**: `Percentage of requests with 4xx or 5xx status codes`
6. Click **Apply**

**What this query does**: Calculates percentage of requests with error status codes (4xx/5xx).

#### Panel 2: p95 Latency

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket{
       service="$service",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `p95 Latency`
5. **Unit**: `seconds`
6. **Decimals**: `3`
7. **Thresholds**:
   - **Green**: `null` to `0.2` (< 200ms)
   - **Yellow**: `0.2` to `0.5` (200-500ms)
   - **Red**: `0.5` to `10` (> 500ms)
8. **Description**: `95th percentile response time`
9. Click **Apply**

#### Panel 3: Request Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Stat`
4. **Title**: `Request Rate`
5. **Unit**: `reqps` (requests per second)
6. **Decimals**: `2`
7. **Description**: `Total requests per second across all instances`
8. Click **Apply**

#### Panel 4: Active Instances Count

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   count(up{service="$service"} == 1)
   ```
3. **Visualization**: `Stat`
4. **Title**: `Active Instances`
5. **Unit**: `short`
6. **Thresholds**:
   - **Red**: `null` to `0.5` (No instances)
   - **Yellow**: `0.5` to `1.5` (One instance)
   - **Green**: `1.5` to `10` (Multiple instances)
7. Click **Apply**

### Step 4: Create Row 2 - Traffic (Time Series)

Traffic panels show request patterns over time.

#### Panel 1: Request Rate by Method

1. **Add row** → Type: `Traffic`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_count{
     service="$service",
     instance=~"$instance"
   }[5m])) by (method)
   ```
4. **Visualization**: `Time series`
5. **Title**: `Request Rate by Method`
6. **Legend**: `{{method}}`
7. **Unit**: `reqps`
8. **Options** → **Legend**: `Right`, `Show`: `Yes`
9. Click **Apply**

#### Panel 2: Request Rate by Endpoint Category

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_count{
     service="$service",
     instance=~"$instance",
     uri=~"/api/(heartbeat|services|drift|admin|registry|config-server).*"
   }[5m])) by (uri)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Request Rate by Endpoint Category`
5. **Legend**: `{{uri}}`
6. **Unit**: `reqps`
7. Click **Apply**

**Note**: Adjust URI patterns based on your actual endpoint structure.

#### Panel 3: Request Rate Top 10 Endpoints

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   topk(10, sum(rate(http_server_requests_seconds_count{
     service="$service",
     instance=~"$instance"
   }[5m])) by (uri))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Top 10 Endpoints by Request Rate`
5. **Legend**: `{{uri}}`
6. **Unit**: `reqps`
7. Click **Apply**

### Step 5: Create Row 3 - Latency (Time Series)

Latency panels show response time patterns and percentiles.

#### Panel 1: Response Time p50/p95/p99 (Overlay)

Create three queries in one panel:

1. **Add row** → `Latency`
2. **Add panel** → **Add visualization**
3. **Query A** (p50):
   ```promql
   histogram_quantile(0.50,
     sum(rate(http_server_requests_seconds_bucket{
       service="$service",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
4. **Query B** (p95):
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket{
       service="$service",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
5. **Query C** (p99):
   ```promql
   histogram_quantile(0.99,
     sum(rate(http_server_requests_seconds_bucket{
       service="$service",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
6. **Visualization**: `Time series`
7. **Title**: `Response Time Percentiles`
8. **Legend**: 
   - **Format**: `{{query}}`
   - Or set custom: `p50`, `p95`, `p99` via **Legend overrides**
9. **Unit**: `seconds`
10. **Options** → **Legend**: `Right`
11. Click **Apply**

#### Panel 2: Response Time by Endpoint Category

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket{
       service="$service",
       instance=~"$instance",
       uri=~"/api/(heartbeat|services|drift|admin|registry).*"
     }[5m])) by (le, uri)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `p95 Latency by Endpoint Category`
5. **Legend**: `{{uri}}`
6. **Unit**: `seconds`
7. Click **Apply**

#### Panel 3: Response Time Heatmap (Advanced)

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_bucket{
     service="$service",
     instance=~"$instance"
   }[5m])) by (le)
   ```
3. **Visualization**: `Heatmap`
4. **Title**: `Response Time Distribution`
5. **Options** → **Calculation**: `Count`
6. **Options** → **Bucket offset**: `0`
7. Click **Apply**

**Note**: Heatmap requires histogram buckets. Ensure your service emits histogram metrics.

### Step 6: Create Row 4 - Errors (Time Series + Table)

Error panels show failure patterns and help identify problematic endpoints.

#### Panel 1: Error Rate Over Time

1. **Add row** → `Errors`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   100 * (
     sum(rate(http_server_requests_seconds_count{
       service="$service",
       status=~"5..|4..",
       instance=~"$instance"
     }[5m]))
     /
     (sum(rate(http_server_requests_seconds_count{
       service="$service",
       instance=~"$instance"
     }[5m])) > 0)
   )
   ```
4. **Visualization**: `Time series`
5. **Title**: `Error Rate Over Time (%)`
6. **Unit**: `percent (0-100)`
7. **Thresholds**: Add thresholds at Y-axis (1%, 5%)
8. Click **Apply**

#### Panel 2: Error Rate by Status Code

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_count{
     service="$service",
     status=~"4..|5..",
     instance=~"$instance"
   }[5m])) by (status)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Error Rate by Status Code`
5. **Legend**: `{{status}} - {{query}}`
6. **Unit**: `reqps`
7. Click **Apply**

#### Panel 3: Top 10 Error Endpoints (Table)

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   topk(10, sum(rate(http_server_requests_seconds_count{
     service="$service",
     status=~"4..|5..",
     instance=~"$instance"
   }[5m])) by (uri))
   ```
3. **Visualization**: `Table`
4. **Title**: `Top 10 Error Endpoints`
5. **Transform** → **Organize fields**:
   - Show: `uri` (rename to "Endpoint")
   - Show: `Value` (rename to "Error Rate", unit: `reqps`)
   - Hide: other fields
6. **Options** → **Sort by**: `Error Rate`, **Descending**: `Yes`
7. Click **Apply**

#### Panel 4: Error Rate by Exception Type (If Instrumented)

If your service emits exception type labels:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_count{
     service="$service",
     status=~"5..",
     exception=~".+",
     instance=~"$instance"
   }[5m])) by (exception)
   ```
3. **Visualization**: `Time series` or `Table`
4. **Title**: `Error Rate by Exception Type`
5. Click **Apply**

**Note**: This panel only works if your metrics include exception type labels. Adjust based on your instrumentation.

### Step 7: Create Row 5 - Saturation (JVM)

Saturation panels show resource utilization (heap, GC, threads).

#### Panel 1: JVM Heap Usage %

1. **Add row** → `Saturation (JVM)`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   100 * (
     sum(jvm_memory_used_bytes{
       service="$service",
       area="heap",
       instance=~"$instance"
     })
     /
     sum(jvm_memory_max_bytes{
       service="$service",
       area="heap",
       instance=~"$instance"
     })
   )
   ```
4. **Visualization**: `Time series`
5. **Title**: `JVM Heap Usage %`
6. **Unit**: `percent (0-100)`
7. **Thresholds**: 
   - Add Y-axis threshold at 80% (warning)
   - Add Y-axis threshold at 90% (critical)
8. Click **Apply**

#### Panel 2: JVM Heap Usage with Max Line

1. **Add panel** → **Add visualization**
2. **Query A** (Used):
   ```promql
   sum(jvm_memory_used_bytes{
     service="$service",
     area="heap",
     instance=~"$instance"
   })
   ```
3. **Query B** (Max):
   ```promql
   sum(jvm_memory_max_bytes{
     service="$service",
     area="heap",
     instance=~"$instance"
   })
   ```
4. **Visualization**: `Time series`
5. **Title**: `JVM Heap Usage`
6. **Legend**: `Used`, `Max`
7. **Unit**: `bytes` (or `decbytes` for MB/GB)
8. Click **Apply**

#### Panel 3: JVM Non-Heap Memory

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(jvm_memory_used_bytes{
     service="$service",
     area=~"nonheap",
     instance=~"$instance"
   })
   ```
3. **Visualization**: `Time series`
4. **Title**: `JVM Non-Heap Memory`
5. **Unit**: `bytes`
6. Click **Apply**

#### Panel 4: GC Pause Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(jvm_gc_pause_seconds_sum{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `GC Pause Time (per second)`
5. **Unit**: `seconds`
6. **Legend**: `{{gc}}` (if gc label exists)
7. Click **Apply**

**Alternative**: Total GC pause time over period
```promql
sum(rate(jvm_gc_pause_seconds_sum{
  service="$service",
  instance=~"$instance"
}[5m]))
```

#### Panel 5: GC Collection Count

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(jvm_gc_pause_seconds_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `GC Collection Rate`
5. **Unit**: `ops/sec` (operations per second)
6. **Legend**: `{{gc}}`
7. Click **Apply**

#### Panel 6: Thread Pool Utilization

If your service exposes thread pool metrics:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(jvm_threads_live_threads{
       service="$service",
       instance=~"$instance"
     })
     /
     sum(jvm_threads_peak_threads{
       service="$service",
       instance=~"$instance"
     })
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Thread Pool Utilization %`
5. **Unit**: `percent (0-100)`
6. Click **Apply**

**Note**: Thread pool metrics might be named differently. Check your actual metrics and adjust.

### Step 8: Create Row 6 - Request Details

Detailed request information panels.

#### Panel 1: Top 10 Endpoints by Request Count

1. **Add row** → `Request Details`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   topk(10, sum(rate(http_server_requests_seconds_count{
     service="$service",
     instance=~"$instance"
   }[5m])) by (uri))
   ```
4. **Visualization**: `Table`
5. **Title**: `Top 10 Endpoints by Request Count`
6. **Transform** → **Organize fields**:
   - Show: `uri` → "Endpoint"
   - Show: `Value` → "Request Rate" (unit: `reqps`)
   - Hide: other fields
7. **Options** → **Sort by**: `Request Rate`, **Descending**: `Yes`
8. Click **Apply**

#### Panel 2: Request Duration Distribution

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_bucket{
     service="$service",
     instance=~"$instance"
   }[5m])) by (le)
   ```
3. **Visualization**: `Histogram` (if available) or `Time series`
4. **Title**: `Request Duration Distribution`
5. **Legend**: `{{le}}`
6. **Unit**: `seconds`
7. Click **Apply**

#### Panel 3: Active Requests (Gauge)

If your service exposes active request metrics:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(http_server_active_requests{
     service="$service",
     instance=~"$instance"
   })
   ```
3. **Visualization**: `Stat` or `Gauge`
4. **Title**: `Active Requests`
5. **Unit**: `short`
6. Click **Apply**

**Note**: Active request metrics might not be available by default. This panel is optional.

### Step 9: Save Dashboard

1. Click **Save dashboard** (floppy disk icon)
2. **Name**: `Config Control Service - Golden Signals`
3. **Folder**: Create or select `1-Services`
4. **Tags**: `golden-signals`, `config-control-service`, `monitoring`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### Understanding Key Queries

#### Error Rate Calculation

```promql
100 * (
  sum(rate(http_server_requests_seconds_count{status=~"5..|4.."}[5m]))
  /
  (sum(rate(http_server_requests_seconds_count[5m])) > 0)
)
```

**Breakdown**:
1. `status=~"5..|4.."` - Regex match: matches status codes starting with 4 or 5 (4xx, 5xx errors)
2. `rate(...[5m])` - Calculate per-second rate over 5 minutes
3. `sum(...)` - Sum across all labels (methods, URIs, instances)
4. Divide errors by total requests
5. `> 0` - Prevents division by zero (returns 0 if no requests)
6. `* 100` - Convert to percentage

**Why use `> 0`?** Without it, if there are no requests, the denominator is 0 and the result is NaN. With `> 0`, if denominator is 0, result is 0.

#### Percentile Calculation

```promql
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
)
```

**Breakdown**:
1. `http_server_requests_seconds_bucket` - Histogram buckets (distribution of latencies)
2. Each bucket has `le` label (less than or equal) indicating threshold
3. `rate(...[5m])` - Rate of requests falling into each bucket
4. `sum(...) by (le)` - Sum buckets with same `le` across instances/methods
5. `histogram_quantile(0.95, ...)` - Calculate 95th percentile from distribution

**Why sum by (le)?** Multiple instances each have their own buckets. We need to combine buckets with the same threshold (`le`) to get overall distribution.

#### Heap Usage Percentage

```promql
100 * (
  sum(jvm_memory_used_bytes{area="heap"})
  /
  sum(jvm_memory_max_bytes{area="heap"})
)
```

**Breakdown**:
1. `jvm_memory_used_bytes{area="heap"}` - Current heap usage per instance
2. `jvm_memory_max_bytes{area="heap"}` - Maximum heap size per instance
3. `sum(...)` - Sum across all instances (if multiple)
4. Divide used by max
5. `* 100` - Convert to percentage

### Alternative Queries

#### Error Rate (Alternative - Simpler)

```promql
100 * (
  sum(rate(http_server_requests_seconds_count{status=~"5..|4.."}[5m]))
  /
  sum(rate(http_server_requests_seconds_count[5m]))
)
```

**Trade-off**: Simpler but may produce NaN if no requests. Use if you're certain requests exist.

#### Percentiles (Multiple in One Query)

To show p50, p95, p99 in one panel:
- Create separate queries (Query A, B, C)
- Use different `histogram_quantile` values (0.50, 0.95, 0.99)
- Label them in legend overrides

#### Request Rate by Method (With Error Breakdown)

Show total and errors:
```promql
# Query A: Total
sum(rate(http_server_requests_seconds_count[5m])) by (method)

# Query B: Errors
sum(rate(http_server_requests_seconds_count{status=~"5..|4.."}[5m])) by (method)
```

Stack them to show error portion.

### Performance Optimizations

1. **Use recording rules** for expensive percentile calculations
2. **Limit time range** - Use `[5m]` not `[1h]` for rate calculations
3. **Filter early** - Apply service/instance filters first
4. **Avoid high-cardinality labels** in every query (e.g., avoid `uri` in all panels)

---

## ⚙️ Panel Configuration (Advanced)

### Advanced Time Series Options

#### Multiple Y-Axes

For panels with different units (e.g., request rate vs error rate):
1. **Field** → **Overrides** → **Add field override**
2. **Fields with name**: Select field (e.g., `Error Rate`)
3. **Add override**: **Custom**: `Unit` → `percent`
4. **Add override**: **Axis**: **Placement** → `Right`

#### Stacking

For stacked area charts:
1. **Options** → **Stacking** → **Mode**: `Normal`
2. **Fill opacity**: Adjust transparency
3. Useful for showing request breakdown by method/status

#### Thresholds on Time Series

Add horizontal threshold lines:
1. **Field** → **Thresholds**
2. Add thresholds (e.g., 1% error rate, 500ms latency)
3. **Mode**: `Absolute` or `Percentage`

### Transformations

#### Organize Fields (Table)

1. **Transform** tab → **Add transformation** → **Organize fields**
2. **Hide**: Hide unwanted columns
3. **Rename**: Rename columns (e.g., `uri` → `Endpoint`)
4. **Custom**: Adjust column order

#### Calculate Field

Create derived metrics:
1. **Transform** → **Add transformation** → **Add field from calculation**
2. **Operation**: Choose operation (Add, Multiply, etc.)
3. **Alias**: Name for calculated field

Example: Error rate from two queries:
- **Operation**: `(A / B) * 100`

### Overrides

#### Conditional Formatting

Color-code rows in table based on value:
1. **Field** → **Overrides** → **Add field override**
2. **Fields with name**: Select field
3. **Add override**: **Custom**: **Cell display mode** → `Color background`
4. **Add override**: **Thresholds** → Set color thresholds

#### Custom Colors

1. **Field** → **Overrides** → **Add field override**
2. **Fields with name**: Select series
3. **Add override**: **Color**: Choose specific color

---

## 🏷️ Variables & Templating

### Using Variables in Queries

Reference variables:
```promql
# Single value
rate(http_server_requests_seconds_count{service="$service"}[5m])

# Multi-value (use regex match)
rate(http_server_requests_seconds_count{instance=~"$instance"}[5m])

# With include all
rate(http_server_requests_seconds_count{service=~"$service"}[5m])
```

### Variable Types

- **Query**: Pulls values from Prometheus (e.g., service names)
- **Custom**: Manually defined options
- **Text**: Free text input
- **Constant**: Fixed value

### Chained Variables

Chain `instance` variable to `service`:
1. **Instance variable** query:
   ```promql
   label_values(http_server_requests_seconds_count{service="$service"}, instance)
   ```
2. When `service` changes, `instance` options update automatically

### Multi-Value Variables

Enable multi-value:
- **Multi-value**: `Yes`
- **Include All**: `Yes` (adds "All" option)
- Use `=~` in queries: `{instance=~"$instance"}`

---

## 🔗 Links & Drilldowns

### Dashboard Links

Link to detailed dashboards:
1. Dashboard settings → **Links** → **Add link**
2. **Type**: `Dashboard`
3. **Dashboard**: Select target (e.g., Instance-Level Dashboard)
4. **Keep time**: `Yes`
5. **Include variables**: `Yes`

### Panel Links

Link to logs/traces from panels:
1. Panel options → **Links**
2. **Add link**
3. **Title**: `View Logs`
4. **URL**: `/explore?orgId=1&left=["now-1h","now","Loki",{"expr":"{service=\"$service\",instance=\"$instance\"}"}]`

### Data Links

Link specific data points:
1. Panel options → **Data links**
2. **Add link**
3. **Title**: `View Endpoint Details`
4. **URL**: `/d/endpoint-detail?var-endpoint=${__field.labels.uri}`

---

## ✅ Best Practices

### Performance

1. **Use recording rules** for expensive percentile calculations
2. **Limit time ranges** - `[5m]` for rate calculations
3. **Filter early** - Apply service/instance filters first
4. **Reduce cardinality** - Avoid high-cardinality labels in every query

### Organization

1. **Use rows** to group related panels
2. **Clear panel titles** - Describe what metrics show
3. **Add descriptions** - Explain SLOs and thresholds
4. **Consistent units** - Use standard units (reqps, seconds, percent)

### Thresholds

1. **Based on SLOs** - Set thresholds from your Service Level Objectives
2. **Color coding** - Green/yellow/red for quick recognition
3. **Document thresholds** - Add notes on why thresholds were chosen

### Monitoring Strategy

1. **Watch p95/p99** - These catch tail latencies
2. **Monitor error rate trends** - Not just absolute values
3. **Track saturation** - Resource exhaustion precedes errors
4. **Correlate metrics** - Errors often correlate with latency spikes

---

## 🐛 Troubleshooting

### "No data" in panels

**Check**:
1. Service variable is set correctly: `$service = config-control-service`
2. Metrics exist: `http_server_requests_seconds_count`
3. Label values match: Check actual `service` label value
4. Time range has data

**Debug**:
```promql
# Start simple
http_server_requests_seconds_count

# Add service filter
http_server_requests_seconds_count{service="config-control-service"}

# Check label values
label_values(http_server_requests_seconds_count, service)
```

### Incorrect error rate

**Check**:
1. Status code filter: `status=~"5..|4.."` matches your error codes
2. Both numerator and denominator queries work separately
3. Division by zero handling: `> 0` is working

**Debug**:
```promql
# Check errors exist
rate(http_server_requests_seconds_count{status=~"5..|4.."}[5m])

# Check total requests
rate(http_server_requests_seconds_count[5m])

# Verify division
(...) / (...) > 0
```

### Percentiles showing incorrect values

**Check**:
1. Histogram buckets exist: `http_server_requests_seconds_bucket`
2. `le` label is present
3. Sum by `le` is correct
4. Enough data points for calculation

**Debug**:
```promql
# Check buckets exist
http_server_requests_seconds_bucket

# Check rate calculation
rate(http_server_requests_seconds_bucket[5m])

# Check sum by le
sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
```

### High CPU usage

**Solutions**:
1. Use recording rules for expensive queries
2. Increase `scrape_interval` in Prometheus
3. Reduce number of panels
4. Simplify queries (remove unnecessary aggregations)

---

## 📚 References

### Related Dashboards
- [Platform Overview](01-platform-overview.md) - High-level platform view
- [System Health](03-system-health.md) - System health indicators
- [Config Control Service - Business Operations](../phase2-service-deep-dive/01-config-control-business-operations.md) - Business metrics

### External Documentation
- [Golden Signals Methodology](https://sre.google/sre-book/monitoring-distributed-systems/)
- [Prometheus Histograms](https://prometheus.io/docs/practices/histograms/)
- [Grafana Time Series Panels](https://grafana.com/docs/grafana/latest/panels-visualizations/visualizations/time-series/)

### Metrics Reference
- HTTP Metrics: `http_server_requests_seconds_*`
- JVM Metrics: `jvm_memory_*`, `jvm_gc_*`, `jvm_threads_*`
- See [MetricsNames.java](../../../../src/main/java/com/example/control/infrastructure/observability/MetricsNames.java)

---

**Next**: [System Health Dashboard](03-system-health.md)  
**Previous**: [Platform Overview Dashboard](01-platform-overview.md)

