# Data Seeding
## Centralized Configuration Management System

---

## Overview

The system provides comprehensive data seeding capabilities for development, testing, and demonstration purposes. Seeding can be performed automatically on startup or manually via API endpoints, with support for clean-and-seed or seed-only operations.

### Purpose

- **Development Setup**: Quickly populate database with realistic test data
- **Testing**: Generate consistent test datasets for integration/E2E tests
- **Demos**: Create sample data for presentations and demonstrations
- **Local Development**: Bootstrap local environment with meaningful data

---

## Features

### 1. Auto-Seeding on Startup

**Configuration:** `application-app.yml:144-204`

**Behavior:**
- Automatically seeds database when application starts
- Configurable via `seeding.enabled` and `seeding.auto-run-on-startup`
- Executes after Spring Boot context initialization
- Failures logged but don't prevent application startup

**Configuration:**
```yaml
seeding:
  enabled: true
  auto-run-on-startup: true
  clean-before-seed: true
```

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/seeding/runner/SeederApplicationRunner.java`

### 2. Clean and Seed Operations

**Clean Operation:**
- Removes all domain data (ApplicationService, ServiceInstance, DriftEvent, etc.)
- Preserves IAM data (Keycloak users, teams)
- Transactional integrity
- Selective cleaning (respects referential integrity)

**Seed Operation:**
- Generates mock data using `MockDataGenerator`
- Inserts data in dependency order
- Transactional integrity
- Idempotent (can be run multiple times)

**Combined Operation:**
- Clean-then-seed in single transaction
- Ensures fresh dataset
- Most common use case for development

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/seeding/service/DataSeederService.java`

### 3. Mock Data Generation

**Components:**
- `MockDataGenerator` - Main data generation engine
- `KeycloakUserResolver` - Resolves user IDs from Keycloak
- Realistic data generation with proper relationships

**Generated Data:**
- Application Services (with team ownership)
- Service Instances (linked to services)
- Drift Events (with various statuses and severities)
- Service Shares (with permissions)
- Approval Requests (pending, approved, rejected)
- Approval Decisions (linked to requests)
- Key-Value store entries (if enabled)

**Data Distribution:**
- Configurable team counts and service counts
- Orphan services (no owner) for testing ownership workflows
- Multiple instances per service
- Realistic drift events with timestamps
- Service shares with various permissions

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/seeding/service/MockDataGenerator.java`

### 4. Key-Value Store Seeding

**Features:**
- Generates KV entries for each service
- Supports multiple entry types:
  - Leaf entries (simple key-value)
  - List entries (comma-separated or structured)
  - Leaf-list entries (structured lists)
- Category-based distribution (config, secrets, feature-flags)
- Configurable entry counts per service

**Configuration:**
```yaml
seeding:
  kv:
    enabled: true
    clean-before-seed: true
    entries-per-service:
      min: 5
      max: 15
```

**Reference:** `application-app.yml:173-204`

---

## Components

### DataSeederService

**Purpose:** Main service for seeding operations

**Methods:**
- `clean()` - Removes all domain data
- `seed()` - Generates and persists mock data
- `cleanAndSeed()` - Combined clean-then-seed operation

**Features:**
- Transactional operations
- Comprehensive logging
- Mock SecurityContext for audit trail
- Idempotent operations

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/seeding/service/DataSeederService.java`

### MockDataGenerator

**Purpose:** Generates realistic mock data

**Features:**
- Configurable data generation
- Realistic relationships between entities
- Proper referential integrity
- Support for various test scenarios

**Generated Entities:**
- ApplicationService (with environments, lifecycle)
- ServiceInstance (with config hashes, status)
- DriftEvent (with severity, status, timestamps)
- ServiceShare (with permissions, filters)
- ApprovalRequest (with gates, decisions)
- KV entries (with various structures)

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/seeding/service/MockDataGenerator.java`

### SeederApplicationRunner

**Purpose:** Auto-seeding on application startup

**Behavior:**
- Executes after Spring Boot context initialization
- Checks configuration flags
- Performs clean-and-seed or seed-only based on config
- Logs results but doesn't fail startup on errors

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/seeding/runner/SeederApplicationRunner.java`

### SeederController

**Purpose:** Manual seeding via REST API

**Endpoints:**
- `POST /api/admin/seeder/clean` - Clean database
- `POST /api/admin/seeder/seed` - Seed database
- `POST /api/admin/seeder/clean-and-seed` - Clean and seed

**Security:**
- Requires SYS_ADMIN role
- Protected by Spring Security

**Reference:** `config-control-service/src/main/java/com/example/control/api/http/controller/infra/SeederController.java`

---

## Configuration

### Seeding Configuration

**Location:** `application-app.yml:144-204`

