# HTTP Clients Configuration

## Overview

RestClient timeout configuration, Config Server client, Consul client, and Keycloak Admin client configuration. These settings control HTTP client timeouts, retries, and connection pooling, affecting connection resources and network I/O.

**Configuration File**: `application-infrastructure.yml` (rest-client.*, config-server.*, consul.*)
**Java Configuration**: `RestClientConfiguration.java`, `KeycloakAdminProperties.java`

## RestClient Timeout Configuration

RestClient is Spring's HTTP client abstraction. Timeout configuration applies to all RestClient instances unless overridden per-client.

### Global RestClient Configuration (`rest-client`)

#### `rest-client.connect-timeout`
- **Default**: `5s`
- **What it does**: Maximum time to wait for establishing a TCP connection
- **Resource impact**:
  - **Network I/O**: Connection attempts that exceed timeout are aborted
  - **Latency**: Adds up to 5s latency for connection establishment
  - **Connection Resources**: Frees connection resources on timeout
- **Configuration location**: `rest-client.connect-timeout` in `application-infrastructure.yml`

#### `rest-client.read-timeout`
- **Default**: `10s`
- **What it does**: Maximum time to wait for reading response data
- **Resource impact**:
  - **Network I/O**: Slow responses are aborted after timeout
  - **Latency**: Adds up to 10s latency for response reading
  - **Connection Resources**: Frees connection resources on timeout
- **Configuration location**: `rest-client.read-timeout` in `application-infrastructure.yml`

#### `rest-client.write-timeout`
- **Default**: `10s`
- **What it does**: Maximum time to wait for writing request data
- **Resource impact**:
  - **Network I/O**: Slow request writes are aborted after timeout
  - **Latency**: Adds up to 10s latency for request writing
  - **Connection Resources**: Frees connection resources on timeout
- **Configuration location**: `rest-client.write-timeout` in `application-infrastructure.yml`

### Per-Client RestClient Configuration

Individual clients can override global timeouts:

#### `rest-client.clients.configserver`
- **Connect Timeout**: `5s`
- **Read Timeout**: `10s`
- **Write Timeout**: `10s`
- **Resource impact**: Same as global, but specific to Config Server client

#### `rest-client.clients.consul`
- **Connect Timeout**: `5s`
- **Read Timeout**: `10s`
- **Write Timeout**: `10s`
- **Resource impact**: Same as global, but specific to Consul client

#### `rest-client.clients.keycloak`
- **Connect Timeout**: `5s`
- **Read Timeout**: `10s`
- **Write Timeout**: `10s`
- **Resource impact**: Same as global, but specific to Keycloak client

## Config Server Client Configuration

Config Server client configuration for fetching configuration from Spring Cloud Config Server.

### `config-server.url`
- **Default**: `http://config-server:8888`
- **What it does**: Base URL for Config Server
- **Resource impact**: None (URL only)
- **Configuration location**: `config-server.url` in `application-infrastructure.yml`
- **Notes**: Can be overridden via `CONFIG_SERVER_URL` environment variable

### `config-server.timeout`
- **Default**: `5000` (5s)
- **What it does**: HTTP request timeout for Config Server calls
- **Resource impact**:
  - **Network I/O**: Config Server calls that exceed timeout are aborted
  - **Latency**: Adds up to 5s latency for Config Server requests
  - **Connection Resources**: Frees connection resources on timeout
- **Configuration location**: `config-server.timeout` in `application-infrastructure.yml`

### `config-server.retry.max-attempts`
- **Default**: `3`
- **What it does**: Maximum number of retry attempts for failed Config Server calls
- **Resource impact**:
  - **Network I/O**: Retries consume additional network resources
  - **CPU**: Re-processing consumes CPU cycles
  - **Latency**: Retries add latency (see backoff configuration)
- **Configuration location**: `config-server.retry.max-attempts` in `application-infrastructure.yml`

### `config-server.retry.backoff-delay`
- **Default**: `1000` (1s)
- **What it does**: Delay before retry attempts
- **Resource impact**: Adds latency to retry attempts
- **Configuration location**: `config-server.retry.backoff-delay` in `application-infrastructure.yml`

### `config-server.service-discovery.enabled`
- **Default**: `true`
- **What it does**: Enables service discovery for Config Server (finds Config Server via Consul)
- **Resource impact**: Adds Consul lookup overhead before Config Server calls
- **Configuration location**: `config-server.service-discovery.enabled` in `application-infrastructure.yml`

### `config-server.service-discovery.service-name`
- **Default**: `config-server`
- **What it does**: Service name for Config Server in Consul
- **Resource impact**: None (service name only)
- **Configuration location**: `config-server.service-discovery.service-name` in `application-infrastructure.yml`

### `config-server.service-discovery.fallback-to-url`
- **Default**: `true`
- **What it does**: Falls back to configured URL if service discovery fails
- **Resource impact**: Provides resilience during Consul outages
- **Configuration location**: `config-server.service-discovery.fallback-to-url` in `application-infrastructure.yml`

### `config-server.loadbalancer.strategy`
- **Default**: `round-robin`
- **Options**: `round-robin`, `random`, `health-based`
- **What it does**: Load balancing strategy for multiple Config Server instances
- **Resource impact**: Minimal (load balancing logic overhead)
- **Configuration location**: `config-server.loadbalancer.strategy` in `application-infrastructure.yml`

