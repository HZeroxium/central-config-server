# Capacity Planning Dashboard

This dashboard helps plan capacity based on traffic trends, resource utilization, and growth patterns.

## 📊 Overview

### Purpose
Analyze traffic trends, resource utilization patterns, and growth rates to plan capacity and scaling strategies.

### Target Audience
- **SRE teams** - Capacity planning and scaling decisions
- **Operations teams** - Resource planning and provisioning
- **Management** - Growth trends and capacity needs

### Key Metrics Covered
- Traffic trends and growth rate
- Resource utilization trends
- Capacity headroom
- Scaling recommendations based on trends

### Prerequisites
- Historical metrics data (1-3 months recommended)
- Traffic and resource metrics available
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))
- Understanding of capacity planning principles

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build trend analysis panels
3. Analyze growth patterns

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Set up variables: `service`, `time_window` (optional, for different analysis periods)

### Step 2: Create Row 1 - Traffic Trends

#### Panel 1: Request Rate Trend (7d)

1. Click **Add visualization**
2. **Query tab**:
   ```promql
   sum(rate(http_server_requests_seconds_count{
     service="$service"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Request Rate Trend (7 days)`
5. **Unit**: `reqps`
6. **Time range**: Set to last 7 days (or use variable)
7. Click **Apply**

#### Panel 2: Request Rate Growth Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Compare current vs 7 days ago
   (
     avg_over_time(
       sum(rate(http_server_requests_seconds_count{service="$service"}[5m]))[7d:1h]
     )
     -
     avg_over_time(
       sum(rate(http_server_requests_seconds_count{service="$service"}[5m]))[14d:7d:1h]
     )
   )
   /
   avg_over_time(
     sum(rate(http_server_requests_seconds_count{service="$service"}[5m]))[14d:7d:1h]
   )
   * 100
   ```
3. **Visualization**: `Stat`
4. **Title**: `Request Rate Growth (7d vs previous 7d)`
5. **Unit**: `percent`
6. **Description**: `Percentage increase in request rate`
7. Click **Apply**

**Note**: This compares average of last 7 days vs previous 7 days.

#### Panel 3: Request Rate Projection

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Linear projection based on growth rate
   # Current rate
   sum(rate(http_server_requests_seconds_count{service="$service"}[5m]))
   *
   # Growth factor (simplified, use linear extrapolation)
   (1 + (growth_rate / 100 * 30))  # Project 30 days ahead
   ```
3. **Visualization**: `Time series`
4. **Title**: `Projected Request Rate (30 days)`
5. **Unit**: `reqps`
6. Click **Apply**

**Note**: This is a simplified projection. Use more sophisticated forecasting for production.

### Step 3: Create Row 2 - Resource Utilization Trends

#### Panel 1: Memory Utilization Trend

1. **Add row** → `Resource Utilization Trends`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(jvm_memory_used_bytes{service="$service", area="heap"})
     /
     sum(jvm_memory_max_bytes{service="$service", area="heap"})
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Memory Utilization Trend (7 days)`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Y-axis at 70% (warning), 85% (critical)
7. Click **Apply**

#### Panel 2: CPU Utilization Trend

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 - (avg(rate(process_cpu_seconds_total{
     service="$service"
   }[5m])) * 100)
   ```
3. **Visualization**: `Time series`
4. **Title**: `CPU Utilization Trend (7 days)`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Y-axis at 70% (warning), 85% (critical)
7. Click **Apply**

#### Panel 3: Connection Pool Utilization Trend

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(spring_data_mongodb_repository_health_indicator_active_connections{service="$service"})
     /
     sum(spring_data_mongodb_repository_health_indicator_max_connections{service="$service"})
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Connection Pool Utilization Trend`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Y-axis at 70% (warning), 85% (critical)
7. Click **Apply**

### Step 4: Create Row 3 - Capacity Headroom

#### Panel 1: Memory Headroom

1. **Add row** → `Capacity Headroom`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Available memory headroom
   (
     sum(jvm_memory_max_bytes{service="$service", area="heap"})
     -
     sum(jvm_memory_used_bytes{service="$service", area="heap"})
   )
   /
   sum(jvm_memory_max_bytes{service="$service", area="heap"})
   * 100
   ```
