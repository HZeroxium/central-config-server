# Platform Overview Dashboard

This dashboard provides high-level visibility into your entire platform's health, traffic, and errors. It's designed for executives, on-call engineers, and anyone who needs a quick platform health check.

## 📊 Overview

### Purpose
Monitor overall platform health at a glance, identify platform-wide issues quickly, and track key business metrics.

### Target Audience
- **On-call engineers** - Quick health checks during incidents
- **Executives** - High-level platform status
- **Operations teams** - Platform-wide monitoring
- **SRE teams** - Capacity and health monitoring

### Key Metrics Covered
- Total services and active instances
- Platform-wide error rate and latency
- Traffic patterns by service
- Infrastructure component health
- Business metrics (heartbeat rate, drift events, approvals)

### Prerequisites
- Prometheus scraping metrics
- Grafana configured with Prometheus data source
- Config Control Service running and exposing metrics
- Basic understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))

## 🚀 Quick Start (Fast Track)

If you want to import the dashboard immediately:

### Step 1: Import JSON File

1. Open Grafana
2. Go to **Dashboards** → **Import**
3. Upload `01-platform-overview.json` from this folder
4. Click **Load**
5. Verify data source is set to **Prometheus**
6. Click **Import**

### Step 2: Verify Dashboard Works

After import:
1. Check all panels show data (not "No data")
2. Verify time picker shows appropriate time range
3. Check variables dropdowns work (if configured)

### Step 3: Customize for Your Environment

If needed:
1. Adjust service name filters in queries
2. Update infrastructure component names
3. Modify thresholds for your SLOs

**That's it!** You now have a working Platform Overview Dashboard.

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

This tutorial walks you through creating the dashboard step-by-step. You'll learn how each panel works and how to customize it.

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard** (or **+** → **Dashboard**)
2. Click **Add visualization** (or **Add** → **Visualization**)
3. You'll see the panel editor

### Step 2: Set Up Variables (Optional but Recommended)

Variables make your dashboard reusable. Set up these variables:

#### Variable 1: Environment

1. Click **Dashboard settings** (gear icon) → **Variables** → **Add variable**
2. Configure:
   - **Name**: `environment`
   - **Type**: `Query`
   - **Data source**: `Prometheus`
   - **Query**: `label_values(http_server_requests_seconds_count, environment)`
   - **Label**: `Environment`
   - **Multi-value**: No (or Yes if you want multiple environments)
   - **Include All**: Yes
3. Click **Apply**

#### Variable 2: Service (Optional)

1. Add another variable:
   - **Name**: `service`
   - **Type**: `Query`
   - **Query**: `label_values(http_server_requests_seconds_count, service)`
   - **Label**: `Service`
   - **Multi-value**: Yes
   - **Include All**: Yes
2. Click **Apply**

**Note**: For Platform Overview, you might not need service variables as it shows all services. Variables are useful for filtering or linking to service-specific dashboards.

### Step 3: Create Row 1 - KPIs (Stat Panels)

KPI panels provide at-a-glance metrics with color coding.

#### Panel 1: Total Services Count

1. Click **Add visualization**
2. **Query tab**:
   ```promql
   count(count by (service) (up{job=~"config-control-service.*|config-server.*"}))
   ```
3. **Visualization tab**:
   - **Visualization**: `Stat`
   - **Title**: `Total Services`
   - **Options** → **Text**: `Value`
   - **Field** → **Unit**: `short`
4. **Panel options** → **Description**: `Number of services monitored`
5. Click **Apply**

**What this query does**: Counts unique services that are up.

#### Panel 2: Total Instances Count

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   count(up{job=~"config-control-service.*|config-server.*"} == 1)
   ```
3. **Visualization**: `Stat`
4. **Title**: `Total Instances`
5. **Unit**: `short`
6. Click **Apply**

#### Panel 3: Platform Error Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(rate(http_server_requests_seconds_count{status=~"5..|4.."}[5m]))
     /
     (sum(rate(http_server_requests_seconds_count[5m])) > 0)
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Platform Error Rate`
5. **Unit**: `percent (0-100)`
6. **Thresholds**:
   - **Green**: `null` to `1`
   - **Yellow**: `1` to `5`
   - **Red**: `5` to `100`
7. Click **Apply**

**What this query does**: Calculates percentage of requests with 4xx/5xx status codes.

