# SDK Integration Guide
## ZCM Spring SDK Starter

---

## Overview

The ZCM (Zero Configuration Management) Spring SDK Starter provides automatic configuration management, service discovery, and drift detection capabilities for Spring Boot applications. It integrates seamlessly with the Centralized Configuration Management system.

### Key Features

- **Automatic Configuration Management**: Fetches and refreshes configuration from Spring Cloud Config Server
- **Service Discovery**: Registers with Consul and provides client-side load balancing
- **Drift Detection**: Periodic heartbeat with config hash for automatic drift detection
- **Event-Driven Refresh**: Kafka-based refresh events for zero-downtime configuration updates

**Reference:** `zcm-spring-sdk-starter/README.md`

---

## Getting Started

### 1. Add Dependency

Add the ZCM SDK Starter to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.vng.zing:zcm-spring-sdk-starter:1.0.0'
}
```

### 2. Enable Auto-Configuration

The SDK uses Spring Boot auto-configuration. Enable `@ConfigurationPropertiesScan`:

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 3. Configure SDK

Add configuration to `application.yml`:

```yaml
zcm:
  sdk:
    service:
      name: my-service
    config:
      server:
        url: http://config-server:8888
    control:
      url: http://config-control-service:8080
    ping:
      enabled: true
      fixed-delay: 30000  # 30 seconds
    bus:
      refresh:
        enabled: true
        topic: config-refresh
    discovery:
      consul:
        host: consul
        register: true
```

**Reference:** `sample-service/src/main/resources/application.yml:3-38`

---

## Configuration Reference

### Core Settings

```yaml
zcm:
  sdk:
    service:
      name: my-service              # Service name for registration
    instance:
      id: my-service-1              # Instance ID (auto-generated if not set)
    config:
      server:
        url: http://config:8888     # Config Server URL
```

### Heartbeat Configuration

```yaml
zcm:
  sdk:
    control:
      url: http://control:8080      # Config Control Service URL
    ping:
      enabled: true                 # Enable ping for drift detection
      fixed-delay: 30000           # Ping interval (milliseconds)
      protocol: HTTP                # Protocol: HTTP, THRIFT, GRPC, or KAFKA
```

**Protocol Options:**
- `HTTP`: HTTP REST endpoint (default)
- `THRIFT`: Apache Thrift RPC
- `GRPC`: gRPC protocol
- `KAFKA`: Kafka messaging (high-throughput)

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/pingconfig/strategy/`

### Kafka Protocol Optimizations

When using Kafka protocol for heartbeat communication, the SDK implements several optimizations to improve reliability, performance, and resilience.

#### Kafka Config Cache

The SDK uses a thread-safe cache (`KafkaConfigCache`) to store Kafka configuration (bootstrap servers and topic) fetched from config-control-service.

**Lazy Initialization:**
- Configuration is fetched on first access (lazy loading)
- Subsequent calls return cached value until refresh interval expires
- Falls back to property/environment-based configuration if fetch fails

**Thread-Safe Implementation:**
```java
private final ReentrantLock lock = new ReentrantLock();
private volatile KafkaConfig cachedConfig;
private volatile Instant lastFetchTime;
private volatile boolean initialized = false;

public KafkaConfig get() {
    // Double-check locking pattern
    if (!needsRefresh && cachedConfig != null) {
        return cachedConfig;  // Fast path (no lock)
    }
    
    lock.lock();
    try {
        // Double-check after acquiring lock
        if (!initialized || needsRefresh) {
            cachedConfig = getFetcher().fetch();
            lastFetchTime = Instant.now();
            initialized = true;
        }
        return cachedConfig;
    } finally {
        lock.unlock();
    }
}
```

**Periodic Refresh:**
- Scheduled refresh every 5 minutes (configurable via `zcm.sdk.ping.kafka.config-refresh-interval`)
- Background refresh ensures cache stays up-to-date
- Graceful fallback: keeps stale config if refresh fails

**Configuration:**
```yaml
zcm:
  sdk:
    ping:
      kafka:
        config-refresh-interval: 300000  # 5 minutes (milliseconds)
```

