# Phase 2: Service Deep Dive Dashboards

Phase 2 provides detailed service and infrastructure dependency monitoring. These dashboards offer deeper insights into business operations, infrastructure dependencies, and Config Server health.

## 📊 Dashboards in This Phase

### 1. Config Control Service - Business Operations Dashboard
**Purpose**: Monitor business-specific operations (Heartbeat, Drift Detection, Service Management, Approval Workflow, Cleanup)

**Key Metrics**:
- Heartbeat processing rate and latency
- Drift detection and resolution metrics
- Service management operations
- Approval workflow statistics
- Cleanup operation metrics

**Use Cases**:
- Business operations monitoring
- Workflow performance tracking
- Operational issue debugging

[📖 Tutorial](01-config-control-business-operations.md)

---

### 2. Config Control Service - Infrastructure Dependencies Dashboard
**Purpose**: Monitor dependencies (MongoDB, Redis, Kafka, Consul, Config Server)

**Key Metrics**:
- MongoDB connection pool and operation metrics
- Redis cache performance and hit ratios
- Kafka message publishing and consumption
- Consul service discovery operations
- Config Server proxy metrics

**Use Cases**:
- Dependency health monitoring
- Performance bottleneck identification
- Capacity planning for dependencies

[📖 Tutorial](02-config-control-infrastructure-dependencies.md)

---

### 3. Config Server Dashboard
**Purpose**: Monitor Config Server (Spring Cloud Config Server) health and operations

**Key Metrics**:
- Config Server health status
- Configuration fetch operations
- Git backend operations
- Response times and error rates

**Use Cases**:
- Config Server performance monitoring
- Configuration delivery tracking
- Git repository sync status

[📖 Tutorial](03-config-server.md)

---

## 🎯 Implementation Order

Recommended order for implementation:

1. **Business Operations** - Understanding business metrics
2. **Infrastructure Dependencies** - Dependency health and performance
3. **Config Server** - Configuration server monitoring

## ⏱️ Estimated Time

- **Business Operations**: 3-4 hours
- **Infrastructure Dependencies**: 2-3 hours
- **Config Server**: 2 hours

**Total**: ~7-9 hours for all Phase 2 dashboards

## ✅ Prerequisites

Before starting Phase 2:

- [ ] Completed Phase 1 dashboards
- [ ] Understanding of business metrics (heartbeat, drift, approvals)
- [ ] Access to infrastructure component metrics (MongoDB, Redis, Kafka, Consul)
- [ ] Config Server exposing metrics

## 📚 Learning Path

1. Read each dashboard tutorial
2. Build dashboards step-by-step
3. Understand business metric patterns
4. Learn dependency monitoring patterns

## 🔗 Navigation

After completing Phase 2, you'll have:
- Business operations visibility
- Dependency health monitoring
- Config Server insights

**Next**: [Phase 3: Infrastructure Dashboards](../phase3-infrastructure/README.md)

---

**Back**: [Phase 1: Foundation](../phase1-foundation/README.md) | [Main Documentation](../../README.md)

