# User REST Spring Service (Java 21 + Spring Boot)

Architecture

- Feature-oriented packages: `user` (controller, service, port), `adapter.thrift` for outbound calls.
- Clean Architecture: domain model + ports; adapters wire Spring specifics.

Build

- Prereqs: Java 21, Gradle, thrift compiler in PATH.
- Commands:
  - `./gradlew spotlessApply check build`

Run

- Configure Thrift server location via env vars:
  - `THRIFT_HOST=localhost THRIFT_PORT=9090`
- Start: `./gradlew bootRun`

Endpoints

- `GET /users/ping`
- `GET /users/{id}`, `POST /users`, `PUT /users/{id}`, `DELETE /users/{id}`, `GET /users`

Logging & Metrics

- Log4j2 JSON console; Actuator exposes `/actuator/health` and `/actuator/prometheus`.

Tests

- Unit tests mock the Thrift client port. Run: `./gradlew test`
