# Instance-Level Dashboard

This dashboard provides deep dive into specific service instances, showing instance health, metrics, dependencies, and drift history.

## 📊 Overview

### Purpose
Monitor individual service instances in detail, track instance-specific metrics, dependencies, and configuration drift history.

### Target Audience
- **SRE teams** - Instance-level troubleshooting and debugging
- **Developers** - Understanding instance behavior
- **Operations teams** - Instance health monitoring

### Key Metrics Covered
- **Instance Health**: Status, last heartbeat, config hash match
- **Instance Metrics**: Request rate, error rate, latency for this instance
- **Instance Dependencies**: MongoDB, Redis, Kafka operations from this instance
- **Drift History**: Drift events timeline, config hash changes, auto-refresh triggers

### Prerequisites
- Prometheus scraping instance-level metrics
- Instance IDs tracked in metrics
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))
- Understanding of instance identification

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build instance-specific panels step-by-step
3. Configure instance variable to select instances

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Set up instance variable for selection

### Step 2: Set Up Variables

#### Variable 1: Service

1. Dashboard settings → **Variables** → **Add variable**
2. Configure:
   - **Name**: `service`
   - **Type**: `Query`
   - **Query**: `label_values(http_server_requests_seconds_count, service)`
   - **Default value**: `config-control-service`
3. Click **Apply**

#### Variable 2: Instance

1. Add variable:
   - **Name**: `instance`
   - **Type**: `Query`
   - **Query**: `label_values(http_server_requests_seconds_count{service="$service"}, instance)`
   - **Label**: `Instance`
   - **Multi-value**: `No` (select one instance for deep dive)
   - **Include All**: `No`
2. Click **Apply**

**Note**: This dashboard is designed to focus on one instance at a time.

### Step 3: Create Row 1 - Instance Health

#### Panel 1: Instance Status

1. Click **Add visualization**
2. **Query tab**:
   ```promql
   up{service="$service", instance="$instance"}
   ```
3. **Visualization**: `Stat`
4. **Title**: `Instance Status`
5. **Thresholds**: Red (0-0.5), Green (0.5-1)
6. **Value mappings**: `1` → `HEALTHY`, `0` → `UNHEALTHY`
7. Click **Apply**

#### Panel 2: Last Heartbeat Timestamp

If heartbeat timestamp is tracked:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   heartbeat.last_seen_timestamp{service="$service", instance="$instance"}
   ```
   Or convert from time since last heartbeat:
   ```promql
   time() - heartbeat.last_heartbeat_timestamp_seconds{service="$service", instance="$instance"}
   ```
3. **Visualization**: `Stat` or `Time series`
4. **Title**: `Time Since Last Heartbeat`
5. **Unit**: `seconds`
6. **Thresholds**: Green (0-30s), Yellow (30-60s), Red (>60s)
7. Click **Apply**

**Note**: Adjust based on available heartbeat metrics.

#### Panel 3: Config Hash Match Status

If config hash match is tracked:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   config_control_instance_config_hash_match{
     service="$service",
     instance="$instance"
   }
   ```
   Or:
   ```promql
   # 1 = match, 0 = mismatch
   config_control_drift_event_current_count{
     service="$service",
     instance="$instance",
     resolved="false"
   } == 0
   ```
3. **Visualization**: `Stat`
4. **Title**: `Config Hash Match Status`
5. **Thresholds**: Green (1), Red (0)
6. **Value mappings**: `1` → `MATCH`, `0` → `MISMATCH`
7. Click **Apply**

#### Panel 4: Instance Metadata

1. **Add panel** → **Add visualization**
2. **Query**: Query for metadata labels (version, environment, zone)
   ```promql
   up{service="$service", instance="$instance"}
   ```
   Use label values in panel description or table:
   ```promql
   # Query returns instance with labels
   # Use field overrides to display labels
   up{service="$service", instance="$instance"}
   ```
3. **Visualization**: `Table` or `Stat`
4. **Title**: `Instance Metadata`
5. **Transform** → **Organize fields**: Show labels (version, environment, zone)
6. Click **Apply**

**Alternative**: Use **Text** panel with variable values:
- Version: `${__field.labels.version}`
- Environment: `${__field.labels.environment}`
- Zone: `${__field.labels.zone}`

### Step 4: Create Row 2 - Instance Metrics

#### Panel 1: Request Rate for This Instance

