# Data & Processing Decisions

## Why Batch Processing over Single Processing?

### Decision

Implement asynchronous batch processing for heartbeat ingestion instead of synchronous single processing.

### Context

The system needs to process 10,000+ heartbeats per minute. Synchronous processing creates a bottleneck with one database write and one config server call per heartbeat.

### Alternatives Considered

1. **Synchronous Single Processing**
   - Simple implementation
   - Low throughput (2,000 heartbeats/min)
   - High database load
   - Sequential processing

2. **Micro-batching (Smaller batches)**
   - Lower latency
   - Still high database load
   - More complex than single, less efficient than large batches

3. **Streaming Processing (Kafka Streams)**
   - Real-time processing
   - More complex setup
   - Overkill for current requirements

### Trade-offs

| Aspect | Batch Processing | Single Processing | Streaming |
|--------|-----------------|-------------------|-----------|
| **Throughput** | 10,000+ heartbeats/min | 2,000 heartbeats/min | Very High |
| **Latency** | 200-500ms (batch wait) | 50-100ms | < 100ms |
| **Database Load** | Low (bulk operations) | High (N writes) | Medium |
| **Complexity** | Medium | Low | High |
| **Scalability** | Excellent | Limited | Excellent |

### Rationale

1. **5x Throughput Improvement**: Process 50-100 heartbeats per batch
2. **50-100x Database Write Reduction**: N writes → 1 bulk write
3. **~10x Config Server Call Reduction**: Deduplication by service:env
4. **Parallel Execution**: Config hash fetching in parallel
5. **Scalability**: Can handle 10,000+ instances

### Implementation

**Batch Processing Flow:**
1. Kafka consumer receives batch (50-100 heartbeats)
2. Batch load ServiceInstances and ApplicationServices
3. Parallel fetch config hashes (grouped by service:env)
4. In-memory processing with drift detection
5. Bulk upsert to MongoDB
6. Grouped bus refresh (one per service)

**Reference:** `config-control-service/src/main/java/com/example/control/application/service/infra/HeartbeatBatchService.java`

**Performance Metrics:**
- Throughput: 10,000+ heartbeats/min (5x improvement)
- Database writes: 1 per batch (50-100x reduction)
- Config server calls: 1 per service:env (~10x reduction)

### When to Reconsider

- If latency requirements become critical (< 100ms)
- If volume is low (< 100 instances)
- If real-time processing is required
- If batch processing complexity becomes prohibitive

---

## Why Config Hash Caching?

### Decision

Cache config hashes from Config Server to reduce load and improve performance.

### Context

Config Server is called frequently to fetch expected config hashes for drift detection. Many instances of the same service share the same config hash.

### Alternatives Considered

1. **No Caching**
   - Simple
   - High Config Server load
   - Slower drift detection

2. **In-Memory Cache Only**
   - Fast
   - Lost on restart
   - Not shared across instances

3. **Redis Cache Only**
   - Shared across instances
   - Network latency
   - More complex

### Trade-offs

| Aspect | Multi-level Cache | No Cache | Redis Only |
|--------|------------------|----------|------------|
| **Config Server Load** | Low | High | Low |
| **Latency** | Very Low (L1) | High | Medium |
| **Complexity** | Medium | Low | Low |
| **Memory Usage** | Medium | Low | Low |
| **Shared Cache** | Yes (L2) | No | Yes |

### Rationale

1. **Reduced Config Server Load**: Cache reduces HTTP calls by ~80%
2. **Faster Drift Detection**: L1 cache (Caffeine) provides < 1ms access
3. **Shared Cache**: L2 cache (Redis) shared across service instances
4. **Cache Hit Rate**: > 80% hit rate in production
5. **Cost Efficiency**: Reduces infrastructure costs

### Implementation

**Multi-level Caching:**
- **L1 (Caffeine)**: Local in-memory cache, < 1ms access
- **L2 (Redis)**: Shared cache across instances, < 5ms access
- **TTL**: 5 minutes (config changes propagate within 5 min)

**Cache Key:** `config:hash:{serviceName}:{environment}`

**Reference:** `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/pingconfig/cache/ConfigHashCacheConfig.java`

### When to Reconsider

- If Config Server can handle full load
- If cache complexity becomes prohibitive
- If cache hit rate is low (< 50%)
- If memory becomes constrained

