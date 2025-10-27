# E2E Testing Framework

This directory contains the End-to-End (E2E) testing framework for the Config Control Service. The framework provides comprehensive testing capabilities for all API endpoints and business workflows.

## Overview

The E2E testing framework is designed to test the complete application against running services without mocking dependencies. It includes:

- **Smoke Tests**: Critical happy path scenarios
- **Workflow Tests**: End-to-end business scenarios  
- **Regression Tests**: Comprehensive coverage for all endpoints
- **Authentication & Authorization**: Token management and security testing
- **Allure Reporting**: Detailed test reports with request/response capture

## Architecture

### Test Structure

```
src/e2eTest/
├── java/com/example/control/e2e/
│   ├── base/                    # Base infrastructure
│   │   ├── BaseE2ETest.java    # Abstract base class
│   │   ├── TestConfig.java     # Configuration management
│   │   └── AuthTokenManager.java # Token management
│   ├── client/                 # API clients
│   │   ├── ApiClient.java      # REST Assured wrapper
│   │   └── HealthCheckClient.java # Health check utilities
│   ├── fixtures/               # Test data generation
│   │   ├── TestDataGenerator.java # Unique data generation
│   │   └── TestUsers.java      # User constants
│   ├── smoke/                  # Smoke tests
│   │   ├── AuthenticationSmokeTest.java
│   │   ├── ApplicationServiceSmokeTest.java
│   │   ├── ServiceInstanceSmokeTest.java
│   │   └── ApprovalRequestSmokeTest.java
│   ├── workflows/              # Workflow tests
│   │   ├── ServiceOwnershipWorkflowTest.java
│   │   ├── ServiceSharingWorkflowTest.java
│   │   ├── DriftDetectionWorkflowTest.java
│   │   └── MultiGateApprovalWorkflowTest.java
│   └── regression/             # Regression tests
│       ├── ApplicationServiceRegressionTest.java
│       ├── ApprovalRequestRegressionTest.java
│       ├── ServiceShareRegressionTest.java
│       ├── ServiceInstanceRegressionTest.java
│       ├── DriftEventRegressionTest.java
│       ├── IamUserRegressionTest.java
│       ├── IamTeamRegressionTest.java
│       └── SeederRegressionTest.java
└── resources/
    ├── test-config.properties  # Test configuration
    ├── allure.properties       # Allure configuration
    └── logback-test.xml        # Logging configuration
```

## Configuration

### Test Configuration

The framework uses `test-config.properties` for configuration:

```properties
# Service URLs
api.base.url=http://localhost:8081/api
keycloak.base.url=http://localhost:8080
keycloak.realm=config-control

# Test Users
test.admin.username=admin
test.admin.password=admin123
test.user1.username=user1
test.user1.password=user123
# ... user2-5

# Timeouts
health.check.timeout.seconds=120
api.request.timeout.seconds=30
```

### Test Users

The framework includes predefined test users:

- **admin**: System administrator with SYS_ADMIN role
- **user1**: Team1 member, manager (no manager_id)
- **user2**: Team1 member, reports to user1 (manager_id set)
- **user3**: Team2 member, team lead (no manager_id)
- **user4**: Team2 member, reports to user3 (manager_id set)
- **user5**: No team membership, can request ownership

All users have additional attributes: employee_id, phone, department, job_title, office_location, hire_date.

## Running Tests

### Prerequisites

1. **Docker & Docker Compose**: For running infrastructure services
2. **Java 21**: For running the application and tests
3. **Gradle**: For building and running tests
4. **curl**: For health checks

### Quick Start

Use the provided script for complete orchestration:

```bash
./run-e2e-tests.sh
```

This script will:
1. Build the application
2. Start all required services
3. Run E2E tests
4. Generate Allure reports
5. Clean up resources

### Manual Execution

1. **Start Services**:
   ```bash
   # Start Keycloak
   docker-compose -f docker-compose.kc.yml up -d
   
   # Start main services
   docker-compose up -d
   ```

2. **Run Tests**:
   ```bash
   ./gradlew e2eTest
   ```

3. **Generate Reports**:
   ```bash
   ./gradlew allureReport
   ```

