# Config Control Service - Business Operations Dashboard

This dashboard monitors business-specific operations in Config Control Service, including heartbeat processing, drift detection, service management, approval workflows, and cleanup operations.

## 📊 Overview

### Purpose
Monitor business operations performance, track workflow metrics, and identify operational issues in Config Control Service.

### Target Audience
- **Operations teams** - Business operations monitoring and workflow tracking
- **Developers** - Understanding business metric performance
- **SRE teams** - Operational health and workflow optimization
- **Product teams** - Business process metrics and trends

### Key Metrics Covered
- **Heartbeat Operations**: Processing rate, latency, success/failure
- **Drift Detection**: Detection rate, resolution rate, active drift events
- **Service Management**: Instance operations, application service operations, sharing operations
- **Approval Workflow**: Request creation, decision rates, pending approvals
- **Cleanup Operations**: Stale instance marking and deletion

### Prerequisites
- Prometheus scraping Config Control Service metrics
- Grafana configured with Prometheus data source
- Custom business metrics enabled (heartbeat, drift, approvals)
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))
- Completed [Golden Signals Dashboard](../phase1-foundation/02-config-control-service-golden-signals.md) (recommended)

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build each panel step-by-step
3. Customize metric names based on your actual metric names from MetricsNames.java

### Step 2: Verify Dashboard Works

After creating all panels:
1. Check all panels show data (not "No data")
2. Verify custom metric names match your instrumentation
3. Test variables (service, instance) if configured
4. Verify business workflows are being tracked

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

This tutorial walks you through creating the complete Business Operations dashboard.

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Click **Add visualization** → **Add panel**
3. Click **Dashboard settings** (gear icon)

### Step 2: Set Up Variables

#### Variable 1: Service

1. Dashboard settings → **Variables** → **Add variable**
2. Configure:
   - **Name**: `service`
   - **Type**: `Query`
   - **Query**: `label_values(http_server_requests_seconds_count, service)`
   - **Label**: `Service`
   - **Default value**: `config-control-service`
3. Click **Apply**

#### Variable 2: Instance (Optional)

1. Add variable:
   - **Name**: `instance`
   - **Type**: `Query`
   - **Query**: `label_values(http_server_requests_seconds_count{service="$service"}, instance)`
   - **Multi-value**: `Yes`
   - **Include All**: `Yes`
2. Click **Apply**

### Step 3: Create Row 1 - Heartbeat Operations

Heartbeat processing metrics show how well the service receives and processes heartbeats from client instances.

#### Panel 1: Heartbeat Processing Rate

