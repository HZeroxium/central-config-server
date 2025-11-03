Phân tích các metrics có sẵn và đề xuất cấu trúc dashboard từ cơ bản đến nâng cao.


[4 tools called]

Phân tích các metrics có sẵn và đề xuất cấu trúc dashboard từ cơ bản đến nâng cao, theo best practices.


[3 tools called]

## 🎯 Dashboard Architecture & Design Strategy

Cấu trúc dashboard theo Golden Signals, RED, và USE, tổ chức theo hierarchy: Overview → Service → Instance.

---

## 📊 Dashboard Hierarchy (Overview → Service → Instance)

### 🏗️ Level 1: Platform Overview Dashboard

**Mục đích:** Xem tổng quan health, traffic, và errors trên toàn platform

**Structure:**
- **Row 1 - KPIs (Stat Panels):**
  - Total Services (active count)
  - Total Instances (healthy/unhealthy)
  - Platform Error Rate (%)
  - Platform p95 Latency (s)
  - Total Heartbeat Rate (req/s)
  - Active Drift Events Count
- **Row 2 - Traffic Overview:**
  - Request Rate by Service (Time Series - stacked)
  - Request Rate by Endpoint Category: `/api/heartbeat`, `/api/services`, `/api/drift`, `/api/admin`
- **Row 3 - Error Overview:**
  - Error Rate by Service (%)
  - Error Rate by Status Code (4xx vs 5xx breakdown)
  - Top 10 Error Endpoints (Table)
- **Row 4 - Infrastructure Health:**
  - Service Health Status (Stat - color-coded: green/yellow/red)
  - Infrastructure Health (Redis, MongoDB, Kafka, Consul, Config Server)
- **Row 5 - Business Metrics Overview:**
  - Heartbeat Processing Rate (req/s)
  - Drift Detection Rate (events/min)
  - Approval Request Rate (requests/min)

**Variables:**
- `$environment` (dev/prod)
- `$timeRange` (1h/6h/24h/7d)

---

### 🎯 Level 2: Service Dashboard - Config Control Service

**Mục đích:** Chi tiết metrics cho config-control-service theo Golden Signals

**Structure:**

#### **Page 1: Golden Signals**

- **Row 1 - KPIs (Stat Panels):**
  - Error Rate % (threshold: 1% yellow, 5% red)
  - p95 Latency (threshold: 200ms yellow, 500ms red)
  - Request Rate (req/s)
  - Active Instances Count
- **Row 2 - Traffic (Time Series):**
  - Request Rate by Method (GET/POST/PUT/DELETE)
  - Request Rate by Endpoint Category (`/api/heartbeat`, `/api/services`, `/api/drift`, `/api/admin`)
  - Request Rate Top 10 Endpoints
- **Row 3 - Latency (Time Series):**
  - Response Time p50/p95/p99 (overlay)
  - Response Time by Endpoint Category
  - Response Time Heatmap (by endpoint)
- **Row 4 - Errors (Time Series + Table):**
  - Error Rate Over Time (%)
  - Error Rate by Status Code (4xx/5xx breakdown)
  - Top 10 Error Endpoints (Table với links to logs)
  - Error Rate by Exception Type (if instrumented)
- **Row 5 - Saturation (JVM):**
  - JVM Heap Usage % (with max line)
  - JVM Non-Heap Memory
  - GC Pause Time (Time Series)
  - GC Collection Count
  - Thread Pool Utilization
- **Row 6 - Request Details:**
  - Top 10 Endpoints by Request Count (Table)
  - Request Duration Distribution (Histogram)
  - Active Requests (Gauge)

**Variables:**
- `$service` = "config-control-service"
- `$instance` (multi-select)
- `$endpoint` (optional)

#### **Page 2: Business Operations**

- **Row 1 - Heartbeat Operations:**
  - Heartbeat Processing Rate (req/s)
  - Heartbeat Processing Latency (p50/p95/p99)
  - Heartbeat Success/Failure Rate
  - Active Heartbeat Connections
- **Row 2 - Drift Detection:**
  - Drift Events Created Rate (events/min)
  - Drift Events Resolved Rate (events/min)
  - Active Unresolved Drift Events Count
  - Drift Detection Latency (time to detect)
  - Drift Events by Severity (Critical/Warning/Info)
- **Row 3 - Service Management:**
  - Service Instance Operations (save rate)
  - Application Service Operations (save, transfer_ownership rate)
  - Service Share Operations (grant, revoke rate)
- **Row 4 - Approval Workflow:**
  - Approval Requests Created Rate
  - Approval Decisions (approve/reject rate)
  - Pending Approval Requests Count
  - Approval Processing Time
- **Row 5 - Cleanup Operations:**
  - Stale Instances Marked Rate
  - Stale Instances Deleted Rate
  - Cleanup Operation Duration

#### **Page 3: Infrastructure Dependencies**