**Benefits:**
- **Reduced Latency**: Cached config eliminates HTTP call overhead
- **High Availability**: Fallback to property-based config if service unavailable
- **Thread Safety**: ReentrantLock ensures safe concurrent access
- **Self-Healing**: Periodic refresh ensures config stays current

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/pingconfig/strategy/KafkaConfigCache.java:80-125`

#### Circuit Breaker Protection

The SDK implements Resilience4j circuit breaker protection for Kafka ping operations to fail-fast when Kafka is unavailable.

**Configuration:**
```yaml
zcm:
  sdk:
    ping:
      circuit-breaker:
        enabled: true
        failure-rate-threshold: 50  # Open circuit at 50% failure rate
        wait-duration-in-open-state: 30000  # 30 seconds
        permitted-number-of-calls-in-half-open-state: 3
        sliding-window-size: 10
```

**Circuit Breaker States:**
- **CLOSED**: Normal operation, requests pass through
- **OPEN**: Circuit opened due to high failure rate, requests fail-fast
- **HALF_OPEN**: Testing recovery, limited requests allowed

**Benefits:**
- **Fail-Fast**: Prevents retry storms when Kafka is down
- **Resource Protection**: Reduces scheduler thread blocking
- **Automatic Recovery**: Transitions to HALF_OPEN to test recovery
- **Observability**: Metrics track circuit breaker state transitions

**Implementation:**
```java
@Bean(name = "kafkaPingCircuitBreaker")
public CircuitBreaker kafkaPingCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("kafka-ping-producer", config);
}

// Usage in KafkaPingStrategy
@Override
public void sendHeartbeat(String endpoint, HeartbeatPayload payload) throws Exception {
    if (circuitBreaker != null) {
        circuitBreaker.executeCallable(() -> {
            // Send heartbeat to Kafka
            return sendToKafka(topic, payload);
        });
    }
}
```

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/pingconfig/strategy/KafkaPingCircuitBreakerConfig.java`

#### Kafka Config Fetching

The SDK fetches Kafka configuration from config-control-service with graceful fallback handling.

**Fetching Logic:**
1. **Primary**: Fetch from `/api/infrastructure/kafka-config` endpoint
2. **Fallback 1**: Use `ZCM_SDK_PING_KAFKA_BOOTSTRAP_SERVERS` environment variable
3. **Fallback 2**: Use `zcm.sdk.ping.kafka.bootstrap-servers` property
4. **Fallback 3**: Use `spring.kafka.bootstrap-servers` property

**API Key Authentication:**
- Includes `X-API-Key` header if API key authentication is enabled
- Bypasses JWT authentication for SDK operations
- Provides SYS_ADMIN privileges for accessing protected endpoints

**Metrics Tracking:**
- `zcm.ping.kafka.config.fetch` - Successful config fetch count
- `zcm.ping.kafka.config.fetch.failure` - Failed config fetch count

**Implementation:**
```java
public KafkaConfig fetch() {
    try {
        if (pingMetrics != null) {
            pingMetrics.recordKafkaConfigFetch();
        }
        
        // Add API key header if configured
        var requestBuilder = restClient.get()
            .uri(controlUrl + "/api/infrastructure/kafka-config");
        
        if (apiKeyEnabled) {
            requestBuilder.header("X-API-Key", apiKey);
        }
        
        KafkaConfigResponse response = requestBuilder
            .retrieve()
            .body(KafkaConfigResponse.class);
        
        return new KafkaConfig(response.getBootstrapServers(), response.getTopic());
    } catch (Exception e) {
        if (pingMetrics != null) {
            pingMetrics.recordKafkaConfigFetchFailure();
        }
        return getFallbackConfig();  // Graceful fallback
    }
}
```

