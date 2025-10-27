# Config Control Service - Test Execution Guide

This guide explains how to run the various test suites for the config-control-service.

## Prerequisites

1. **Docker and Docker Compose** - For running the full stack
2. **Java 21** - For running unit tests
3. **Gradle** - For building the project
4. **jq** - For JSON processing in shell scripts

## Test Types

### 1. Unit Tests

Run all unit tests:

```bash
./gradlew test
```

Run specific test class:

```bash
./gradlew test --tests Auth2E2Test
```

### 2. Integration Tests

Run integration tests:

```bash
./gradlew integrationTest
```

### 3. Security Smoke Tests

The security smoke test validates authentication and authorization:

```bash
# Start the full stack
docker compose up -d

# Wait for services to be ready (about 30-60 seconds)
sleep 60

# Run security smoke test
./config-control-service/test/keycloak/security-smoke-test.sh
```

**Expected Results:**
- All endpoints return expected status codes
- Authentication works correctly
- Authorization prevents unauthorized access

### 4. CRUD Smoke Tests

The CRUD smoke test validates comprehensive business operations:

```bash
# Ensure services are running
docker compose up -d

# Wait for services to be ready
sleep 60

# Run CRUD smoke test
./config-control-service/test/crud-smoke-test.sh
```

**Expected Results:**
- Admin can CRUD all resources
- Users can CRUD resources in owned team services
- Users cannot access other team resources without share
- Service shares grant correct permissions
- Approval workflow completes successfully
- Service ownership transfer works correctly
- DriftEvent creation respects permissions
- Unauthorized access properly rejected

### 5. End-to-End Authentication Tests

Run the Java-based E2E authentication test:

```bash
./gradlew test --tests Auth2E2Test
```

## Test Data Setup

### Keycloak Users

The following test users are automatically created:

| Username | Password | Teams | Roles |
|----------|----------|-------|-------|
| admin | admin123 | - | SYS_ADMIN |
| user1 | user123 | team_core | USER |
| user2 | user123 | team_analytics | USER |
| user3 | user123 | team_infrastructure | USER |

### Seed Data

In `dev` profile, the following seed data is automatically created:

**ApplicationServices:**
- `payment-service` (owned by team_core)
- `analytics-service` (owned by team_analytics)
- `orphan-service` (no owner - for testing ownership requests)
- `infrastructure-service` (owned by team_infrastructure)

**ServiceShares:**
- team_infrastructure can VIEW_INSTANCE and VIEW_DRIFT payment-service (dev, staging)
- team_core can VIEW_SERVICE and VIEW_INSTANCE analytics-service (dev)

**ServiceInstances:**
- payment-service instances in dev environment
- analytics-service instances in dev environment
- orphan-service instances in dev environment

**DriftEvents:**
- Sample drift events for testing

## Troubleshooting

### Common Issues

1. **Services not ready**
   ```bash
   # Check service health
   curl http://localhost:8081/actuator/health
   curl http://localhost:8080/realms/config-control
   ```

2. **Authentication failures**
   ```bash
   # Check Keycloak logs
   docker compose logs keycloak
   ```

3. **Permission errors**
   ```bash
   # Check application logs
   docker compose logs config-control-service
   ```

4. **Database connection issues**
   ```bash
   # Check MongoDB logs
   docker compose logs mongodb
   ```

### Debug Mode

Enable debug logging:

```bash
# Set environment variable
export LOGGING_LEVEL_COM_EXAMPLE_CONTROL=DEBUG

# Restart services
docker compose restart config-control-service
```

### Clean Restart

If tests fail due to stale data:

```bash
# Stop and remove all containers
docker compose down -v

# Remove any local volumes
docker volume prune -f

# Start fresh
docker compose up -d

# Wait for services
sleep 60
```

## Test Scenarios Covered

### Authentication & Authorization
- [x] JWT token validation
- [x] User context extraction
- [x] Team membership validation
- [x] Role-based access control
- [x] SYS_ADMIN permissions

### Service Management
- [x] ApplicationService CRUD operations
- [x] ServiceInstance CRUD operations
- [x] DriftEvent CRUD operations
- [x] Service ownership validation
- [x] Team-based access control

### Service Sharing
- [x] Grant service shares
- [x] Permission-based access
- [x] Environment filtering
- [x] Share expiration
- [x] Share revocation

### Approval Workflow
- [x] Ownership transfer requests
- [x] Multi-gate approval logic
- [x] SYS_ADMIN and LINE_MANAGER gates
- [x] Approval decision tracking
- [x] Cascade ownership updates

### Data Consistency
- [x] Team ownership changes
- [x] Service instance linking
- [x] Drift event association
- [x] Share permission inheritance

## Performance Testing

For load testing, use tools like Apache Bench or JMeter:

```bash
# Example load test
ab -n 1000 -c 10 -H "Authorization: Bearer $TOKEN" \
   http://localhost:8081/api/application-services
```

## Security Testing

Additional security tests can be performed:

1. **Token manipulation** - Test with invalid/expired tokens
2. **Permission escalation** - Attempt to access restricted resources
3. **SQL injection** - Test input validation
4. **Rate limiting** - Test API rate limits

## Continuous Integration

For CI/CD pipelines, use:

```bash
# Full test suite
./gradlew build test integrationTest

# Docker build and test
./gradlew buildDocker
docker compose up -d
./config-control-service/test/crud-smoke-test.sh
docker compose down
```