## Consul Client Configuration

Consul client configuration for service discovery and key-value operations.

### `consul.url`
- **Default**: `http://consul:8500`
- **What it does**: Base URL for Consul API
- **Resource impact**: None (URL only)
- **Configuration location**: `consul.url` in `application-infrastructure.yml`
- **Notes**: Can be overridden via `CONSUL_URL` environment variable

### `consul.timeout`
- **Default**: `5000` (5s)
- **What it does**: HTTP request timeout for Consul calls
- **Resource impact**:
  - **Network I/O**: Consul calls that exceed timeout are aborted
  - **Latency**: Adds up to 5s latency for Consul requests
  - **Connection Resources**: Frees connection resources on timeout
- **Configuration location**: `consul.timeout` in `application-infrastructure.yml`

### `consul.retry.max-attempts`
- **Default**: `3`
- **What it does**: Maximum number of retry attempts for failed Consul calls
- **Resource impact**: Same as Config Server retry
- **Configuration location**: `consul.retry.max-attempts` in `application-infrastructure.yml`

### `consul.retry.backoff-delay`
- **Default**: `1000` (1s)
- **What it does**: Delay before retry attempts
- **Resource impact**: Same as Config Server retry
- **Configuration location**: `consul.retry.backoff-delay` in `application-infrastructure.yml`

### Consul SDK Configuration (`consulclient`)

#### `consulclient.consul-url`
- **Default**: `http://consul:8500`
- **What it does**: Consul URL for SDK client (may use different client library)
- **Resource impact**: None (URL only)
- **Configuration location**: `consulclient.consul-url` in `application-infrastructure.yml`

#### `consulclient.connect-timeout`
- **Default**: `5s`
- **What it does**: Connection timeout for Consul SDK client
- **Resource impact**: Same as RestClient connect timeout
- **Configuration location**: `consulclient.connect-timeout` in `application-infrastructure.yml`

#### `consulclient.read-timeout`
- **Default**: `10s`
- **What it does**: Read timeout for Consul SDK client
- **Resource impact**: Same as RestClient read timeout
- **Configuration location**: `consulclient.read-timeout` in `application-infrastructure.yml`

#### `consulclient.write-timeout`
- **Default**: `10s`
- **What it does**: Write timeout for Consul SDK client
- **Resource impact**: Same as RestClient write timeout
- **Configuration location**: `consulclient.write-timeout` in `application-infrastructure.yml`

## Keycloak Admin Client Configuration

Keycloak Admin client configuration for IAM operations (user/team sync).

### `keycloak.admin.url`
- **Default**: `http://keycloak:8080`
- **What it does**: Base URL for Keycloak Admin API
- **Resource impact**: None (URL only)
- **Configuration location**: `keycloak.admin.url` in `application-security.yml`
- **Notes**: Can be overridden via `KEYCLOAK_ADMIN_URL` environment variable

### `keycloak.admin.realm`
- **Default**: `config-control`
- **What it does**: Keycloak realm name
- **Resource impact**: None (realm name only)
- **Configuration location**: `keycloak.admin.realm` in `application-security.yml`

### `keycloak.admin.client-id`
- **Default**: `config-control-service`
- **What it does**: Keycloak client ID for admin operations
- **Resource impact**: None (client ID only)
- **Configuration location**: `keycloak.admin.client-id` in `application-security.yml`

### `keycloak.admin.client-secret`
- **Default**: `config-control-service-secret`
- **What it does**: Keycloak client secret for admin operations
- **Resource impact**: None (authentication credential)
- **Configuration location**: `keycloak.admin.client-secret` in `application-security.yml`

**Note**: Keycloak Admin client timeout is managed by RestClient configuration (see `rest-client.clients.keycloak`).

## Connection Pooling

HTTP clients use connection pooling (via Apache HttpClient5) to reuse connections and reduce connection establishment overhead.

### Connection Pool Behavior
- **Pool Size**: Managed by Apache HttpClient5 (defaults vary by HTTP version)
- **Connection Reuse**: Connections are reused for multiple requests
- **Idle Timeout**: Idle connections are closed after timeout
- **Max Connections Per Route**: Limits connections per destination

### Resource Impact
- **Memory**: Connection pool consumes memory (connection objects)
- **Network**: Reuses connections, reducing connection establishment overhead
- **Latency**: Reused connections reduce connection establishment latency

## Resource Usage Summary

| Client | Connect Timeout | Read Timeout | Write Timeout | Retry Attempts | Retry Delay |
|--------|----------------|--------------|---------------|----------------|-------------|
| RestClient (Global) | 5s | 10s | 10s | - | - |
| Config Server | 5s | 10s | 10s | 3 | 1s |
| Consul | 5s | 10s | 10s | 3 | 1s |
| Keycloak | 5s | 10s | 10s | - | - |
| Consul SDK | 5s | 10s | 10s | - | - |

**Note**: Timeouts and retries are applied via Resilience4j (see [Resilience](resilience.md)) in addition to HTTP client timeouts.

## See Also

- [Resilience](resilience.md) - Circuit breakers, retries, and time limiters for HTTP clients
- [Database Connections](database-connections.md) - Connection pool configuration
- [Spring RestClient Documentation](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)

