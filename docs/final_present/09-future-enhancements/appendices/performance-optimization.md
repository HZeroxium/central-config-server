# Performance Optimization Details
## Logic Optimization for 10x Growth

**Target:** Support 100,000+ service instances, 100,000+ heartbeats/minute

---

## Current Performance Baseline

### Throughput Metrics

| Metric | Current Value | Target (12 months) |
|--------|--------------|-------------------|
| **Heartbeat Processing** | 10,000+ heartbeats/minute | 100,000+ heartbeats/minute |
| **Batch Size** | 50-100 heartbeats | 200-500 heartbeats |
| **API Throughput** | 1,000+ requests/second | 10,000+ requests/second |
| **Database Writes** | ~200 writes/second | ~2,000 writes/second |

### Latency Metrics

| Operation | Current (p95) | Target (p95) |
|-----------|--------------|-------------|
| **Heartbeat Processing** | 100ms | 50ms |
| **Batch Processing** | 500ms | 200ms |
| **API Response Time** | 200ms | 100ms |
| **Database Queries** | 50ms | 20ms |

---

## 1. Batch Processing Optimization

### Current Implementation

**Batch Size:** 50-100 heartbeats  
**Processing:** Sequential within batch  
**Database Calls:** ~5-10 per batch

### Optimization Strategy

#### Increase Batch Size

**Target:** 200-500 heartbeats per batch

**Configuration:**
```yaml
# application-app.yml
heartbeat:
  batch:
    size: 200  # Increased from 50-100
    max-size: 500  # Maximum batch size
    timeout: 5s  # Max wait time for batch
```

**Adaptive Batch Sizing:**
```java
@Service
public class AdaptiveBatchProcessor {
    
    private int currentBatchSize = 200;
    private final int minBatchSize = 100;
    private final int maxBatchSize = 500;
    
    public int calculateOptimalBatchSize(
            int queueSize, 
            double processingTime) {
        
        // Increase if queue is large and processing is fast
        if (queueSize > 1000 && processingTime < 200) {
            currentBatchSize = Math.min(
                currentBatchSize + 50, 
                maxBatchSize
            );
        }
        // Decrease if processing is slow
        else if (processingTime > 500) {
            currentBatchSize = Math.max(
                currentBatchSize - 50, 
                minBatchSize
            );
        }
        
        return currentBatchSize;
    }
}
```

**Expected Impact:**
- 2-5x reduction in Kafka consumer overhead
- 2-5x reduction in transaction overhead
- 30-50% improvement in throughput

#### Parallel Batch Processing

**Current:** Sequential processing within batch  
**Target:** Parallel processing with controlled concurrency

**Implementation:**
```java
@Service
public class ParallelBatchProcessor {
    
    @Autowired
    private AsyncTaskExecutor batchExecutor;
    
    public void processBatch(List<HeartbeatPayload> payloads) {
        // Split batch into chunks
        int chunkSize = 50;
        List<List<HeartbeatPayload>> chunks = Lists.partition(
            payloads, chunkSize
        );
        
        // Process chunks in parallel
        List<CompletableFuture<Void>> futures = chunks.stream()
            .map(chunk -> CompletableFuture.runAsync(
                () -> processChunk(chunk),
                batchExecutor
            ))
            .collect(Collectors.toList());
        
        // Wait for all chunks
        CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        ).join();
    }
}
```

**Configuration:**
```yaml
spring:
  task:
    execution:
      pool:
        core-size: 10
        max-size: 20
        queue-capacity: 100
```

**Expected Impact:**
- 3-5x improvement in batch processing time
- Better CPU utilization
- 50% reduction in p95 latency

---

## 2. Query Optimization

### MongoDB Index Optimization

#### Current Indexes

**ServiceInstance:**
```javascript
db.serviceInstances.createIndex({ serviceId: 1, instanceId: 1 })
db.serviceInstances.createIndex({ teamId: 1, hasDrift: 1 })
```

#### Optimized Indexes

**Composite Indexes for Common Queries:**
```javascript
// Batch load by IDs (most common)
db.serviceInstances.createIndex({ 
    _id: 1, 
    serviceId: 1, 
    teamId: 1 
})

// Team-based queries with drift filter
db.serviceInstances.createIndex({ 
    teamId: 1, 
    hasDrift: 1, 
    lastHeartbeatAt: -1 
})

// Service-based queries
db.serviceInstances.createIndex({ 
    serviceId: 1, 
    status: 1, 
    lastHeartbeatAt: -1 
})

// Drift event queries
db.driftEvents.createIndex({ 
    serviceId: 1, 
    status: 1, 
    detectedAt: -1 
}, { 
    partialFilterExpression: { status: { $in: ["DETECTED", "RESOLVED"] } }
})
```

