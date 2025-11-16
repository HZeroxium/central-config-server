# Data Flow Diagrams
## Request Processing and Event Flows

---

## Heartbeat Processing Flow

### Single Heartbeat Processing

```mermaid
sequenceDiagram
    participant SDK as ZCM SDK
    participant API as HeartbeatController
    participant SVC as HeartbeatService
    participant CS as Config Server
    participant MONGO as MongoDB
    participant REDIS as Redis
    participant KAFKA as Kafka
    
    SDK->>API: POST /api/heartbeat<br/>{serviceName, instanceId, configHash}
    API->>SVC: processHeartbeat(payload)
    
    SVC->>MONGO: Find ServiceInstance by ID
    MONGO-->>SVC: ServiceInstance (or create new)
    
    SVC->>REDIS: Check cache for config hash
    alt Cache Hit
        REDIS-->>SVC: Expected hash
    else Cache Miss
        SVC->>CS: GET /{service}/{profile}/config
        CS-->>SVC: Configuration
        SVC->>SVC: Calculate expected hash
        SVC->>REDIS: Cache hash (30m TTL)
    end
    
    SVC->>SVC: Compare hashes<br/>appliedHash vs expectedHash
    
    alt Drift Detected
        SVC->>MONGO: Create DriftEvent<br/>Status: DETECTED
        SVC->>SVC: Mark instance as DRIFT
        SVC->>KAFKA: Publish refresh event<br/>Topic: config-refresh
        KAFKA->>SDK: Refresh event received
        SDK->>SDK: Reload configuration
    else No Drift
        SVC->>SVC: Clear drift flag (if exists)
    end
    
    SVC->>MONGO: Save/Update ServiceInstance
    SVC->>REDIS: Evict cache (instance, drift events)
    SVC-->>API: ServiceInstance
    API-->>SDK: 200 OK
```

**Reference:** `config-control-service/src/main/java/com/example/control/application/service/infra/HeartbeatService.java:97-138`

---

## Batch Heartbeat Processing

```mermaid
sequenceDiagram
    participant SDK as Multiple SDKs
    participant KAFKA_IN as Kafka Queue
    participant BATCH as HeartbeatBatchProcessor
    participant BATCH_SVC as HeartbeatBatchService
    participant CS as Config Server
    participant MONGO as MongoDB
    participant KAFKA_OUT as Kafka (Refresh)
    
    SDK->>KAFKA_IN: Send heartbeats (async)
    KAFKA_IN->>BATCH: Batch of 50-100 heartbeats
    
    BATCH->>BATCH_SVC: processBatch(payloads)
    
    Note over BATCH_SVC: Batch Load Phase
    BATCH_SVC->>MONGO: Batch load ServiceInstances by IDs
    MONGO-->>BATCH_SVC: Map<ID, ServiceInstance>
    
    BATCH_SVC->>MONGO: Batch load ApplicationServices by names
    MONGO-->>BATCH_SVC: Map<Name, ApplicationService>
    
    BATCH_SVC->>CS: Batch fetch config hashes<br/>(grouped by service:env)
    CS-->>BATCH_SVC: Map<service:env, hash>
    
    Note over BATCH_SVC: Processing Phase
    loop For each heartbeat
        BATCH_SVC->>BATCH_SVC: Compare hashes
        alt Drift Detected
            BATCH_SVC->>BATCH_SVC: Create DriftEvent (in memory)
            BATCH_SVC->>BATCH_SVC: Mark for refresh
        end
        BATCH_SVC->>BATCH_SVC: Update ServiceInstance (in memory)
    end
    
    Note over BATCH_SVC: Persistence Phase
    BATCH_SVC->>MONGO: Bulk upsert ServiceInstances
    BATCH_SVC->>MONGO: Save DriftEvents (if any)
    
    alt Drift Events Created
        BATCH_SVC->>KAFKA_OUT: Publish refresh events
        KAFKA_OUT->>SDK: Refresh events
        SDK->>SDK: Reload configurations
    end
    
    BATCH_SVC-->>BATCH: Success
```

**Reference:** `config-control-service/src/main/java/com/example/control/application/service/infra/HeartbeatBatchService.java:110-175`

