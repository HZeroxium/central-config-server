# Service Splitting Strategy
## Gradual Microservices Extraction

**Strategy:** Extract services gradually based on business needs, maintaining backward compatibility

---

## Current Architecture

### Monolithic Service: `config-control-service`

**Responsibilities:**
- Heartbeat processing and drift detection
- Service instance management
- Application service management
- Notification handling (email, events)
- Analytics computation
- API endpoints (REST, Thrift, gRPC)
- Access control and authorization
- Approval workflows

**Current Scale:**
- ~50,000 lines of code
- 10+ domain aggregates
- 20+ application services
- 30+ REST endpoints

**Challenges:**
- Tight coupling between components
- Difficult to scale independently
- Technology constraints (Java for all)
- Deployment bottlenecks

---

## Target Architecture

### Microservices Vision

```
┌─────────────────────────────────────────────────────────┐
│                    API Gateway                          │
│              (Spring Cloud Gateway)                     │
└─────────────────────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
┌───────▼──────┐ ┌─────▼──────┐ ┌──────▼──────┐
│   Core       │ │   Drift    │ │ Notification│
│   Service    │ │ Detection  │ │   Service   │
│              │ │  Service   │ │             │
└───────┬──────┘ └─────┬──────┘ └──────┬──────┘
        │               │               │
        └───────────────┼───────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
┌───────▼──────┐ ┌─────▼──────┐ ┌──────▼──────┐
│  Analytics   │ │   Event    │ │   Config    │
│   Service    │ │   Bus      │ │   Service   │
│              │ │  (Kafka)   │ │             │
└──────────────┘ └────────────┘ └─────────────┘
```

---

## Phase 1: Extract Drift Detection Service (Months 6-9)

### Rationale

**Why Extract:**
- High computational load (ML models in future)
- Independent scaling needs (CPU-intensive)
- Technology flexibility (Python for ML)
- Clear service boundaries

**Benefits:**
- Scale drift detection independently
- Use Python for ML/AI capabilities
- Reduce load on core service
- Better fault isolation

### Service Boundaries

**Drift Detection Service Responsibilities:**
- Receive heartbeat events (via Kafka)
- Calculate config hashes
- Compare with expected hashes
- Detect drift anomalies
- Create drift events
- Publish drift events to Kafka

**Core Service Responsibilities:**
- Service instance management
- Application service management
- API endpoints
- Access control

### Communication Pattern

**Event-Driven:**
```
Heartbeat Event (Kafka) → Drift Detection Service
                                    │
                                    ▼
                          Drift Event (Kafka) → Core Service
```

**Implementation:**
```java
// Core Service publishes heartbeat events
@KafkaListener(topics = "heartbeat-queue")
public void processHeartbeat(HeartbeatPayload payload) {
    // Publish to drift detection topic
    kafkaTemplate.send("drift-detection-queue", payload);
}

// Drift Detection Service consumes and processes
@KafkaListener(topics = "drift-detection-queue")
public void detectDrift(HeartbeatPayload payload) {
    DriftEvent event = driftDetector.detect(payload);
    if (event != null) {
        kafkaTemplate.send("drift-events", event);
    }
}

// Core Service consumes drift events
@KafkaListener(topics = "drift-events")
public void handleDriftEvent(DriftEvent event) {
    driftEventService.save(event);
    // Trigger notifications, etc.
}
```

### Technology Stack

**Drift Detection Service:**
- **Language:** Python 3.11+
- **Framework:** FastAPI
- **ML Libraries:** Scikit-learn, TensorFlow
- **Database:** MongoDB (read-only access)
- **Messaging:** Kafka (consumer/producer)

**Core Service:**
- **Language:** Java 21
- **Framework:** Spring Boot 3.3
- **Database:** MongoDB (write access)
- **Messaging:** Kafka (producer)

### Data Model

**Shared Collections:**
- `ServiceInstance` (read by drift service, write by core)
- `ApplicationService` (read by drift service)
- `DriftEvent` (write by drift service, read by core)

**Data Access Pattern:**
- **Drift Service:** Read-only access via MongoDB change streams
- **Core Service:** Full CRUD access

### Migration Strategy

**Step 1: Deploy Drift Service (Parallel)**
- Deploy drift detection service
- Process events in parallel with core service
- Compare results (validation)

**Step 2: Switch Traffic (Gradual)**
- Route 10% of events to drift service
- Monitor performance and accuracy
- Gradually increase to 100%

