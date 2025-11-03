# Phase 3: Infrastructure Dashboards

Phase 3 provides detailed infrastructure component monitoring using the USE method (Utilization, Saturation, Errors). These dashboards monitor Redis, MongoDB, Kafka, and Consul at the infrastructure level.

## 📊 Dashboards in This Phase

### 1. Redis Dashboard (USE Method)
**Purpose**: Monitor Redis performance using USE method (Utilization, Saturation, Errors)

**Key Metrics**:
- Utilization: Memory usage, connection pool usage
- Saturation: Memory pressure, connection pool exhaustion
- Errors: Command errors, connection errors, timeouts

[📖 Tutorial](01-redis.md)

---

### 2. MongoDB Dashboard (USE Method)
**Purpose**: Monitor MongoDB performance using USE method

**Key Metrics**:
- Utilization: Connection pool usage, CPU usage
- Saturation: Queue length, lock wait time
- Errors: Operation errors, connection errors, replication lag

[📖 Tutorial](02-mongodb.md)

---

### 3. Kafka Dashboard (USE Method)
**Purpose**: Monitor Kafka broker performance using USE method

**Key Metrics**:
- Utilization: Broker CPU/Memory, disk usage
- Saturation: Consumer lag, queue size
- Errors: Producer/consumer errors, topic errors

[📖 Tutorial](03-kafka.md)

---

### 4. Consul Dashboard
**Purpose**: Monitor Consul service discovery and key-value store

**Key Metrics**:
- Service registry health
- Service instance count by status
- Health check success/failure rate
- Consul API performance

[📖 Tutorial](04-consul.md)

---

## 🎯 USE Method Overview

The USE method monitors:
- **Utilization**: How much of the resource is being used (percentage)
- **Saturation**: How full the queue is (wait time, queue length)
- **Errors**: How many errors occurred (error count, error rate)

This method complements the RED method (Rate, Errors, Duration) used for services.

## ⏱️ Estimated Time

- **Redis**: 2 hours
- **MongoDB**: 2 hours
- **Kafka**: 2-3 hours
- **Consul**: 1-2 hours

**Total**: ~7-9 hours for all Phase 3 dashboards

## ✅ Prerequisites

Before starting Phase 3:

- [ ] Infrastructure components are instrumented
- [ ] Prometheus is scraping infrastructure metrics
- [ ] Understanding of USE method
- [ ] Component exporters installed (if needed)

## 📚 Learning Path

1. Learn USE method principles
2. Build each infrastructure dashboard
3. Understand utilization vs saturation
4. Set up alerting thresholds

**Next**: [Phase 4: Business Intelligence](../phase4-business-intelligence/README.md)

---

**Back**: [Phase 2: Service Deep Dive](../phase2-service-deep-dive/README.md) | [Main Documentation](../../README.md)