1. Click **Add visualization**
2. **Query tab**:
   ```promql
   sum(rate(heartbeat.process_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
   Or if using timer metric:
   ```promql
   sum(rate(heartbeat.process{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Heartbeat Processing Rate`
5. **Unit**: `reqps` (requests per second)
6. **Legend**: `{{instance}}` (if multiple instances)
7. **Description**: `Rate of heartbeat messages processed per second`
8. Click **Apply**

**Note**: Metric name `heartbeat.process` comes from `MetricsNames.Heartbeat.PROCESS`. Adjust if using different metric names.

#### Panel 2: Heartbeat Processing Latency p50/p95/p99

1. **Add panel** → **Add visualization**
2. **Query A** (p50):
   ```promql
   histogram_quantile(0.50,
     sum(rate(heartbeat.process_bucket{
       service="$service",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
3. **Query B** (p95):
   ```promql
   histogram_quantile(0.95,
     sum(rate(heartbeat.process_bucket{
       service="$service",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
4. **Query C** (p99):
   ```promql
   histogram_quantile(0.99,
     sum(rate(heartbeat.process_bucket{
       service="$service",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
5. **Visualization**: `Time series`
6. **Title**: `Heartbeat Processing Latency`
7. **Legend**: `p50`, `p95`, `p99` (use legend overrides)
8. **Unit**: `seconds`
9. Click **Apply**

**Note**: Requires histogram buckets. Ensure `heartbeat.process` has histogram enabled in `application-observability.yml`.

#### Panel 3: Heartbeat Success/Failure Rate

If heartbeat processing tracks success/failure:

1. **Add panel** → **Add visualization**
2. **Query A** (Success):
   ```promql
   sum(rate(heartbeat.process_count{
     service="$service",
     status="success",
     instance=~"$instance"
   }[5m]))
   ```
3. **Query B** (Failure):
   ```promql
   sum(rate(heartbeat.process_count{
     service="$service",
     status="failure",
     instance=~"$instance"
   }[5m]))
   ```
4. **Visualization**: `Time series`
5. **Title**: `Heartbeat Success vs Failure Rate`
6. **Legend**: `Success`, `Failure`
7. **Unit**: `reqps`
8. Click **Apply**

**Note**: Adjust based on actual metric labels. If no status label, track via separate metrics.

#### Panel 4: Active Heartbeat Connections (If Available)

If your service tracks active heartbeat connections:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(heartbeat.active_connections{
     service="$service",
     instance=~"$instance"
   })
   ```
3. **Visualization**: `Stat` or `Time series`
4. **Title**: `Active Heartbeat Connections`
5. **Unit**: `short`
6. Click **Apply**

**Note**: This metric might not exist. Create if needed or skip this panel.

### Step 4: Create Row 2 - Drift Detection

Drift detection metrics show configuration drift detection and resolution.

#### Panel 1: Drift Events Created Rate

1. **Add row** → `Drift Detection`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   sum(rate(config_control.drift_event.save_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
   Or if using counter:
   ```promql
   sum(rate(config_control_drift_event_save_total{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
4. **Visualization**: `Time series`
5. **Title**: `Drift Events Created Rate`
6. **Unit**: `events/min` or `ops/sec`
7. **Description**: `Rate at which configuration drift events are detected and created`
8. Click **Apply**

**Note**: Metric name from `MetricsNames.DriftEvent.SAVE`.

#### Panel 2: Drift Events Resolved Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.drift_event.resolve_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
   Or:
   ```promql
   sum(rate(config_control_drift_event_resolve_total{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Drift Events Resolved Rate`
5. **Unit**: `events/min`
6. Click **Apply**

#### Panel 3: Active Unresolved Drift Events Count

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(increase(config_control.drift_event.save_count[1h]))
   -
   sum(increase(config_control.drift_event.resolve_count[1h]))
   ```
   Or if you have a gauge metric:
   ```promql
   sum(config_control_drift_event_unresolved_count{
     service="$service",
     instance=~"$instance"
   })
   ```
3. **Visualization**: `Stat`
4. **Title**: `Active Unresolved Drift Events`
5. **Unit**: `short`
6. **Thresholds**:
   - **Green**: `null` to `0` (No drift)
   - **Yellow**: `0` to `5` (Some drift)
   - **Red**: `5` to `100` (Many drift events)
7. Click **Apply**

**Note**: Calculation method depends on available metrics. Adjust based on your implementation.

#### Panel 4: Drift Detection Latency

If drift detection tracks time-to-detect:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(config_control.drift_event.detection_duration_bucket{
       service="$service",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Drift Detection Latency (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

**Note**: This metric might not exist. Skip if not available.

#### Panel 5: Drift Events by Severity

If drift events have severity labels:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.drift_event.save_count{
     service="$service",
     instance=~"$instance"
   }[5m])) by (severity)
   ```
3. **Visualization**: `Time series` or `Stat`
4. **Title**: `Drift Events by Severity`
5. **Legend**: `{{severity}}`
6. **Unit**: `events/min`
7. Click **Apply**

**Note**: Adjust based on actual severity label name and values (e.g., `severity`, `level`).

### Step 5: Create Row 3 - Service Management

Service management operation metrics.

#### Panel 1: Service Instance Save Operations

1. **Add row** → `Service Management`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   sum(rate(config_control.service_instance.save_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
   Or:
   ```promql
   sum(rate(config_control_service_instance_save_total{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
4. **Visualization**: `Time series`
5. **Title**: `Service Instance Save Operations Rate`
6. **Unit**: `ops/sec`
7. **Description**: `Rate of service instance save operations (from heartbeat processing)`
8. Click **Apply**

**Note**: Metric from `MetricsNames.ServiceInstance.SAVE`.

#### Panel 2: Application Service Save Operations

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.application_service.save_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Application Service Save Operations Rate`
5. **Unit**: `ops/sec`
6. Click **Apply**

#### Panel 3: Application Service Ownership Transfer Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.application_service.transfer_ownership_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Ownership Transfer Rate`
5. **Unit**: `ops/sec`
6. Click **Apply**

**Note**: Metric from `MetricsNames.ApplicationService.TRANSFER_OWNERSHIP`.

#### Panel 4: Service Share Grant Operations

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.service_share.grant_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Service Share Grant Operations Rate`
5. **Unit**: `ops/sec`
6. Click **Apply**

**Note**: Metric from `MetricsNames.ServiceShare.GRANT`.

#### Panel 5: Service Share Revoke Operations

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.service_share.revoke_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Service Share Revoke Operations Rate`
5. **Unit**: `ops/sec`
6. Click **Apply**

**Note**: Metric from `MetricsNames.ServiceShare.REVOKE`.

### Step 6: Create Row 4 - Approval Workflow

Approval workflow metrics track the approval process for service ownership.

#### Panel 1: Approval Request Creation Rate

1. **Add row** → `Approval Workflow`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   sum(rate(config_control.approval.create_request_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
   Or:
   ```promql
   sum(rate(config_control_approval_create_request_total{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
4. **Visualization**: `Time series`
5. **Title**: `Approval Request Creation Rate`
6. **Unit**: `requests/min`
7. **Description**: `Rate at which approval requests are created`
8. Click **Apply**

**Note**: Metric from `MetricsNames.Approval.CREATE_REQUEST`.

#### Panel 2: Approval Decisions Rate (Approve vs Reject)

1. **Add panel** → **Add visualization**
2. **Query A** (Approve):
   ```promql
   sum(rate(config_control.approval.approve_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
3. **Query B** (Reject):
   ```promql
   sum(rate(config_control.approval.reject_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
4. **Visualization**: `Time series`
5. **Title**: `Approval Decisions Rate`
6. **Legend**: `Approve`, `Reject`
7. **Unit**: `decisions/min`
8. Click **Apply**

**Note**: Metrics from `MetricsNames.Approval.APPROVE` and `MetricsNames.Approval.REJECT`.

#### Panel 3: Pending Approval Requests Count

If you have a gauge metric for pending approvals:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(config_control_approval_pending_count{
     service="$service",
     instance=~"$instance"
   })
   ```
   Or calculate from counters:
   ```promql
   sum(increase(config_control.approval.create_request_count[1h]))
   -
   (sum(increase(config_control.approval.approve_count[1h]))
   + sum(increase(config_control.approval.reject_count[1h])))
   ```
3. **Visualization**: `Stat`
4. **Title**: `Pending Approval Requests`
5. **Unit**: `short`
6. **Thresholds**:
   - **Green**: `null` to `0` (No pending)
   - **Yellow**: `0` to `5` (Some pending)
   - **Red**: `5` to `100` (Many pending)
7. Click **Apply**

#### Panel 4: Approval Processing Time

If approval processing tracks duration:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(config_control.approval.processing_duration_bucket{
       service="$service",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Approval Processing Time (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

**Note**: This metric might not exist. Skip if not available.

### Step 7: Create Row 5 - Cleanup Operations

Cleanup operation metrics for stale instance management.

#### Panel 1: Stale Instances Marked Rate

1. **Add row** → `Cleanup Operations`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   sum(rate(config_control.cleanup.stale_instances_marked_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
   Or:
   ```promql
   sum(rate(config_control_cleanup_stale_instances_marked_total{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
4. **Visualization**: `Time series`
5. **Title**: `Stale Instances Marked Rate`
6. **Unit**: `instances/min`
7. **Description**: `Rate at which instances are marked as stale`
8. Click **Apply**

**Note**: Metric from `MetricsNames.Cleanup.STALE_INSTANCES_MARKED`.

#### Panel 2: Stale Instances Deleted Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control.cleanup.stale_instances_deleted_count{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
   Or:
   ```promql
   sum(rate(config_control_cleanup_stale_instances_deleted_total{
     service="$service",
     instance=~"$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Stale Instances Deleted Rate`
5. **Unit**: `instances/min`
6. Click **Apply**

**Note**: Metric from `MetricsNames.Cleanup.STALE_INSTANCES_DELETED`.

#### Panel 3: Cleanup Operation Duration

If cleanup operations track duration:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(config_control.cleanup.duration_bucket{
       service="$service",
       instance=~"$instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Cleanup Operation Duration (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

**Note**: This metric might not exist. Skip if not available.

### Step 8: Save Dashboard

1. Click **Save dashboard**
2. **Name**: `Config Control Service - Business Operations`
3. **Folder**: Create or select `1-Services`
4. **Tags**: `business-operations`, `config-control-service`, `workflows`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### Understanding Business Metrics

Business metrics are typically:
- **Counters**: Operations performed (save, create, delete)
- **Timers**: Duration of operations (processing time, latency)
- **Gauges**: Current state (pending count, active connections)

### Counter-Based Metrics

Counters only increase. Always use `rate()` or `increase()`:

```promql
# Rate (per second)
rate(config_control.drift_event.save_count[5m])

# Total increase over period
increase(config_control.drift_event.save_count[1h])
```

### Timer/Histogram Metrics

For latency/duration metrics with histogram buckets:

```promql
histogram_quantile(0.95,
  sum(rate(heartbeat.process_bucket[5m])) by (le)
)
```

### Calculating Unresolved Events

If you only have create/resolve counters:

```promql
# Unresolved = Created - Resolved
increase(config_control.drift_event.save_count[1h])
-
increase(config_control.drift_event.resolve_count[1h])
```

**Limitation**: This calculation resets if counters reset. Better to use a gauge metric if available.

### Alternative Queries

#### Heartbeat Rate (Alternative)

If heartbeat metric doesn't exist, use HTTP endpoint:
```promql
sum(rate(http_server_requests_seconds_count{
  uri="/api/heartbeat",
  method="POST"
}[5m]))
```

#### Drift Events (If Using Different Metric Names)

Adjust queries based on actual metric names:
```promql
# Check available drift metrics
{__name__=~"config_control.*drift.*"}

# Use actual metric name found
sum(rate(<actual_metric_name>[5m]))
```

---

## ⚙️ Panel Configuration (Advanced)

### Time Series with Multiple Series

When showing approve vs reject:
1. Use separate queries (Query A, B)
2. Use different colors
3. Stack if showing totals
4. Add threshold lines for targets

### Stat Panel Thresholds

For pending approvals:
1. **Thresholds**: Green (0), Yellow (5), Red (10)
2. **Color mode**: `Background` for visual impact
3. **Value mappings**: Add text for status

### Table Panels for Operations

Show top operations:
1. **Query**: `topk(10, sum(rate(...)) by (operation))`
2. **Transform**: Organize fields
3. **Sort**: By rate descending
4. **Color code**: Based on values

---

## 🏷️ Variables & Templating

### Filter by Workflow Type

If workflows have type labels:
```promql
sum(rate(config_control.approval.create_request_count{
  workflow_type="$workflow_type"
}[5m]))
```

### Time Range Variable

For different time windows:
```promql
increase(config_control.drift_event.save_count[$time_range])
```

---

## 🔗 Links & Drilldowns

### Link to Detailed Workflow Dashboards

Link approval panels to detailed approval workflow dashboard:
1. Panel options → **Links**
2. **Add link** → **Type**: `Dashboard`
3. **Dashboard**: Approval Workflow Dashboard
4. **Keep time**: `Yes`

### Link to Instance Dashboard

Link service instance panels to instance-level dashboard:
1. **Data links** → **Add link**
2. **URL**: `/d/instance-detail?var-instance=${__field.labels.instance}`

---

## ✅ Best Practices

### Business Metrics Monitoring

1. **Track rates not absolute values** - Use `rate()` for counters
2. **Monitor trends** - Watch for sudden changes
3. **Set meaningful thresholds** - Based on business SLAs
4. **Correlate with latency** - Slow operations might indicate issues

### Workflow Metrics

1. **Monitor end-to-end** - Track workflow from start to finish
2. **Track bottlenecks** - Identify slow steps
3. **Watch pending counts** - High pending indicates backlogs
4. **Monitor error rates** - Failed workflow steps

### Performance

1. **Use recording rules** for expensive aggregations
2. **Limit time ranges** - Use `[5m]` for rate calculations
3. **Filter early** - Apply service/instance filters first

---

## 🐛 Troubleshooting

### "No data" for business metrics

**Check**:
1. Metric names match actual instrumentation
2. Service is performing operations (heartbeats, approvals)
3. Metrics are being exported from application

**Debug**:
```promql
# List all config_control metrics
{__name__=~"config_control.*"}

# Check specific metric exists
heartbeat.process_count

# Check with service filter
heartbeat.process_count{service="config-control-service"}
```

### Counter metrics not increasing

**Check**:
1. Operations are actually happening
2. Metrics are being recorded in application code
3. Counter increment code is being executed

### Calculation showing negative values

**Possible cause**: Counter reset or calculation error

**Solution**: Use `increase()` instead of subtraction:
```promql
# Instead of: created - resolved
# Use increase for period
increase(created[1h]) - increase(resolved[1h])
```

---

## 📚 References

### Related Dashboards
- [Config Control Service - Golden Signals](../phase1-foundation/02-config-control-service-golden-signals.md)
- [Config Control Service - Infrastructure Dependencies](02-config-control-infrastructure-dependencies.md)
- [Approval Workflow Dashboard](../phase4-business-intelligence/03-approval-workflow.md)

### Metrics Reference
- **Heartbeat**: `heartbeat.process` (from `MetricsNames.Heartbeat.PROCESS`)
- **Drift Events**: `config_control.drift_event.*` (from `MetricsNames.DriftEvent`)
- **Approvals**: `config_control.approval.*` (from `MetricsNames.Approval`)
- **Service Operations**: `config_control.service_instance.*`, `config_control.application_service.*`
- **Cleanup**: `config_control.cleanup.*` (from `MetricsNames.Cleanup`)

See [MetricsNames.java](../../../../src/main/java/com/example/control/infrastructure/observability/MetricsNames.java) for all metric names.

---

**Next**: [Config Control Service - Infrastructure Dependencies](02-config-control-infrastructure-dependencies.md)  
**Previous**: [Config Control Service - Golden Signals](../phase1-foundation/02-config-control-service-golden-signals.md)