**Step 3: Remove Old Code**
- Remove drift detection from core service
- Update dependencies
- Clean up code

### Success Metrics

- **Processing Latency:** <100ms (p95)
- **Accuracy:** 100% match with old implementation
- **Throughput:** 10,000+ events/minute
- **Availability:** 99.9% uptime

---

## Phase 2: Extract Notification Service (Months 9-12)

### Rationale

**Why Extract:**
- Different reliability requirements
- Multiple notification channels (email, Slack, PagerDuty, SMS)
- Rate limiting and retry logic
- Independent scaling needs

**Benefits:**
- Better reliability isolation
- Easier to add new channels
- Independent scaling
- Technology flexibility

### Service Boundaries

**Notification Service Responsibilities:**
- Receive notification events (via Kafka)
- Route to appropriate channels
- Handle retries and rate limiting
- Track delivery status
- Manage notification templates

**Core Service Responsibilities:**
- Business logic
- Event generation
- Publish notification events

### Communication Pattern

**Event-Driven:**
```
Business Event → Core Service → Notification Event (Kafka) → Notification Service
```

**Implementation:**
```java
// Core Service publishes notification events
public void handleDriftEvent(DriftEvent event) {
    // Save event
    driftEventService.save(event);
    
    // Publish notification event
    NotificationEvent notification = NotificationEvent.builder()
        .type(NotificationType.DRIFT_DETECTED)
        .recipients(getRecipients(event))
        .data(event)
        .build();
    
    kafkaTemplate.send("notification-queue", notification);
}

// Notification Service consumes and sends
@KafkaListener(topics = "notification-queue")
public void sendNotification(NotificationEvent event) {
    for (NotificationChannel channel : event.getChannels()) {
        notificationSender.send(channel, event);
    }
}
```

### Technology Stack

**Notification Service:**
- **Language:** Java 21 (or Node.js for flexibility)
- **Framework:** Spring Boot 3.3
- **Channels:**
  - Email: JavaMail / SendGrid
  - Slack: Slack API
  - PagerDuty: PagerDuty API
  - SMS: Twilio
- **Queue:** Kafka + Redis (for rate limiting)

### Notification Channels

**Email:**
- SMTP integration
- Template engine (Thymeleaf, Handlebars)
- HTML + plain text
- Attachments support

**Slack:**
- Webhook integration
- Rich message formatting
- Thread replies
- Channel routing

**PagerDuty:**
- Incident creation
- Escalation policies
- On-call routing

**SMS:**
- Twilio integration
- Short message format
- Delivery receipts

### Rate Limiting & Retry

**Rate Limiting:**
- Per-channel limits (e.g., 100 emails/hour)
- Per-recipient limits
- Redis-based token bucket

**Retry Strategy:**
- Exponential backoff
- Max 3 retries
- Dead letter queue for failures

### Migration Strategy

**Step 1: Deploy Notification Service**
- Deploy service with all channels
- Test with staging events

**Step 2: Gradual Migration**
- Route 10% of notifications to new service
- Monitor delivery rates
- Gradually increase to 100%

**Step 3: Remove Old Code**
- Remove notification code from core service
- Update event publishing
- Clean up dependencies

### Success Metrics

- **Delivery Rate:** >99%
- **Delivery Latency:** <5s (p95)
- **Channel Availability:** 99.9%
- **Retry Success Rate:** >90%

---

## Phase 3: Extract Analytics Service (Months 12-15)

### Rationale

**Why Extract:**
- Heavy computational workloads
- Different data access patterns (read-heavy)
- Real-time vs batch processing
- Separate data models (CQRS)

**Benefits:**
- Optimized for analytics workloads
- Independent data models
- Better performance
- Technology flexibility (e.g., ClickHouse, TimescaleDB)

### Service Boundaries

**Analytics Service Responsibilities:**
- Consume domain events (via Kafka)
- Build read models (CQRS)
- Compute analytics (aggregations, trends)
- Serve analytics queries
- Generate reports

**Core Service Responsibilities:**
- Write domain events
- Business logic
- Command handling

### Communication Pattern

**Event Sourcing + CQRS:**
```
Domain Event (Kafka) → Analytics Service → Read Model (MongoDB/ClickHouse)
                                                      │
                                                      ▼
                                            Analytics Queries
```