**Performance:** 5x throughput improvement vs single processing

---

## Configuration Refresh Flow

```mermaid
sequenceDiagram
    participant ADMIN as Admin/System
    participant API as AdminController
    participant ORCH as RefreshOrchestrator
    participant KAFKA as Kafka
    participant SDK as ZCM SDK
    participant CS as Config Server
    participant APP as Application
    
    alt Manual Refresh
        ADMIN->>API: POST /api/admin/refresh?destination=service:**
        API->>ORCH: triggerRefresh(destination)
    else Auto Refresh (Drift Detected)
        Note over ORCH: Drift detected in heartbeat
        ORCH->>ORCH: triggerRefresh(service:instance)
    end
    
    ORCH->>KAFKA: Publish refresh event<br/>Topic: config-refresh<br/>{destination, timestamp}
    
    KAFKA->>SDK: Refresh event received
    SDK->>SDK: Check if destination matches
    
    alt Destination Matches
        SDK->>CS: GET /{service}/{profile}/config
        CS-->>SDK: New configuration
        SDK->>SDK: Calculate new hash
        SDK->>SDK: Compare with current hash
        
        alt Configuration Changed
            SDK->>APP: Refresh Spring Context<br/>@RefreshScope beans
            APP->>APP: Reload @ConfigurationProperties
            SDK->>SDK: Update config hash
            SDK->>SDK: Send next heartbeat with new hash
        else No Change
            SDK->>SDK: Log: No changes detected
        end
    else Destination Doesn't Match
        SDK->>SDK: Ignore event
    end
```

**Reference:** `config-control-service/src/main/java/com/example/control/application/service/infra/ConfigRefreshOrchestrator.java`

---

## Service Discovery Flow

```mermaid
sequenceDiagram
    participant SDK as ZCM SDK
    participant CONSUL as Consul
    participant CCS as Config Control Service
    participant LB as LoadBalancer
    
    Note over SDK: Service Startup
    SDK->>CONSUL: Register service<br/>TTL health check
    CONSUL-->>SDK: Registration confirmed
    
    Note over SDK: Periodic Heartbeat
    SDK->>CONSUL: TTL heartbeat (every 10s)
    CONSUL->>CONSUL: Update health status
    
    Note over SDK: Service Discovery Request
    SDK->>CCS: GET /api/services/{name}/instances
    CCS->>CONSUL: GET /v1/health/service/{name}?passing=true
    CONSUL-->>CCS: List of healthy instances
    CCS-->>SDK: Service instances
    
    SDK->>LB: Select instance<br/>(Round Robin, Random, etc.)
    LB-->>SDK: Selected instance
    SDK->>SDK: Make request to instance
```

**Reference:** `config-control-service/src/main/java/com/example/control/api/http/controller/infra/ServiceRegistryController.java`

---

## Approval Workflow Flow

```mermaid
sequenceDiagram
    participant USER as User
    participant API as ApprovalRequestController
    participant SVC as ApprovalRequestService
    participant MONGO as MongoDB
    participant ADMIN as SYS_ADMIN
    participant MANAGER as LINE_MANAGER
    participant EVENT as Event Publisher
    
    USER->>API: POST /api/approval-requests<br/>{serviceId, targetTeamId}
    API->>SVC: createRequest(request)
    
    SVC->>SVC: Evaluate approval gates<br/>(SYS_ADMIN, LINE_MANAGER)
    SVC->>MONGO: Save ApprovalRequest<br/>Status: PENDING
    
    Note over ADMIN,MANAGER: Approval Phase
    ADMIN->>API: POST /api/approval-requests/{id}/approve
    API->>SVC: approveRequest(id, decision)
    
    SVC->>MONGO: Load request (with optimistic lock)
    SVC->>SVC: Check if all gates approved
    
    alt All Gates Approved
        SVC->>SVC: Transfer ownership
        SVC->>MONGO: Update ApplicationService<br/>ownerTeamId = targetTeamId
        SVC->>MONGO: Update ApprovalRequest<br/>Status: APPROVED
        SVC->>EVENT: Publish ApprovalRequestApprovedEvent
        EVENT->>EVENT: Notify requester (email)
    else Gates Not Met
        SVC->>MONGO: Update ApprovalRequest<br/>Update gate status
    end
```

