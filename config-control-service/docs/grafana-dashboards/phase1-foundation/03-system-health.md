# System Health Dashboard

This dashboard provides overall system health indicators, monitoring service availability, infrastructure component health, and dependency status.

## 📊 Overview

### Purpose
Monitor overall system health at a glance, identify service availability issues, and track infrastructure component status.

### Target Audience
- **On-call engineers** - Quick system health checks during incidents
- **Operations teams** - Service availability monitoring
- **SRE teams** - System-wide health assessment
- **Infrastructure teams** - Component health tracking

### Key Metrics Covered
- Service health status (up/down)
- Infrastructure component health (MongoDB, Redis, Kafka, Consul, Config Server)
- Dependency connectivity status
- Service discovery health
- Configuration health

### Prerequisites
- Prometheus scraping metrics from all services
- Grafana configured with Prometheus data source
- All services running and exposing metrics
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))
- Completed [Platform Overview](01-platform-overview.md) (recommended)

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build each panel step-by-step
3. Customize service and component names for your environment

### Step 2: Verify Dashboard Works

After creating all panels:
1. Check all panels show correct status (up=1, down=0)
2. Verify component names match your Prometheus scrape configs
3. Test color coding (green=up, red=down)
4. Verify links to detailed dashboards work

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

This tutorial walks you through creating the complete System Health dashboard.

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Click **Add visualization** → **Add panel**
3. Click **Dashboard settings** (gear icon) to configure

### Step 2: Set Up Variables (Optional)

Variables can make the dashboard reusable across environments.

#### Variable 1: Environment

1. Dashboard settings → **Variables** → **Add variable**
2. Configure:
   - **Name**: `environment`
   - **Type**: `Query`
   - **Data source**: `Prometheus`
   - **Query**: `label_values(up, environment)`
   - **Label**: `Environment`
   - **Default value**: `All` or your environment
3. Click **Apply**

### Step 3: Create Row 1 - Service Health Status

Service health panels show if services are up (1) or down (0).

#### Panel 1: Config Control Service Health

1. Click **Add visualization**
2. **Query tab**:
   ```promql
   up{job=~"config-control-service.*"}
   ```
3. **Visualization tab**:
   - **Visualization**: `Stat`
   - **Title**: `Config Control Service`
   - **Options** → **Text**: `Value and name`
   - **Field** → **Unit**: `short`
4. **Field** → **Thresholds**:
   - **Mode**: `Absolute`
   - **Steps**:
     - **Red**: `null` to `0.5` (Down)
     - **Green**: `0.5` to `1` (Up)
5. **Field** → **Value mappings**:
   - **Value**: `1` → **Text**: `UP`
   - **Value**: `0` → **Text**: `DOWN`
6. **Panel options** → **Description**: `Config Control Service availability status`
7. Click **Apply**

**What this query does**: Returns 1 if service is up, 0 if down. The `up` metric comes from Prometheus target status.

#### Panel 2: Config Server Health

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   up{job=~"config-server.*"}
   ```
3. **Visualization**: `Stat`
4. **Title**: `Config Server`
5. **Thresholds**: Same as above (Red 0-0.5, Green 0.5-1)
6. **Value mappings**: `1` → `UP`, `0` → `DOWN`
7. Click **Apply**

#### Panel 3: All Services Health (Table)

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   up{job=~"config-control-service.*|config-server.*|.*-service.*"}
   ```
3. **Visualization**: `Table`
4. **Title**: `All Services Health`
5. **Transform** → **Organize fields**:
   - Show: `job` → "Service"
   - Show: `instance` → "Instance"
   - Show: `Value` → "Status" (with value mappings: 1→UP, 0→DOWN)
   - Hide: other fields
6. **Field** → **Overrides**:
   - **Fields with name**: `Status`
   - **Add override**: **Thresholds** → Red (0-0.5), Green (0.5-1)
   - **Add override**: **Custom**: **Cell display mode** → `Color background`
7. Click **Apply**

**Note**: Adjust job name pattern based on your Prometheus scrape configuration.

### Step 4: Create Row 2 - Infrastructure Health

Infrastructure component health panels.

#### Panel 1: MongoDB Health

