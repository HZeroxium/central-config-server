# User REST Spring Service

REST API service that acts as a Thrift client, forwarding HTTP requests to the Thrift server.

## Quick Start

### Prerequisites

- JDK 21
- Docker & Docker Compose

### Running Locally

1. **Setup environment**:

   ```bash
   cp env.example .env
   # Edit .env if needed
   ```

2. **Start infrastructure**:

   ```bash
   docker compose up -d
   ```

3. **Build and run the service**:

   ```bash
   ./gradlew bootRun
   ```

4. **Access the service**:
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Actuator: http://localhost:8080/actuator/health

### Running with Docker

1. **Build the image**:

   ```bash
   ./gradlew buildDocker
   ```

2. **Start everything**:
   ```bash
   docker compose up -d
   ```

### Testing

- **Unit tests**: `./gradlew test`
- **Integration tests**: `./gradlew integrationTest`
- **E2E tests**: `./gradlew e2eTest` (requires running system)

### Key Dependencies

- **user-contracts**: `com.example:user-contracts:1.0.0` (from Maven Local)
- **Spring Boot**: Web, Actuator, Validation, Cache
- **Thrift**: For RPC communication
- **Redis**: For caching
- **Resilience4j**: Circuit breaker, retry, timeout

### Environment Variables

See `env.example` for all available configuration options.

### API Endpoints

- `GET /api/v1/users/{id}` - Get user by ID
- `POST /api/v1/users` - Create user
- `PUT /api/v1/users/{id}` - Update user
- `DELETE /api/v1/users/{id}` - Delete user
- `GET /api/v1/users` - List users with pagination

### Development Notes

- This service is designed to be completely independent
- It uses `user-contracts` library for shared types and serializers
- Redis is used for caching responses
- Circuit breaker protects against Thrift server failures