- **Row 1 - MongoDB:**
  - MongoDB Connection Pool (active/available)
  - MongoDB Operations Rate (read/write)
  - MongoDB Operation Latency (p95)
  - MongoDB Error Rate
- **Row 2 - Redis Cache:**
  - Cache Hit Ratio (L1/L2 if two-level)
  - Cache Operations Rate (get/put/evict)
  - Cache Error Rate
  - Cache Size/Memory Usage
- **Row 3 - Kafka:**
  - Kafka Message Publish Rate
  - Kafka Consumer Lag (if applicable)
  - Kafka Operation Latency
  - Kafka Error Rate
- **Row 4 - Consul Service Discovery:**
  - Consul API Calls Rate
  - Consul Operation Latency
  - Consul Health Check Failures
- **Row 5 - Config Server Proxy:**
  - Config Server Request Rate
  - Config Server Response Time
  - Config Server Error Rate

#### **Page 4: Resilience & Circuit Breakers**

- **Row 1 - Circuit Breakers:**
  - Circuit Breaker State by Client (Open/Closed/Half-Open)
  - Circuit Breaker Failure Rate
  - Circuit Breaker Call Count
- **Row 2 - Retry Metrics:**
  - Retry Budget Utilization by Service
  - Retry Attempts Count
  - Retry Success Rate
- **Row 3 - Rate Limiting:**
  - Rate Limit Allowed/Rejected Count
  - Rate Limit Utilization
- **Row 4 - Bulkhead & Timeouts:**
  - Bulkhead Utilization
  - Timeout Failures Count
  - Async Task Failures

---

### ⚙️ Level 3: Config Server Dashboard

**Mục đích:** Monitor Config Server (Spring Cloud Config Server)

**Structure:**
- **Row 1 - KPIs:**
  - Config Server Health Status
  - Request Rate
  - Error Rate
  - Response Time p95
- **Row 2 - Config Operations:**
  - Configuration Fetch Rate (by application/profile)
  - Config Server Response Time
  - Config Refresh Operations
- **Row 3 - Git Backend:**
  - Git Operations Latency
  - Git Error Rate
  - Repository Sync Status
- **Row 4 - Health:**
  - JVM Metrics (heap, GC)
  - Connection Pool Metrics

---

### 🏢 Level 4: Infrastructure Dashboards

#### **4.1 Redis Dashboard (USE Method)**
- Utilization: Memory Usage %, Connection Pool Usage
- Saturation: Memory Pressure, Connection Pool Exhaustion
- Errors: Command Errors, Connection Errors, Timeouts

#### **4.2 MongoDB Dashboard (USE Method)**
- Utilization: Connection Pool Usage, CPU Usage
- Saturation: Queue Length, Lock Wait Time
- Errors: Operation Errors, Connection Errors, Replication Lag

#### **4.3 Kafka Dashboard (USE Method)**
- Utilization: Broker CPU/Memory, Disk Usage
- Saturation: Consumer Lag, Queue Size
- Errors: Producer/Consumer Errors, Topic Errors

#### **4.4 Consul Dashboard**
- Service Registry Health
- Service Instance Count by Status
- Health Check Success/Failure Rate
- Consul API Performance

---

### 📈 Level 5: Business Intelligence Dashboard

**Mục đích:** Business metrics cho operations teams

**Structure:**
- **Row 1 - Service Discovery:**
  - Total Registered Services Count
  - Total Active Instances Count
  - Services by Team (pie chart)
  - Orphan Services Count
- **Row 2 - Configuration Health:**
  - Services with Active Drift (count)
  - Drift Resolution Rate (%)
  - Average Time to Resolve Drift
  - Drift Events by Service (top 10)
- **Row 3 - Approval Workflow:**
  - Pending Approvals Count
  - Approval Processing Time (avg/p95)
  - Approval Success Rate
  - Approvals by Team
- **Row 4 - Service Ownership:**
  - Ownership Transfers Rate
  - Services Claimed Rate
  - Services by Owner Team
- **Row 5 - Cache Performance:**
  - Overall Cache Hit Ratio
  - Cache Hit Ratio by Cache Name
  - Cache Eviction Rate

---

### 🔍 Level 6: Instance-Level Dashboard

**Mục đích:** Deep dive vào một service instance cụ thể

**Structure:**
- **Row 1 - Instance Health:**
  - Instance Status (healthy/unhealthy/stale)
  - Last Heartbeat Timestamp
  - Config Hash Match Status
  - Instance Metadata (version, environment, zone)
- **Row 2 - Instance Metrics:**
  - Request Rate for This Instance
  - Error Rate for This Instance
  - Response Time for This Instance
  - JVM Metrics (heap, GC, threads)
- **Row 3 - Instance Dependencies:**
  - MongoDB Operations from This Instance
  - Redis Operations from This Instance
  - Kafka Operations from This Instance
- **Row 4 - Drift History:**
  - Drift Events Timeline for This Instance
  - Config Hash Changes Timeline
  - Auto-Refresh Trigger Events