**Index Usage Analysis:**
```javascript
// Analyze query performance
db.serviceInstances.find({ 
    teamId: "team-123", 
    hasDrift: true 
}).explain("executionStats")

// Check index usage
db.serviceInstances.aggregate([
    { $indexStats: {} }
])
```

**Expected Impact:**
- 50-70% reduction in query time
- 30-40% reduction in database load
- Better index hit rate (>95%)

### Aggregation Pipeline Optimization

#### Current Queries

**Service Health Aggregation:**
```java
// Current: Multiple queries
List<ServiceInstance> instances = repository.findByServiceId(serviceId);
long healthyCount = instances.stream()
    .filter(i -> i.getStatus() == Status.HEALTHY)
    .count();
```

#### Optimized Aggregations

**Single Aggregation Pipeline:**
```java
@Repository
public interface ServiceInstanceRepository extends MongoRepository<...> {
    
    @Aggregation(pipeline = {
        "{ $match: { serviceId: ?0 } }",
        "{ $group: { " +
            "_id: '$status', " +
            "count: { $sum: 1 }, " +
            "avgLastHeartbeat: { $avg: '$lastHeartbeatAt' } " +
        "} }"
    })
    List<StatusCount> aggregateByStatus(String serviceId);
}
```

**MongoDB Aggregation:**
```javascript
db.serviceInstances.aggregate([
    { $match: { serviceId: "sample-service" } },
    { $group: {
        _id: "$status",
        count: { $sum: 1 },
        avgLastHeartbeat: { $avg: "$lastHeartbeatAt" }
    }},
    { $sort: { count: -1 } }
])
```

**Expected Impact:**
- 60-80% reduction in database round trips
- 40-50% reduction in network traffic
- 30-40% improvement in query performance

### Query Result Caching

#### Cache Strategy

**Cache Keys:**
- Service health: `service:health:{serviceId}`
- Team services: `team:services:{teamId}`
- Config hash: `config:hash:{serviceId}:{environment}`

**Cache TTL:**
```yaml
cache:
  service-health:
    ttl: 30s  # Short TTL for real-time data
  team-services:
    ttl: 5m  # Medium TTL for team data
  config-hash:
    ttl: 30m  # Long TTL for config data
```

**Cache Warming:**
```java
@Scheduled(fixedRate = 60000) // Every minute
public void warmCache() {
    // Pre-load frequently accessed data
    List<String> popularServices = getPopularServices();
    popularServices.forEach(serviceId -> {
        cacheService.getServiceHealth(serviceId);
    });
}
```

**Expected Impact:**
- 80-90% cache hit rate
- 70-80% reduction in database queries
- 50-60% improvement in response time

---

## 3. Caching Strategy Enhancement

### Multi-Tier Caching

#### Current: L1 (Caffeine) + L2 (Redis)

**Architecture:**
```
Request → L1 Cache (Caffeine) → L2 Cache (Redis) → Database
```

#### Enhanced: L1 + L2 + L3

**Architecture:**
```
Request → L1 (Caffeine, 1s TTL) 
       → L2 (Redis, 5m TTL) 
       → L3 (MongoDB Read Replica, 30m TTL)
       → Primary Database
```

**Implementation:**
```java
@Service
public class MultiTierCacheService {
    
    @Autowired
    private Cache l1Cache; // Caffeine
    
    @Autowired
    private RedisTemplate<String, Object> l2Cache; // Redis
    
    @Autowired
    private MongoTemplate l3Cache; // MongoDB read replica
    
    public <T> T get(String key, Class<T> type, Supplier<T> loader) {
        // L1 cache
        T value = l1Cache.get(key, type);
        if (value != null) return value;
        
        // L2 cache
        value = l2Cache.opsForValue().get(key);
        if (value != null) {
            l1Cache.put(key, value);
            return value;
        }
        
        // L3 cache (read replica)
        value = l3Cache.findById(key, type);
        if (value != null) {
            l2Cache.opsForValue().set(key, value, 5, TimeUnit.MINUTES);
            l1Cache.put(key, value);
            return value;
        }
        
        // Load from primary
        value = loader.get();
        l3Cache.save(value);
        l2Cache.opsForValue().set(key, value, 5, TimeUnit.MINUTES);
        l1Cache.put(key, value);
        return value;
    }
}
```

