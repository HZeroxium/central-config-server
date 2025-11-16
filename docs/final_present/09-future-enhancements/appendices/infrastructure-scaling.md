# Infrastructure Scaling Strategy
## Scaling for 10x Growth

**Target:** Support 100,000+ service instances, 100,000+ heartbeats/minute

---

## MongoDB Scaling

### Current State

- **Deployment:** Single MongoDB instance
- **Data Size:** ~50GB
- **Service Instances:** ~10,000
- **Write Throughput:** ~200 writes/second
- **Read Throughput:** ~500 reads/second

### Target State (12 months)

- **Deployment:** Sharded cluster with replica sets
- **Data Size:** ~500GB (projected)
- **Service Instances:** ~100,000
- **Write Throughput:** ~2,000 writes/second
- **Read Throughput:** ~5,000 reads/second

### Scaling Strategy

#### Phase 1: Replica Sets (Months 1-2)

**Configuration:**
- Primary node + 2 secondary nodes
- Read preference: `primaryPreferred` (default)
- Write concern: `majority`
- Automatic failover enabled

**Benefits:**
- High availability (99.9% uptime)
- Read scaling (secondary reads)
- Data redundancy

**Implementation Steps:**
1. Deploy MongoDB replica set (3 nodes)
2. Configure connection string with replica set name
3. Update Spring Data MongoDB configuration
4. Test failover scenarios
5. Monitor replication lag

**Configuration Example:**
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://mongodb-1:27017,mongodb-2:27017,mongodb-3:27017/config_control?replicaSet=rs0
      read-preference: primaryPreferred
      write-concern: majority
```

#### Phase 2: Sharding (Months 3-6)

**Sharding Architecture:**
- 3 config servers (metadata)
- 2 shards (initially), scale to 10+ shards
- 3-node replica set per shard
- Mongos routers (2-3 instances)

**Shard Key Strategy:**

**ServiceInstance Collection:**
```javascript
sh.shardCollection("config_control.serviceInstances", 
  { serviceId: 1, teamId: 1 }, 
  { numInitialChunks: 10 }
)
```
- **Rationale:** Even distribution by service, team isolation
- **Chunk Size:** 64MB (default)
- **Balancing:** Automatic

**DriftEvent Collection:**
```javascript
sh.shardCollection("config_control.driftEvents", 
  { serviceId: 1, environment: 1, detectedAt: 1 }, 
  { numInitialChunks: 10 }
)
```
- **Rationale:** Time-based queries, service isolation
- **TTL Index:** 90 days (auto-delete old events)

**ApplicationService Collection:**
- **Strategy:** No sharding initially (small collection)
- **Alternative:** Shard by `ownerTeamId` if >1M documents

**Indexes per Shard:**
```javascript
// ServiceInstance indexes
db.serviceInstances.createIndex({ serviceId: 1, teamId: 1, status: 1 })
db.serviceInstances.createIndex({ teamId: 1, hasDrift: 1, lastHeartbeatAt: -1 })
db.serviceInstances.createIndex({ serviceId: 1, instanceId: 1 }, { unique: true })

// DriftEvent indexes
db.driftEvents.createIndex({ serviceId: 1, status: 1, detectedAt: -1 })
db.driftEvents.createIndex({ teamId: 1, severity: 1, detectedAt: -1 })
db.driftEvents.createIndex({ detectedAt: 1 }, { expireAfterSeconds: 7776000 }) // 90 days
```

**Capacity Planning:**

| Metric | Current | Target (12 months) | Target (24 months) |
|--------|---------|-------------------|---------------------|
| **Documents** | 10M | 100M | 500M |
| **Data Size** | 50GB | 500GB | 2TB |
| **Indexes Size** | 10GB | 100GB | 400GB |
| **Shards** | 1 | 5 | 10 |
| **Nodes per Shard** | 1 | 3 | 3 |
| **Total Nodes** | 1 | 15 | 30 |

**Cost Estimation:**
- **Current:** $200/month (single instance)
- **12 months:** $2,000/month (5 shards × 3 nodes)
- **24 months:** $4,000/month (10 shards × 3 nodes)

#### Read Preferences

**Configuration:**
- **Primary reads:** Critical queries (heartbeat processing)
- **Secondary reads:** Analytics, reporting, dashboards
- **Nearest reads:** Low-latency requirements

**Spring Data Configuration:**
```yaml
spring:
  data:
    mongodb:
      read-preference: primaryPreferred
      # Override per query for analytics
```

**Query-Level Override:**
```java
@Query(value = "{...}", readPreference = "secondaryPreferred")
List<DriftEvent> findAnalyticsEvents(...);
```

#### Write Concerns

**Configuration:**
- **Default:** `majority` (consistency)
- **High throughput:** `w: 1` (acknowledged, faster)
- **Critical writes:** `w: majority, j: true` (journaled)

**Application Configuration:**
```yaml
spring:
  data:
    mongodb:
      write-concern: majority
