# Troubleshooting Guide
## Common Issues and Solutions

---

## Service Instances Not Appearing

### Symptoms

- Service instances not visible in Admin Dashboard
- Consul shows no registered instances
- Service discovery queries return empty results

### Diagnosis

**Check Consul Connectivity:**
```bash
# Check Consul is accessible
curl http://localhost:20500/v1/catalog/services

# Check service registration
curl http://localhost:20500/v1/health/service/sample-service
```

**Check SDK Configuration:**
```bash
# Verify ZCM SDK is enabled
docker logs sample-service | grep "ZCM SDK"
docker logs sample-service | grep "Consul registration"
```

**Check Docker Network:**
```bash
# Verify service is on infra-network
docker network inspect infra-network | grep sample-service

# Check DNS resolution
docker exec sample-service ping consul
```

### Solutions

1. **Verify Consul Host/Port Configuration:**
   - Check `ZCM_SDK_DISCOVERY_CONSUL_HOST` environment variable
   - Local: `consul` (Docker DNS name)
   - Remote: IP address or hostname

2. **Verify Registration Enabled:**
   ```yaml
   zcm:
     sdk:
       discovery:
         consul:
           register: true
           heartbeat:
             enabled: true
   ```

3. **Check Service Health:**
   - Service must be healthy for Consul registration
   - Verify `/actuator/health` endpoint returns 200

4. **Network Connectivity:**
   - Ensure service is on `infra-network`
   - Verify Consul port is accessible (8500 local, 20500 remote)

**Reference:** `sample-service/src/main/resources/application.yml:28-33`

---

## Drift Not Detected

### Symptoms

- Configuration changes not detected
- Drift events not created
- Refresh events not triggered

### Diagnosis

**Check Heartbeat Processing:**
```bash
# Verify heartbeat is received
docker logs config-control-service | grep "heartbeat"
docker logs config-control-service | grep "drift"

# Check heartbeat metrics
curl http://localhost:28081/actuator/prometheus | grep heartbeat
```

**Check Config Server Accessibility:**
```bash
# Verify Config Server is accessible
curl http://localhost:28888/actuator/health

# Check config hash fetching
docker logs config-control-service | grep "config hash"
```

**Verify Hash Calculation:**
```bash
# Check config hash comparison
docker logs config-control-service | grep "hash mismatch"
docker logs config-control-service | grep "hash match"
```

### Solutions

1. **Verify Ping is Enabled:**
   ```yaml
   zcm:
     sdk:
       ping:
         enabled: true
         fixed-delay: 30000  # 30 seconds
   ```

2. **Check Config Server URL:**
   - Verify `CONFIG_SERVER_URL` is correct
   - Test config server endpoint: `GET /{service}/{profile}/config`

3. **Verify Hash Calculation:**
   - Config hash should be SHA-256 of configuration JSON
   - Check logs for hash calculation errors

4. **Check Drift Detection Logic:**
   - Verify `appliedHash` vs `expectedHash` comparison
   - Check DriftEvent creation logic

**Reference:** `config-control-service/src/main/java/com/example/control/application/service/infra/HeartbeatService.java`

---

## Refresh Not Working

### Symptoms

- Configuration changes not applied to client services
- Refresh events not received
- Client services show old configuration

### Diagnosis

**Check Kafka Connectivity:**
```bash
# Verify Kafka is accessible
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# Check config-refresh topic
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic config-refresh

# Check consumer groups
docker exec kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

**Check Refresh Events:**
```bash
# Verify refresh events are published
docker logs config-control-service | grep "refresh event"
docker logs config-control-service | grep "config-refresh"