**Expected Impact:**
- 90%+ cache hit rate
- 80% reduction in primary database load
- 60% improvement in response time

### Predictive Cache Preloading

**Strategy:**
- Pre-load data based on access patterns
- Time-based preloading (e.g., morning reports)
- User behavior-based preloading

**Implementation:**
```java
@Service
public class PredictiveCachePreloader {
    
    @Scheduled(cron = "0 0 8 * * *") // 8 AM daily
    public void preloadMorningReports() {
        // Pre-load team dashboards
        teamService.getAllTeams().forEach(team -> {
            cacheService.preloadTeamDashboard(team.getId());
        });
    }
    
    public void preloadBasedOnUser(String userId) {
        // Pre-load user's frequently accessed services
        List<String> frequentServices = getUserFrequentServices(userId);
        frequentServices.forEach(serviceId -> {
            cacheService.preloadServiceHealth(serviceId);
        });
    }
}
```

**Expected Impact:**
- 20-30% improvement in cache hit rate
- 15-20% reduction in perceived latency
- Better user experience

---

## 4. Database Connection Pooling

### Current Configuration

**Default Spring Boot:**
- Max connections: 100
- Min idle: 10
- Connection timeout: 30s

### Optimized Configuration

**Per-Service Pools:**
```yaml
spring:
  data:
    mongodb:
      options:
        max-pool-size: 200  # Increased for high throughput
        min-pool-size: 50   # Keep connections warm
        max-wait-time: 5s   # Fail fast
        max-connection-idle-time: 30s
        max-connection-life-time: 300s
```

**Connection Pool Monitoring:**
```java
@Component
public class ConnectionPoolMonitor {
    
    @Scheduled(fixedRate = 60000)
    public void monitorPool() {
        MongoClient mongoClient = mongoTemplate.getMongoClientFactory().getMongoClient();
        ConnectionPoolStats stats = mongoClient.getClusterDescription()
            .getServerDescriptions()
            .stream()
            .map(ServerDescription::getConnectionPool)
            .findFirst()
            .orElse(null);
        
        if (stats != null) {
            log.info("Connection pool: active={}, idle={}, max={}",
                stats.getActive(), stats.getIdle(), stats.getMax());
            
            // Alert if pool is exhausted
            if (stats.getActive() > stats.getMax() * 0.9) {
                alertService.sendAlert("Connection pool near exhaustion");
            }
        }
    }
}
```

**Expected Impact:**
- 30-40% reduction in connection overhead
- Better connection reuse
- Reduced connection establishment time

---

## 5. Async Processing Enhancements

### Current: Kafka-Based Async

**Architecture:**
```
API → Kafka → Batch Processor → Database
```

### Enhanced: Multi-Stage Async Pipeline

**Architecture:**
```
API → Kafka (Ingestion)
   → Kafka Streams (Enrichment)
   → Batch Processor (Processing)
   → Database (Persistence)
   → Kafka (Events)
```

**Kafka Streams Enrichment:**
```java
@Configuration
public class HeartbeatEnrichmentStream {
    
    @Bean
    public KStream<String, HeartbeatPayload> enrichHeartbeats(
            StreamsBuilder streamsBuilder) {
        
        KStream<String, HeartbeatPayload> heartbeats = streamsBuilder
            .stream("heartbeat-queue");
        
        // Enrich with service metadata
        KTable<String, ApplicationService> services = streamsBuilder
            .table("application-services");
        
        KStream<String, EnrichedHeartbeat> enriched = heartbeats
            .leftJoin(services,
                (heartbeat, service) -> EnrichedHeartbeat.builder()
                    .heartbeat(heartbeat)
                    .service(service)
                    .build(),
                Joined.with(Serdes.String(), heartbeatSerde, serviceSerde)
            );
        
        enriched.to("enriched-heartbeat-queue");
        return heartbeats;
    }
}
```

**Expected Impact:**
- 20-30% reduction in processing time
- Better data quality
- Reduced database queries

---

## 6. JVM Optimization

### Current JVM Settings

```bash
JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
```

### Optimized JVM Settings

**For High Throughput:**
```bash
JAVA_OPTS="
  -XX:+UseG1GC
  -XX:MaxRAMPercentage=75.0
  -XX:MaxGCPauseMillis=200
  -XX:ParallelGCThreads=8
  -XX:ConcGCThreads=2
  -XX:InitiatingHeapOccupancyPercent=45
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat
  -XX:+UseCompressedOops
  -XX:+UseCompressedClassPointers
  -Xlog:gc*:file=/var/log/gc.log:time,uptime,level,tags
"
```