```

---

## Redis Scaling

### Current State

- **Deployment:** Single Redis instance
- **Memory:** 2GB
- **Throughput:** ~5,000 ops/second
- **Cache Hit Rate:** ~80%

### Target State (12 months)

- **Deployment:** Redis Cluster (6+ nodes)
- **Memory:** 20GB+ (distributed)
- **Throughput:** ~50,000 ops/second
- **Cache Hit Rate:** >85%

### Scaling Strategy

#### Phase 1: Redis Sentinel (Months 1-2)

**Configuration:**
- 1 master + 2 replicas
- 3 Sentinel instances
- Automatic failover

**Benefits:**
- High availability
- Automatic failover
- Read scaling

**Implementation:**
```yaml
# docker-compose.yml
redis-master:
  image: redis:latest
  command: redis-server --appendonly yes
  
redis-replica-1:
  image: redis:latest
  command: redis-server --replicaof redis-master 6379
  
redis-replica-2:
  image: redis:latest
  command: redis-server --replicaof redis-master 6379

sentinel-1:
  image: redis:latest
  command: redis-sentinel /etc/redis/sentinel.conf
```

#### Phase 2: Redis Cluster (Months 3-6)

**Cluster Configuration:**
- 3 master nodes (each handles hash slots)
- 3 replica nodes (1 per master)
- Hash slot distribution: 16,384 slots / 3 masters

**Cluster Setup:**
```bash
# Create cluster
redis-cli --cluster create \
  redis-1:6379 redis-2:6379 redis-3:6379 \
  redis-4:6379 redis-5:6379 redis-6:6379 \
  --cluster-replicas 1

# Verify cluster
redis-cli --cluster check redis-1:6379
```

**Spring Configuration:**
```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - redis-1:6379
          - redis-2:6379
          - redis-3:6379
          - redis-4:6379
          - redis-5:6379
          - redis-6:6379
        max-redirects: 3
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

**Key Distribution:**
- **Hash tags:** Use `{serviceId}` for related keys
- **Example:** `{sample-service}:config-hash`, `{sample-service}:instances`

**Persistence Strategy:**
- **AOF (Append-Only File):** Enabled for durability
- **RDB Snapshots:** Hourly backups
- **Replication:** Async replication to replicas

**Capacity Planning:**

| Metric | Current | Target (12 months) | Target (24 months) |
|--------|---------|-------------------|---------------------|
| **Memory** | 2GB | 20GB | 50GB |
| **Nodes** | 1 | 6 | 12 |
| **Throughput** | 5K ops/s | 50K ops/s | 100K ops/s |
| **Cache Hit Rate** | 80% | 85% | 90% |

**Cost Estimation:**
- **Current:** $50/month
- **12 months:** $500/month (6 nodes)
- **24 months:** $1,000/month (12 nodes)

---

## Consul Scaling

### Current State

- **Deployment:** Single Consul server (dev mode)
- **Services:** ~1,000 registered services
- **Health Checks:** ~5,000 checks
- **Datacenter:** Single

### Target State (12 months)

- **Deployment:** Consul cluster with federation
- **Services:** 10,000+ registered services
- **Health Checks:** 50,000+ checks
- **Datacenters:** 3+ datacenters

### Scaling Strategy

#### Phase 1: Consul Cluster (Months 1-2)

**Configuration:**
- 3-5 Consul servers (consensus)
- Raft consensus algorithm
- Automatic leader election

**Server Configuration:**
```hcl
# consul.hcl
server = true
bootstrap_expect = 3
datacenter = "dc1"
data_dir = "/consul/data"
ui = true

retry_join = [
  "consul-1",
  "consul-2",
  "consul-3"
]
```

**Client Configuration:**
```hcl
server = false
datacenter = "dc1"
retry_join = [
  "consul-1",
  "consul-2",
  "consul-3"
]
```

#### Phase 2: Multi-Datacenter Federation (Months 3-6)

**Architecture:**
- Primary DC: 5 servers
- Secondary DCs: 3 servers each
- WAN gossip for cross-DC communication

**Federation Setup:**
```hcl
# Primary DC (dc1)
server = true
bootstrap_expect = 5
datacenter = "dc1"

# WAN join
retry_join_wan = [
  "consul-dc2-1",
  "consul-dc3-1"
]
```

**Service Discovery:**
- **Local queries:** Fast (LAN gossip)
- **Cross-DC queries:** Slower (WAN gossip)
- **Service mesh:** Connect for secure communication

**Capacity Planning:**

| Metric | Current | Target (12 months) | Target (24 months) |
|--------|---------|-------------------|---------------------|
| **Services** | 1,000 | 10,000 | 50,000 |
| **Health Checks** | 5,000 | 50,000 | 250,000 |
| **Servers** | 1 | 11 (3 DCs) | 20 (5 DCs) |
| **Datacenters** | 1 | 3 | 5 |

**Cost Estimation:**
- **Current:** $100/month
- **12 months:** $1,100/month (11 servers)
- **24 months:** $2,000/month (20 servers)

---

## Kafka Scaling

### Current State

- **Deployment:** Single Kafka broker
- **Topics:** 3 topics
- **Partitions:** 1 partition per topic
- **Throughput:** ~1,000 messages/second

