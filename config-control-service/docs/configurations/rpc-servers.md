# RPC Servers Configuration

## Overview

Thrift and gRPC server configuration including thread pools and ports. These settings control RPC server lifecycle and resource usage, affecting additional threads and network ports.

**Configuration File**: `application-infrastructure.yml` (rpc.server.*)
**Java Configuration**: `ThriftServer.java` (hardcoded thread pool settings)

## RPC Server Ports

### `rpc.server.thrift-port`
- **Default**: `9090`
- **What it does**: Port number for the Thrift RPC server
- **Resource impact**:
  - **Network**: Listens on TCP port 9090 for Thrift connections
  - **Ports**: Consumes one network port
- **Configuration location**: `rpc.server.thrift-port` in `application-infrastructure.yml`
- **Notes**: 
  - Can be overridden via `THRIFT_PORT` environment variable
  - Thrift server is started asynchronously using `rpcExecutor` (see [Executor Pools](executor-pools.md))

### `rpc.server.grpc-port`
- **Default**: `9091`
- **What it does**: Port number for the gRPC RPC server
- **Resource impact**:
  - **Network**: Listens on TCP port 9091 for gRPC connections
  - **Ports**: Consumes one network port
- **Configuration location**: `rpc.server.grpc-port` in `application-infrastructure.yml`
- **Notes**: 
  - Can be overridden via `GRPC_PORT` environment variable
  - gRPC server is managed by `grpc-spring-boot-starter`

## Thrift Server Thread Pool

**Note**: Thrift server thread pool configuration is **hardcoded** in `ThriftServer.java` and cannot be configured via YAML.

### Thread Pool Configuration (Hardcoded)
- **Min Worker Threads**: `20`
- **Max Worker Threads**: `200`
- **What it does**: Thread pool for handling Thrift RPC requests
- **Resource impact**:
  - **Memory**: Each thread consumes stack memory (~1MB per thread)
    - Minimum: 20 threads ≈ 20MB
    - Maximum: 200 threads ≈ 200MB
  - **CPU**: Higher values increase context switching overhead
  - **Threads**: Directly determines thread count for Thrift request handling
- **Configuration location**: `ThriftServer.java` (lines 45-46)
- **Notes**:
  - Thread pool starts with `minWorkerThreads` and grows up to `maxWorkerThreads` under load
  - Thread pool is separate from HTTP server thread pool (Tomcat) and executor pools
  - This adds significant thread overhead to the application

### Thrift Server Lifecycle
1. **Startup**: Started asynchronously using `@Async("rpcExecutor")` when application is ready
2. **Listening**: Listens on `thrift-port` for incoming connections
3. **Processing**: Each request is handled by a thread from the thread pool
4. **Shutdown**: Stopped gracefully during application shutdown (`@PreDestroy`)

## gRPC Server Thread Pool

gRPC server thread pool is managed by Netty (underlying transport layer) and `grpc-spring-boot-starter`.

### Default Configuration
- **Thread Pool**: Managed by Netty event loop groups
- **What it does**: Handles gRPC RPC requests using Netty's event-driven model
- **Resource impact**:
  - **Memory**: Netty event loops consume minimal memory (few threads per CPU core)
  - **CPU**: Event-driven model is efficient for I/O-bound operations
  - **Threads**: Minimal thread count (typically 2 × CPU cores for event loops)
- **Configuration location**: Managed by `grpc-spring-boot-starter` and Netty
- **Notes**:
  - gRPC uses Netty's event-driven architecture (not thread-per-request)
  - More efficient than Thrift's thread-per-request model for high-concurrency scenarios
  - Thread count is much lower than Thrift thread pool

## Thrift Server Configuration Details

### Server Type
- **Type**: `TThreadPoolServer`
- **What it does**: Thread-per-request server model (each request gets its own thread)
- **Resource impact**:
  - **Threads**: Creates threads as needed up to `maxWorkerThreads`
  - **Memory**: High memory usage due to thread-per-request model
  - **CPU**: Context switching overhead for many threads
- **Configuration location**: `ThriftServer.java`
- **Notes**: 
  - Traditional thread-per-request model
  - Suitable for low-to-medium concurrency
  - Not ideal for high-concurrency scenarios (consider async Thrift server types)

### Transport
- **Type**: `TServerSocket`
- **What it does**: TCP socket transport for Thrift communication
- **Resource impact**: Standard TCP socket resources (file descriptors, network buffers)
- **Configuration location**: `ThriftServer.java`
- **Notes**: Blocking I/O model (each thread blocks on socket I/O)

## RPC Server Resource Usage

### Thread Count
- **Thrift**: 20-200 threads (separate from HTTP server and executor pools)
- **gRPC**: ~2 × CPU cores (event loops, minimal threads)

### Memory Usage
- **Thrift**: 20-200MB for thread stacks (plus request/response objects)
- **gRPC**: Minimal (few event loop threads)

### Network Ports
- **Thrift**: Port 9090 (configurable)
- **gRPC**: Port 9091 (configurable)

### CPU Usage
- **Thrift**: Context switching overhead for thread-per-request model
- **gRPC**: Efficient event-driven model (lower CPU overhead)

## RPC Server Integration

### Consul Service Discovery
Thrift and gRPC ports are registered with Consul for service discovery:

- **Metadata**: `thrift_port`, `grpc_port` in Consul service metadata
- **Configuration**: `spring.cloud.consul.discovery.metadata.*` in `application-security.yml`

### Health Checks
RPC servers can be health-checked via:
- **Thrift**: Custom health check implementation (if provided)
- **gRPC**: gRPC health check service (via `grpc-spring-boot-starter`)

## Resource Usage Summary

| Server | Thread Pool | Memory Impact | CPU Impact | Network Ports |
|--------|-------------|---------------|------------|---------------|
| Thrift | 20-200 threads | High (20-200MB stacks) | Medium (context switching) | 1 (port 9090) |
| gRPC | ~2 × CPU cores | Low (event loops) | Low (event-driven) | 1 (port 9091) |

**Total Additional Threads**: 20-200 for Thrift + ~2 × CPU cores for gRPC

## Notes

- Thrift thread pool is hardcoded and cannot be configured via YAML (would require code changes)
- Thrift uses thread-per-request model, which is less efficient than gRPC's event-driven model
- Consider migrating to async Thrift server types or gRPC for better resource efficiency
- RPC servers are started asynchronously and do not block application startup
- Ports can be configured via environment variables (`THRIFT_PORT`, `GRPC_PORT`)

## See Also

- [Executor Pools](executor-pools.md) - RPC executor used for Thrift server startup
- [HTTP Server](http-server.md) - HTTP server thread pool (separate from RPC)
- [Thrift Documentation](https://thrift.apache.org/)
- [gRPC Spring Boot Starter](https://github.com/grpc/grpc-java/tree/master/examples/example-spring-boot)