3. **Visualization**: `Time series`
4. **Title**: `Memory Headroom (%)`
5. **Unit**: `percent (0-100)`
6. **Description**: `Remaining memory capacity before max`
7. Click **Apply**

#### Panel 2: CPU Headroom

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Available CPU headroom
   100 - (100 - (avg(rate(process_cpu_seconds_total{service="$service"}[5m])) * 100))
   ```
   Simplified:
   ```promql
   # If CPU usage is tracked as percentage
   100 - avg(cpu_usage_percent{service="$service"})
   ```
3. **Visualization**: `Time series`
4. **Title**: `CPU Headroom (%)`
5. **Unit**: `percent (0-100)`
6. Click **Apply**

#### Panel 3: Connection Pool Headroom

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     (
       sum(spring_data_mongodb_repository_health_indicator_max_connections{service="$service"})
       -
       sum(spring_data_mongodb_repository_health_indicator_active_connections{service="$service"})
     )
     /
     sum(spring_data_mongodb_repository_health_indicator_max_connections{service="$service"})
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Connection Pool Headroom (%)`
5. **Unit**: `percent (0-100)`
6. Click **Apply**

### Step 5: Create Row 4 - Scaling Recommendations

#### Panel 1: Instance Count Recommendation

1. **Add row** → `Scaling Recommendations`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Current instance count
   count(up{service="$service"} == 1)
   *
   # Growth factor (if request rate increased 20%, need 20% more instances)
   (
     sum(rate(http_server_requests_seconds_count{service="$service"}[5m]))
     /
     avg_over_time(
       sum(rate(http_server_requests_seconds_count{service="$service"}[5m]))[7d:1h]
     )
   )
   ```
3. **Visualization**: `Stat` or `Table`
4. **Title**: `Recommended Instance Count`
5. **Unit**: `short`
6. **Description**: `Suggested instance count based on traffic growth`
7. Click **Apply**

**Note**: This is a simplified calculation. Adjust based on your scaling strategy.

#### Panel 2: Memory Size Recommendation

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Current memory usage trend
   avg_over_time(
     (sum(jvm_memory_used_bytes{service="$service", area="heap"}) / 1024 / 1024)[7d:1h]
   )
   *
   # Safety factor (1.5x for headroom)
   1.5
   ```
3. **Visualization**: `Stat`
4. **Title**: `Recommended Memory Size (MB)`
5. **Unit**: `MB` or `decbytes`
6. **Description**: `Suggested memory size based on utilization trends`
7. Click **Apply**