# Check SDK refresh listener
docker logs sample-service | grep "refresh event"
docker logs sample-service | grep "ZCM refresh"
```

**Verify Refresh Listener:**
```bash
# Check refresh listener is enabled
docker logs sample-service | grep "refresh listener"
```

### Solutions

1. **Verify Kafka Configuration:**
   ```yaml
   zcm:
     sdk:
       bus:
         refresh:
           enabled: true
           topic: config-refresh
         kafka:
           bootstrap-servers: kafka:9092  # Local
           # bootstrap-servers: 10.40.30.161:20092  # Remote
   ```

2. **Check Topic Exists:**
   - Topic `config-refresh` should exist in Kafka
   - Create if missing: `kafka-topics.sh --create --topic config-refresh`

3. **Verify Refresh Listener:**
   - SDK must have `bus.refresh.enabled=true`
   - Check Kafka consumer group is active

4. **Check Refresh Execution:**
   - Verify Spring ContextRefresher is called
   - Check `@RefreshScope` beans are reloaded

**Reference:** `zcm-spring-sdk-starter/README.md`, `sample-service/src/main/resources/application.yml:16-27`

---

## Gateway Routing Issues

### Symptoms

- Requests to gateway return 502 Bad Gateway
- Circuit breaker trips frequently
- Backend services not discovered

### Diagnosis

**Check Gateway Health:**
```bash
# Verify gateway is healthy
curl http://localhost:28082/actuator/health

# Check backend health indicator
curl http://localhost:28082/actuator/health/readiness
```

**Check Service Discovery:**
```bash
# Verify backend service is in Consul
curl http://localhost:20500/v1/health/service/config-control-service?passing=true

# Check gateway route configuration
curl http://localhost:28082/actuator/gateway/routes
```

**Check Circuit Breaker:**
```bash
# Verify circuit breaker state
curl http://localhost:28082/actuator/prometheus | grep circuitbreaker