4. **View Reports**:
   ```bash
   ./gradlew openAllureReport
   ```

### Test Categories

#### Smoke Tests
Critical functionality tests that should pass for basic system health:

```bash
./gradlew e2eTest --tests "*SmokeTest"
```

#### Workflow Tests
End-to-end business scenario tests:

```bash
./gradlew e2eTest --tests "*WorkflowTest"
```

#### Regression Tests
Comprehensive coverage tests:

```bash
./gradlew e2eTest --tests "*RegressionTest"
```

## Test Patterns

### Authentication

All tests extend `BaseE2ETest` which provides automatic token management:

```java
public class MyTest extends BaseE2ETest {
    @Test
    void myTest() {
        // Tokens are automatically available
        String adminToken = getAdminToken();
        String user1Token = getUser1Token();
        
        // Use ApiClient for requests
        Response response = ApiClient.given(adminToken)
            .when()
            .get("/application-services")
            .then()
            .statusCode(200)
            .extract().response();
    }
}
```

### Test Data Generation

Use `TestDataGenerator` for unique test data:

```java
String serviceName = TestDataGenerator.generateServiceName();
String instanceId = TestDataGenerator.generateInstanceId();
Map<String, Object> testData = TestDataGenerator.generateTestAttributes();
```

### Allure Reporting

Tests automatically capture request/response data for Allure reports:

```java
@Step("Create Service")
@Description("Create a new application service")
void createService() {
    // Test implementation
    logTestData("Service Name", serviceName);
    logTestResult("Service Created", "Service created successfully");
}
```

## Troubleshooting

### Common Issues

1. **Services Not Ready**:
   - Check Docker containers: `docker ps`
   - Check service logs: `docker-compose logs`
   - Verify health endpoints manually

2. **Authentication Failures**:
   - Verify Keycloak is running: `curl http://localhost:8080/realms/config-control`
   - Check user credentials in `test-config.properties`
   - Verify JWT token format

3. **Test Failures**:
   - Check test logs in `build/test.log`
   - Verify service endpoints are accessible
   - Check database connectivity

### Debug Mode

Enable debug logging by setting system properties:

```bash
./gradlew e2eTest -Dlogging.level.com.example.control.e2e=DEBUG
```

### Health Checks

Manual health check commands:

```bash
# Keycloak
curl http://localhost:8080/realms/config-control

# Config Control Service
curl http://localhost:8081/actuator/health

# MongoDB
curl http://localhost:27017

# Redis
curl http://localhost:6379
```

## Best Practices

### Test Design

1. **Use Descriptive Names**: Test methods should clearly describe what they test
2. **Single Responsibility**: Each test should verify one specific behavior
3. **Independent Tests**: Tests should not depend on each other
4. **Cleanup**: Always clean up test data after tests

### Data Management

1. **Unique Data**: Use `TestDataGenerator` for unique identifiers
2. **Test Isolation**: Each test should use its own data
3. **Cleanup**: Clean up created resources in `@AfterEach` methods

### Error Handling

1. **Meaningful Assertions**: Use descriptive assertion messages
2. **Error Context**: Include context in error messages
3. **Logging**: Use appropriate logging levels

## Contributing

### Adding New Tests

1. **Choose Category**: Determine if it's smoke, workflow, or regression
2. **Extend BaseE2ETest**: Use the base class for common functionality
3. **Follow Patterns**: Use existing tests as templates
4. **Add Documentation**: Include JavaDoc and Allure annotations

### Test Data

1. **Use TestDataGenerator**: For unique test data
2. **Use TestUsers**: For predefined user constants
3. **Clean Up**: Always clean up test data

### Reporting

1. **Use Allure Annotations**: `@Step`, `@Description`, `@Epic`, `@Feature`, `@Story`
2. **Log Test Data**: Use `logTestData()` for important data
3. **Log Results**: Use `logTestResult()` for outcomes

## Support

For issues or questions:

1. Check the logs in `build/` directory
2. Review Allure reports for detailed test information
3. Verify service health and connectivity
4. Check configuration in `test-config.properties`

## License

This E2E testing framework is part of the Config Control Service project and follows the same licensing terms.