#### Panel 3: Capacity Warning

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Alert if utilization trending upward and approaching limit
   (
     (avg_over_time(memory_util[7d:1h]) > 70)
     or
     (avg_over_time(cpu_util[7d:1h]) > 70)
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Capacity Warning`
5. **Value mappings**: `1` → `NEEDS CAPACITY INCREASE`, `0` → `OK`
6. **Thresholds**: Red (1), Green (0)
7. Click **Apply**

**Note**: Adjust threshold and warning criteria based on your capacity planning strategy.

### Step 6: Create Row 5 - Growth Analysis

#### Panel 1: Daily Growth Rate

1. **Add row** → `Growth Analysis`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Daily request rate
   sum(rate(http_server_requests_seconds_count{service="$service"}[5m])) * 86400
   ```
3. **Visualization**: `Time series`
4. **Title**: `Daily Request Count`
5. **Unit**: `requests/day`
6. Click **Apply**

#### Panel 2: Growth Rate Trend

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Week-over-week growth rate
   (
     (avg_over_time(request_rate[7d:1h]) - avg_over_time(request_rate[14d:7d:1h]))
     /
     avg_over_time(request_rate[14d:7d:1h])
   ) * 100
   ```
3. **Visualization**: `Time series`
4. **Title**: `Growth Rate Trend (%)`
5. **Unit**: `percent`
6. Click **Apply**

#### Panel 3: Peak vs Average Traffic

1. **Add panel** → **Add visualization**
2. **Query A** (Peak):
   ```promql
   max_over_time(
     sum(rate(http_server_requests_seconds_count{service="$service"}[5m]))[7d:1h]
   )
   ```
3. **Query B** (Average):
   ```promql
   avg_over_time(
     sum(rate(http_server_requests_seconds_count{service="$service"}[5m]))[7d:1h]
   )
   ```
4. **Visualization**: `Time series`
4. **Title**: `Peak vs Average Traffic`
5. **Legend**: `Peak`, `Average`
6. **Unit**: `reqps`
7. Click **Apply**

### Step 7: Save Dashboard

1. Click **Save dashboard**
2. **Name**: `Capacity Planning Dashboard`
3. **Folder**: `5-Advanced`
4. **Tags**: `capacity-planning`, `scaling`, `trends`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### Trend Analysis

#### Growth Rate Calculation

```promql
# Week-over-week growth
(
  (current_avg - previous_avg)
  /
  previous_avg
) * 100
```

#### Linear Projection

```promql
# Simple linear projection
current_value * (1 + growth_rate / 100 * days_ahead)
```

**Limitation**: Assumes linear growth. For production, use more sophisticated forecasting.

### Capacity Headroom

#### Available Capacity

```promql
# Memory headroom
(max - used) / max * 100
```

#### Time to Capacity Limit

Estimate time until capacity limit:
```promql
# Days until 80% utilization (if current trend continues)
(0.8 * max - current_used)
/
(growth_rate_per_day)
```

**Note**: Requires growth rate calculation.

---

## ⚙️ Panel Configuration (Advanced)

### Trend Lines

Add trend line overlays:
1. **Visualization**: `Time series`
2. **Options** → **Trend lines**: Enable
3. **Options** → **Show regression**: Enable

### Forecast Visualization

Show projections:
1. Use separate query for projection
2. Overlay on current metrics
3. Use different color/style for projection

---

## 🏷️ Variables & Templating

### Time Window Variable

For different analysis periods:
```promql
# Use variable for time range
avg_over_time(request_rate[$time_window:1h])
```

Where `$time_window` is a time range (7d, 30d, 90d).

---

## ✅ Best Practices

### Capacity Planning

1. **Monitor trends** - Watch for consistent growth patterns
2. **Plan ahead** - Project 30-90 days in advance
3. **Set thresholds** - Alert when approaching capacity limits
4. **Review regularly** - Update capacity plans quarterly

### Growth Analysis

1. **Track growth rate** - Monitor week-over-week growth
2. **Identify patterns** - Seasonal variations, daily patterns
3. **Project future needs** - Plan capacity based on trends
4. **Buffer for spikes** - Account for peak traffic

---

## 🐛 Troubleshooting

### Incorrect growth rate calculation

**Check**:
1. Time ranges are correct
2. `avg_over_time` function is used correctly
3. Comparison periods align correctly

**Debug**:
```promql
# Check current average
avg_over_time(request_rate[7d:1h])

# Check previous average
avg_over_time(request_rate[14d:7d:1h])

# Verify calculation
(current - previous) / previous * 100
```

---

## 📚 References

### Related Dashboards
- [Platform Overview](../phase1-foundation/01-platform-overview.md)
- [Config Control Service - Golden Signals](../phase1-foundation/02-config-control-service-golden-signals.md)
- [Cost Optimization Dashboard](02-cost-optimization.md)

### External Documentation
- [Capacity Planning Best Practices](https://www.oreilly.com/library/view/site-reliability-engineering/9781491929117/ch04.html)
- [Time Series Forecasting](https://prometheus.io/docs/prometheus/latest/querying/functions/#predict_linear)

---

**Previous**: [Cost Optimization Dashboard](02-cost-optimization.md)  
**Back**: [Phase 5 Overview](README.md) | [Main Documentation](../../README.md)