1. **Add row** → `Infrastructure Health`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   up{job=~".*mongodb.*"}
   ```
4. **Visualization**: `Stat`
5. **Title**: `MongoDB`
6. **Thresholds**: Red (0-0.5), Green (0.5-1)
7. **Value mappings**: `1` → `UP`, `0` → `DOWN`
8. **Description**: `MongoDB database health status`
9. Click **Apply**

**Note**: Adjust job name pattern. If MongoDB is scraped differently, use appropriate job name.

#### Panel 2: Redis Health

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   up{job=~".*redis.*"}
   ```
3. **Visualization**: `Stat`
4. **Title**: `Redis`
5. **Thresholds**: Red (0-0.5), Green (0.5-1)
6. **Value mappings**: `1` → `UP`, `0` → `DOWN`
7. Click **Apply**

#### Panel 3: Kafka Health

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   up{job=~".*kafka.*"}
   ```
3. **Visualization**: `Stat`
4. **Title**: `Kafka`
5. **Thresholds**: Red (0-0.5), Green (0.5-1)
6. **Value mappings**: `1` → `UP`, `0` → `DOWN`
7. Click **Apply**

#### Panel 4: Consul Health

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   up{job=~".*consul.*"}
   ```
3. **Visualization**: `Stat`
4. **Title**: `Consul`
5. **Thresholds**: Red (0-0.5), Green (0.5-1)
6. **Value mappings**: `1` → `UP`, `0` → `DOWN`
7. Click **Apply**

#### Panel 5: Prometheus Health

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   up{job="prometheus"}
   ```
3. **Visualization**: `Stat`
4. **Title**: `Prometheus`
5. **Thresholds**: Red (0-0.5), Green (0.5-1)
6. **Value mappings**: `1` → `UP`, `0` → `DOWN`
7. Click **Apply**

**Note**: Prometheus usually has `job="prometheus"` for self-monitoring.

#### Panel 6: Grafana Health (If Monitored)

If Grafana is monitored by Prometheus:

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   up{job=~".*grafana.*"}
   ```
3. **Visualization**: `Stat`
4. **Title**: `Grafana`
5. **Thresholds**: Red (0-0.5), Green (0.5-1)
6. Click **Apply**

### Step 5: Create Row 3 - Infrastructure Component Table

A consolidated table view of all infrastructure components.

1. **Add row** → `Infrastructure Overview`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   up{job=~".*mongodb.*|.*redis.*|.*kafka.*|.*consul.*|prometheus|.*grafana.*"}
   ```
4. **Visualization**: `Table`
5. **Title**: `Infrastructure Components Health`
6. **Transform** → **Organize fields**:
   - Show: `job` → "Component"
   - Show: `instance` → "Instance"
   - Show: `Value` → "Status" (with value mappings)
   - Hide: other fields
7. **Field** → **Overrides**:
   - **Fields with name**: `Status`
   - **Thresholds**: Red (0-0.5), Green (0.5-1)
   - **Cell display mode**: `Color background`
8. Click **Apply**

### Step 6: Create Row 4 - Dependency Health

Dependency connectivity health panels.

#### Panel 1: Config Server Connectivity

1. **Add row** → `Dependency Health`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   up{job=~"config-server.*"} or on() vector(0)
   ```
   Or use health check metric if available:
   ```promql
   # If you have a health check metric
   config_server_health_check{service="config-control-service"}
   ```
4. **Visualization**: `Stat`
5. **Title**: `Config Server Connectivity`
6. **Thresholds**: Red (0-0.5), Green (0.5-1)
7. **Description**: `Ability of Config Control Service to reach Config Server`
8. Click **Apply**

**Note**: Adjust based on available metrics. You might need to create a custom health check metric.

#### Panel 2: MongoDB Connectivity

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   up{job=~".*mongodb.*"} or on() vector(0)
   ```
   Or use Spring Boot health metric:
   ```promql
   # If available
   spring_data_mongodb_repository_health_indicator_health{status="UP"}
   ```
3. **Visualization**: `Stat`
4. **Title**: `MongoDB Connectivity`
5. **Thresholds**: Red (0-0.5), Green (0.5-1)
6. Click **Apply**

#### Panel 3: Redis Connectivity

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   up{job=~".*redis.*"} or on() vector(0)
   ```
   Or use Spring Boot health metric:
   ```promql
   spring_data_redis_repository_health_indicator_health{status="UP"}
   ```