### Target State (12 months)

- **Deployment:** Kafka cluster (3+ brokers)
- **Topics:** 5+ topics
- **Partitions:** 10-20 partitions per topic
- **Throughput:** ~10,000 messages/second

### Scaling Strategy

#### Phase 1: Kafka Cluster (Months 1-2)

**Configuration:**
- 3 Kafka brokers
- Zookeeper ensemble (3 nodes) or KRaft mode
- Replication factor: 3

**Broker Configuration:**
```properties
# server.properties
broker.id=1
num.network.threads=8
num.io.threads=8
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600
log.retention.hours=168  # 7 days
log.segment.bytes=1073741824  # 1GB
```

#### Phase 2: Partitioning Strategy (Months 2-3)

**Topic Partitioning:**

**heartbeat-queue:**
- **Partitions:** 10-20 (based on throughput)
- **Replication:** 3
- **Key:** `serviceId` (for ordering)
- **Retention:** 7 days

**config-refresh:**
- **Partitions:** 5-10
- **Replication:** 3
- **Key:** `serviceId`
- **Retention:** 3 days

**drift-events:**
- **Partitions:** 5-10
- **Replication:** 3
- **Key:** `serviceId`
- **Retention:** 30 days

**Partition Assignment:**
```java
// Producer configuration
properties.put("partitioner.class", "org.apache.kafka.clients.producer.internals.DefaultPartitioner");
properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");

// Consumer configuration
properties.put("group.id", "heartbeat-processor");
properties.put("max.partition.fetch.bytes", "1048576"); // 1MB
```

#### Consumer Group Optimization

**Parallel Processing:**
- **Consumers per partition:** 1 (max)
- **Total consumers:** Equal to partition count
- **Consumer lag monitoring:** Critical

**Consumer Configuration:**
```yaml
spring:
  kafka:
    consumer:
      group-id: heartbeat-processor
      auto-offset-reset: earliest
      enable-auto-commit: false
      max-poll-records: 200
      fetch-min-size: 1024
      fetch-max-wait: 500ms
    listener:
      concurrency: 10  # Match partition count
      type: batch
      ack-mode: manual
```

**Capacity Planning:**

| Metric | Current | Target (12 months) | Target (24 months) |
|--------|---------|-------------------|---------------------|
| **Brokers** | 1 | 3 | 5 |
| **Partitions (total)** | 3 | 30 | 60 |
| **Throughput** | 1K msg/s | 10K msg/s | 50K msg/s |
| **Storage** | 10GB | 100GB | 500GB |
| **Retention** | 7 days | 7 days | 7 days |

**Cost Estimation:**
- **Current:** $150/month
- **12 months:** $450/month (3 brokers)
- **24 months:** $750/month (5 brokers)

---

## Monitoring & Alerting

### Key Metrics

**MongoDB:**
- Replication lag
- Shard balance
- Query performance
- Connection pool usage

**Redis:**
- Memory usage
- Cache hit rate
- Replication lag
- Cluster health

**Consul:**
- Service registration rate
- Health check failures
- WAN gossip latency
- Leader election events

**Kafka:**
- Consumer lag
- Partition balance
- Broker disk usage
- Network throughput

### Alerting Thresholds

| Component | Metric | Warning | Critical |
|-----------|--------|---------|----------|
| **MongoDB** | Replication lag | >5s | >30s |
| **MongoDB** | Shard imbalance | >20% | >50% |
| **Redis** | Memory usage | >80% | >95% |
| **Redis** | Cache hit rate | <75% | <60% |
| **Kafka** | Consumer lag | >1,000 | >10,000 |
| **Kafka** | Broker disk | >80% | >90% |

---

## Migration Strategy

### Phased Rollout

1. **Phase 1:** Deploy replica sets/clusters alongside existing
2. **Phase 2:** Migrate read traffic (test)
3. **Phase 3:** Migrate write traffic (gradual)
4. **Phase 4:** Decommission old infrastructure

### Rollback Plan

- Keep old infrastructure for 2 weeks
- Feature flags for gradual migration
- Automated rollback triggers
- Data synchronization verification

---

## Cost Summary

| Component | Current | 12 Months | 24 Months | Growth |
|-----------|---------|-----------|-----------|--------|
| **MongoDB** | $200 | $2,000 | $4,000 | 20x |
| **Redis** | $50 | $500 | $1,000 | 20x |
| **Consul** | $100 | $1,100 | $2,000 | 20x |
| **Kafka** | $150 | $450 | $750 | 5x |
| **Total** | $500 | $4,050 | $7,750 | 15.5x |

**Note:** Costs are estimates for cloud infrastructure. On-premises costs may vary.

---

## References

- [MongoDB Sharding Guide](https://docs.mongodb.com/manual/sharding/)
- [Redis Cluster Tutorial](https://redis.io/topics/cluster-tutorial)
- [Consul Federation](https://www.consul.io/docs/enterprise/federation)
- [Kafka Scaling](https://kafka.apache.org/documentation/#operations)

---

**Next:** Review [Performance Optimization Details](./performance-optimization.md) for application-level optimizations.

