# E2E Testing Guide for User REST Spring Service

This guide explains how to run end-to-end tests for the User CRUD API endpoints.

## Overview

The E2E testing strategy includes two approaches:

1. **Standalone E2E Tests** - Run against a running system (recommended for development)
2. **Integration Tests with Testcontainers** - Run with containerized dependencies (recommended for CI/CD)

## Prerequisites

- Java 21
- Gradle
- Docker and Docker Compose
- All services running via `docker-compose up -d`

## Test Structure

```
user-rest-spring-service/src/test/java/com/example/rest/user/e2e/
├── UserE2EStandaloneTest.java      # Tests against running system
└── UserE2ETest.java               # Tests with Testcontainers
```

## Running E2E Tests

### Option 1: Standalone E2E Tests (Recommended)

These tests run against your running system and are faster to execute.

#### Step 1: Start the System

```bash
# From project root
docker-compose up -d

# Wait for all services to be healthy
docker-compose ps
```

#### Step 2: Run E2E Tests

```bash
# From project root
./gradlew :user-rest-spring-service:e2eTest

# Or using the convenience script
cd user-rest-spring-service
./run-e2e-tests.sh  # Linux/Mac
# or
run-e2e-tests.bat   # Windows
```

### Option 2: Integration Tests with Testcontainers

These tests spin up their own containers and are more isolated.

```bash
# From project root
./gradlew :user-rest-spring-service:integrationTest
```

## Test Coverage

### Valid Cases

- ✅ `GET /users/ping` - Health check
- ✅ `POST /users` - Create user
- ✅ `GET /users` - List users with pagination
- ✅ `GET /users/{id}` - Get specific user
- ✅ `PUT /users/{id}` - Update user
- ✅ `DELETE /users/{id}` - Delete user

### Invalid Cases

- ❌ `POST /users` - Invalid input validation
- ❌ `GET /users/{id}` - Non-existent user (404)
- ❌ `PUT /users/{id}` - Non-existent user (404)
- ❌ `DELETE /users/{id}` - Non-existent user (404)
- ❌ `POST /users` - Missing required fields

## Test Configuration

### Application Properties

- `application-test.yml` - Test-specific configuration
- Disables caching for predictable test results
- Uses test-specific logging levels

### Test Dependencies

- **RestAssured** - HTTP client for API testing
- **JUnit 5** - Test framework
- **AssertJ** - Assertion library
- **Testcontainers** - Container management (for integration tests)

## Test Execution Order

Tests are executed in a specific order using `@Order` annotations:

1. **Ping Test** - Verify system is responsive
2. **Create User** - Create a test user
3. **List Users** - Verify user appears in list
4. **Get User** - Retrieve the created user
5. **Update User** - Modify the user
6. **Delete User** - Remove the user
7. **Invalid Cases** - Test error scenarios

## Troubleshooting

### Common Issues

#### 1. System Not Running

```
❌ System is not running. Please start it first:
   docker-compose up -d
```

**Solution**: Start the system and wait for all services to be healthy.

#### 2. Service Not Healthy

```
❌ user-rest-spring-service is not healthy
```

**Solution**: Check service logs and ensure all dependencies are running.

#### 3. Port Conflicts

```
Connection refused: localhost:8083
```

**Solution**: Ensure the service is running on the expected port.

### Debug Mode

Run tests with verbose output:

```bash
./gradlew :user-rest-spring-service:e2eTest --info --debug
```

### Manual Testing

You can also test the API manually:

```bash
# Health check
curl http://localhost:8083/users/ping

# Create user
curl -X POST http://localhost:8083/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","phone":"+1234567890","address":"Test Address"}'

# List users
curl http://localhost:8083/users?page=0&size=10

# Get user (replace {id} with actual user ID)
curl http://localhost:8083/users/{id}

# Update user
curl -X PUT http://localhost:8083/users/{id} \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated User","phone":"+1234567890","address":"Updated Address"}'

# Delete user
curl -X DELETE http://localhost:8083/users/{id}
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: E2E Tests
on: [push, pull_request]

jobs:
  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: "21"
          distribution: "temurin"
      - name: Start services
        run: docker-compose up -d
      - name: Wait for services
        run: sleep 30
      - name: Run E2E tests
        run: ./gradlew :user-rest-spring-service:e2eTest
      - name: Cleanup
        run: docker-compose down
```

## Best Practices

1. **Test Isolation**: Each test should be independent
2. **Clean State**: Tests should clean up after themselves
3. **Realistic Data**: Use realistic test data
4. **Error Scenarios**: Test both success and failure cases
5. **Performance**: Monitor test execution time
6. **Logging**: Use appropriate log levels for tests

## Monitoring and Metrics

The tests include correlation IDs for tracking requests across the system. You can monitor:

- Request/response times
- Error rates
- System health
- Kafka message flow
- Database operations

## Next Steps

- Add performance tests for load testing
- Add security tests for authentication/authorization
- Add contract tests for API compatibility
- Add chaos engineering tests for resilience