---

## Why Multi-level Caching (Caffeine + Redis)?

### Decision

Use two-level caching: Caffeine (L1) for local cache and Redis (L2) for shared cache.

### Context

The system needs fast local access (L1) and shared cache across instances (L2). Different data types have different caching requirements.

### Alternatives Considered

1. **Caffeine Only**
   - Very fast
   - Not shared across instances
   - Memory per instance

2. **Redis Only**
   - Shared across instances
   - Network latency
   - Single point of failure

3. **No Caching**
   - Simplest
   - High database/API load
   - Slower performance

### Trade-offs

| Aspect | Multi-level | Caffeine Only | Redis Only |
|--------|------------|---------------|------------|
| **Local Access Speed** | Very Fast (L1) | Very Fast | Medium |
| **Shared Cache** | Yes (L2) | No | Yes |
| **Network Latency** | Low (L1 hit) | None | Always |
| **Complexity** | Medium | Low | Low |
| **Memory Usage** | Medium | High (per instance) | Low |

### Rationale

1. **Best of Both Worlds**: Fast local access + shared cache
2. **Reduced Network Calls**: L1 cache handles most requests
3. **Shared State**: L2 cache ensures consistency across instances
4. **Performance**: < 1ms for L1 hits, < 5ms for L2 hits
5. **Scalability**: Each instance has local cache, shared cache scales independently

### Implementation

**Caching Strategy:**
- **L1 (Caffeine)**: 
  - Size: 10,000 entries per instance
  - TTL: 5 minutes
  - Eviction: LRU
  
- **L2 (Redis)**:
  - Shared across all instances
  - TTL: 5 minutes
  - Pattern: Cache-aside

**Cache Types:**
- Config hashes (multi-level)
- Service instances (Redis only)
- Application services (Redis only)
- Drift events (Redis only)

**Reference:** 
- `zcm-spring-sdk-starter/src/main/java/com/vng/zing/zcm/pingconfig/cache/ConfigHashCacheConfig.java`
- `config-control-service/src/main/java/com/example/control/infrastructure/cache/`

### When to Reconsider

- If network latency to Redis becomes negligible
- If memory becomes constrained
- If cache hit rates are low
- If single-level cache is sufficient

---

## Why In-Memory Processing Before Database Writes?

### Decision

Process all heartbeats in memory before performing database writes in batch operations.

### Context

Batch processing needs to detect drift transitions, create drift events, and update instances before writing to the database.

### Alternatives Considered

1. **Database-First Processing**
   - Simpler
   - More database round-trips
   - Less efficient

2. **Hybrid (Some in-memory, some DB)**
   - Balanced
   - More complex
   - Inconsistent

3. **Streaming Processing**
   - Real-time
   - More complex
   - Harder to batch

### Trade-offs

| Aspect | In-Memory First | Database-First | Streaming |
|--------|----------------|----------------|-----------|
| **Database Load** | Low (bulk writes) | High (many writes) | Medium |
| **Atomicity** | Batch-level | Per-operation | Event-level |
| **Complexity** | Medium | Low | High |
| **Performance** | High | Medium | High |
| **Error Handling** | Batch-level | Per-operation | Event-level |

### Rationale

1. **Batch Atomicity**: All heartbeats processed together
2. **Reduced Database Load**: Single bulk write instead of N writes
3. **Efficient Processing**: In-memory operations are fast
4. **Drift Transition Tracking**: Can track state changes before persistence
5. **Error Handling**: Batch-level error handling is simpler

### Implementation

**Processing Flow:**
1. Load all required data (instances, services, config hashes)
2. Process each heartbeat in memory
3. Track drift transitions
4. Collect instances and events to save
5. Bulk write to database

**Reference:** `config-control-service/src/main/java/com/example/control/application/service/infra/HeartbeatBatchService.java:138-175`

### When to Reconsider

- If memory becomes constrained
- If batch size becomes too large
- If real-time processing is required
- If error handling becomes too complex

---

## Summary

Data and processing decisions prioritize:
1. **Throughput and Scalability** (Batch processing)
2. **Performance** (Multi-level caching)
3. **Efficiency** (In-memory processing, bulk operations)
4. **Cost Reduction** (Reduced database and API calls)

These optimizations enable the system to handle 10,000+ service instances efficiently.