3. **Visualization**: `Stat`
4. **Title**: `Redis Connectivity`
5. **Thresholds**: Red (0-0.5), Green (0.5-1)
6. Click **Apply**

#### Panel 4: Kafka Connectivity

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   up{job=~".*kafka.*"} or on() vector(0)
   ```
   Or use health metric if available
3. **Visualization**: `Stat`
4. **Title**: `Kafka Connectivity`
5. **Thresholds**: Red (0-0.5), Green (0.5-1)
6. Click **Apply**

#### Panel 5: Consul Connectivity

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   up{job=~".*consul.*"} or on() vector(0)
   ```
   Or use Spring Cloud Consul health metric
3. **Visualization**: `Stat`
4. **Title**: `Consul Connectivity`
5. **Thresholds**: Red (0-0.5), Green (0.5-1)
6. Click **Apply**

### Step 7: Create Row 5 - Service Discovery Health

Service discovery and registration health.

#### Panel 1: Services Registered in Consul

1. **Add row** → `Service Discovery`
2. **Add panel** → **Add visualization**
3. **Query**:
   ```promql
   # If Consul exposes service count metric
   consul_catalog_services_total
   ```
   Or count from service instances:
   ```promql
   count(up{job=~"config-control-service.*|config-server.*"} == 1)
   ```
4. **Visualization**: `Stat`
5. **Title**: `Services Registered`
6. **Unit**: `short`
7. Click **Apply**

**Note**: Adjust based on available Consul metrics or service scrape configs.

#### Panel 2: Service Instance Count

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   count(up{job=~".*-service.*"} == 1)
   ```
3. **Visualization**: `Stat`
4. **Title**: `Total Service Instances`
5. **Unit**: `short`
6. Click **Apply**

#### Panel 3: Healthy vs Unhealthy Instances

1. **Add panel** → **Add visualization**
2. **Query A** (Healthy):
   ```promql
   count(up{job=~".*-service.*"} == 1)
   ```
3. **Query B** (Unhealthy):
   ```promql
   count(up{job=~".*-service.*"} == 0)
   ```
4. **Visualization**: `Time series` or `Stat`
5. **Title**: `Instance Health Status`
6. **Legend**: `Healthy`, `Unhealthy`
7. Click **Apply**

### Step 8: Create Row 6 - Configuration Health

Configuration and deployment health indicators.

#### Panel 1: Configuration Sync Status

If you have a metric tracking config sync:

1. **Add row** → `Configuration Health`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   # Example: If you have a config sync status metric
   config_control_config_sync_status{status="synced"}
   ```
   Or track via drift events:
   ```promql
   # Services with no active drift (healthy config)
   count(config_control_drift_event_count{resolved="true"}) - count(config_control_drift_event_count{resolved="false"})
   ```
3. **Visualization**: `Stat`
4. **Title**: `Configuration Sync Status`
5. Click **Apply**

**Note**: Adjust based on actual metrics available in your system.

