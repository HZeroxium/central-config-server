# Gateway Service

Spring Cloud Gateway service providing API gateway functionality with service discovery, load balancing, circuit breaking, rate limiting, and observability.

## Features

- **Service Discovery**: Integrates with Consul for automatic service discovery
- **Load Balancing**: Round-robin load balancing across multiple backend instances
- **Circuit Breaker**: Resilience4j circuit breaker with configurable thresholds
- **Rate Limiting**: Per-user rate limiting based on JWT `sub` claim (falls back to IP)
- **CORS**: Centralized CORS handling at gateway level
- **JWT Forwarding**: Forwards JWT tokens to backend (backend validates)
- **Health Checks**: Custom health indicator that checks backend availability
- **Observability**: Full metrics, tracing, and structured logging

## Configuration

### Port
- Internal: `8080`
- External: `28082` (mapped in docker-compose)

### Routes
- `/api/**` → `lb://config-control-service`

### Circuit Breaker
- Failure rate threshold: 50%
- Timeout: 5s
- Minimum calls: 10
- Sliding window size: 10

### Rate Limiting
- Replenish rate: 100 requests/second
- Burst capacity: 200 requests
- Key resolver: JWT `sub` claim (falls back to IP)

## Environment Variables

- `CONSUL_HOST`: Consul host (default: consul)
- `CONSUL_PORT`: Consul port (default: 8500)
- `REDIS_URL`: Redis URL for rate limiting
- `GATEWAY_CORS_ALLOWED_ORIGINS`: Allowed CORS origins (default: ["*"])
- `GATEWAY_RATE_LIMIT_REPLENISH`: Rate limit replenish rate (default: 100)
- `GATEWAY_RATE_LIMIT_BURST`: Rate limit burst capacity (default: 200)
- `GATEWAY_TIMEOUT`: Request timeout (default: 5s)

## Health Checks

- `/actuator/health`: Basic health check
- `/actuator/health/readiness`: Readiness check (depends on backend availability)

## Observability

- Metrics: `/actuator/prometheus`
- Gateway routes: `/actuator/gateway/routes`
- Tracing: OpenTelemetry (if configured)

## Building

```bash
./gradlew :gateway-service:build
./gradlew :gateway-service:buildDocker
```

## Running

```bash
docker-compose up gateway-service
```

## Architecture

```
admin-dashboard (nginx) → gateway-service:28082 → lb://config-control-service (instances)
```

The gateway:
1. Receives requests from admin-dashboard
2. Adds correlation ID
3. Applies rate limiting (per-user)
4. Routes to backend via load balancer
5. Applies circuit breaker
6. Forwards JWT token (backend validates)
7. Returns response with CORS headers

