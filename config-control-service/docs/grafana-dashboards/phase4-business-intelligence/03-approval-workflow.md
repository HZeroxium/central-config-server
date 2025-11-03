# Approval Workflow Dashboard

This dashboard monitors the approval workflow for service ownership, tracking request creation, decision rates, processing time, and workflow metrics.

## 📊 Overview

### Purpose
Monitor approval workflow performance, track approval rates, processing times, and identify bottlenecks in the service ownership approval process.

### Target Audience
- **Operations teams** - Approval workflow monitoring and optimization
- **SRE teams** - Workflow performance tracking
- **Management** - Approval metrics and trends

### Key Metrics Covered
- Approval request creation rate
- Approval decisions (approve/reject rates)
- Pending approval requests count
- Approval processing time (avg, p95, p99)
- Approvals by team/requester

### Prerequisites
- Approval workflow enabled in Config Control Service
- Approval metrics enabled (`config_control.approval.*`)
- Prometheus scraping approval metrics
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build approval workflow panels step-by-step
3. Adjust metric names based on actual instrumentation

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Set up variables: `team`, `requester` (optional)

### Step 2: Create Row 1 - Approval Workflow KPIs

#### Panel 1: Pending Approval Requests

1. Click **Add visualization**
2. **Query tab**:
   ```promql
   sum(config_control_approval_pending_count)
   ```
   Or calculate:
   ```promql
   sum(increase(config_control.approval.create_request_count[1h]))
   -
   (sum(increase(config_control.approval.approve_count[1h]))
   + sum(increase(config_control.approval.reject_count[1h])))
   ```
3. **Visualization**: `Stat`
4. **Title**: `Pending Approval Requests`
5. **Unit**: `short`
6. **Thresholds**: Green (0), Yellow (1-10), Red (10-100)
7. **Description**: `Number of approval requests waiting for decision`
8. Click **Apply**

#### Panel 2: Approval Request Creation Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.approval.create_request_count[5m]))
   ```
3. **Visualization**: `Stat`
4. **Title**: `Approval Request Creation Rate`
5. **Unit**: `requests/min`
6. Click **Apply**

#### Panel 3: Approval Success Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(rate(config_control.approval.approve_count[5m]))
     /
     (sum(rate(config_control.approval.approve_count[5m]))
     + sum(rate(config_control.approval.reject_count[5m])))
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Approval Success Rate (%)`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (70-100%), Yellow (50-70%), Red (0-50%)
7. Click **Apply**

