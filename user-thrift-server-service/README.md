# User Thrift Server Service

Thrift RPC server that processes user operations and publishes events to Kafka.

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
   - Thrift RPC: localhost:9090
   - HTTP Actuator: http://localhost:8080/actuator/health
   - Kafka UI: http://localhost:8084

### Running with Docker

1. **Build the image**:

   ```bash
   ./gradlew buildDocker
   ```

2. **Start everything**:
   ```bash
   docker compose up -d
   ```

### Building User Contracts Library

This service contains the `user-contracts` subproject:

```bash
# Build and publish contracts library
./gradlew :user-contracts:publishToMavenLocal

# Generate Thrift classes
./gradlew :user-contracts:generateThrift
```

### Key Dependencies

- **user-contracts**: Internal subproject containing Thrift definitions
- **Spring Boot**: Web, Actuator, Kafka
- **Apache Thrift**: RPC framework
- **Kafka**: Message broker
- **Redis**: For caching and RPC state

### Architecture

```
Thrift Client → Thrift Server → Kafka → Watcher Service
                      ↓
                   Redis Cache
```

### Thrift Services

- `UserService.ping()` - Health check
- `UserService.createUser()` - Create new user
- `UserService.getUser()` - Get user by ID
- `UserService.updateUser()` - Update existing user
- `UserService.deleteUser()` - Delete user
- `UserService.listUsers()` - List users with pagination

### Kafka Topics

- `user.create.request/response`
- `user.get.request/response`
- `user.update.request/response`
- `user.delete.request/response`
- `user.list.request/response`

### Development Notes

- Contains `user-contracts` library as subproject
- Publishes Thrift contracts to Maven Local for other services
- Uses Redis for caching and operation tracking
- All RPC calls are bridged to Kafka for async processing
