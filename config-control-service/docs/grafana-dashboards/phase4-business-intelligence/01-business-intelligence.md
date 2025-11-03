# Business Intelligence Dashboard

This dashboard provides business-focused metrics for operations teams, monitoring service discovery, ownership, configuration health, and cache performance.

## 📊 Overview

### Purpose
Monitor business metrics at the platform level: service discovery statistics, configuration health, approval workflows, service ownership, and cache performance.

### Target Audience
- **Operations teams** - Business metrics and operations monitoring
- **Product teams** - Service usage and ownership statistics
- **Management** - High-level business KPIs
- **SRE teams** - Configuration health and service discovery status

### Key Metrics Covered
- **Service Discovery**: Registered services, active instances, services by team
- **Configuration Health**: Services with active drift, drift resolution rate, time to resolve
- **Approval Workflow**: Pending approvals, approval processing time, success rate
- **Service Ownership**: Ownership transfers, services claimed, services by team
- **Cache Performance**: Overall cache hit ratio, hit ratio by cache name, eviction rate

### Prerequisites
- Prometheus scraping metrics from Config Control Service
- Custom business metrics enabled (drift, approvals, ownership)
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))
- Understanding of business workflows

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build each section step-by-step
3. Adjust queries based on actual metric availability

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Set up variables: `environment`, `team` (optional)

### Step 2: Create Row 1 - Service Discovery

#### Panel 1: Total Registered Services Count

1. Click **Add visualization**
2. **Query tab**:
   ```promql
   count(count by (service) (up{job=~".*-service.*"} == 1))
   ```
   Or if using Consul metrics:
   ```promql
   consul_catalog_services_total
   ```
3. **Visualization**: `Stat`
4. **Title**: `Total Registered Services`
5. **Unit**: `short`
6. **Description**: `Number of unique services registered in the system`
7. Click **Apply**

#### Panel 2: Total Active Instances Count

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   count(up{job=~".*-service.*"} == 1)
   ```
   Or:
   ```promql
   consul_catalog_service_instances_total
   ```
3. **Visualization**: `Stat`
4. **Title**: `Total Active Instances`
5. **Unit**: `short`
6. **Description**: `Number of active service instances across all services`
7. Click **Apply**

#### Panel 3: Services by Team (Pie Chart)

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   count by (team) (up{job=~".*-service.*",team=~".+"} == 1)
   ```
   Or if using application service metrics:
   ```promql
   count by (owner_team) (config_control_application_service_count{owner_team=~".+"})
   ```
3. **Visualization**: `Pie chart` or `Time series`
4. **Title**: `Services by Team`
5. **Legend**: `{{team}}` or `{{owner_team}}`
6. Click **Apply**

**Note**: Adjust label name based on actual metric labels. Team information might come from metadata.

#### Panel 4: Orphan Services Count

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   count(config_control_application_service_count{owner_team=""} or on() vector(0))
   ```
   Or:
   ```promql
   count(config_control_application_service_count{owner_team=~"null|^$"})
   ```
3. **Visualization**: `Stat`
4. **Title**: `Orphan Services`
5. **Unit**: `short`
6. **Description**: `Services without an assigned owner team`
7. Click **Apply**

**Note**: Adjust query based on how orphan services are tracked (empty string, null, or special value).

### Step 3: Create Row 2 - Configuration Health

#### Panel 1: Services with Active Drift

1. **Add row** → `Configuration Health`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   count(
     increase(config_control.drift_event.save_count[1h]) 
     > 
     increase(config_control.drift_event.resolve_count[1h])
   )
   ```
   Or if using gauge:
   ```promql
   count(config_control_drift_event_unresolved_count > 0)
   ```
3. **Visualization**: `Stat`
4. **Title**: `Services with Active Drift`
5. **Unit**: `short`
6. **Thresholds**: Green (0), Yellow (1-5), Red (5-100)
7. Click **Apply**