**For Low Latency:**
```bash
JAVA_OPTS="
  -XX:+UseZGC  # or -XX:+UseShenandoahGC
  -XX:MaxRAMPercentage=75.0
  -XX:UnlockExperimentalVMOptions
  -XX:+UseTransparentHugePages
  -Xlog:gc*:file=/var/log/gc.log:time,uptime,level,tags
"
```

**Expected Impact:**
- 20-30% reduction in GC pause time
- 10-15% improvement in throughput
- Better memory utilization

---

## 7. Network Optimization

### HTTP/2 and Connection Pooling

**RestClient Configuration:**
```java
@Bean
public RestClient.Builder restClientBuilder() {
    return RestClient.builder()
        .requestFactory(new HttpComponentsClientHttpRequestFactory(
            HttpClients.custom()
                .setMaxConnTotal(200)
                .setMaxConnPerRoute(50)
                .setConnectionTimeToLive(30, TimeUnit.SECONDS)
                .evictIdleConnections(30, TimeUnit.SECONDS)
                .evictExpiredConnections()
                .build()
        ));
}
```

**Expected Impact:**
- 30-40% reduction in connection overhead
- Better connection reuse
- Reduced latency

---

## Performance Testing

### Load Testing Strategy

**Tools:**
- JMeter for API load testing
- Gatling for high-throughput testing
- Custom scripts for heartbeat simulation

**Test Scenarios:**
1. **Normal Load:** 10,000 heartbeats/minute
2. **Peak Load:** 50,000 heartbeats/minute
3. **Stress Test:** 100,000+ heartbeats/minute

**Metrics to Monitor:**
- Throughput (heartbeats/second)
- Latency (p50, p95, p99)
- Error rate
- Resource utilization (CPU, memory, I/O)
- Database connection pool usage
- Cache hit rates

### Benchmarking

**Before Optimization:**
- 10,000 heartbeats/minute
- p95 latency: 500ms
- Database load: 200 writes/second

**After Optimization (Target):**
- 100,000 heartbeats/minute (10x)
- p95 latency: 200ms (60% improvement)
- Database load: 1,000 writes/second (5x, but optimized)

---

## Success Metrics

### Performance Improvements

| Metric | Current | Target | Improvement |
|--------|---------|--------|-------------|
| **Throughput** | 10K/min | 100K/min | 10x |
| **Batch Processing (p95)** | 500ms | 200ms | 60% |
| **API Response (p95)** | 200ms | 100ms | 50% |
| **Database Queries** | 200/s | 1,000/s | 5x |
| **Cache Hit Rate** | 80% | 90% | 12.5% |

### Resource Efficiency

| Metric | Current | Target | Improvement |
|--------|---------|--------|-------------|
| **CPU Utilization** | 70% | 60% | 14% |
| **Memory Usage** | 4GB | 6GB | 50% (acceptable) |
| **Database Connections** | 100 | 200 | 2x (optimized) |
| **Network Bandwidth** | 100 Mbps | 200 Mbps | 2x |

---

## Implementation Priority

### Phase 1 (Months 1-2): Quick Wins

1. ✅ Increase batch size to 200
2. ✅ Optimize MongoDB indexes
3. ✅ Enhance caching strategy
4. ✅ JVM tuning

**Expected Impact:** 2-3x improvement

### Phase 2 (Months 3-4): Advanced Optimizations

1. ✅ Parallel batch processing
2. ✅ Aggregation pipeline optimization
3. ✅ Predictive cache preloading
4. ✅ Connection pool optimization

**Expected Impact:** Additional 2-3x improvement

### Phase 3 (Months 5-6): Fine-Tuning

1. ✅ Multi-tier caching
2. ✅ Kafka Streams enrichment
3. ✅ Network optimization
4. ✅ Performance testing and tuning

**Expected Impact:** Additional 1.5-2x improvement

**Total Expected Improvement:** 6-18x (conservative: 10x)

---

## References

- [MongoDB Performance Best Practices](https://docs.mongodb.com/manual/administration/analyzing-mongodb-performance/)
- [Spring Boot Performance Tuning](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.spring-application.application-properties.performance)
- [JVM Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/)
- [Kafka Performance Tuning](https://kafka.apache.org/documentation/#performance)

---

**Next:** Review [Cost Analysis & Projections](./cost-analysis.md) for infrastructure cost planning.