**Benefits:**
- **High Availability**: Multiple fallback levels ensure configuration is always available
- **Observability**: Metrics track fetch success/failure rates
- **Authentication**: API key support enables secure SDK communication
- **Resilience**: Graceful degradation if config-control-service unavailable

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/pingconfig/strategy/KafkaConfigFetcher.java`

#### Kafka Ping Strategy: Async Fire-and-Forget

The Kafka ping strategy uses an async fire-and-forget pattern to avoid blocking the scheduler thread:

**Implementation:**
```java
@Override
public void sendHeartbeat(String endpoint, HeartbeatPayload payload) throws Exception {
    // Get Kafka config from cache (fast path)
    KafkaConfig config = configCache.get();
    String topic = config.topic();
    String partitionKey = payload.getServiceName(); // Service ordering
    
    // Check circuit breaker state (fail-fast if open)
    if (circuitBreaker != null && circuitBreaker.getState() == State.OPEN) {
        log.warn("Circuit breaker OPEN, skipping heartbeat");
        return; // Don't throw - fire-and-forget pattern
    }
    
    // Async send (non-blocking)
    CompletableFuture<SendResult<String, HeartbeatPayload>> future = 
        kafkaTemplate.send(topic, partitionKey, payload);
    
    // Handle completion asynchronously
    future.whenComplete((result, exception) -> {
        if (exception != null) {
            circuitBreaker.onError(duration, exception);
            pingMetrics.recordPingFailure(protocol, serviceName);
        } else {
            circuitBreaker.onSuccess(duration);
            pingMetrics.recordPingSuccess(protocol, duration, serviceName);
        }
    });
    
    // Return immediately - scheduler thread not blocked
}
```

**Benefits:**
- **Non-Blocking**: Scheduler thread returns immediately, doesn't wait for Kafka acknowledgment
- **Partition Ordering**: Uses serviceName as partition key for per-service ordering
- **Circuit Breaker Integration**: Checks state before sending, records success/failure
- **Metrics Tracking**: Tracks send success/failure and latency asynchronously

**Kafka Producer Configuration:**
- **Batch Size**: 16KB (default)
- **Linger MS**: 10ms (batching for efficiency)
- **Compression**: gzip (reduces network overhead)
- **Acks**: 1 (leader acknowledgment, low latency)
- **Retries**: 3 (with exponential backoff)
- **Retry Backoff**: 100ms base

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/pingconfig/strategy/KafkaPingStrategy.java:52-126`, `KafkaPingProducerConfig.java:47-81`

### Service Discovery Configuration

```yaml
zcm:
  sdk:
    discovery:
      provider: CONSUL              # Discovery provider (CONSUL, CONTROL)
      consul:
        host: consul               # Consul host (Docker DNS name or IP)
        port: 8500                 # Consul port
        register: true             # Auto-register with Consul
        heartbeat:
          enabled: true            # Enable Consul TTL heartbeat
          ttl: 10s                 # TTL for health checks
```

**Registration Behavior:**
- Service automatically registers with Consul on startup
- TTL heartbeat keeps service registered
- Service deregisters on shutdown

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/client/discovery/consul/`

### Bus Configuration (Config Refresh)

```yaml
zcm:
  sdk:
    bus:
      refresh:
        enabled: true              # Enable refresh listener
        topic: config-refresh      # Kafka topic for refresh events
      kafka:
        bootstrap-servers: kafka:9092  # Kafka bootstrap servers
```

**Refresh Flow:**
1. Config Control Service detects drift
2. Publishes refresh event to Kafka topic
3. SDK receives event via Kafka consumer
4. SDK calls Spring ContextRefresher to reload configuration
5. `@RefreshScope` beans are refreshed

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/bus/refresh/`

### Load Balancing Configuration

```yaml
zcm:
  sdk:
    client:
      loadbalancer:
        strategy: ROUND_ROBIN      # Strategy: ROUND_ROBIN, RANDOM, WEIGHTED_RANDOM, etc.
```

**Available Strategies:**
- `ROUND_ROBIN`: Round-robin distribution
- `RANDOM`: Random selection
- `WEIGHTED_RANDOM`: Weighted random selection
- `RENDEZVOUS_HASHING`: Rendezvous hashing
- `CONSISTENT_HASHING`: Consistent hashing

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/client/loadbalancer/strategy/`

### API Key Authentication

```yaml
zcm:
  sdk:
    api-key:
      enabled: true                # Enable API key authentication
      key: your-api-key-here       # API key value
```

**Purpose:**
- SDK clients use API key for authentication (preferred over JWT)
- Bypasses JWT authentication for protected endpoints
- Provides SYS_ADMIN privileges for SDK operations

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/client/http/`

---

## Environment Variables

The SDK can be configured via environment variables:

```bash
# Service identification
ZCM_SDK_SERVICE_NAME=my-service
ZCM_SDK_INSTANCE_ID=my-service-1

# Config Server
ZCM_SDK_CONFIG_SERVER_URL=http://config-server:8888

# Control Service
ZCM_SDK_CONTROL_URL=http://config-control-service:8080

# Ping settings
ZCM_SDK_PING_ENABLED=true
ZCM_SDK_PING_FIXED_DELAY=30000
ZCM_SDK_PING_PROTOCOL=HTTP

# API Key Authentication
ZCM_SDK_API_KEY_ENABLED=true
ZCM_SDK_API_KEY=your-api-key-here

# Discovery
ZCM_SDK_DISCOVERY_CONSUL_HOST=consul
ZCM_SDK_DISCOVERY_CONSUL_REGISTER=true

# Kafka/Bus
ZCM_SDK_BUS_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ZCM_SDK_BUS_REFRESH_ENABLED=true
```