#### Panel 4: Platform p95 Latency

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Platform p95 Latency`
5. **Unit**: `seconds`
6. **Thresholds**:
   - **Green**: `null` to `0.2`
   - **Yellow**: `0.2` to `0.5`
   - **Red**: `0.5` to `10`
7. Click **Apply**

#### Panel 5: Total Heartbeat Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(heartbeat.process_count[5m]))
   ```
3. **Visualization**: `Stat`
4. **Title**: `Total Heartbeat Rate`
5. **Unit**: `reqps` (requests per second)
6. Click **Apply**

**Note**: If `heartbeat.process_count` doesn't exist, use:
```promql
sum(rate(heartbeat.process[5m]))
```

#### Panel 6: Active Drift Events Count

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(config_control.drift_event.save_count) - sum(config_control.drift_event.resolve_count)
   ```
   Or if using counters:
   ```promql
   sum(increase(config_control.drift_event.save_count[1h])) - sum(increase(config_control.drift_event.resolve_count[1h]))
   ```
3. **Visualization**: `Stat`
4. **Title**: `Active Drift Events`
5. **Unit**: `short`
6. Click **Apply**

**Tip**: For counters, you might need to track unresolved events differently if you have a gauge metric. Adjust based on your actual metrics.

### Step 4: Create Row 2 - Traffic Overview

#### Panel 1: Request Rate by Service

1. **Add row** → Type row name: `Traffic Overview`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_count[5m])) by (service)
   ```
4. **Visualization**: `Time series`
5. **Title**: `Request Rate by Service`
6. **Legend**: `{{service}}`
7. **Unit**: `reqps`
8. **Stack**: `Normal` (optional, for stacked view)
9. Click **Apply**

#### Panel 2: Request Rate by Endpoint Category

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_count{uri=~"/api/(heartbeat|services|drift|admin|registry).*"}[5m])) by (uri)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Request Rate by Endpoint Category`
5. **Legend**: `{{uri}}`
6. **Unit**: `reqps`
7. Click **Apply**

**Tip**: Adjust URI patterns based on your actual endpoint structure.

### Step 5: Create Row 3 - Error Overview

#### Panel 1: Error Rate by Service

1. **Add row** → `Error Overview`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   100 * (
     sum(rate(http_server_requests_seconds_count{status=~"5..|4.."}[5m])) by (service)
     /
     (sum(rate(http_server_requests_seconds_count[5m])) by (service) > 0)
   )
   ```
4. **Visualization**: `Time series`
5. **Title**: `Error Rate by Service (%)`
6. **Legend**: `{{service}}`
7. **Unit**: `percent (0-100)`
8. Click **Apply**

#### Panel 2: Error Rate by Status Code

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])) by (status)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Error Rate by Status Code`
5. **Legend**: `{{status}}`
6. **Unit**: `reqps`
7. Click **Apply**

#### Panel 3: Top 10 Error Endpoints (Table)

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   topk(10, sum(rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])) by (uri))
   ```
3. **Visualization**: `Table`
4. **Title**: `Top 10 Error Endpoints`
5. **Transform** → **Organize fields**:
   - Show `uri` and value column
   - Hide other fields
6. Click **Apply**

### Step 6: Create Row 4 - Infrastructure Health

#### Panel 1: Service Health Status

1. **Add row** → `Infrastructure Health`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   up{job=~"config-control-service.*|config-server.*"}
   ```
4. **Visualization**: `Stat`
5. **Title**: `Service Health Status`
6. **Value options** → **Calculation**: `Last`
7. **Thresholds**:
   - **Red**: `null` to `0.5`
   - **Green**: `0.5` to `1`
8. Click **Apply**

**Tip**: This shows if services are up (1) or down (0). You might want multiple panels for each service, or use a table.

#### Panel 2-5: Infrastructure Components

For each infrastructure component (MongoDB, Redis, Kafka, Consul), create a health panel:

**MongoDB**:
```promql
up{job=~".*mongodb.*"}
```

**Redis**:
```promql
up{job=~".*redis.*"}
```

**Kafka**:
```promql
up{job=~".*kafka.*"}
```

**Consul**:
```promql
up{job=~".*consul.*"}
```

**Note**: Adjust job names based on your Prometheus scrape configuration.

### Step 7: Create Row 5 - Business Metrics Overview

#### Panel 1: Heartbeat Processing Rate

1. **Add row** → `Business Metrics`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   sum(rate(heartbeat.process_count[5m]))
   ```