1. **Add row** → `Instance Metrics`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(http_server_requests_seconds_count{
     service="$service",
     instance="$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Request Rate`
5. **Unit**: `reqps`
6. Click **Apply**

#### Panel 2: Error Rate for This Instance

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(rate(http_server_requests_seconds_count{
       service="$service",
       instance="$instance",
       status=~"5..|4.."
     }[5m]))
     /
     sum(rate(http_server_requests_seconds_count{
       service="$service",
       instance="$instance"
     }[5m]))
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Error Rate (%)`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Y-axis at 1% (warning), 5% (critical)
7. Click **Apply**

#### Panel 3: Response Time for This Instance

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket{
       service="$service",
       instance="$instance"
     }[5m])) by (le)
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `Response Time (p95)`
5. **Unit**: `seconds`
6. Click **Apply**

#### Panel 4: JVM Metrics for This Instance

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     sum(jvm_memory_used_bytes{
       service="$service",
       instance="$instance",
       area="heap"
     })
     /
     sum(jvm_memory_max_bytes{
       service="$service",
       instance="$instance",
       area="heap"
     })
   )
   ```
3. **Visualization**: `Time series`
4. **Title**: `JVM Heap Usage %`
5. **Unit**: `percent (0-100)`
6. Click **Apply**

#### Panel 5: GC Metrics for This Instance

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(jvm_gc_pause_seconds_sum{
     service="$service",
     instance="$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `GC Pause Time (per second)`
5. **Unit**: `seconds`
6. **Legend**: `{{gc}}`
7. Click **Apply**

#### Panel 6: Thread Count

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(jvm_threads_live_threads{
     service="$service",
     instance="$instance"
   })
   ```
3. **Visualization**: `Time series`
4. **Title**: `Thread Count`
5. **Unit**: `short`
6. Click **Apply**

### Step 5: Create Row 3 - Instance Dependencies

#### Panel 1: MongoDB Operations from This Instance

1. **Add row** → `Instance Dependencies`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(mongo_commands_total{
     service="$service",
     instance="$instance"
   }[5m])) by (command)
   ```
   Or if using HTTP client metrics:
   ```promql
   sum(rate(http_client_requests_seconds_count{
     service="$service",
     instance="$instance",
     uri=~".*mongodb.*"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `MongoDB Operations from This Instance`
5. **Legend**: `{{command}}` or `All`
6. **Unit**: `ops/sec`
7. Click **Apply**

#### Panel 2: Redis Operations from This Instance

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(cache.gets{
     service="$service",
     instance="$instance"
   }[5m])) by (cache_name)
   ```
   Or:
   ```promql
   sum(rate(http_client_requests_seconds_count{
     service="$service",
     instance="$instance",
     uri=~".*redis.*"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Redis Operations from This Instance`
5. **Legend**: `{{cache_name}}` or `All`
6. **Unit**: `ops/sec`
7. Click **Apply**

#### Panel 3: Kafka Operations from This Instance

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(spring_kafka_producer_records_total{
     service="$service",
     instance="$instance"
   }[5m])) by (topic)
   ```
   Or:
   ```promql
   sum(rate(spring_kafka_consumer_records_total{
     service="$service",
     instance="$instance"
   }[5m])) by (topic)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Kafka Operations from This Instance`
5. **Legend**: `{{topic}}`
6. **Unit**: `messages/sec`
7. Click **Apply**

### Step 6: Create Row 4 - Drift History

#### Panel 1: Drift Events Timeline for This Instance

1. **Add row** → `Drift History`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(increase(config_control.drift_event.save_count{
     service="$service",
     instance="$instance"
   }[1h])) by (severity)
   ```
   Or if using event metrics:
   ```promql
   config_control_drift_event_count{
     service="$service",
     instance="$instance"
   }
   ```
3. **Visualization**: `Time series`
4. **Title**: `Drift Events Timeline`
5. **Legend**: `{{severity}}`
6. **Unit**: `events`
7. Click **Apply**

#### Panel 2: Config Hash Changes Timeline

If config hash changes are tracked:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   changes(config_control_instance_config_hash{
     service="$service",
     instance="$instance"
   }[1h])
   ```
3. **Visualization**: `Time series`
4. **Title**: `Config Hash Changes`
5. **Unit**: `changes`
6. Click **Apply**

**Note**: `changes()` function counts the number of times a value changes.

#### Panel 3: Auto-Refresh Trigger Events

If auto-refresh triggers are tracked:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(config_control_drift_auto_refresh_triggered_count{
     service="$service",
     instance="$instance"
   }[5m]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Auto-Refresh Triggers`
5. **Unit**: `triggers/min`
6. Click **Apply**

**Note**: This metric might not exist. Skip if not available.

#### Panel 4: Drift Resolution Timeline

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(increase(config_control.drift_event.resolve_count{
     service="$service",
     instance="$instance"
   }[1h]))
   ```
3. **Visualization**: `Time series`
4. **Title**: `Drift Resolution Timeline`
5. **Unit**: `events`
6. Click **Apply**

### Step 7: Save Dashboard

1. Click **Save dashboard**
2. **Name**: `Instance-Level Dashboard`
3. **Folder**: `4-Detailed` or `3-Business`
4. **Tags**: `instance-level`, `detailed`, `monitoring`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### Instance-Level Filtering

All queries filter by both service and instance:
```promql
metric_name{
  service="$service",
  instance="$instance"
}
```

### Time Since Last Heartbeat

If last heartbeat timestamp is tracked:
```promql
time() - heartbeat.last_heartbeat_timestamp_seconds{
  service="$service",
  instance="$instance"
}
```

**What it does**: Current time minus last heartbeat time = seconds since last heartbeat.

### Config Hash Match Detection

Track if config hash matches:
```promql
# If hash match metric exists
config_control_instance_config_hash_match{
  service="$service",
  instance="$instance"
}

# Or infer from drift events
config_control_drift_event_current_count{
  service="$service",
  instance="$instance",
  resolved="false"
} == 0  # No unresolved drift = match
```

### Changes() Function

Count value changes:
```promql
changes(config_hash_value[1h])
```

Returns number of times the value changed in the time range.

---

## ⚙️ Panel Configuration (Advanced)

### Instance Selection

Use variable dropdown:
1. Set instance variable to single-value (not multi-value)
2. Use `instance="$instance"` in queries
3. Update panel titles to include instance: `Metrics for ${instance}`

### Metadata Display

Show instance metadata:
1. Use **Text** panel with markdown
2. Use variable values: `${instance}`, `${service}`
3. Or use **Table** panel with label values

---

## 🏷️ Variables & Templating

### Instance Variable

Single-instance selection:
```promql
metric{instance="$instance"}
```

### Chained Variables

Instance depends on service:
1. **Instance variable** query:
   ```promql
   label_values(http_server_requests_seconds_count{service="$service"}, instance)
   ```
2. Instance options update when service changes

---

## 🔗 Links & Drilldowns

### Link from Service Dashboard

Link from service-level dashboard:
1. Service dashboard panel → **Links**
2. **Add link** → **Type**: `Dashboard`
3. **Dashboard**: Instance-Level Dashboard
4. **Variable**: `instance=${__field.labels.instance}`

### Link to Logs

Link to instance logs:
1. Panel options → **Data links**
2. **Add link**
3. **URL**: `/explore?orgId=1&left=["now-1h","now","Loki",{"expr":"{service=\"$service\",instance=\"$instance\"}"}]`

---

## ✅ Best Practices

### Instance-Level Monitoring

1. **Focus on one instance** - Dashboard shows one instance at a time
2. **Compare with service average** - Show instance vs service average
3. **Track drift history** - Identify recurring issues
4. **Monitor dependencies** - Instance-specific dependency performance

---

## 🐛 Troubleshooting

### "No data" for instance

**Check**:
1. Instance variable is set correctly
2. Instance exists: `up{instance="$instance"}`
3. Metrics have instance label

**Debug**:
```promql
# Check instance exists
up{instance="$instance"}

# List all instances
label_values(http_server_requests_seconds_count, instance)

# Check metrics for instance
http_server_requests_seconds_count{instance="$instance"}
```

---

## 📚 References

### Related Dashboards
- [Config Control Service - Golden Signals](../phase1-foundation/02-config-control-service-golden-signals.md)
- [Business Intelligence Dashboard](01-business-intelligence.md)

### External Documentation
- [Grafana Variables](https://grafana.com/docs/grafana/latest/dashboards/variables/)

---

**Next**: [Approval Workflow Dashboard](03-approval-workflow.md)  
**Previous**: [Business Intelligence Dashboard](01-business-intelligence.md)

