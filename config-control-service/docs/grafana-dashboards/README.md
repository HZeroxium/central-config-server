# Grafana Dashboards Documentation

Comprehensive guide to creating and managing Grafana dashboards for the Config Control Service observability stack.

## 📚 Documentation Structure

This documentation is organized into phases, following the implementation priority:

### Foundation (Start Here)
- **[Getting Started](01-getting-started.md)** - Introduction to Grafana and Prometheus
- **[Prerequisites](02-prerequisites.md)** - Setup requirements and data source configuration
- **[PromQL Basics](03-promql-basics.md)** - PromQL fundamentals for beginners

### Phase 1: Foundation Dashboards (Priority 1)
Essential dashboards for basic monitoring:

1. **[Platform Overview](phase1-foundation/01-platform-overview.md)** - High-level platform health and metrics
2. **[Config Control Service - Golden Signals](phase1-foundation/02-config-control-service-golden-signals.md)** - Service-level metrics (Latency, Traffic, Errors, Saturation)
3. **[System Health](phase1-foundation/03-system-health.md)** - Overall system health indicators

### Phase 2: Service Deep Dive
Detailed service and infrastructure dependency monitoring:

1. **[Config Control Service - Business Operations](phase2-service-deep-dive/01-config-control-business-operations.md)** - Heartbeat, Drift Detection, Service Management
2. **[Config Control Service - Infrastructure Dependencies](phase2-service-deep-dive/02-config-control-infrastructure-dependencies.md)** - MongoDB, Redis, Kafka, Consul
3. **[Config Server Dashboard](phase2-service-deep-dive/03-config-server.md)** - Config Server monitoring

### Phase 3: Infrastructure Dashboards
Infrastructure monitoring using USE method:

1. **[Redis Dashboard](phase3-infrastructure/01-redis.md)** - Redis metrics (Utilization, Saturation, Errors)
2. **[MongoDB Dashboard](phase3-infrastructure/02-mongodb.md)** - MongoDB metrics
3. **[Kafka Dashboard](phase3-infrastructure/03-kafka.md)** - Kafka metrics
4. **[Consul Dashboard](phase3-infrastructure/04-consul.md)** - Consul service discovery metrics

### Phase 4: Business Intelligence
Business and operations dashboards:

1. **[Business Intelligence Dashboard](phase4-business-intelligence/01-business-intelligence.md)** - Service discovery, ownership, cache performance
2. **[Instance-Level Dashboard](phase4-business-intelligence/02-instance-level.md)** - Deep dive into specific service instances
3. **[Approval Workflow Dashboard](phase4-business-intelligence/03-approval-workflow.md)** - Approval workflow metrics

### Phase 5: Advanced Dashboards
Advanced monitoring and optimization:

1. **[SLO Dashboards](phase5-advanced/01-slo-dashboards.md)** - Service Level Objectives with error budgets
2. **[Cost Optimization](phase5-advanced/02-cost-optimization.md)** - Cost analysis and optimization metrics
3. **[Capacity Planning](phase5-advanced/03-capacity-planning.md)** - Capacity and resource planning

## 🚀 Quick Start

### Option 1: Import JSON Dashboards (Fast Track)

1. Navigate to the dashboard folder for your phase (e.g., `phase1-foundation/`)
2. Find the JSON file (e.g., `01-platform-overview.json`)
3. In Grafana: Dashboards → Import → Upload JSON file
4. Verify the dashboard works with your Prometheus data source

### Option 2: Step-by-Step Tutorial (Learning Path)

1. Read **[Getting Started](01-getting-started.md)** to understand Grafana basics
2. Complete **[Prerequisites](02-prerequisites.md)** to ensure your setup is ready
3. Learn **[PromQL Basics](03-promql-basics.md)** to understand query language
4. Follow the tutorial in each dashboard document to build it step-by-step

## 📖 How to Use This Documentation

Each dashboard document follows a consistent structure:

1. **Overview** - Purpose, use cases, target audience
2. **Quick Start** - Import JSON file quickly
3. **Step-by-Step Tutorial (Basic)** - Build from scratch, learn each step
4. **PromQL Deep Dive (Advanced)** - Understand queries in detail
5. **Panel Configuration (Advanced)** - Advanced settings and customizations
6. **Variables & Templating** - Make dashboards reusable
7. **Links & Drilldowns** - Connect dashboards together
8. **Best Practices** - Optimization and performance tips
9. **Troubleshooting** - Common issues and solutions
10. **References** - Related resources