#### Panel 2: Drift Resolution Rate (%)

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(increase(config_control.drift_event.resolve_count[1h]))
     /
     (sum(increase(config_control.drift_event.save_count[1h])) > 0)
   )
   ```
   Or:
   ```promql
   100 * (
     sum(config_control.drift_event.resolve_count)
     /
     sum(config_control.drift_event.save_count)
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Drift Resolution Rate (%)`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (90-100%), Yellow (70-90%), Red (0-70%)
7. Click **Apply**

#### Panel 3: Average Time to Resolve Drift

If drift resolution time is tracked:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   avg(config_control_drift_event_resolution_time_seconds{
     resolved="true"
   })
   ```
   Or calculate from timestamps:
   ```promql
   avg(
     config_control_drift_event_resolved_timestamp_seconds
     -
     config_control_drift_event_created_timestamp_seconds
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Avg Time to Resolve Drift`
5. **Unit**: `seconds`
6. Click **Apply**

**Note**: This metric might not be available. Skip if not tracked.

#### Panel 4: Drift Events by Service (Top 10)

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   topk(10, sum(increase(config_control.drift_event.save_count[1h])) by (service))
   ```
   Or:
   ```promql
   topk(10, sum(config_control.drift_event.save_count) by (service))
   ```
3. **Visualization**: `Table` or `Time series`
4. **Title**: `Top 10 Services by Drift Events`
5. **Transform** → **Organize fields**: Show `service` and value
6. Click **Apply**

### Step 4: Create Row 3 - Approval Workflow

#### Panel 1: Pending Approval Requests Count

1. **Add row** → `Approval Workflow`
2. **Add panel** → **Add visualization**
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
3. **Visualization**: `Stat`
4. **Title**: `Pending Approval Requests`
5. **Unit**: `short`
6. **Thresholds**: Green (0), Yellow (1-10), Red (10-100)
7. Click **Apply**

#### Panel 2: Approval Processing Time

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(config_control.approval.processing_duration_bucket[5m])) by (le)
   )
   ```
   Or average:
   ```promql
   avg(config_control_approval_processing_time_seconds{status=~"approved|rejected"})
   ```
3. **Visualization**: `Time series` or `Stat`
4. **Title**: `Approval Processing Time (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

**Note**: Adjust based on actual metric availability.

#### Panel 3: Approval Success Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(increase(config_control.approval.approve_count[1h]))
     /
     (sum(increase(config_control.approval.approve_count[1h]))
     + sum(increase(config_control.approval.reject_count[1h])))
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Approval Success Rate (%)`
5. **Unit**: `percent (0-100)`
6. Click **Apply**

#### Panel 4: Approvals by Team

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(increase(config_control.approval.approve_count[1h])) by (team)
   ```
   Or:
   ```promql
   count by (team) (config_control.approval.approve_count{team=~".+"})
   ```
3. **Visualization**: `Time series` or `Table`
4. **Title**: `Approvals by Team`
5. **Legend**: `{{team}}`
6. **Unit**: `short`
7. Click **Apply**

### Step 5: Create Row 4 - Service Ownership

#### Panel 1: Ownership Transfers Rate

1. **Add row** → `Service Ownership`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.application_service.transfer_ownership_count[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Ownership Transfer Rate`
5. **Unit**: `transfers/min`
6. Click **Apply**

#### Panel 2: Services Claimed Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.approval.approve_count{
     workflow_type="service_claim"
   }[5m]))
   ```
   Or:
   ```promql
   count(increase(config_control_application_service_count{
     owner_team=~".+",
     previous_owner_team=""
   }[1h]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Services Claimed Rate`
5. **Unit**: `claims/min`
6. Click **Apply**

**Note**: Adjust based on how service claiming is tracked.

#### Panel 3: Services by Owner Team

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   count by (owner_team) (config_control_application_service_count{owner_team=~".+"})
   ```
   Or:
   ```promql
   sum(config_control_application_service_count) by (owner_team)
   ```
3. **Visualization**: `Table` or `Pie chart`
4. **Title**: `Services by Owner Team`
5. **Legend**: `{{owner_team}}`
6. **Unit**: `short`
7. Click **Apply**

### Step 6: Create Row 5 - Cache Performance

#### Panel 1: Overall Cache Hit Ratio

1. **Add row** → `Cache Performance`
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
4. **Title**: `Overall Cache Hit Ratio (%)`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (80-100%), Yellow (60-80%), Red (0-60%)
7. Click **Apply**

#### Panel 2: Cache Hit Ratio by Cache Name

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(rate(cache.gets{result="hit",service="$service"}[5m])) by (cache_name)
     /
     sum(rate(cache.gets{service="$service"}[5m])) by (cache_name)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Cache Hit Ratio by Cache Name`
5. **Legend**: `{{cache_name}}`
6. **Unit**: `percent (0-100)`
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
7. Click **Apply**

#### Panel 4: L1 vs L2 Cache Hit Ratio (If Two-Level Cache)

If you have two-level cache (L1/L2):

1. **Add panel** → **Add visualization**
2. **Query A** (L1):
   ```promql
   100 * (
     sum(rate(cache.custom.l1.hits{service="$service"}[5m]))
     /
     sum(rate(cache.custom.l1.requests{service="$service"}[5m]))
   )
   ```
3. **Query B** (L2):
   ```promql
   100 * (
     sum(rate(cache.custom.l2.hits{service="$service"}[5m]))
     /
     sum(rate(cache.custom.l2.requests{service="$service"}[5m]))
   )
   ```
4. **Visualization**: `Time series`
5. **Title**: `L1 vs L2 Cache Hit Ratio`
6. **Legend**: `L1`, `L2`
7. **Unit**: `percent (0-100)`
8. Click **Apply**

**Note**: Adjust metric names based on actual two-level cache metrics from CacheMetrics.java.

### Step 7: Save Dashboard

1. Click **Save dashboard**
2. **Name**: `Business Intelligence`
3. **Folder**: `3-Business`
4. **Tags**: `business-intelligence`, `operations`, `metrics`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### Business Metric Patterns

#### Service Count Calculation

```promql
count(count by (service) (up{job=~".*-service.*"} == 1))
```

**Breakdown**:
1. `up{job=~".*-service.*"} == 1` - Select healthy services
2. `count by (service)` - Count instances per service (one series per service)
3. `count(...)` - Count number of services (unique service values)

#### Drift Resolution Rate

```promql
100 * (
  sum(increase(config_control.drift_event.resolve_count[1h]))
  /
  sum(increase(config_control.drift_event.save_count[1h]))
)
```

**Breakdown**:
1. `increase(...[1h])` - Total increase over 1 hour
2. Divide resolved by created
3. `* 100` - Convert to percentage

**Limitation**: If counters reset, calculation might be incorrect. Use gauge metrics if available.

### Alternative Queries

#### Orphan Services (Alternative)

If orphan tracked differently:
```promql
count(config_control_application_service_count{owner_team=""})
```

Or:
```promql
count(config_control_application_service_count) 
- 
count(config_control_application_service_count{owner_team=~".+"})
```

#### Services by Team (Alternative)

If team info in different label:
```promql
count by (metadata_team) (up{metadata_team=~".+"} == 1)
```

---

## ⚙️ Panel Configuration (Advanced)

### Pie Chart Configuration

For services by team:
1. **Visualization**: `Pie chart`
2. **Options** → **Legend**: `Show`
3. **Options** → **Values**: `Show`
4. **Options** → **Percentage**: `Show`

### Table Configuration

For top N lists:
1. **Transform** → **Organize fields**: Show relevant columns
2. **Options** → **Sort by**: Value column, descending
3. **Options** → **Page size**: 10 or 20

### Gauge Visualization

For percentages (hit ratio):
1. **Visualization**: `Gauge`
2. **Options** → **Min**: `0`, **Max**: `100`
3. **Options** → **Thresholds**: Configure color zones
4. **Field** → **Unit**: `percent (0-100)`

---

## 🏷️ Variables & Templating

### Team Variable

Filter by team:
```promql
count by (team) (up{team="$team"} == 1)
```

### Service Variable

Filter by service:
```promql
config_control_drift_event_save_count{service="$service"}
```

---

## 🔗 Links & Drilldowns

### Link to Service Dashboards

Link service panels to service-specific dashboards:
1. Panel options → **Links**
2. **Add link** → **Type**: `Dashboard`
3. **Dashboard**: Service detail dashboard
4. **Variable**: `service=${__field.labels.service}`

### Link to Approval Workflow

Link approval panels to detailed approval dashboard:
1. **Data links** → **Add link**
2. **URL**: `/d/approval-workflow?var-team=${__field.labels.team}`

---

## ✅ Best Practices

### Business Metrics Monitoring

1. **Track trends** - Watch for changes over time
2. **Set business thresholds** - Based on operational SLAs
3. **Monitor orphan services** - High count indicates ownership issues
4. **Watch drift resolution** - Low rate indicates operational issues

### Performance Optimization

1. **Cache hit ratios** - Low ratios need optimization
2. **Approval processing time** - Long times indicate bottlenecks
3. **Drift resolution time** - Slow resolution impacts reliability

---

## 🐛 Troubleshooting

### "No data" for business metrics

**Check**:
1. Business metrics are enabled and tracked
2. Operations are happening (heartbeats, approvals)
3. Metric names match actual instrumentation

**Debug**:
```promql
# List all config_control metrics
{__name__=~"config_control.*"}

# Check specific metric
config_control.drift_event.save_count

# Check if operations happening
increase(config_control.approval.create_request_count[1h])
```

### Incorrect team information

**Check**:
1. Team labels are being set in metrics
2. Team information is available in metadata
3. Label name matches query (team vs owner_team)

---

## 📚 References

### Related Dashboards
- [Platform Overview](../phase1-foundation/01-platform-overview.md)
- [Config Control Service - Business Operations](../phase2-service-deep-dive/01-config-control-business-operations.md)
- [Approval Workflow Dashboard](03-approval-workflow.md)

### Metrics Reference
- **Business metrics**: `config_control.*` (from `MetricsNames.java`)
- **Cache metrics**: `cache.*` (from Spring Boot auto-instrumentation)
- **Service metrics**: `up`, `http_server_requests_seconds_*`

---

**Next**: [Instance-Level Dashboard](02-instance-level.md)  
**Previous**: [Consul Dashboard](../phase3-infrastructure/04-consul.md)