**Reference:** `config-control-service/src/main/java/com/example/control/application/service/ApprovalRequestService.java`

---

## Service Sharing Flow

```mermaid
sequenceDiagram
    participant OWNER as Service Owner
    participant API as ServiceShareController
    participant SVC as ServiceShareService
    participant MONGO as MongoDB
    participant GRANTEE as Grantee User/Team
    participant PERM as PermissionEvaluator
    
    OWNER->>API: POST /api/service-shares<br/>{serviceId, grantToId, permissions}
    API->>SVC: createShare(share)
    
    SVC->>SVC: Validate permissions<br/>(Owner or SYS_ADMIN)
    SVC->>MONGO: Save ServiceShare
    
    Note over GRANTEE: Access Request
    GRANTEE->>API: GET /api/services/{id}
    API->>PERM: Check permissions
    
    PERM->>MONGO: Load ServiceShare<br/>for serviceId + granteeId
    MONGO-->>PERM: ServiceShare (if exists)
    
    alt Share Exists
        PERM->>PERM: Check permissions<br/>(VIEW_SERVICE, EDIT_SERVICE, etc.)
        PERM-->>API: Permission granted
        API->>MONGO: Load ApplicationService
        MONGO-->>API: ApplicationService
        API-->>GRANTEE: Service data
    else No Share
        PERM-->>API: Permission denied
        API-->>GRANTEE: 403 Forbidden
    end
```

**Reference:** `config-control-service/src/main/java/com/example/control/application/service/ServiceShareService.java`

---

## Key-Value Store Flow

```mermaid
sequenceDiagram
    participant CLIENT as Client Service
    participant SDK as ZCM SDK
    participant API as KVController
    participant SVC as KVService
    participant KV_STORE as KV Store (Consul/etcd)
    participant KC as Keycloak
    
    Note over CLIENT: Read Operation
    CLIENT->>SDK: getKV(key)
    SDK->>KC: Get JWT token<br/>(Client credentials or pass-through)
    KC-->>SDK: JWT token
    SDK->>API: GET /api/kv/{key}<br/>Header: Authorization: Bearer {token}
    
    API->>API: Validate JWT token
    API->>SVC: getValue(key)
    SVC->>KV_STORE: GET /v1/kv/{key}
    KV_STORE-->>SVC: Value
    SVC-->>API: Value
    API-->>SDK: Value
    SDK-->>CLIENT: Value
    
    Note over CLIENT: Write Operation (Admin Only)
    CLIENT->>SDK: putKV(key, value)
    SDK->>API: PUT /api/kv/{key}<br/>Body: {value}
    API->>API: Check SYS_ADMIN role
    API->>SVC: putValue(key, value)
    SVC->>KV_STORE: PUT /v1/kv/{key}
    KV_STORE-->>SVC: Success
    SVC-->>API: Success
    API-->>SDK: Success
```

**Reference:** `config-control-service/src/main/java/com/example/control/api/http/controller/kv/KVController.java`

---

## Cache Invalidation Flow

```mermaid
sequenceDiagram
    participant SVC as Service
    participant CACHE as TwoLevelCache
    participant L1 as Caffeine (L1)
    participant L2 as Redis (L2)
    participant PUB as CacheInvalidationPublisher
    participant SUB as CacheInvalidationSubscriber
    
    SVC->>CACHE: @CacheEvict("service-instances")
    CACHE->>L1: Evict key
    CACHE->>L2: Evict key
    CACHE->>PUB: Publish invalidation message<br/>Topic: cache-invalidation
    
    PUB->>SUB: Invalidation message<br/>{cacheName, key}
    SUB->>L1: Evict key (if same cache)
    SUB->>L2: Evict key (if same cache)
```

**Reference:** `config-control-service/src/main/java/com/example/control/infrastructure/cache/CacheInvalidationPublisher.java`

---

## References

- [Heartbeat Processing](../README.md#heartbeat-processing-flow)
- [Batch Processing](../README.md#batch-processing-pattern)
- [Approval Workflow](../README.md#approval-workflow-state-machine)