**Implementation:**
```java
// Core Service publishes domain events
public void saveServiceInstance(ServiceInstance instance) {
    // Save to write model
    repository.save(instance);
    
    // Publish event
    DomainEvent event = ServiceInstanceUpdatedEvent.builder()
        .instanceId(instance.getId())
        .serviceId(instance.getServiceId())
        .status(instance.getStatus())
        .timestamp(Instant.now())
        .build();
    
    kafkaTemplate.send("domain-events", event);
}

// Analytics Service consumes and builds read models
@KafkaListener(topics = "domain-events")
public void updateReadModel(DomainEvent event) {
    // Update analytics read model
    analyticsRepository.updateReadModel(event);
    
    // Compute aggregations
    analyticsComputer.computeAggregations(event);
}
```

### Technology Stack

**Analytics Service:**
- **Language:** Java 21 or Python
- **Framework:** Spring Boot 3.3 or FastAPI
- **Database Options:**
  - MongoDB (for flexibility)
  - ClickHouse (for time series)
  - TimescaleDB (for PostgreSQL compatibility)
- **Stream Processing:** Kafka Streams or Apache Flink
- **Caching:** Redis

### Read Models

**Service Health Read Model:**
```java
@Document(collection = "service_health_analytics")
public class ServiceHealthAnalytics {
    private String serviceId;
    private String environment;
    private LocalDate date;
    private int totalInstances;
    private int healthyInstances;
    private int driftInstances;
    private double avgResponseTime;
    private int driftEventCount;
    // ... aggregations
}
```

**Drift Trend Read Model:**
```java
@Document(collection = "drift_trend_analytics")
public class DriftTrendAnalytics {
    private String serviceId;
    private String environment;
    private LocalDate date;
    private int driftCount;
    private double avgResolutionTime;
    private List<DriftSeverityBreakdown> severityBreakdown;
    // ... trend data
}
```

### Analytics Queries

**Real-Time Queries:**
- Current service health
- Recent drift events
- Active incidents

**Historical Queries:**
- Drift trends (7d, 30d, 90d)
- Service health over time
- Configuration change history
- Performance metrics

**Aggregations:**
- Daily/weekly/monthly summaries
- Team-level analytics
- Environment comparisons

### Migration Strategy

**Step 1: Build Read Models (Parallel)**
- Deploy analytics service
- Consume events and build read models
- Validate against core service queries

**Step 2: Switch Queries (Gradual)**
- Route 10% of analytics queries to new service
- Compare results
- Gradually increase to 100%

**Step 3: Remove Old Code**
- Remove analytics computation from core service
- Update API endpoints
- Clean up code

### Success Metrics

- **Query Latency:** <200ms (p95)
- **Data Freshness:** <5s lag
- **Query Accuracy:** 100% match
- **Throughput:** 1,000+ queries/second

---

## Service Communication Patterns

### Event-Driven Architecture

**Benefits:**
- Loose coupling
- Scalability
- Resilience
- Technology diversity

**Event Types:**
- Domain events (business events)
- Integration events (cross-service)
- Technical events (system events)

### Service Mesh Integration

**Options:**
- **Istio:** Full-featured, complex
- **Linkerd:** Lightweight, simple
- **Consul Connect:** Native Consul integration

**Benefits:**
- Automatic mTLS
- Circuit breaking
- Retry policies
- Distributed tracing
- Load balancing

**Implementation:**
```yaml
# Istio VirtualService
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: drift-detection-service
spec:
  hosts:
  - drift-detection-service
  http:
  - route:
    - destination:
        host: drift-detection-service
      weight: 100
    timeout: 5s
    retries:
      attempts: 3
      perTryTimeout: 2s
```

### API Gateway Integration

**Gateway Responsibilities:**
- Request routing
- Authentication/authorization
- Rate limiting
- Circuit breaking
- Request/response transformation

**Service Registration:**
- Services register with Consul
- Gateway discovers services
- Load balancing across instances

---

## Data Management

### Database per Service

**Strategy:**
- Each service owns its data
- No shared databases
- Event-driven data synchronization

**Core Service:**
- MongoDB: Domain data (ServiceInstance, ApplicationService)

**Drift Detection Service:**
- MongoDB: Read-only access to core data
- Local cache for performance

**Notification Service:**
- MongoDB: Notification history, templates

**Analytics Service:**
- ClickHouse/TimescaleDB: Read models, time series data

### Data Synchronization

**Event Sourcing:**
- Domain events as source of truth
- Services build their own views
- Event replay for recovery

**Change Data Capture (CDC):**
- MongoDB change streams
- Debezium for CDC
- Kafka Connect integration