---

## 📋 Dashboard Organization Strategy

### **Folder Structure (Grafana):**
```
/0-Overview
  ├── Platform Overview Dashboard
  └── System Health Dashboard

/1-Services
  ├── Config Control Service Dashboard
  ├── Config Server Dashboard
  └── Service Template (for future services)

/2-Infrastructure
  ├── Redis Dashboard
  ├── MongoDB Dashboard
  ├── Kafka Dashboard
  └── Consul Dashboard

/3-Business
  ├── Service Discovery Dashboard
  ├── Configuration Health Dashboard
  ├── Approval Workflow Dashboard
  └── Service Management Dashboard

/4-Detailed
  ├── Instance Dashboard (template)
  └── Endpoint Drilldown Dashboard
```

---

## 🎨 Visualization Types by Use Case

| **Metric Type** | **Visualization** | **Purpose** |
|----------------|-------------------|-------------|
| **RPS/Traffic** | Time Series (line) | Trend analysis, traffic patterns |
| **Latency** | Time Series (multiple lines for p50/p95/p99) | Performance monitoring |
| **Error Rate** | Stat (with thresholds) | Quick health check |
| **Error Details** | Table (top N) | Drill-down analysis |
| **Health Status** | Stat (color-coded) | At-a-glance status |
| **Distribution** | Histogram/Heatmap | Understanding patterns |
| **Counts** | Stat or Time Series | Business metrics |
| **Percentages** | Stat (with gauges) | Utilization, hit ratios |

---

## 🔗 Navigation & Drilldowns

### **Dashboard Links:**
- Overview → Service Dashboard (preserve service variable)
- Service Dashboard → Instance Dashboard (preserve instance variable)
- Service Dashboard → Infrastructure Dashboard (for dependency analysis)
- Any Dashboard → Explore (for ad-hoc queries)

### **Data Links:**
- Error Table → Loki logs (same service/instance/timeframe)
- Latency Panel → Trace exemplars (if Tempo configured)
- Drift Events → Detail page (URL with event ID)
- Service Instance → Admin Dashboard (deep link to instance management)

---

## 🏷️ Label Strategy & Variables

### **Standard Labels:**
- `service`: Service name (config-control-service, config-server)
- `instance`: Instance ID
- `location`: local/remote
- `environment`: dev/prod
- `team`: Owner team ID (if available)
- `method`: HTTP method
- `status`: HTTP status code
- `uri`: Normalized URI (bounded cardinality)

### **Variables Template:**
```yaml
Variables:
  - $environment: label_values(environment)
  - $service: label_values(http_server_requests_seconds_count, service)
  - $instance: label_values(http_server_requests_seconds_count{service="$service"}, instance)
  - $endpoint: label_values(http_server_requests_seconds_count{service="$service"}, uri) (optional)
```

---

## 🚨 Alerting Integration Points

Dashboards có thể link đến alert rules:
1. **Golden Signals Alerts:**
   - Error Rate > 2% for 10m
   - p95 Latency > 500ms for 10m
   - Heap Usage > 80% for 5m
2. **Business Alerts:**
   - Drift Events > 10 unresolved for 1h
   - Heartbeat Rate drops > 50% for 5m
   - Approval Requests pending > 10 for 1h
3. **Infrastructure Alerts:**
   - Redis Cache Hit Ratio < 70%
   - MongoDB Connection Pool > 80%
   - Kafka Consumer Lag > 1000

---

## 📝 Implementation Priority

### **Phase 1 - Foundation (Week 1):**
1. Platform Overview Dashboard
2. Config Control Service - Golden Signals (Page 1)
3. System Health Dashboard

### **Phase 2 - Service Deep Dive (Week 2):**
1. Config Control Service - Business Operations (Page 2)
2. Config Control Service - Infrastructure Dependencies (Page 3)
3. Config Server Dashboard

### **Phase 3 - Infrastructure (Week 3):**
1. Redis Dashboard
2. MongoDB Dashboard
3. Kafka Dashboard
4. Consul Dashboard

### **Phase 4 - Business Intelligence (Week 4):**
1. Business Intelligence Dashboard
2. Instance-Level Dashboard
3. Approval Workflow Dashboard

### **Phase 5 - Advanced (Ongoing):**
1. SLO Dashboards (with error budgets)
2. Cost Optimization Dashboards
3. Capacity Planning Dashboards

---

## 💡 Best Practices Applied

1. **Golden Signals:** Latency, Traffic, Errors, Saturation
2. **RED Method:** Rate, Errors, Duration cho HTTP services
3. **USE Method:** Utilization, Saturation, Errors cho infrastructure
4. **Hierarchical Navigation:** Overview → Service → Instance
5. **Templating:** Reusable dashboards với variables
6. **Library Panels:** Common KPIs (p95, Error Rate, RPS)
7. **Provisioning:** Dashboards as code (JSON files in Git)

---