#### Panel 4: Average Approval Processing Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   avg(config_control_approval_processing_time_seconds{
     status=~"approved|rejected"
   })
   ```
   Or if using histogram:
   ```promql
   histogram_quantile(0.50,
     sum(rate(config_control.approval.processing_duration_bucket[5m])) by (le)
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Avg Approval Processing Time`
5. **Unit**: `seconds`
6. Click **Apply**

**Note**: Adjust based on available metrics.

### Step 3: Create Row 2 - Approval Request Trends

#### Panel 1: Approval Request Creation Over Time

1. **Add row** → `Approval Request Trends`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.approval.create_request_count[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Approval Request Creation Rate`
5. **Unit**: `requests/min`
6. Click **Apply**

#### Panel 2: Approval vs Reject Rate

1. **Add panel** → **Add visualization**
2. **Query A** (Approve):
   ```promql
   sum(rate(config_control.approval.approve_count[5m]))
   ```
3. **Query B** (Reject):
   ```promql
   sum(rate(config_control.approval.reject_count[5m]))
   ```
4. **Visualization**: `Time series`
4. **Title**: `Approval Decisions Rate`
5. **Legend**: `Approve`, `Reject`
6. **Unit**: `decisions/min`
7. Click **Apply**

#### Panel 3: Pending Requests Over Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(config_control_approval_pending_count)
   ```
   Or calculate:
   ```promql
   sum(increase(config_control.approval.create_request_count[1h]))
   -
   (sum(increase(config_control.approval.approve_count[1h]))
   + sum(increase(config_control.approval.reject_count[1h])))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Pending Requests Over Time`
5. **Unit**: `short`
6. Click **Apply**

### Step 4: Create Row 3 - Approval Processing Time

#### Panel 1: Approval Processing Time (p50/p95/p99)

1. **Add row** → `Approval Processing Time`
2. **Add panel** → **Add visualization**
2. **Query A** (p50):
   ```promql
   histogram_quantile(0.50,
     sum(rate(config_control.approval.processing_duration_bucket[5m])) by (le)
   )
   ```
3. **Query B** (p95):
   ```promql
   histogram_quantile(0.95,
     sum(rate(config_control.approval.processing_duration_bucket[5m])) by (le)
   )
   ```
4. **Query C** (p99):
   ```promql
   histogram_quantile(0.99,
     sum(rate(config_control.approval.processing_duration_bucket[5m])) by (le)
   )
   ```
5. **Visualization**: `Time series`
6. **Title**: `Approval Processing Time Percentiles`
7. **Legend**: `p50`, `p95`, `p99`
8. **Unit**: `seconds`
9. Click **Apply**

**Note**: Requires histogram buckets. Ensure approval processing time is tracked as histogram.

#### Panel 2: Approval Processing Time by Team

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(config_control.approval.processing_duration_bucket[5m])) by (le, team)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Approval Processing Time by Team (p95)`
5. **Legend**: `{{team}}`
6. **Unit**: `seconds`
7. Click **Apply**

**Note**: Adjust if team label exists in metrics.

#### Panel 3: Approval Processing Time Distribution

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.approval.processing_duration_bucket[5m])) by (le)
   ```
3. **Visualization**: `Histogram` (if available) or `Time series`
4. **Title**: `Approval Processing Time Distribution`
5. **Legend**: `{{le}}`
6. **Unit**: `seconds`
7. Click **Apply**

### Step 5: Create Row 4 - Approval by Team

#### Panel 1: Approval Requests by Team

1. **Add row** → `Approval by Team`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.approval.create_request_count[5m])) by (team)
   ```
   Or:
   ```promql
   count by (team) (config_control.approval.create_request_count{team=~".+"})
   ```
3. **Visualization**: `Time series` or `Table`
4. **Title**: `Approval Requests by Team`
5. **Legend**: `{{team}}`
6. **Unit**: `requests/min`
7. Click **Apply**

**Note**: Adjust label name based on actual metrics (team, owner_team, requester_team).

#### Panel 2: Approvals by Team

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.approval.approve_count[5m])) by (team)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Approvals by Team`
5. **Legend**: `{{team}}`
6. **Unit**: `approvals/min`
7. Click **Apply**

#### Panel 3: Rejections by Team

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.approval.reject_count[5m])) by (team)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Rejections by Team`
5. **Legend**: `{{team}}`
6. **Unit**: `rejections/min`
7. Click **Apply**

### Step 6: Create Row 5 - Approval Workflow Status

#### Panel 1: Approval Request Status Breakdown

1. **Add row** → `Approval Status`
2. **Add panel** → **Add visualization**
2. **Query A** (Pending):
   ```promql
   sum(config_control_approval_pending_count)
   ```
3. **Query B** (Approved):
   ```promql
   sum(increase(config_control.approval.approve_count[24h]))
   ```
4. **Query C** (Rejected):
   ```promql
   sum(increase(config_control.approval.reject_count[24h]))
   ```
5. **Visualization**: `Time series` or `Pie chart`
6. **Title**: `Approval Request Status (24h)`
7. **Legend**: `Pending`, `Approved`, `Rejected`
8. **Unit**: `short`
9. Click **Apply**

#### Panel 2: Approval Requests by Workflow Type

If workflow types are tracked:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.approval.create_request_count[5m])) by (workflow_type)
   ```
3. **Visualization**: `Time series` or `Table`
4. **Title**: `Approval Requests by Workflow Type`
5. **Legend**: `{{workflow_type}}`
6. **Unit**: `requests/min`
7. Click **Apply**

**Note**: Adjust based on workflow type label availability.

#### Panel 3: Approval Gate Status

If approval gates are tracked:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(config_control_approval_gate_status{status=~"pending|approved|rejected"}) by (gate_type, status)
   ```