# Check circuit breaker metrics
curl http://localhost:28082/actuator/circuitbreakers
```

### Solutions

1. **Verify Service Discovery:**
   - Gateway must discover `config-control-service` via Consul
   - Check Consul service registration

2. **Check Route Configuration:**
   ```yaml
   spring:
     cloud:
       gateway:
         routes:
           - id: config-control-service
             uri: lb://config-control-service
             predicates:
               - Path=/api/**
   ```

3. **Verify Circuit Breaker:**
   - Check failure threshold (50% default)
   - Verify backend service is healthy
   - Check timeout configuration (5s default)

4. **Check Rate Limiting:**
   - Verify Redis is accessible for rate limiting
   - Check rate limiter configuration

**Reference:** `gateway-service/src/main/resources/application-gateway.yml`

---

## Keycloak Authentication Problems

### Symptoms

- 401 Unauthorized errors
- Token validation failures
- OAuth2 redirect loops
- Swagger UI authentication fails

### Diagnosis

**Check Keycloak Health:**
```bash
# Verify Keycloak is healthy
curl http://localhost:28080/health

# Check realm configuration
curl http://localhost:28080/realms/config-control/.well-known/openid-configuration
```

**Check JWT Token:**
```bash
# Verify token structure
echo $TOKEN | cut -d. -f2 | base64 -d | jq

# Check token claims
curl -H "Authorization: Bearer $TOKEN" http://localhost:28081/api/services
```

**Verify Keycloak URLs:**
```bash
# Check KEYCLOAK_PUBLIC_URL
echo $KEYCLOAK_PUBLIC_URL

# Check KEYCLOAK_ISSUER_URI
echo $KEYCLOAK_ISSUER_URI
```

### Solutions

1. **Verify Keycloak URLs:**
   - **Public URL**: Browser-accessible URL (`http://localhost:28080` or `http://10.40.30.161:28080`)
   - **Internal URL**: Service-to-service URL (always `http://keycloak:8080`)
   - **Issuer URI**: `{KEYCLOAK_PUBLIC_URL}/realms/config-control`

2. **Check Realm Configuration:**
   - Realm must be `config-control`
   - Client ID must match (e.g., `config-control-service`)
   - Audience must match (e.g., `config-control-service`)

3. **Verify Token Validation:**
   - Check Spring Security OAuth2 Resource Server configuration
   - Verify issuer URI matches Keycloak public URL
   - Check audience validation

4. **Swagger UI OAuth2:**
   - Ensure `SWAGGER_OAUTH2_REDIRECT_URL` is browser-accessible
   - Verify Keycloak public URL matches redirect URL host

**Reference:** `config-control-service/src/main/resources/application-security.yml`, `config/env/env.infra-remote:42-52`

---

## MongoDB Connection Issues

### Symptoms

- Services fail to start
- Database operations fail
- Connection timeout errors

### Diagnosis

**Check MongoDB Health:**
```bash
# Verify MongoDB is accessible
docker exec mongodb mongosh --eval "db.adminCommand('ping')"

# Check connection string
echo $MONGODB_URI
```

**Check Network Connectivity:**
```bash
# Verify service can reach MongoDB
docker exec config-control-service ping mongodb

# Check port accessibility
docker exec config-control-service nc -zv mongodb 27017
```

**Check Connection Configuration:**
```bash
# Verify MongoDB URI format
# Local: mongodb://mongodb:27017/config_control
# Remote: mongodb://10.40.30.161:20017/config_control
```

### Solutions

1. **Verify MongoDB URI:**
   - **Local**: `mongodb://mongodb:27017/config_control`
   - **Remote**: `mongodb://10.40.30.161:20017/config_control`

2. **Check Network:**
   - Ensure service is on `infra-network`
   - Verify MongoDB port is accessible (27017 local, 20017 remote)

3. **Check MongoDB Health:**
   - Verify MongoDB container is running
   - Check MongoDB logs for errors

---

## Redis Connection Issues

### Symptoms

- Cache operations fail
- Rate limiting not working
- Connection timeout errors

### Diagnosis

**Check Redis Health:**
```bash
# Verify Redis is accessible
docker exec redis redis-cli ping

# Check connection string
echo $REDIS_URL
```

**Check Network Connectivity:**
```bash
# Verify service can reach Redis
docker exec config-control-service ping redis

# Check port accessibility
docker exec config-control-service nc -zv redis 6379
```

### Solutions

1. **Verify Redis URL:**
   - **Local**: `redis://redis:6379`
   - **Remote**: `redis://10.40.30.161:20379`

2. **Check Network:**
   - Ensure service is on `infra-network`
   - Verify Redis port is accessible (6379 local, 20379 remote)

---

## Debugging Tips

### Log Analysis

**Check Service Logs:**
```bash
# View recent logs
docker logs --tail 100 config-control-service

# Follow logs in real-time
docker logs -f config-control-service

# Filter logs by pattern
docker logs config-control-service | grep "ERROR"
docker logs config-control-service | grep "heartbeat"
```

**Check Infrastructure Logs:**
```bash
# MongoDB
docker logs mongodb

# Redis
docker logs redis

# Kafka
docker logs kafka

# Consul
docker logs consul
```

### Health Checks

**Check All Services:**
```bash
# Application services
docker-compose -f docker-compose.yml ps

# Infrastructure services
docker-compose -f docker-compose.infra.yml ps
```

**Check Specific Service:**
```bash
# Config Control Service
curl http://localhost:28081/actuator/health

# Gateway Service
curl http://localhost:28082/actuator/health

# Sample Service
curl http://localhost:28080/actuator/health
```

### Network Connectivity

**Test DNS Resolution:**
```bash
# Test service name resolution
docker exec config-control-service nslookup consul
docker exec config-control-service nslookup mongodb
```

**Test Port Connectivity:**
```bash
# Test specific port
docker exec config-control-service nc -zv consul 8500
docker exec config-control-service nc -zv mongodb 27017
```

### Configuration Validation

**Check Environment Variables:**
```bash
# View all environment variables
docker exec config-control-service env | grep -E "(CONFIG|CONSUL|MONGODB|REDIS|KAFKA|KEYCLOAK)"
```

**Check Application Configuration:**
```bash
# View configuration properties
curl http://localhost:28081/actuator/configprops

# View environment
curl http://localhost:28081/actuator/env
```

---

## References

- [Config Control Service README](../../../config-control-service/README.md)
- [ZCM SDK Documentation](../../../zcm-spring-sdk-starter/README.md)
- [Deployment Guide](../README.md)