#### Panel 2: Active Configuration Drift

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   count(config_control_drift_event_count{resolved="false"})
   ```
   Or use increase metric:
   ```promql
   sum(config_control.drift_event.save_count) - sum(config_control.drift_event.resolve_count)
   ```
3. **Visualization**: `Stat`
4. **Title**: `Active Configuration Drift Events`
5. **Unit**: `short`
6. **Thresholds**:
   - **Green**: `null` to `0` (No drift)
   - **Yellow**: `0` to `5` (Some drift)
   - **Red**: `5` to `100` (Many drift events)
7. Click **Apply**

### Step 9: Create Row 7 - System Overview Panel

A comprehensive overview panel combining multiple health indicators.

1. **Add row** → `System Overview`
2. **Add panel** → **Add visualization**
3. **Query**: Combine multiple health checks
   ```promql
   # Overall system health score (simplified)
   (
     (up{job=~"config-control-service.*"} == 1) +
     (up{job=~"config-server.*"} == 1) +
     (up{job=~".*mongodb.*"} == 1) +
     (up{job=~".*redis.*"} == 1) +
     (up{job=~".*kafka.*"} == 1) +
     (up{job=~".*consul.*"} == 1)
   ) / 6 * 100
   ```
4. **Visualization**: `Stat`
5. **Title**: `Overall System Health Score`
6. **Unit**: `percent (0-100)`
7. **Thresholds**:
   - **Red**: `null` to `50` (< 50% healthy)
   - **Yellow**: `50` to `80` (50-80% healthy)
   - **Green**: `80` to `100` (> 80% healthy)
8. **Description**: `Percentage of critical components that are healthy`
9. Click **Apply**

**Note**: Adjust component list and scoring logic based on your architecture.

### Step 10: Save Dashboard

1. Click **Save dashboard** (floppy disk icon)
2. **Name**: `System Health`
3. **Folder**: Create or select `0-Overview`
4. **Tags**: `system-health`, `infrastructure`, `monitoring`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### Understanding the `up` Metric

The `up` metric is automatically generated by Prometheus for each scrape target.

**Values**:
- `1`: Target is up (scrape succeeded)
- `0`: Target is down (scrape failed)

**Labels**:
- `job`: Scrape job name (from `prometheus.yml`)
- `instance`: Target instance URL

**Example**:
```promql
up{job="config-control-service-local", instance="config-control-service:8080"}
```

This returns `1` if Prometheus can scrape `http://config-control-service:8080/actuator/prometheus` successfully.

### Health Check Patterns

#### Simple Health Check

```promql
up{job="service-name"}
```

Returns `1` if service is up, `0` if down.

#### Health Check with Fallback

```promql
up{job="service-name"} or on() vector(0)
```

Returns `0` if metric doesn't exist (service not in scrape config).

#### Multiple Service Health

```promql
up{job=~"service1|service2|service3"}
```

Checks multiple services with regex match.

#### Health Percentage

```promql
avg(up{job=~".*-service.*"}) * 100
```

Calculates percentage of services that are up.

### Spring Boot Actuator Health Metrics

If your services expose Spring Boot Actuator health metrics:

```promql
# Overall health
spring_boot_health_indicator_health{status="UP"}

# Component health
spring_data_mongodb_repository_health_indicator_health{status="UP"}
spring_data_redis_repository_health_indicator_health{status="UP"}
```

**Note**: Metric names may vary. Check `/actuator/prometheus` endpoint for actual metric names.

### Custom Health Metrics

If you have custom health check metrics:

```promql
# Example custom health metric
service_health_check{component="mongodb"} == 1
```

### Alternative Health Indicators

Instead of `up`, you might use:

1. **HTTP endpoint health**: Monitor `/actuator/health` endpoint
2. **Custom health metric**: Application-specific health metric
3. **Dependency health**: Health of dependent services/components

---

## ⚙️ Panel Configuration (Advanced)

### Color Coding in Stat Panels

#### Background Color Mode

1. **Options** → **Color mode**: `Background`
2. **Thresholds**: Set color thresholds
3. Background color changes based on value

#### Value Color Mode

1. **Options** → **Color mode**: `Value`
2. Only text color changes, not background

#### Value Mappings

Map numeric values to text:

1. **Field** → **Value mappings**
2. **Add value mapping**:
   - **Type**: `Value`
   - **Value**: `1` → **Text**: `UP`
   - **Value**: `0` → **Text**: `DOWN`

### Table Panel Enhancements

#### Color-Coded Cells

1. **Field** → **Overrides**
2. **Fields with name**: Select field (e.g., `Status`)
3. **Add override**: **Thresholds** → Set color thresholds
4. **Add override**: **Custom**: **Cell display mode** → `Color background`

#### Column Formatting

1. **Transform** → **Organize fields**
2. Rename columns
3. Adjust column order
4. Set units per column

### Conditional Formatting

Format cells based on conditions:

1. **Field** → **Overrides**
2. **Fields with name**: Select field
3. **Add override**: **Custom**: **Cell display mode** → `Color text` or `Color background`
4. **Add override**: **Thresholds** → Set conditions

