# SLO Dashboards

This dashboard tracks Service Level Objectives (SLOs) with error budgets and burn rate monitoring.

## 📊 Overview

### Purpose
Monitor Service Level Objectives, track error budgets, calculate burn rates, and ensure services meet their SLOs.

### Target Audience
- **SRE teams** - SLO tracking and error budget management
- **Management** - Service reliability metrics
- **Operations teams** - SLO compliance monitoring

### Key Metrics Covered
- **Availability SLI**: Service availability percentage
- **Latency SLI**: Response time percentiles
- **Error Rate SLI**: Error rate percentage
- **Error Budget**: Remaining error budget
- **Burn Rate**: Error budget consumption rate

### SLO Concepts

**SLO (Service Level Objective)**: Target for service reliability (e.g., 99.9% availability)

**SLI (Service Level Indicator)**: Measurable metric (e.g., successful requests / total requests)

**Error Budget**: Allowable failures (e.g., 0.1% of requests can fail)

**Burn Rate**: Rate at which error budget is consumed

### Prerequisites
- Understanding of SLOs and error budgets
- Prometheus recording rules configured (recommended)
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))
- Completed [Golden Signals Dashboard](../phase1-foundation/02-config-control-service-golden-signals.md)

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Set up recording rules for SLO calculations
3. Build SLO panels step-by-step

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

### Step 1: Create Recording Rules (Recommended)

Recording rules pre-compute SLO metrics for performance.

#### Example Recording Rules

Create `prometheus-rules-slo.yml`:

```yaml
groups:
  - name: slo_availability
    interval: 30s
    rules:
      # Availability SLI (good requests / total requests)
      - record: sli:availability:ratio
        expr: |
          sum(rate(http_server_requests_seconds_count{
            service="config-control-service",
            status!~"5..|4.."
          }[5m]))
          /
          sum(rate(http_server_requests_seconds_count{
            service="config-control-service"
          }[5m]))
      
      # Error budget burn rate (1h window)
      - record: sli:error_budget_burn_rate:1h
        expr: |
          (1 - sli:availability:ratio) * 3600
          /
          (1 - 0.999)  # SLO: 99.9% availability
      
      # Error budget burn rate (6h window)
      - record: sli:error_budget_burn_rate:6h
        expr: |
          (1 - sli:availability:ratio) * 21600
          /
          (1 - 0.999)

  - name: slo_latency
    interval: 30s
    rules:
      # Latency SLI (p95 latency)
      - record: sli:latency:p95
        expr: |
          histogram_quantile(0.95,
            sum(rate(http_server_requests_seconds_bucket{
              service="config-control-service"
            }[5m])) by (le)
          )
```

**Note**: Adjust SLO values (0.999 = 99.9%) based on your requirements.

### Step 2: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Set up variables: `service`, `slo_target` (optional)

### Step 3: Create Row 1 - SLO KPIs

#### Panel 1: Availability SLI

1. Click **Add visualization**
2. **Query tab**:
   ```promql
   sli:availability:ratio * 100
   ```
   Or calculate directly:
   ```promql
   100 * (
     sum(rate(http_server_requests_seconds_count{
       service="$service",
       status!~"5..|4.."
     }[5m]))
     /
     sum(rate(http_server_requests_seconds_count{
       service="$service"
     }[5m]))
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Availability SLI`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: 
   - **Green**: 99.9% to 100% (meets SLO)
   - **Yellow**: 99.5% to 99.9% (warning)
   - **Red**: 0% to 99.5% (breach)
7. **Description**: `Percentage of successful requests (SLO: 99.9%)`
8. Click **Apply**

#### Panel 2: Latency SLI (p95)

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sli:latency:p95
   ```
   Or calculate:
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket{
       service="$service"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Latency SLI (p95)`
5. **Unit**: `seconds`
6. **Thresholds**: Green (<200ms), Yellow (200-500ms), Red (>500ms)
7. **Description**: `95th percentile latency (SLO: <500ms)`
8. Click **Apply**

#### Panel 3: Error Rate SLI

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(rate(http_server_requests_seconds_count{
       service="$service",
       status=~"5..|4.."
     }[5m]))
     /
     sum(rate(http_server_requests_seconds_count{
       service="$service"
     }[5m]))
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Error Rate SLI`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-0.1%), Yellow (0.1-0.5%), Red (>0.5%)
7. Click **Apply**

### Step 4: Create Row 2 - Error Budget

#### Panel 1: Error Budget Remaining

1. **Add row** → `Error Budget`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
       1  # SLO target (99.9% = 0.999)
       -
      (1 - sli:availability:ratio)
    )
   ```
   Or calculate directly:
   ```promql
   # Error budget: 0.1% (for 99.9% SLO)
   # Remaining = SLO - (1 - availability)
   100 * (0.999 - (1 - sli:availability:ratio))
   ```
