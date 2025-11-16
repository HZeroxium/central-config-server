# Configuration Documentation - Resource Management

## Overview

This documentation explains resource-related configurations in `config-control-service`. The focus is on understanding **what each configuration means** and how it affects resource usage (CPU, memory, threads, connections, network I/O).

This documentation does not provide tuning recommendations, as those require SLO/SLA metrics and workload analysis that are not yet available.

## Purpose

Understanding configuration options helps:
- Identify which settings contribute to resource usage
- Understand trade-offs between different resource types
- Make informed decisions when observing resource metrics
- Prepare for future tuning based on actual workload patterns

## Configuration File Structure

Configuration is split across multiple files organized by concern:

| File | Purpose | Resource Categories |
|------|---------|-------------------|
| `application.yml` | Main configuration, HTTP server settings | HTTP Server |
| `application-app.yml` | Application-specific settings (cache, async) | Cache, Executor Pools |
| `application-messaging.yml` | Kafka and Spring Cloud Stream | Messaging |
| `application-datasources.yml` | Database connections | Database Connections |
| `application-infrastructure.yml` | External service clients (Config Server, Consul) | HTTP Clients, RPC Servers |
| `application-resilience.yml` | Circuit breakers, retries, bulkheads | Resilience |
| `application-security.yml` | Security and service discovery | - |
| `application-observability.yml` | Metrics and tracing | - |
| `application-mail.yml` | Email configuration | - |

## Resource Categories

### [HTTP Server](http-server.md)
Tomcat embedded server configuration including thread pool, connection limits, and timeouts.

**Key Settings:**
- Thread pool: `server.tomcat.threads.max`, `server.tomcat.threads.min-spare`
- Connection limits: `server.tomcat.max-connections`, `server.tomcat.accept-count`
- Connection timeout: `server.tomcat.connection-timeout`

### [Messaging](messaging.md)
Kafka producer/consumer configuration, Spring Cloud Stream/Bus, and heartbeat processing.

**Key Settings:**
- Producer batching: `spring.kafka.producer.batch.size`, `spring.kafka.producer.linger.ms`
- Consumer polling: `spring.kafka.consumer.max-poll-records`, `spring.kafka.consumer.fetch-*`
- Heartbeat consumer: `app.heartbeat.kafka.consumer.concurrency`, `app.heartbeat.kafka.consumer.max-poll-records`

### [Database Connections](database-connections.md)
MongoDB and Redis connection pool configuration.

**Key Settings:**
- MongoDB pool: `spring.data.mongodb.options.max-pool-size`, `spring.data.mongodb.options.min-pool-size`
- Redis pool: `spring.data.redis.lettuce.pool.max-active`, `spring.data.redis.lettuce.pool.max-idle`

### [Cache](cache.md)
Caffeine and Redis cache configuration, including two-level cache and per-cache settings.

**Key Settings:**
- Cache provider: `app.cache.provider` (CAFFEINE/REDIS/NOOP)
- Caffeine limits: `app.cache.caffeine.maximum-size`, `app.cache.caffeine.expire-after-*`
- Redis settings: `app.cache.redis.default-ttl`, `app.cache.redis.transaction-aware`
- Per-cache: `app.cache.caches.*`

### [Executor Pools](executor-pools.md)
Async task executor configuration for different workload types (notifications, RPC, config fetching).

**Key Settings:**
- Notification executor: `app.async.notification.*` (virtual threads enabled)
- Default executor: `app.async.default.*`
- Config hash fetch: `app.async.config-hash-fetch.*`
- Shutdown: `app.async.shutdown.*`

### [RPC Servers](rpc-servers.md)
Thrift and gRPC server thread pools and port configuration.

**Key Settings:**
- Thrift port: `rpc.server.thrift-port`
- gRPC port: `rpc.server.grpc-port`
- Thrift thread pool: Hardcoded in `ThriftServer.java` (20-200 threads)

### [Resilience](resilience.md)
Circuit breakers, retries, bulkheads, time limiters, and rate limiters.

**Key Settings:**
- Circuit breaker: `resilience4j.circuitbreaker.*`
- Retry: `resilience4j.retry.*`
- Bulkhead: `resilience4j.bulkhead.*`, `resilience4j.thread-pool-bulkhead.*`
- Time limiter: `resilience4j.timelimiter.*`
- Rate limiter: `resilience4j.ratelimiter.*`

### [HTTP Clients](http-clients.md)
RestClient timeout configuration, Config Server client, Consul client, and Keycloak Admin client.

**Key Settings:**
- RestClient timeouts: `rest-client.connect-timeout`, `rest-client.read-timeout`, `rest-client.write-timeout`
- Config Server: `config-server.timeout`, `config-server.retry.*`
- Consul: `consul.timeout`, `consul.retry.*`

## Quick Reference

| Resource Type | Configuration File | Key Property Prefix |
|--------------|-------------------|---------------------|
| HTTP Server | `application.yml` | `server.tomcat.*` |
| Messaging | `application-messaging.yml` | `spring.kafka.*`, `app.heartbeat.kafka.*` |
| Database | `application-datasources.yml` | `spring.data.mongodb.*`, `spring.data.redis.*` |
| Cache | `application-app.yml` | `app.cache.*` |
| Executor Pools | `application-app.yml` | `app.async.*` |
| RPC Servers | `application-infrastructure.yml` | `rpc.server.*` |
| Resilience | `application-resilience.yml` | `resilience4j.*` |
| HTTP Clients | `application-infrastructure.yml` | `rest-client.*`, `config-server.*`, `consul.*` |

## Resource Impact Summary

| Category | CPU Impact | Memory Impact | Thread Impact | Connection Impact | Network I/O Impact |
|----------|-----------|---------------|---------------|-------------------|-------------------|
| HTTP Server | High (thread context switching) | Medium (thread stacks, buffers) | High (200 max threads) | High (10000 max connections) | High (request/response) |
| Messaging | Medium (compression, serialization) | High (batching buffers) | Medium (consumer threads) | Medium (Kafka connections) | High (message streaming) |
| Database | Low | Medium (connection objects) | Low | High (pool size) | High (query/response) |
| Cache | Low | High (Caffeine heap, Redis network) | Low | Medium (Redis connections) | Medium (Redis I/O) |
| Executor Pools | Medium (context switching) | Medium (thread stacks) | High (multiple pools) | Low | Low |
| RPC Servers | Medium (thread context switching) | Medium (thread stacks) | Medium (200 max for Thrift) | Low | Medium (RPC calls) |
| Resilience | Low | Medium (buffering) | Medium (thread pool bulkhead) | Low | Medium (retries) |
| HTTP Clients | Low | Low (connection objects) | Low | Medium (connection pools) | Medium (client requests) |

## Notes

- All configurations use environment variable overrides via `${ENV_VAR:default}` syntax
- Profile-specific files (e.g., `application-prod.yml`) may override base settings
- Some settings are hardcoded in Java classes (e.g., Thrift thread pool) and not configurable via YAML
- Virtual threads (Java 21+) are used for I/O-bound tasks (notifications) to reduce thread overhead
- Configuration values are defaults; actual values depend on environment variables and profiles

## Related Documentation

- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [Spring Cloud Stream Documentation](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [MongoDB Java Driver Configuration](https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/connection/)
- [Lettuce Redis Client](https://github.com/lettuce-io/lettuce-core)

