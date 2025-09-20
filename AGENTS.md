# AGENTS — Java Spring (JDK 21, Gradle) — Microservices, Gateway, Discovery, Config, Bus

## Default Stack (extended)
- Spring Boot 3.x (Web, Validation, Actuator), Spring Data JPA/JDBC, Flyway.
- Security: Spring Security 6 (OAuth2 Resource Server, JWT).
- Messaging: Spring for Apache Kafka.
- Resilience: Spring Cloud CircuitBreaker (Resilience4j), TimeLimiter, Bulkhead, Retry.
- HTTP Clients: Spring Cloud OpenFeign with per-client timeouts and circuit breakers.
- Config: Spring Cloud Config (Git-backed), @ConfigurationProperties, strict validation.
- Dynamic Config: Spring Cloud Bus (Kafka/Rabbit) with controlled `/actuator/busrefresh`.
- Discovery: Eureka client with instance metadata; DiscoveryClient with client-side load balancing.
- Gateway: Spring Cloud Gateway with per-route filters, auth offload, rate limiting, and circuit breakers.
- Caching: Redis (Lettuce) + Caffeine.
- Observability: Micrometer + OpenTelemetry (OTLP), structured JSON logs.
- Testing: JUnit 5, AssertJ, Mockito, Spring Boot Test slices, Testcontainers, contract tests.
- Build: Gradle Kotlin DSL, Spotless, Checkstyle/PMD/SpotBugs, Error Prone.
- Delivery: Layered Docker images; Helm/Kustomize; canary/blue-green with SLO gates.

## Task Playbooks

### Implement a New Service (microservice)
1. Generate module skeleton (hexagonal layout).
2. Expose REST endpoints with DTOs; OpenAPI first.
3. Wire persistence (entities/repositories, Flyway migration).
4. Register with Eureka; add health/readiness.
5. Add outbound clients via OpenFeign; define per-client resilience policies.
6. Add metrics/tracing/logs; update CI (slices, integration, contract tests).
7. Package container; Helm chart values; set probes; deploy canary via Gateway route.
8. Document ADR and README.

### Add/Change Config
1. Commit changes to config repo with env-scoped overlays.
2. Staging test with refresh disabled, then enable targeted `/actuator/busrefresh`.
3. Validate post-change metrics (latency/error) and logs; roll back on budget burn.

### Harden an Edge Route (Gateway)
1. Identify route; set predicates and per-route timeouts.
2. Add auth filter, request size limits, schema guard, and circuit breaker + fallback.
3. Configure rate limiter (burst/sustained); add metrics and alerts.
4. Canary route; promote after SLO observation window.

### Adopt/Refactor for Tracing
1. Replace Sleuth with Micrometer Tracing; ensure W3C propagation across Gateway/Kafka.
2. Update dashboards/alerts and sampling; validate end-to-end traces in staging.

## Checklists

### Discovery & Clients
- ServiceId stable; metadata includes version/zone.
- Client configs: connection pool, timeouts, breaker, retry (idempotent only).
- Prefer logical names over static URLs.

### Config & Bus
- @ConfigurationProperties with validation; secrets external.
- Bus refresh gated and audited; RefreshScope only for safe beans.

### Gateway
- Per-route policies; auth offload; rate limits; backpressure for fan-out.
- Problem+json errors; fallback responses typed and observable.

### Resilience
- Circuit breaker and time limiter per client; bulkhead for noisy neighbors.
- Exponential backoff with jitter; retry budgets; no retry for non-idempotent ops.

### Kubernetes Probes
- Enable actuator probes; distinct readiness/liveness/startup.
- Readiness reflects dependencies; safe thresholds documented.

## PR Expectations (unchanged but enforced for microservices)
- CI green (lint/unit/integration/contract/coverage/SCA).
- Include route/service/resilience configs and Helm updates.
- Include metrics/tracing additions; attach SLO impact notes for edge or config changes.

## When Unsure
- Provide two options with explicit trade-offs (latency, error budgets, cost, operability); recommend one.
- Attach rollback plan and monitoring signals to revisit after rollout.
