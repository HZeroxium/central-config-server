# ZCM Spring SDK Starter

A Spring Boot starter for the ZCM (Zero Configuration Management) SDK that provides automatic configuration management, service discovery, and drift detection capabilities.

## Overview

The ZCM SDK Starter automatically configures your Spring Boot application with:

- **Configuration Management**: Automatic fetching and refreshing of configuration from Spring Cloud Config Server
- **Service Discovery**: Registration with Consul and client-side load balancing
- **Drift Detection**: Periodic ping to config-control-service with config hash for automatic drift detection and remediation
- **Event Bus Integration**: Kafka-based refresh events for configuration updates

## Quick Start

### 1. Add Dependency

```gradle
dependencies {
    implementation 'com.vng.zing:zcm-spring-sdk-starter:1.0.0'
}
```

### 2. Configure SDK

Add to your `application.yml`:

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

### 3. Enable Configuration Properties

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

## Configuration Drift Detection

The SDK automatically:

1. **Calculates SHA-256 hash** of current configuration
2. **Pings config-control-service** every 30 seconds with hash
3. **Listens for refresh events** on Kafka topic `config-refresh`
4. **Auto-refreshes configuration** when drift is detected

### Configuration

```yaml
zcm:
  sdk:
    ping:
      enabled: true
      fixed-delay: 30000  # 30 seconds
    control:
      url: http://config-control-service:8080
    bus:
      refresh:
        enabled: true
        topic: config-refresh
```

### How It Works

1. **Bootstrap**: SDK fetches configuration from Config Server on startup
2. **Hash Calculation**: SDK calculates SHA-256 hash of effective configuration
3. **Periodic Ping**: Every 30 seconds, SDK sends heartbeat to config-control-service with:
   - Service name and instance ID
   - Current config hash
   - Host, port, environment, version
   - Metadata
4. **Drift Detection**: config-control-service compares hash with expected hash from Config Server
5. **Auto-Refresh**: If drift detected, config-control-service publishes refresh event to Kafka
6. **Configuration Update**: SDK receives event, calls Spring ContextRefresher to reload configuration

## API Usage

### Configuration Access

```java
@Component
public class MyService {
    
    @Value("${my.property}")
    private String myProperty;
    
    @ConfigurationProperties(prefix = "my")
    @Component
    public static class MyProperties {
        private String property;
        // getters/setters
    }
}
```

### Service Discovery

```java
@Service
public class MyService {
    
    @Autowired
    private ClientApi zcmClient;
    
    public String callOtherService() {
        // SDK automatically discovers and load balances
        return zcmClient.get("other-service", "/api/data", String.class);
    }
}
```

### Manual Configuration Refresh

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

### Ping Configuration

```yaml
zcm:
  sdk:
    control:
      url: http://control:8080      # Config Control Service URL
    ping:
      enabled: true                 # Enable ping for drift detection
      fixed-delay: 30000           # Ping interval in milliseconds
```

### Bus Configuration

```yaml
zcm:
  sdk:
    bus:
      refresh:
        enabled: true              # Enable refresh listener
        topic: config-refresh      # Kafka topic for refresh events
      kafka:
        bootstrap-servers: localhost:9092  # Kafka bootstrap servers
```

### Discovery Configuration

```yaml
zcm:
  sdk:
    discovery:
      provider: CONSUL             # Discovery provider (CONSUL, CONTROL)
      consul:
        host: consul               # Consul host
        port: 8500                # Consul port
        register: true            # Auto-register with Consul
        heartbeat:
          enabled: true           # Enable Consul TTL heartbeat
          ttl: 10s               # TTL for health checks
```

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

# Discovery
ZCM_SDK_DISCOVERY_CONSUL_HOST=consul
ZCM_SDK_DISCOVERY_CONSUL_REGISTER=true

# Kafka/Bus
ZCM_SDK_BUS_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
ZCM_SDK_BUS_REFRESH_ENABLED=true
```

## Auto-Configuration

The SDK automatically configures:

- **Spring Cloud Config Client**: Fetches configuration from Config Server
- **Spring Cloud Consul Discovery**: Registers with Consul
- **Spring Cloud LoadBalancer**: Client-side load balancing
- **Spring Cloud Bus**: Kafka-based refresh events
- **Ping Scheduler**: Periodic heartbeat to config-control-service
- **Refresh Listener**: Kafka consumer for refresh events

## Monitoring

### Health Checks

The SDK exposes health information:

```bash
curl http://localhost:8080/actuator/health
```

### Metrics

Prometheus metrics for drift detection:

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

## Troubleshooting

### Ping Not Working

1. Check control service URL is accessible
2. Verify ping is enabled in configuration
3. Check logs for ping errors

```bash
# Check configuration
curl http://localhost:8080/actuator/configprops | grep zcm

# Check ping logs
docker logs my-service | grep "ZCM ping"
```

### Refresh Not Working

1. Verify Kafka connectivity
2. Check refresh listener is enabled
3. Verify Kafka topic exists

```bash
# Check Kafka topics
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# Check consumer group
docker exec kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

### Service Discovery Issues

1. Check Consul connectivity
2. Verify service registration
3. Check discovery client configuration

```bash
# Check Consul services
curl http://localhost:8500/v1/catalog/services

# Check service health
curl http://localhost:8500/v1/health/service/my-service
```

## Development

### Building from Source

```bash
git clone <repository>
cd zcm-spring-sdk-starter
./gradlew build
```

### Publishing to Local Maven

```bash
./gradlew publishToMavenLocal
```

### Testing

```bash
./gradlew test
```

## Examples

See the `sample-service` module for a complete example of SDK usage.

## References

- [Spring Cloud Config](https://cloud.spring.io/spring-cloud-config/)
- [Spring Cloud Consul](https://cloud.spring.io/spring-cloud-consul/)
- [Spring Cloud Bus](https://docs.spring.io/spring-cloud-bus/)
- [Config Control Service](../config-control-service/README.md)