**Key Properties:**
```yaml
seeding:
  enabled: false  # Enable only in dev/seed-data profile
  auto-run-on-startup: false
  clean-before-seed: true
  
  data:
    teams:
      count: 2
      ids: [team1, team2]
    services:
      team1-count: 5
      team2-count: 5
      orphan-count: 90  # Mostly orphan services for testing
    instances-per-service:
      min: 5
      max: 10
    drift-events:
      min-per-service: 2
      max-per-service: 5
    shares:
      count: 5
    approval-requests:
      pending: 3
      approved: 4
      rejected: 2
```

### KV Seeding Configuration

```yaml
seeding:
  kv:
    enabled: true
    clean-before-seed: true
    entries-per-service:
      min: 5
      max: 15
    distribution:
      leaf-percentage: 20
      list-percentage: 50
      leaf-list-percentage: 30
    categories:
      config:
        enabled: true
        min-entries: 2
        max-entries: 6
      secrets:
        enabled: true
        min-entries: 1
        max-entries: 3
      feature-flags:
        enabled: true
        min-entries: 1
        max-entries: 4
```

---

## Use Cases

### Development Setup

**Scenario:** Developer needs to start with realistic data

**Steps:**
1. Set `seeding.enabled=true` and `seeding.auto-run-on-startup=true`
2. Start application
3. Database automatically populated with test data

**Result:** Ready-to-use environment with services, instances, and drift events

### Testing

**Scenario:** Integration/E2E tests need consistent test data

**Steps:**
1. Use `DataSeederService` in test setup
2. Call `cleanAndSeed()` before test execution
3. Tests run against known dataset

**Result:** Deterministic test environment

### Demos

**Scenario:** Presentation or demonstration needs sample data

**Steps:**
1. Use manual seeding via API: `POST /api/admin/seeder/clean-and-seed`
2. Or configure auto-seeding for demo environment

**Result:** Realistic dataset for demonstrations

### Local Development

**Scenario:** Developer wants fresh data after schema changes

**Steps:**
1. Call `POST /api/admin/seeder/clean-and-seed`
2. Database reset with new seed data

**Result:** Clean slate with realistic data

---

## Data Generation Details

### Application Services

- Generated with realistic names and metadata
- Assigned to teams or left as orphan
- Multiple environments per service
- Various lifecycle states

### Service Instances

- Linked to ApplicationServices
- Realistic config hashes
- Various statuses (HEALTHY, UNHEALTHY, DRIFT)
- Timestamps and metadata

### Drift Events

- Linked to ServiceInstances
- Various severities (LOW, MEDIUM, HIGH, CRITICAL)
- Statuses (DETECTED, RESOLVED, IGNORED)
- Realistic timestamps

### Service Shares

- Shared between teams/users
- Various permission combinations
- Environment filters
- Expiration dates (some expired, some active)

### Approval Requests

- Pending requests (awaiting approval)
- Approved requests (with decisions)
- Rejected requests (with rejection reasons)
- Multi-gate approval scenarios

---

## Best Practices

1. **Profile-Based**: Enable seeding only in dev/test profiles
2. **Idempotent**: Seeding can be run multiple times safely
3. **Transactional**: Clean-and-seed operations are transactional
4. **Selective**: Clean operation preserves IAM data
5. **Configurable**: All data generation is configurable
6. **Realistic**: Generated data reflects real-world scenarios

---

## API Usage

### Manual Seeding

**Clean Database:**
```bash
curl -X POST http://localhost:8080/api/admin/seeder/clean \
  -H "Authorization: Bearer $TOKEN"
```

**Seed Database:**
```bash
curl -X POST http://localhost:8080/api/admin/seeder/seed \
  -H "Authorization: Bearer $TOKEN"
```

**Clean and Seed:**
```bash
curl -X POST http://localhost:8080/api/admin/seeder/clean-and-seed \
  -H "Authorization: Bearer $TOKEN"
```

**Response:**
```json
{
  "servicesSeeded": 100,
  "instancesSeeded": 750,
  "driftEventsSeeded": 300,
  "sharesSeeded": 5,
  "approvalRequestsSeeded": 9,
  "kvEntriesSeeded": 1000
}
```

---

## References

- [Seeder Service Implementation](../../../config-control-service/src/main/java/com/example/control/infrastructure/seeding/service/DataSeederService.java)
- [Mock Data Generator](../../../config-control-service/src/main/java/com/example/control/infrastructure/seeding/service/MockDataGenerator.java)
- [Seeder Configuration](../../../config-control-service/src/main/resources/application-app.yml:144-204)
- [Seeder README](../../../config-control-service/src/main/java/com/example/control/infrastructure/seeding/README.md)