---

## 🏷️ Variables & Templating

### Environment Variable

Filter by environment:
```promql
up{job=~".*", environment="$environment"}
```

### Service Filter Variable

Filter services:
```promql
up{job=~"$service"}
```

Where `$service` variable queries:
```promql
label_values(up, job)
```

---

## 🔗 Links & Drilldowns

### Link to Service Dashboards

Link health panels to detailed service dashboards:

1. Panel options → **Links**
2. **Add link**
3. **Type**: `Dashboard`
4. **Dashboard**: Select service-specific dashboard
5. **Keep time**: `Yes`

Example: Config Control Service health panel → Golden Signals dashboard

### Link to Component Dashboards

Link infrastructure panels to detailed component dashboards:

1. MongoDB health panel → MongoDB dashboard
2. Redis health panel → Redis dashboard
3. Kafka health panel → Kafka dashboard

### Data Links to Logs

Link to logs for troubleshooting:

1. Panel options → **Data links**
2. **Add link**
3. **Title**: `View Logs`
4. **URL**: `/explore?orgId=1&left=["now-1h","now","Loki",{"expr":"{job=\"$job\"}"}]`

---

## ✅ Best Practices

### Health Check Strategy

1. **Monitor `up` metric** - Basic availability check
2. **Monitor health endpoints** - Application-level health
3. **Monitor dependencies** - Dependency connectivity
4. **Monitor custom metrics** - Business-specific health indicators

### Panel Organization

1. **Group by category** - Services, Infrastructure, Dependencies
2. **Use rows** - Organize related panels
3. **Consistent color coding** - Green=up, Red=down across all panels
4. **Clear naming** - Descriptive panel titles

### Alerting Integration

1. **Set thresholds** - Red/yellow/green based on criticality
2. **Link to alerts** - Connect health panels to alert rules
3. **Document runbooks** - Link panels to troubleshooting docs

### Performance

1. **Limit query complexity** - Simple `up` queries are fast
2. **Avoid high cardinality** - Don't query all instances individually if not needed
3. **Use tables** - Efficient for multiple components

---

## 🐛 Troubleshooting

### All panels show "No data"

**Check**:
1. Prometheus is scraping targets: Go to Prometheus UI → Status → Targets
2. Job names in queries match scrape config: Check `prometheus.yml`
3. Time range includes data: Check if services existed during time range

**Debug**:
```promql
# Check if any services are up
up

# Check specific job
up{job="config-control-service-local"}

# Check all jobs
label_values(up, job)
```

### Components show as down but are running

**Possible causes**:
1. Wrong job name in query
2. Service not in Prometheus scrape config
3. Network connectivity issue (Prometheus can't reach service)
4. Service not exposing `/actuator/prometheus` endpoint

**Solutions**:
1. Check Prometheus targets page
2. Verify scrape configuration
3. Test connectivity: `curl http://service:port/actuator/prometheus`
4. Check Prometheus logs for scrape errors

### Health score calculation is incorrect

**Check**:
1. Component list in query matches actual components
2. Division logic is correct (e.g., `/ 6` if 6 components)
3. All components have `up` metric

**Debug**:
```promql
# Check individual components
up{job="component1"}
up{job="component2"}

# Check calculation step by step
(up{job="component1"} == 1) + (up{job="component2"} == 1)
```

---

## 📚 References

### Related Dashboards
- [Platform Overview](01-platform-overview.md) - High-level platform view
- [Config Control Service - Golden Signals](02-config-control-service-golden-signals.md) - Service metrics

### External Documentation
- [Prometheus `up` Metric](https://prometheus.io/docs/concepts/jobs_instances/)
- [Spring Boot Actuator Health](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.health)
- [Grafana Stat Panels](https://grafana.com/docs/grafana/latest/panels-visualizations/visualizations/stat/)

### Metrics Reference
- Prometheus: `up` metric
- Spring Boot: `spring_boot_health_indicator_health`
- Component-specific health metrics

---

**Previous**: [Config Control Service - Golden Signals](02-config-control-service-golden-signals.md)  
**Next**: [Phase 2: Service Deep Dive](../phase2-service-deep-dive/README.md)