---

## Deployment Strategy

### Containerization

**Each Service:**
- Independent Docker image
- Separate deployment pipeline
- Independent scaling

**Docker Compose (Development):**
```yaml
services:
  core-service:
    build: ./core-service
    ports:
      - "8080:8080"
  
  drift-detection-service:
    build: ./drift-detection-service
    ports:
      - "8081:8080"
  
  notification-service:
    build: ./notification-service
    ports:
      - "8082:8080"
  
  analytics-service:
    build: ./analytics-service
    ports:
      - "8083:8080"
```

### Kubernetes Deployment

**Deployment Strategy:**
- Separate deployments per service
- Horizontal Pod Autoscaling (HPA)
- Service mesh for communication

**Example:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: drift-detection-service
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: drift-detection
        image: drift-detection:latest
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
          limits:
            cpu: 2000m
            memory: 4Gi
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: drift-detection-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: drift-detection-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## Monitoring & Observability

### Distributed Tracing

**Implementation:**
- OpenTelemetry instrumentation
- W3C trace context propagation
- Jaeger/Tempo for trace storage

**Trace Flow:**
```
API Gateway → Core Service → Drift Detection Service
     │              │                  │
     └──────────────┴──────────────────┘
              (Distributed Trace)
```

### Service Metrics

**Per-Service Metrics:**
- Request rate
- Error rate
- Latency (p50, p95, p99)
- Resource utilization

**Cross-Service Metrics:**
- End-to-end latency
- Event processing lag
- Service dependencies

### Health Checks

**Each Service:**
- `/actuator/health` endpoint
- Dependency health (database, Kafka)
- Readiness and liveness probes

---

## Migration Timeline

### Phase 1: Drift Detection Service (Months 6-9)

**Month 6:**
- Design service boundaries
- Implement drift detection service
- Deploy in parallel

**Month 7:**
- Gradual traffic migration (10% → 50%)
- Performance validation
- Bug fixes

**Month 8:**
- Complete migration (100%)
- Remove old code
- Documentation

**Month 9:**
- Optimization
- Monitoring improvements
- Team training

### Phase 2: Notification Service (Months 9-12)

**Month 9:**
- Design service boundaries
- Implement notification service
- Deploy in parallel

**Month 10:**
- Gradual migration (10% → 50%)
- Channel testing
- Performance validation

**Month 11:**
- Complete migration (100%)
- Remove old code
- Documentation

**Month 12:**
- Optimization
- New channel additions
- Team training

### Phase 3: Analytics Service (Months 12-15)

**Month 12:**
- Design service boundaries
- Implement analytics service
- Build read models

**Month 13:**
- Gradual query migration (10% → 50%)
- Performance validation
- Data accuracy validation

**Month 14:**
- Complete migration (100%)
- Remove old code
- Documentation

**Month 15:**
- Optimization
- Advanced analytics features
- Team training

---

## Success Criteria

### Technical Metrics

| Metric | Target |
|--------|--------|
| **Service Independence** | 100% (no shared code) |
| **Deployment Frequency** | Daily per service |
| **Mean Time to Recovery** | <15 minutes |
| **Service Availability** | 99.9% per service |
| **End-to-End Latency** | <500ms (p95) |

### Business Metrics

| Metric | Target |
|--------|--------|
| **Development Velocity** | 2x improvement |
| **Deployment Risk** | 50% reduction |
| **Operational Costs** | 20% reduction |
| **Team Autonomy** | Independent deployments |

---

## Risks & Mitigation

### Risk: Service Communication Failures

**Mitigation:**
- Circuit breakers
- Retry policies
- Fallback mechanisms
- Event replay capability

### Risk: Data Consistency

**Mitigation:**
- Event sourcing
- Eventual consistency model
- Saga pattern for transactions
- Compensation actions

### Risk: Increased Complexity

**Mitigation:**
- Service mesh for communication
- Centralized logging and tracing
- Comprehensive documentation
- Team training

### Risk: Deployment Coordination

**Mitigation:**
- Independent deployments
- Backward compatibility
- Feature flags
- Gradual rollouts

---

## References

- [Microservices Patterns](https://microservices.io/patterns/)
- [Event-Driven Architecture](https://martinfowler.com/articles/201701-event-driven.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Service Mesh](https://istio.io/latest/docs/concepts/what-is-istio/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/)

---

**Next:** Review [Performance Optimization Details](./performance-optimization.md) for application-level improvements.

