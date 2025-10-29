# Seeding Package

This package contains the mock data generation system for Config Control Service.

## Quick Start

```bash
# Enable seeding in application.yml
seeding:
  enabled: true
  auto-run-on-startup: true

# Or use seed-data profile
export SPRING_PROFILES_ACTIVE=seed-data
```

## Package Structure

- **config/** - Spring configuration and properties binding
- **factory/** - Data generation factories (one per domain entity)
- **service/** - Orchestration and persistence services
- **runner/** - ApplicationRunner for auto-seeding on startup

## Key Classes

- `SeederConfig` - Enables seeding system and creates Faker bean
- `SeederConfigProperties` - Type-safe configuration properties
- `MockDataGenerator` - Orchestrates all factories and generates complete dataset
- `DataSeederService` - Persists generated data to MongoDB
- `SeederApplicationRunner` - Auto-runs seeding on startup (conditional)

## REST API (when enabled)

- `DELETE /api/admin/seed/clean` - Remove all mock data
- `POST /api/admin/seed/seed` - Generate and save new mock data
- `POST /api/admin/seed/clean-and-seed` - Atomic clean-then-seed

## Documentation

See [SEEDING_GUIDE.md](../SEEDING_GUIDE.md) for comprehensive documentation.