**Reference:** `docker-compose.yml:56-64` (sample-service configuration)

---

## Usage Examples

### Configuration Access

Access configuration properties in your application:

```java
@Component
public class MyService {
    
    @Value("${my.property}")
    private String myProperty;
    
    @ConfigurationProperties(prefix = "my")
    @Component
    @RefreshScope  // Reload on config refresh
    public static class MyProperties {
        private String property;
        // getters/setters
    }
}
```

### Service Discovery

Use SDK client API for service discovery and load balancing:

```java
@Service
public class MyService {
    
    @Autowired
    private ClientApi zcmClient;
    
    public String callOtherService() {
        // SDK automatically discovers and load balances
        return zcmClient.get("other-service", "/api/data", String.class);
    }
    
    public List<ServiceInstance> getInstances(String serviceName) {
        // Get all instances of a service
        return zcmClient.getInstances(serviceName);
    }
}
```

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/client/ClientApi.java`

### Manual Configuration Refresh

Trigger manual configuration refresh:

```java
@RestController
public class ConfigController {
    
    @Autowired
    private ConfigRefresher configRefresher;
    
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh() {
        Set<String> changedKeys = configRefresher.refresh();
        return ResponseEntity.ok("Refreshed keys: " + changedKeys);
    }
}
```

### Key-Value Store Access

Access Key-Value store entries:

```java
@Service
public class MyService {
    
    @Autowired
    private KVStoreApi kvStore;
    
    public String getValue(String key) {
        return kvStore.getValue(key);
    }
}
```

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/client/kv/`

---

## Auto-Configuration

The SDK automatically configures:

- **Spring Cloud Config Client**: Fetches configuration from Config Server
- **Spring Cloud Consul Discovery**: Registers with Consul
- **Spring Cloud LoadBalancer**: Client-side load balancing
- **Spring Cloud Bus**: Kafka-based refresh events
- **Ping Scheduler**: Periodic heartbeat to config-control-service
- **Refresh Listener**: Kafka consumer for refresh events

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/config/SdkAutoConfiguration.java`

---

## Monitoring

### Health Checks

The SDK exposes health information:

```bash
curl http://localhost:8080/actuator/health
```

### Metrics

Prometheus metrics for SDK operations:

- `zcm.ping.success` - Successful ping count
- `zcm.ping.failure` - Failed ping count
- `zcm.refresh.count` - Configuration refresh count
- `zcm.drift.detected` - Drift detection count

### Logs

Key log patterns:

```
# Ping activity
ZCM ping sent to config-control-service

# Refresh events
ZCM refresh event received: {...}
ZCM refresh applied; changedKeys=[...], newHash=abc123

# Discovery
ZCM service registered with Consul: my-service
```

---

## Integration Flow

### Startup Sequence

1. **Bootstrap**: SDK fetches configuration from Config Server
2. **Registration**: Service registers with Consul
3. **Heartbeat Start**: Periodic ping to config-control-service begins
4. **Refresh Listener**: Kafka consumer starts listening for refresh events

### Drift Detection Flow

1. **Heartbeat**: SDK sends heartbeat with config hash every 30 seconds
2. **Hash Comparison**: Config Control Service compares hashes
3. **Drift Detection**: If mismatch, drift event created
4. **Refresh Event**: Config Control Service publishes refresh event to Kafka
5. **Configuration Update**: SDK receives event and refreshes Spring context
6. **Verification**: Next heartbeat confirms drift resolution

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/pingconfig/`

---

## Complete Example

See `sample-service` for a complete integration example:

**Configuration:** `sample-service/src/main/resources/application.yml`

**Key Features:**
- HTTP ping protocol
- Consul registration enabled
- Kafka refresh listener enabled
- API key authentication
- Key-Value store access

**Reference:** `sample-service/README.md`

---

## References

- [ZCM SDK Documentation](../../../zcm-spring-sdk-starter/README.md)
- [Sample Service](../../../sample-service/src/main/resources/application.yml)
- [Service Discovery](../../appendices/service-discovery.md)
- [Drift Detection](../../appendices/drift-detection.md)

