# User Watcher Service

Kafka consumer service that processes user events and performs CRUD operations on MongoDB.

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
   - HTTP Actuator: http://localhost:8080/actuator/health
   - MongoDB Express: http://localhost:8081

### Running with Docker

1. **Build the image**:

   ```bash
   ./gradlew buildDocker
   ```

2. **Start everything**:
   ```bash
   docker compose up -d
   ```

### Key Dependencies

- **user-contracts**: `com.example:user-contracts:1.0.0` (from Maven Local)
- **Spring Boot**: Web, Actuator, Data MongoDB
- **MongoDB**: Document database
- **Kafka**: Message consumer
- **Redis**: For caching

### Architecture

```
Kafka Topics → Watcher Service → MongoDB
                     ↓
                 Redis Cache
```

### Kafka Consumers

The service listens to these Kafka topics:

- `user.create.request` → Process user creation
- `user.get.request` → Process user retrieval
- `user.update.request` → Process user updates
- `user.delete.request` → Process user deletion
- `user.list.request` → Process user listing

### MongoDB Collections

- `users` - Main user documents
- Additional indexes on `id`, `email`, `status`

### Key Features

- **Event-driven architecture**: Processes Kafka events
- **MongoDB integration**: Full CRUD operations
- **Caching layer**: Redis for performance
- **Health monitoring**: Spring Actuator endpoints
- **Saga orchestration**: Supports complex workflows

### Development Notes

- Designed to be completely independent
- Uses `user-contracts` for Thrift message types
- Implements outbox pattern for reliable event processing
- Supports both sync and async processing modes
- Includes saga state management for complex operations