4. **Visualization**: `Time series`
5. **Title**: `Heartbeat Processing Rate`
6. **Unit**: `reqps`
7. Click **Apply**

#### Panel 2: Drift Detection Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.drift_event.save_count[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Drift Detection Rate`
5. **Unit**: `events/min`
6. Click **Apply**

#### Panel 3: Approval Request Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.approval.create_request_count[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Approval Request Rate`
5. **Unit**: `requests/min`
6. Click **Apply**

### Step 8: Save Dashboard

1. Click **Save dashboard** (floppy disk icon)
2. **Name**: `Platform Overview`
3. **Folder**: Create or select `0-Overview`
4. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### Understanding the Queries

#### Service Count Query

```promql
count(count by (service) (up{job=~"config-control-service.*|config-server.*"}))
```

**Breakdown**:
1. `up{job=~"...|..."}` - Selects all targets matching job name patterns
2. `count by (service)` - Counts instances per service (creates one series per service)
3. `count(...)` - Counts number of services (unique service values)

**Why nested count?** First `count by (service)` creates one series per service, then outer `count()` counts those series.

#### Error Rate Query

```promql
100 * (
  sum(rate(http_server_requests_seconds_count{status=~"5..|4.."}[5m]))
  /
  (sum(rate(http_server_requests_seconds_count[5m])) > 0)
)
```

**Breakdown**:
1. `rate(...{status=~"5..|4.."}[5m])` - Rate of error requests (4xx/5xx) over 5 minutes
2. `sum(...)` - Sum across all labels
3. `rate(http_server_requests_seconds_count[5m])` - Rate of all requests
4. `sum(...)` - Sum all requests
5. `> 0` - Prevents division by zero
6. Divide error rate by total rate
7. `* 100` - Convert to percentage

**Alternative** (simpler but may divide by zero):
```promql
100 * (
  sum(rate(http_server_requests_seconds_count{status=~"5..|4.."}[5m]))
  /
  sum(rate(http_server_requests_seconds_count[5m]))
)
```

#### p95 Latency Query

```promql
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
)
```

**Breakdown**:
1. `http_server_requests_seconds_bucket` - Histogram buckets (latency distribution)
2. `rate(...[5m])` - Rate of requests in each bucket over 5 minutes
3. `sum(...) by (le)` - Sum buckets by `le` (less than or equal) label
4. `histogram_quantile(0.95, ...)` - Calculate 95th percentile

**Why sum by (le)?** Histogram buckets have `le` label indicating threshold (e.g., `le="0.1"` means ≤100ms). We sum buckets with same `le` across instances.

### Alternative Queries

#### Total Services (Alternative)

```promql
count(count by (service) (up{service=~".+"}))
```

Or if you have a service label:
```promql
count(count by (service) (up))
```

#### Platform Error Rate (Alternative)

Using separate queries for errors and total:
```promql
# Query A: Error rate
sum(rate(http_server_requests_seconds_count{status=~"5..|4.."}[5m]))

# Query B: Total rate
sum(rate(http_server_requests_seconds_count[5m]))

# Then use transformation: (A / B) * 100
```

### Performance Considerations

1. **Use rate() not raw counters** - Counters only increase, rate shows per-second values
2. **Choose appropriate time range** - `[5m]` for most dashboards, `[1m]` for more reactive
3. **Aggregate before calculating percentiles** - More accurate and efficient
4. **Filter early** - Apply label filters early in query to reduce data

---

## ⚙️ Panel Configuration (Advanced)

### Advanced Stat Panel Options

#### Color Modes

- **Background**: Changes background color based on thresholds
- **Value**: Changes text color only
- **None**: No color changes

#### Value Options

- **Calculation**: Choose how to display value (Last, Mean, Max, Min)
- **Unit**: Set appropriate unit (reqps, percent, seconds, etc.)
- **Decimals**: Number of decimal places

#### Thresholds

Set meaningful thresholds:
```yaml
Green: null to 1 (error rate < 1%)
Yellow: 1 to 5 (error rate 1-5%)
Red: 5 to 100 (error rate > 5%)
```

### Time Series Panel Options

#### Legend