3. **Visualization**: `Stat` or `Gauge`
4. **Title**: `Error Budget Remaining (%)`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (>80%), Yellow (20-80%), Red (<20%)
7. Click **Apply**

#### Panel 2: Error Budget Over Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (0.999 - (1 - sli:availability:ratio))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Error Budget Over Time (%)`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Y-axis at 80%, 20%
7. Click **Apply**

#### Panel 3: Error Budget Consumed

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (1 - sli:availability:ratio)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Error Budget Consumed (%)`
5. **Unit**: `percent (0-100)`
6. Click **Apply**

### Step 5: Create Row 3 - Burn Rate

#### Panel 1: Error Budget Burn Rate (1h Window)

1. **Add row** → `Burn Rate`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sli:error_budget_burn_rate:1h
   ```
   Or calculate:
   ```promql
   (1 - sli:availability:ratio) * 3600
   /
   (1 - 0.999)  # Error budget: 0.1%
   ```
3. **Visualization**: `Time series`
4. **Title**: `Error Budget Burn Rate (1h Window)`
5. **Unit**: `burn_rate` (dimensionless)
6. **Thresholds**: Y-axis at 1.0 (normal burn), 6.0 (fast burn)
7. **Description**: `Burn rate >6 means error budget consumed in <1h`
8. Click **Apply**

#### Panel 2: Error Budget Burn Rate (6h Window)

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sli:error_budget_burn_rate:6h
   ```
   Or calculate:
   ```promql
   (1 - sli:availability:ratio) * 21600
   /
   (1 - 0.999)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Error Budget Burn Rate (6h Window)`
5. **Unit**: `burn_rate`
6. **Thresholds**: Y-axis at 1.0, 6.0
7. Click **Apply**

#### Panel 3: Multi-Window Multi-Burn Status

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # High burn if both 1h and 6h burn rate > 6
   (sli:error_budget_burn_rate:1h > 6) and (sli:error_budget_burn_rate:6h > 6)
   ```
3. **Visualization**: `Stat`
4. **Title**: `High Burn Rate Alert`
5. **Value mappings**: `1` → `HIGH BURN`, `0` → `NORMAL`
6. **Thresholds**: Red (1), Green (0)
7. Click **Apply**

**Note**: Burn rate >6 indicates error budget consumed faster than expected.

### Step 6: Create Row 4 - SLO Compliance

#### Panel 1: SLO Compliance Status

1. **Add row** → `SLO Compliance`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # 1 if meets SLO, 0 if not
   sli:availability:ratio >= 0.999
   ```
3. **Visualization**: `Stat`
4. **Title**: `SLO Compliance Status`
5. **Value mappings**: `1` → `MEETS SLO`, `0` → `BREACHES SLO`
6. **Thresholds**: Green (1), Red (0)
7. Click **Apply**

#### Panel 2: SLO Trend Over Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sli:availability:ratio * 100
   ```
3. **Visualization**: `Time series`
4. **Title**: `Availability SLI Over Time`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Y-axis at 99.9% (SLO target), 99.5% (warning)
7. Click **Apply**