## 🎯 Dashboard Organization in Grafana

Recommended folder structure in Grafana:

```
/0-Overview
  ├── Platform Overview Dashboard
  └── System Health Dashboard

/1-Services
  ├── Config Control Service Dashboard
  └── Config Server Dashboard

/2-Infrastructure
  ├── Redis Dashboard
  ├── MongoDB Dashboard
  ├── Kafka Dashboard
  └── Consul Dashboard

/3-Business
  ├── Business Intelligence Dashboard
  ├── Instance-Level Dashboard
  └── Approval Workflow Dashboard
```

## 📊 Key Metrics Reference

### Standard Spring Boot Metrics
- `http_server_requests_seconds_count` - Total HTTP requests
- `http_server_requests_seconds_sum` - Total request duration
- `http_server_requests_seconds_bucket` - Histogram buckets for percentiles
- `jvm_memory_used_bytes` - JVM memory usage
- `jvm_gc_pause_seconds` - GC pause times

### Custom Config Control Service Metrics
- `heartbeat.process` - Heartbeat processing timer
- `config_control.thrift.heartbeat` - Thrift heartbeat counter
- `config_control.cleanup.stale_instances_marked` - Stale instances counter
- `config_control.cleanup.stale_instances_deleted` - Deleted instances counter
- `config_control.application_service.save` - Application service operations
- `config_control.approval.*` - Approval workflow metrics
- `config_control.drift_event.*` - Drift detection metrics
- `config_control.service_instance.save` - Service instance operations
- `config_control.service_share.*` - Service sharing metrics

### Cache Metrics
- `cache.gets` - Cache get operations
- `cache.puts` - Cache put operations
- `cache.evictions` - Cache evictions
- `cache.hit.ratio` - Cache hit ratio

## 🔗 Related Documentation

- [Prometheus & Grafana Guide](../prometheus-grafana.md) - Comprehensive guide to Prometheus and Grafana
- [Grafana Guidelines](../grafana-guideline.md) - Dashboard architecture and design strategy
- [Observability Configuration](../../src/main/resources/application-observability.yml) - Spring Boot observability settings

## 🛠️ Prerequisites

Before creating dashboards, ensure:

1. ✅ Prometheus is running and scraping metrics
2. ✅ Grafana is running and connected to Prometheus
3. ✅ Config Control Service is exposing metrics at `/actuator/prometheus`
4. ✅ Data source is configured in Grafana (see [Prerequisites](02-prerequisites.md))

## 📝 Best Practices

1. **Start with Phase 1** - Foundation dashboards provide the most value
2. **Use Variables** - Make dashboards reusable across services/instances
3. **Follow Golden Signals** - Monitor Latency, Traffic, Errors, Saturation
4. **Set Meaningful Thresholds** - Use color coding for quick health checks
5. **Document Your Queries** - Add descriptions to panels for team understanding
6. **Link Dashboards** - Create navigation paths for easy drill-down
7. **Optimize Queries** - Use recording rules for expensive calculations
8. **Version Control** - Export and commit JSON files to Git

## 🆘 Getting Help

If you encounter issues:

1. Check the **Troubleshooting** section in each dashboard document
2. Verify your metrics are available in Prometheus
3. Test queries in Grafana Explore before adding to panels
4. Review the [PromQL Basics](03-promql-basics.md) guide for query syntax

## 📅 Implementation Timeline

- **Week 1**: Complete Phase 1 dashboards (Platform Overview, Golden Signals, System Health)
- **Week 2**: Complete Phase 2 dashboards (Business Operations, Infrastructure Dependencies, Config Server)
- **Week 3**: Complete Phase 3 dashboards (Infrastructure monitoring)
- **Week 4**: Complete Phase 4 dashboards (Business Intelligence)
- **Ongoing**: Phase 5 advanced dashboards as needed

## 🎓 Learning Path

For beginners, recommended reading order:

1. Read `01-getting-started.md`
2. Complete `02-prerequisites.md` setup
3. Study `03-promql-basics.md`
4. Follow `phase1-foundation/01-platform-overview.md` tutorial
5. Build remaining Phase 1 dashboards
6. Progress through remaining phases as needed

---

**Last Updated**: 2024
**Maintained by**: Config Control Service Team