- **Legend values**: Show additional values (Min, Max, Mean)
- **Legend placement**: Bottom, Right, etc.

#### Stacking

- **Normal**: Overlays series
- **Percent**: Stacks as percentage
- **None**: No stacking

#### Visualization Options

- **Line width**: Make lines thicker/thinner
- **Fill opacity**: Shade area under line
- **Point size**: Show/hide data points

### Transformations

#### Organize Fields

For table panels:
1. Click **Transform** tab
2. Add **Organize fields** transformation
3. Hide/show columns
4. Rename columns

#### Calculate Field

Create calculated fields:
```promql
# Example: Error rate calculation
${A} / ${B} * 100
```

### Overrides

Override field options for specific series:
1. Click **Override** in panel options
2. **Add override** → **Fields with name**
3. Select field (e.g., `Value`)
4. Override specific options (color, unit, etc.)

---

## 🏷️ Variables & Templating

### Using Variables in Queries

Reference variables in queries:
```promql
# Single value
rate(http_server_requests_seconds_count{service="$service"}[5m])

# Multi-value (use regex match)
rate(http_server_requests_seconds_count{service=~"$service"}[5m])

# With include all
rate(http_server_requests_seconds_count{service=~"$service"}[5m])
```

### Chained Variables

Chain variables for dependent selection:
1. Create `service` variable
2. Create `instance` variable with query:
   ```promql
   label_values(http_server_requests_seconds_count{service="$service"}, instance)
   ```
3. `instance` updates when `service` changes

---

## 🔗 Links & Drilldowns

### Dashboard Links

Link to other dashboards:
1. Dashboard settings → **Links** → **Add link**
2. **Type**: `Dashboard`
3. **Dashboard**: Select target dashboard
4. **Keep time**: Yes (preserve time range)
5. **Include variables**: Yes (preserve variables)

Example: Platform Overview → Config Control Service Dashboard

### Panel Links

Link panels to detailed views:
1. Panel options → **Links**
2. **Add link**
3. Configure URL or dashboard link

### Data Links

Link to logs/traces:
1. Panel options → **Data links**
2. **Add link**
3. **Title**: `View Logs`
4. **URL**: Grafana Explore URL with query parameters

Example:
```
/explore?orgId=1&left=["now-1h","now","Loki",{"expr":"{service=\"$service\"}"}]
```

---

## ✅ Best Practices

### Performance

1. **Use recording rules** for expensive queries
2. **Limit time range** in queries (use `[5m]` not `[1h]`)
3. **Filter early** - Apply label filters first
4. **Reduce cardinality** - Avoid high-cardinality labels in every query

### Organization

1. **Use rows** to group related panels
2. **Clear panel titles** - Describe what the panel shows
3. **Add descriptions** - Explain what metrics mean
4. **Set appropriate refresh** - 30s for most dashboards

### Thresholds

1. **Set meaningful thresholds** - Based on your SLOs
2. **Use color coding** - Green/yellow/red for quick recognition
3. **Document thresholds** - Add notes on why thresholds were chosen

---

## 🐛 Troubleshooting

### "No data" in panels

**Check**:
1. Metrics are being scraped: `up{job="config-control-service"}`
2. Query syntax is correct
3. Time range includes data
4. Label filters match actual label values

**Debug**:
```promql
# Start simple
http_server_requests_seconds_count

# Add filters gradually
http_server_requests_seconds_count{service="config-control-service"}
```

### Incorrect values

**Check**:
1. Metric type (counter vs gauge)
2. Using `rate()` for counters
3. Aggregation logic (sum vs avg)
4. Time range alignment

### High CPU usage

**Solutions**:
1. Use recording rules
2. Increase `scrape_interval` in Prometheus
3. Reduce number of panels
4. Simplify queries

---

## 📚 References

- **Related Dashboards**:
  - [Config Control Service - Golden Signals](02-config-control-service-golden-signals.md)
  - [System Health](03-system-health.md)

- **External Documentation**:
  - [Prometheus Querying Basics](https://prometheus.io/docs/prometheus/latest/querying/basics/)
  - [Grafana Panels](https://grafana.com/docs/grafana/latest/panels-visualizations/)
  - [PromQL Functions](https://prometheus.io/docs/prometheus/latest/querying/functions/)

---

**Next**: [Config Control Service - Golden Signals](02-config-control-service-golden-signals.md)