3. **Visualization**: `Table` or `Time series`
4. **Title**: `Approval Gate Status`
5. Click **Apply**

**Note**: This metric might not exist. Adjust based on actual approval gate tracking.

### Step 7: Save Dashboard

1. Click **Save dashboard**
2. **Name**: `Approval Workflow Dashboard`
3. **Folder**: `3-Business`
4. **Tags**: `approval-workflow`, `business`, `operations`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### Approval Metrics

Approval metrics from `MetricsNames.Approval`:
- `config_control.approval.create_request` - Request creation timer
- `config_control.approval.approve` - Approval counter
- `config_control.approval.reject` - Rejection counter

### Calculating Pending Requests

If only counters are available:
```promql
# Created - (Approved + Rejected)
sum(increase(create_count[1h]))
-
(sum(increase(approve_count[1h])) + sum(increase(reject_count[1h])))
```

**Limitation**: Resets if counters reset. Use gauge metric if available.

### Approval Rate Calculation

Success rate:
```promql
100 * (
  sum(rate(approve_count[5m]))
  /
  (sum(rate(approve_count[5m])) + sum(rate(reject_count[5m])))
)
```

**What it does**: Approved divided by total decisions (approved + rejected).

---

## ⚙️ Panel Configuration (Advanced)

### Processing Time Percentiles

Show multiple percentiles:
1. Create separate queries (Query A=p50, B=p95, C=p99)
2. Overlay in one panel
3. Use legend overrides for custom labels

### Approval Status Breakdown

Use pie chart:
1. **Visualization**: `Pie chart`
2. Multiple queries (Pending, Approved, Rejected)
3. Show percentages and values

---

## 🏷️ Variables & Templating

### Team Filter

Filter by team:
```promql
sum(rate(config_control.approval.create_request_count{
  team="$team"
}[5m]))
```

### Time Range Variable

For different time windows:
```promql
increase(config_control.approval.create_request_count[$time_range])
```

---

## 🔗 Links & Drilldowns

### Link to Instance Dashboard

Link to instance details:
1. Panel options → **Links**
2. **Add link** → **Type**: `Dashboard`
3. **Dashboard**: Instance-Level Dashboard
4. **Variable**: `instance=${__field.labels.instance}`

### Link to Approval Details

If approval detail page exists:
1. **Data links** → **Add link**
2. **URL**: `/approvals/${__field.labels.approval_id}`

---

## ✅ Best Practices

### Approval Workflow Monitoring

1. **Monitor pending count** - High pending indicates bottlenecks
2. **Track processing time** - Long times indicate approval delays
3. **Watch approval rates** - Low approval rate might indicate issues
4. **Monitor by team** - Identify teams with high approval volumes

---

## 🐛 Troubleshooting

### "No data" for approval metrics

**Check**:
1. Approval workflow is enabled
2. Approval metrics are being emitted
3. Operations are happening (approval requests created)

**Debug**:
```promql
# List all approval metrics
{__name__=~"config_control.*approval.*"}

# Check specific metric
config_control.approval.create_request_count

# Check if operations happening
increase(config_control.approval.create_request_count[1h])
```

---

## 📚 References

### Related Dashboards
- [Config Control Service - Business Operations](../phase2-service-deep-dive/01-config-control-business-operations.md)
- [Business Intelligence Dashboard](01-business-intelligence.md)

### Metrics Reference
- **Approval metrics**: `config_control.approval.*` (from `MetricsNames.Approval`)
- See [MetricsNames.java](../../../../src/main/java/com/example/control/infrastructure/observability/MetricsNames.java)

---

**Next**: [Phase 5: Advanced Dashboards](../phase5-advanced/README.md)  
**Previous**: [Instance-Level Dashboard](02-instance-level.md)

