# Spring Boot Admin Server

Centralized monitoring and management dashboard for Spring Boot applications in the Thrift Demo system.

## Features

- **Comprehensive Monitoring**: Real-time health checks, metrics, and performance data
- **Application Discovery**: Automatic discovery and registration of Spring Boot applications
- **Detailed Metrics**: JVM metrics, HTTP requests, database connections, cache statistics
- **Log Management**: View and search application logs in real-time
- **Environment Information**: Configuration properties, system properties, and environment variables
- **Circuit Breaker Monitoring**: Resilience4j circuit breaker status and metrics
- **Custom Dashboards**: Configurable monitoring dashboards

## Quick Start

### Prerequisites

- JDK 21
- Docker & Docker Compose

### Running Locally

1. **Build and run**:
   ```bash
   ./gradlew bootRun
   ```

2. **Access the admin UI**:
   - Admin UI: http://localhost:8083/admin
   - Actuator: http://localhost:8083/actuator/health

### Running with Docker

1. **Build the image**:
   ```bash
   ./gradlew buildDocker
   ```

2. **Start with Docker Compose**:
   ```bash
   docker compose up -d
   ```

## Configuration

### Environment Variables

See `env.example` for all available configuration options.

### Key Configuration

- **Port**: 8083 (configurable via `SERVER_PORT`)
- **Context Path**: `/admin` (configurable via `SPRING_BOOT_ADMIN_SERVER_CONTEXT_PATH`)
- **Security**: Disabled for development (configurable via `SecurityConfig`)
- **Monitoring**: 10-second intervals (configurable via `SPRING_BOOT_ADMIN_SERVER_MONITOR_PERIOD`)

## Monitored Applications

This admin server automatically discovers and monitors:

1. **user-rest-spring-service** (Port 28080)
2. **user-thrift-server-service** (Port 28082)
3. **user-watcher-service** (Port 8081)

## Monitoring Features

### Health Checks
- Application status (UP/DOWN)
- Component health (Database, Redis, Kafka, etc.)
- Custom health indicators

### Metrics
- JVM metrics (Memory, GC, Threads)
- HTTP request metrics
- Database connection metrics
- Cache hit/miss ratios
- Circuit breaker metrics

### Logs
- Real-time log viewing
- Log level management
- Log filtering and search

### Environment
- Configuration properties
- System properties
- Environment variables
- Bean definitions

## Security

For development purposes, security is disabled to allow easy access. In production:

1. Enable Spring Security
2. Configure proper authentication
3. Set up role-based access control
4. Use HTTPS

## Troubleshooting

### Applications Not Appearing

1. Check that client applications have Spring Boot Admin Client dependency
2. Verify actuator endpoints are exposed
3. Check network connectivity between admin server and clients
4. Review application logs for registration errors

### Performance Issues

1. Adjust monitoring intervals in configuration
2. Reduce the number of exposed actuator endpoints
3. Configure log retention policies
4. Monitor admin server resource usage

## Development

### Testing

- **Unit tests**: `./gradlew test`
- **Integration tests**: `./gradlew integrationTest`
- **E2E tests**: `./gradlew e2eTest`

### Customization

- Add custom health indicators
- Configure custom metrics
- Create custom monitoring dashboards
- Implement custom notifications
