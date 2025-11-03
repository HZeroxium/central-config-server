# Getting Started with Grafana and Prometheus

This guide introduces you to Grafana and Prometheus, the core technologies used for observability in the Config Control Service.

## 🎯 What is This Guide?

This guide is designed for developers who are new to Grafana and Prometheus. By the end of this guide, you will understand:

- What Grafana and Prometheus are
- How they work together
- Basic concepts you need to know
- How to navigate Grafana UI

## 📊 What is Prometheus?

**Prometheus** is a time-series database and monitoring system that collects and stores metrics.

### Key Concepts

1. **Metrics**: Numerical measurements of your application (request count, latency, memory usage)
2. **Time Series**: A sequence of data points collected over time
3. **Scraping**: Prometheus periodically pulls metrics from your application
4. **Storage**: Metrics are stored as time-series data

### How It Works

```
Application → Exposes /metrics endpoint → Prometheus scrapes → Stores in database
```

Example: Your Config Control Service exposes metrics at `http://localhost:8080/actuator/prometheus`. Prometheus scrapes this endpoint every 15 seconds and stores the data.

### Example Metrics

```
# HELP http_server_requests_seconds Duration of HTTP server request handling
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",status="200",uri="/api/heartbeat"} 1523
http_server_requests_seconds_sum{method="GET",status="200",uri="/api/heartbeat"} 234.5

# HELP jvm_memory_used_bytes Used JVM memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap"} 512000000
```

## 📈 What is Grafana?

**Grafana** is a visualization and analytics platform that creates dashboards from your data.

### Key Concepts

1. **Dashboards**: Collections of panels (charts, graphs, tables)
2. **Panels**: Individual visualizations (time series graph, stat, table)
3. **Data Sources**: Connections to your data (Prometheus, Loki, Tempo)
4. **Queries**: PromQL queries to fetch data from Prometheus

### How It Works

```
Grafana → Queries Prometheus → Processes data → Displays in panels → Updates in real-time
```

Example: You create a dashboard that shows request rate over time. Grafana queries Prometheus, processes the data, and displays it in a time series graph.

## 🔄 How They Work Together

```
Application (Metrics) 
    ↓
Prometheus (Scrapes & Stores)
    ↓
Grafana (Queries & Visualizes)
    ↓
Dashboard (Your View)
```

### The Flow

1. **Collect**: Application exposes metrics at `/actuator/prometheus`
2. **Scrape**: Prometheus scrapes metrics every 15 seconds (configurable)
3. **Store**: Prometheus stores metrics in its time-series database
4. **Query**: Grafana sends PromQL queries to Prometheus
5. **Visualize**: Grafana displays data in panels on dashboards

## 🎓 Key Terms You'll See

### Metrics Types

- **Counter**: Only increases (total requests, total errors)
  - Example: `http_server_requests_seconds_count`
  
- **Gauge**: Can go up or down (memory usage, active connections)
  - Example: `jvm_memory_used_bytes`

- **Histogram**: Measures distribution (latency percentiles)
  - Example: `http_server_requests_seconds_bucket`

- **Summary**: Like histogram but client-side calculated
  - Example: `heartbeat.process` (timer)

### PromQL Basics

PromQL (Prometheus Query Language) is used to query metrics.

**Simple queries:**
```
http_server_requests_seconds_count                    # Get all request counts
http_server_requests_seconds_count{method="GET"}     # Filter by method=GET
```

**Rate calculation:**
```
rate(http_server_requests_seconds_count[5m])         # Requests per second over 5 minutes
```

**Aggregation:**
```
sum(rate(http_server_requests_seconds_count[5m]))    # Total requests across all instances
```

## 🖥️ Grafana UI Overview

When you open Grafana, you'll see:

### Main Navigation

1. **Dashboards** - View and manage dashboards
2. **Explore** - Ad-hoc query interface (test queries here)
3. **Alerting** - Configure alerts
4. **Configuration** - Data sources, users, organizations

### Dashboard View

- **Variables dropdown** - Filter dashboards (service, instance, environment)
- **Time picker** - Select time range (Last 1 hour, Last 24 hours)
- **Refresh button** - Manually refresh data
- **Panels** - Individual charts and graphs

### Panel Editor

When editing a panel:
- **Query tab** - Write PromQL queries
- **Visualization tab** - Choose chart type
- **Panel options** - Title, description, thresholds

## 📋 What You Need Before Starting

Before creating dashboards, ensure:

1. ✅ **Prometheus is running** - Check `http://localhost:9090` (or your Prometheus URL)
2. ✅ **Grafana is running** - Check `http://localhost:3000` (or your Grafana URL)
3. ✅ **Application is running** - Config Control Service exposing metrics
4. ✅ **Data source configured** - Grafana connected to Prometheus (see [Prerequisites](02-prerequisites.md))

## 🚀 Your First Dashboard (Concept)

Here's what you'll do when creating a dashboard:

1. **Create Dashboard** - Click "New Dashboard" in Grafana
2. **Add Panel** - Click "Add panel"
3. **Write Query** - Enter PromQL query in Query tab
4. **Choose Visualization** - Select chart type (Time series, Stat, Table)
5. **Configure Panel** - Set title, units, thresholds
6. **Save** - Click "Apply" and "Save dashboard"

Example panel:
- **Query**: `rate(http_server_requests_seconds_count[5m])`
- **Visualization**: Time series
- **Title**: Request Rate
- **Unit**: req/s

## 🎯 What's Next?

Now that you understand the basics:

1. Read **[Prerequisites](02-prerequisites.md)** to set up your environment
2. Learn **[PromQL Basics](03-promql-basics.md)** to write queries
3. Start with **[Platform Overview Dashboard](phase1-foundation/01-platform-overview.md)** for your first real dashboard

## 💡 Common Questions

**Q: Do I need to know PromQL to use Grafana?**  
A: Not necessarily - you can import pre-made dashboards. But learning PromQL helps you customize and debug.

**Q: How often should I check dashboards?**  
A: Dashboards update automatically (default every 30 seconds). During incidents, refresh more frequently.

**Q: Can I edit dashboards?**  
A: Yes! Click the "Edit" button (pencil icon) to modify panels, queries, and settings.

**Q: What if I see "No data" in a panel?**  
A: Check: (1) Metrics are being scraped, (2) Query syntax is correct, (3) Time range includes data.

## 📚 Additional Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [PromQL Tutorial](https://prometheus.io/docs/prometheus/latest/querying/basics/)

---

**Next**: [Prerequisites](02-prerequisites.md) - Set up your environment

