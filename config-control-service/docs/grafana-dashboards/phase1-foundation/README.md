# Phase 1: Foundation Dashboards

Phase 1 provides essential dashboards for basic monitoring. These are the first dashboards you should create as they provide the most immediate value.

## 📊 Dashboards in This Phase

### 1. Platform Overview Dashboard
**Purpose**: High-level platform health and metrics overview

**Key Metrics**:
- Total services and instances
- Platform-wide error rate and latency
- Traffic overview by service
- Infrastructure health status
- Business metrics summary

**Use Cases**:
- Executive/on-call dashboards
- Quick health check
- Identifying platform-wide issues

[📖 Tutorial](01-platform-overview.md) | [📥 JSON](01-platform-overview.json)

---

### 2. Config Control Service - Golden Signals Dashboard
**Purpose**: Detailed service-level metrics following Golden Signals methodology

**Key Metrics**:
- Latency (p50/p95/p99)
- Traffic (request rate)
- Errors (error rate, status codes)
- Saturation (JVM heap, GC, threads)

**Use Cases**:
- Service performance monitoring
- Debugging performance issues
- Capacity planning

[📖 Tutorial](02-config-control-service-golden-signals.md) | [📥 JSON](02-config-control-service-golden-signals.json)

---

### 3. System Health Dashboard
**Purpose**: Overall system health indicators

**Key Metrics**:
- Service health status
- Infrastructure component health
- Dependency health (MongoDB, Redis, Kafka, Consul)
- Configuration health

**Use Cases**:
- System-wide health monitoring
- Dependency status checks
- Troubleshooting connectivity issues

[📖 Tutorial](03-system-health.md) | [📥 JSON](03-system-health.json)

---

## 🎯 Implementation Order

Recommended order for implementation:

1. **Platform Overview** - Start here for high-level visibility
2. **System Health** - Quick health status checks
3. **Golden Signals** - Deep dive into service metrics

## ⏱️ Estimated Time

- **Platform Overview**: 2-3 hours (including learning)
- **System Health**: 1-2 hours
- **Golden Signals**: 3-4 hours

**Total**: ~6-9 hours for all Phase 1 dashboards

## ✅ Prerequisites

Before starting Phase 1:

- [ ] Completed [Getting Started](../01-getting-started.md)
- [ ] Completed [Prerequisites](../02-prerequisites.md)
- [ ] Completed [PromQL Basics](../03-promql-basics.md)
- [ ] Prometheus is scraping metrics
- [ ] Grafana is configured with Prometheus data source
- [ ] Config Control Service is running and exposing metrics

## 📚 Learning Path

1. Read each dashboard tutorial
2. Try importing JSON first to see the result
3. Follow step-by-step tutorial to build from scratch
4. Experiment with queries in Grafana Explore
5. Customize panels for your needs

## 🔗 Navigation

After completing Phase 1, you'll have:
- Platform-wide visibility
- Service-level monitoring
- System health checks

**Next**: [Phase 2: Service Deep Dive](../phase2-service-deep-dive/README.md)

---

**Back**: [Main Documentation](../README.md)