#### Panel 3: SLO Breach Events

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Count times SLO was breached
   changes((sli:availability:ratio < 0.999)[1h])
   ```
3. **Visualization**: `Stat` or `Time series`
4. **Title**: `SLO Breach Events (1h)`
5. **Unit**: `breaches`
6. Click **Apply**

### Step 7: Save Dashboard

1. Click **Save dashboard**
2. **Name**: `SLO Dashboard`
3. **Folder**: `3-Business` or `5-Advanced`
4. **Tags**: `slo`, `error-budget`, `reliability`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### SLO Calculation

**Availability SLO**:
```promql
# Good requests / Total requests
sum(rate(good_requests[5m]))
/
sum(rate(total_requests[5m]))
```

**Good requests**: Exclude 4xx/5xx errors
**Total requests**: All requests

### Error Budget Calculation

**Error Budget**:
- For 99.9% SLO: Error budget = 0.1% (0.001)
- **Remaining**: `SLO_target - (1 - availability)`
- **Consumed**: `1 - availability`

**Example**:
- SLO: 99.9% (0.999)
- Availability: 99.5% (0.995)
- Remaining: `0.999 - (1 - 0.995) = 0.994` (99.4%)
- Consumed: `1 - 0.995 = 0.005` (0.5%)

### Burn Rate Calculation

**Burn Rate**:
```promql
(1 - availability) * window_duration
/
error_budget
```

**Interpretation**:
- **Burn rate = 1.0**: Normal consumption (on track)
- **Burn rate > 6.0**: Fast burn (error budget consumed in <1/6 of window)
- **Burn rate < 1.0**: Slow burn (error budget consumed slowly)

**Example**:
- Availability: 99.5%
- Window: 1 hour (3600s)
- Error budget: 0.1% (for 99.9% SLO)
- Burn rate: `(1 - 0.995) * 3600 / 0.001 = 18.0` (very fast burn)

### Multi-Window Multi-Burn

Alert on sustained high burn:
- **1h window burn rate > 6**: Fast burn
- **6h window burn rate > 6**: Sustained fast burn
- **Both > 6**: Alert (error budget consumed too quickly)

---

## ⚙️ Panel Configuration (Advanced)

### Gauge Visualization for Error Budget

For error budget remaining:
1. **Visualization**: `Gauge`
2. **Options** → **Min**: `0`, **Max**: `100`
3. **Options** → **Thresholds**: Configure zones (Green: 80-100%, Yellow: 20-80%, Red: 0-20%)
4. **Field** → **Unit**: `percent (0-100)`

### Threshold Lines

Add horizontal threshold lines:
1. **Field** → **Thresholds**
2. Add thresholds at SLO targets (99.9%, 500ms, etc.)
3. Show as horizontal lines on time series

---

## 🏷️ Variables & Templating

### SLO Target Variable

Make SLO target configurable:
```promql
# Use variable for SLO target
sli:availability:ratio >= $slo_target
```

Where `$slo_target` is a constant variable (e.g., 0.999).

### Service Variable

Filter by service:
```promql
sli:availability:ratio{service="$service"}
```

---

## 🔗 Links & Drilldowns

### Link to Service Dashboard

Link from SLO to service detail:
1. Panel options → **Links**
2. **Add link** → **Type**: `Dashboard`
3. **Dashboard**: Config Control Service - Golden Signals
4. **Variable**: `service=$service`

### Link to Error Budget Details

If error budget detail dashboard exists:
1. **Data links** → **Add link**
2. **URL**: `/d/error-budget-detail?var-service=$service`

---

## ✅ Best Practices

### SLO Monitoring

1. **Define clear SLOs** - Specific, measurable targets
2. **Track error budgets** - Monitor remaining budget
3. **Alert on burn rate** - Alert when burn rate > 6.0
4. **Use recording rules** - Pre-compute for performance

### Error Budget Management

1. **Monitor burn rate** - Watch for fast consumption
2. **Track remaining budget** - Ensure sufficient buffer
3. **Alert on breaches** - Notify when SLO breached
4. **Document SLOs** - Clear documentation of targets

---

## 🐛 Troubleshooting

### "No data" for SLO metrics

**Check**:
1. Recording rules are configured and loaded
2. Source metrics exist: `http_server_requests_seconds_count`
3. Recording rule queries are correct

**Debug**:
```promql
# Check source metrics
http_server_requests_seconds_count

# Check recording rules
sli:availability:ratio

# Check rule evaluation
up{job="prometheus"}
```

### Incorrect SLO calculation

**Check**:
1. SLO target is correct (0.999 for 99.9%)
2. Good requests definition is correct (status filter)
3. Calculation logic is correct

**Debug**:
```promql
# Check availability calculation step by step
# Good requests
sum(rate(http_server_requests_seconds_count{status!~"5..|4.."}[5m]))

# Total requests
sum(rate(http_server_requests_seconds_count[5m]))

# Availability ratio
(sum(good) / sum(total))
```

---

## 📚 References

### Related Dashboards
- [Config Control Service - Golden Signals](../phase1-foundation/02-config-control-service-golden-signals.md)
- [Platform Overview](../phase1-foundation/01-platform-overview.md)

### External Documentation
- [SRE Book - SLOs and Error Budgets](https://sre.google/sre-book/slo/)
- [Prometheus Recording Rules](https://prometheus.io/docs/prometheus/latest/configuration/recording_rules/)

---

**Next**: [Cost Optimization Dashboard](02-cost-optimization.md)  
**Previous**: [Approval Workflow Dashboard](../phase4-business-intelligence/03-approval-workflow.md)

