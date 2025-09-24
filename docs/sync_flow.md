I'll scan the key classes used in GET/POST /users across services to explain the end-to-end flow, then reference important code lines directly.

[6 tools called]

Let me map the end-to-end flow of GET /users and POST /users across your services and highlight exactly which classes do what, and why each important line/method/parameter exists.

### Plan

- Walk through request flows for GET /users and POST /users from `user-rest-spring-service` down to `user-watcher-service` and back, citing key code lines.
- Explain the roles of controller, service, port, adapter, server handler, Kafka listeners, mappers, and RPC bridge.
- No code changes required.

### Flow and Key Classes

#### Entry: `user-rest-spring-service` — HTTP Controller

- For POST /users (create) and GET /users (list).

```49:87:user-rest-spring-service/src/main/java/com/example/rest/user/controller/UserController.java
  @PostMapping
  public ResponseEntity<CreateUserResponse> create(@RequestBody @Validated CreateUserRequest request) {
    var domainUser = UserMapper.toDomainFromCreateRequest(request);
    var created = userService.create(domainUser);
    var userResponse = UserMapper.toResponse(created);
    var response = ResponseBuilder.buildCreateUserResponse(userResponse);
    return ResponseEntity.ok(response);
  }
```

- Why:
  - `@PostMapping`: binds POST /users.
  - `@Validated CreateUserRequest request`: JSON body → validated DTO.
  - `UserMapper.toDomain...`: map HTTP DTO → domain model to keep controller thin.
  - `userService.create(...)`: delegate to application service (hexagonal boundary).
  - `ResponseBuilder...`: build API DTO to return.

```176:199:user-rest-spring-service/src/main/java/com/example/rest/user/controller/UserController.java
  @GetMapping
  public ResponseEntity<ListUsersResponse> list(@Validated ListUsersRequest request) {
    var criteria = UserMapper.toQueryCriteria(request);
    var users = userService.listByCriteria(criteria);
    var total = userService.countByCriteria(criteria);
    var totalPages = (int) Math.ceil((double) total / (double) request.getSize());
    var items = users.stream().map(UserMapper::toResponse).toList();
    var response = ResponseBuilder.buildListUsersResponse(items, request.getPage(), request.getSize(), total, totalPages);
    return ResponseEntity.ok(response);
  }
```

- Why:
  - `@GetMapping`: binds GET /users.
  - `ListUsersRequest request`: query params bound to a request DTO.
  - Split into list + count to compute paging.

#### Application service: `UserService` — orchestration, resilience, caching

```46:65:user-rest-spring-service/src/main/java/com/example/rest/user/service/UserService.java
  @CircuitBreaker(name = "thrift-service", fallbackMethod = "createFallback")
  @Retry(name = "thrift-service")
  @Caching(evict = {
    @CacheEvict(value = CacheConstants.USER_BY_ID_CACHE, key = "'" + CacheConstants.USER_SERVICE_GET_BY_ID_KEY_PREFIX + "' + #user.id", condition = "#user != null && #user.id != null"),
    @CacheEvict(value = CacheConstants.USERS_BY_CRITERIA_CACHE, allEntries = true),
    @CacheEvict(value = CacheConstants.COUNT_BY_CRITERIA_CACHE, allEntries = true)
  })
  public User create(User user) {
    return thriftClient.create(user);
  }
```

- Why:
  - Circuit breaker + retry: guard Thrift backend.
  - Cache eviction: mutate ops invalidate read caches.
  - Delegates via port to the adapter (hexagonal).

```129:160:user-rest-spring-service/src/main/java/com/example/rest/user/service/UserService.java
  @CircuitBreaker(name = "thrift-service", fallbackMethod = "listByCriteriaFallback")
  @Cacheable(value = CacheConstants.USERS_BY_CRITERIA_CACHE, key = "'" + CacheConstants.USER_SERVICE_LIST_BY_CRITERIA_KEY_PREFIX + "' + #criteria.hashCode()", sync = true)
  public List<User> listByCriteria(UserQueryCriteria criteria) {
    return thriftClient.listByCriteria(criteria);
  }

  @Cacheable(value = CacheConstants.COUNT_BY_CRITERIA_CACHE, key = "'" + CacheConstants.USER_SERVICE_COUNT_BY_CRITERIA_KEY_PREFIX + "' + #criteria.hashCode()", sync = true)
  public long countByCriteria(UserQueryCriteria criteria) {
    return thriftClient.countByCriteria(criteria);
  }
```

- Why:
  - Cache GET /users path for perf; `sync = true` prevents stampede.

#### Port: `ThriftUserClientPort` — the boundary interface

```9:23:user-rest-spring-service/src/main/java/com/example/rest/user/port/ThriftUserClientPort.java
public interface ThriftUserClientPort {
  User create(User user);
  Optional<User> getById(String id);
  User update(User user);
  void delete(String id);
  List<User> listByCriteria(UserQueryCriteria criteria);
  long countByCriteria(UserQueryCriteria criteria);
}
```

- Why:
  - Defines what the application needs from an external user service without binding to transport details.

#### Adapter: `ThriftUserClientAdapter` — actual Thrift client

- Establishes Thrift RPC connection and maps domain ↔ Thrift DTOs. Uses `TSocket` + `TBinaryProtocol`.

```30:40:user-rest-spring-service/src/main/java/com/example/rest/user/adapter/thrift/ThriftUserClientAdapter.java
  private UserService.Client client() throws Exception {
    TTransport transport = new TSocket(thriftClientProperties.getHost(),
        thriftClientProperties.getPort(),
        thriftClientProperties.getTimeout());
    transport.open();
    TBinaryProtocol protocol = new TBinaryProtocol(transport);
    return new UserService.Client(protocol);
  }
```

- Why:
  - `TSocket(host, port, timeout)`: blocking RPC connection to Thrift server.
  - `TBinaryProtocol`: compact, fast serialization over the socket.

POST /users (create):

```104:134:user-rest-spring-service/src/main/java/com/example/rest/user/adapter/thrift/ThriftUserClientAdapter.java
  public User create(User user) {
    UserService.Client client = null;
    try {
      client = client();
      TCreateUserRequest request = new TCreateUserRequest()
          .setName(user.getName())
          .setPhone(user.getPhone())
          .setAddress(user.getAddress())
          .setStatus(user.getStatus() != null ? TUserStatus.valueOf(user.getStatus().name()) : TUserStatus.ACTIVE)
          .setRole(user.getRole() != null ? TUserRole.valueOf(user.getRole().name()) : TUserRole.USER);

      TCreateUserResponse response = client.createUser(request);
      if (response.getStatus() == 0) {
        User created = toDomain(response.getUser());
        return created;
      } else {
        throw new ThriftServiceException("Failed to create user: " + response.getMessage(), "create");
      }
    } finally {
      closeClient(client);
    }
  }
```

- Why:
  - Build RPC request from domain user; default status/role.
  - Call Thrift method `createUser`.
  - Map Thrift response to domain `User`.

GET /users (list):

```230:263:user-rest-spring-service/src/main/java/com/example/rest/user/adapter/thrift/ThriftUserClientAdapter.java
  public List<User> listByCriteria(UserQueryCriteria criteria) {
    UserService.Client client = null;
    try {
      client = client();
      TListUsersRequest request = toThriftRequest(criteria);

      TListUsersResponse response = client.listUsers(request);
      if (response.getStatus() == 0) {
        List<User> users = response.getItems().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
        return users;
      } else {
        throw new ThriftServiceException("Failed to list users by criteria: " + response.getMessage(), "listByCriteria");
      }
    } finally {
      closeClient(client);
    }
  }
```

- Why:
  - Converts paging/search/sort criteria to Thrift DTO.
  - Maps list of Thrift users → domain users.

#### Thrift Server: `user-thrift-server-service` — RPC facade for Kafka

- Implements Thrift service interface and bridges requests to Kafka “RPC” via a correlation-id mechanism stored with Redis (seen via `redisRpcService`).

POST /users path inside Thrift server:

```147:177:user-thrift-server-service/src/main/java/com/example/thriftserver/service/UserServiceThriftHandler.java
  public TCreateUserResponse createUser(TCreateUserRequest request) throws TException {
    TUserCreateRequest thriftRequest = new TUserCreateRequest();
    thriftRequest.setName(request.getName());
    thriftRequest.setPhone(request.getPhone());
    thriftRequest.setAddress(request.getAddress() != null ? request.getAddress() : "");
    thriftRequest.setStatus(convertToKafkaUserStatus(request.getStatus()));
    thriftRequest.setRole(convertToKafkaUserRole(request.getRole()));

    TUserCreateResponse response = redisRpcService.sendRpcRequest(
        topicsProperties.getUserCreateRequest(),
        topicsProperties.getUserCreateResponse(),
        thriftRequest,
        TUserCreateResponse.class);

    if (response.getUser() != null) {
      TUser tUser = convertToTUser(response.getUser());
      return new TCreateUserResponse(ThriftConstants.STATUS_SUCCESS, ThriftConstants.SUCCESS_USER_CREATED, tUser);
    } else {
      return new TCreateUserResponse(ThriftConstants.STATUS_ERROR, "User creation failed", null);
    }
  }
```

- Why:
  - Converts Thrift-server DTOs (user RPC schema) to Kafka Thrift message schema (decoupled packages).
  - `redisRpcService.sendRpcRequest(reqTopic, replyTopic, payload, respType)`: publishes to Kafka with `KafkaHeaders.REPLY_TOPIC` and `KafkaHeaders.CORRELATION_ID`, stores a future in Redis keyed by correlation-id, waits for the matching response.

GET /users path inside Thrift server:

```271:317:user-thrift-server-service/src/main/java/com/example/thriftserver/service/UserServiceThriftHandler.java
  public TListUsersResponse listUsers(TListUsersRequest request) throws TException {
    TUserListRequest thriftRequest = new TUserListRequest();
    if (request.isSetPage()) thriftRequest.setPage(request.getPage());
    if (request.isSetSize()) thriftRequest.setSize(request.getSize());
    if (request.isSetSearch()) thriftRequest.setSearch(request.getSearch());
    if (request.isSetStatus()) thriftRequest.setStatus(convertToKafkaUserStatus(request.getStatus()));
    if (request.isSetRole()) thriftRequest.setRole(convertToKafkaUserRole(request.getRole()));
    if (request.isSetIncludeDeleted()) thriftRequest.setIncludeDeleted(request.isIncludeDeleted());

    TUserListResponse response = redisRpcService.sendRpcRequest(
        topicsProperties.getUserListRequest(),
        topicsProperties.getUserListResponse(),
        thriftRequest,
        TUserListResponse.class);

    List<TUser> users = response.getItems().stream().map(this::convertToTUser).toList();
    return new TListUsersResponse(
        ThriftConstants.STATUS_SUCCESS,
        ThriftConstants.SUCCESS_USERS_RETRIEVED,
        users,
        response.getPage(), response.getSize(), response.getTotal(), response.getTotalPages());
  }
```

- Why:
  - Same Kafka “RPC” pattern with correlation id.

Response listeners in Thrift server complete the RPC futures:

```323:369:user-thrift-server-service/src/main/java/com/example/thriftserver/service/UserServiceThriftHandler.java
@KafkaListener(topics = "user.create.response", ... )
public void onCreateUserResponse(ConsumerRecord<String, byte[]> record) {
  String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
  TUserCreateResponse response = ThriftKafkaMessageHandler.deserializeMessage(record, TUserCreateResponse.class);
  redisRpcService.handleResponse(correlationId, response);
}
```

- Why:
  - Consumes reply topic, deserializes Thrift payload, signals waiting caller via correlation id.

Deprecated local RPC template (replaced by Redis-backed one):

```21:63:user-thrift-server-service/src/main/java/com/example/thriftserver/service/RpcService.java
@Deprecated
public class RpcService {
  public <T> T sendRpcRequest(String requestTopic, String responseTopic, Object request, Class<T> responseType) {
    String correlationId = UUID.randomUUID().toString();
    ProducerRecord<String, Object> record = new ProducerRecord<>(requestTopic, correlationId, request);
    record.headers().add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, responseTopic.getBytes()));
    record.headers().add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes()));
    // ... store a future and wait; completed by handleResponse(...)
  }
}
```

- Why:
  - Illustrates the correlation-id RPC pattern over Kafka.

#### Worker: `user-watcher-service` — consumes Kafka, does DB, replies

- POST /users handling (create):

```53:71:user-watcher-service/src/main/java/com/example/watcher/kafka/UserServiceThriftKafkaListener.java
@KafkaListener(topics = "user.create.request", ...)
public void onCreateUserRequest(ConsumerRecord<String, byte[]> record) {
  String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
  String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
  TUserCreateRequest request = ThriftKafkaMessageHandler.deserializeMessage(record, TUserCreateRequest.class);

  User domain = userMappingService.createUserFromThriftRequest(request);
  User created = userService.create(domain);
  TUserResponse userResponse = userMappingService.createThriftUserResponse(created);
  TUserCreateResponse response = new TUserCreateResponse(userResponse);
  responseService.sendResponse(replyTopic, correlationId, response);
}
```

- Why:

  - Deserialize Thrift payload from bytes by topic/type.
  - Map to domain, call DB service, map back, send to reply topic with correlation id.

- GET /users handling (list):

```148:172:user-watcher-service/src/main/java/com/example/watcher/kafka/UserServiceThriftKafkaListener.java
@KafkaListener(topics = "user.list.request", ...)
public void onListUsersRequest(ConsumerRecord<String, byte[]> record) {
  String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
  String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
  TUserListRequest request = ThriftKafkaMessageHandler.deserializeMessage(record, TUserListRequest.class);

  UserQueryCriteria criteria = userMappingService.createCriteriaFromThriftRequest(request);
  List<User> users = userService.listByCriteria(criteria);
  long total = userService.countByCriteria(criteria);
  List<TUserResponse> userResponses = users.stream().map(userMappingService::createThriftUserResponse).collect(Collectors.toList());
  int totalPages = (int) Math.ceil((double) total / criteria.getSize());
  TUserListResponse response = new TUserListResponse(userResponses, criteria.getPage(), criteria.getSize(), total, totalPages);
  responseService.sendResponse(replyTopic, correlationId, response);
}
```

- Why:
  - Computes pagination similar to REST layer but within worker, returns in Thrift message.

#### Serialization/Deserialization used in Kafka boundary

- Both Thrift server and watcher use shared helpers to deserialize bytes to Thrift classes and serialize back. This allows using Kafka without Avro/Schema Registry while keeping a schema discipline.

```15:47:shared-common/src/main/java/com/example/kafka/util/ThriftKafkaMessageHandler.java
public static <T extends TBase<?, ?>> T deserializeMessage(ConsumerRecord<String, byte[]> record, Class<T> expectedType) {
  return ThriftMessageDeserializer.deserialize(record.topic(), record.value(), expectedType);
}
```

- Why:
  - Strongly-typed deserialization per topic, reduces boilerplate.

### Summary of GET /users request path

- Client → REST GET /users → `UserController.list()` → `UserService.listByCriteria()` (circuit breaker + cache) → `ThriftUserClientAdapter.listByCriteria()` → Thrift socket RPC `UserService.Client#listUsers()` → `user-thrift-server-service.UserServiceThriftHandler.listUsers()` → publish Kafka request (with correlation id) → `user-watcher-service` consumes, queries DB, builds `TUserListResponse` → publishes reply topic → Thrift server consumes reply, completes Redis future → returns Thrift `TListUsersResponse` to adapter → adapter maps to domain users → service caches + returns → controller maps to API DTO and responds.

### Summary of POST /users request path

- Client → REST POST /users → `UserController.create()` → `UserService.create()` (circuit breaker, evict caches) → `ThriftUserClientAdapter.create()` → Thrift socket RPC `UserService.Client#createUser()` → `user-thrift-server-service.UserServiceThriftHandler.createUser()` → publish Kafka request → `user-watcher-service` consumes, writes DB, replies → Thrift server completes future → returns `TCreateUserResponse` → adapter maps to domain `User` → service returns → controller maps to API DTO.

### Patch

- No changes needed. Code already follows clean boundaries and the flows are consistent with the architecture.

### Commands

- Build + Docker images (skip tests for speed):
  - Windows PowerShell:
    ```bash
    ./gradlew build buildDocker -x test
    ```
- Restart infra:
  ```bash
  docker compose down
  docker compose up -d
  ```
- E2E tests:
  ```bash
  ./gradlew :user-rest-spring-service:e2eTest
  ```

### Verification Checklist

- [ ] `./gradlew build buildDocker -x test` succeeds.
- [ ] `docker compose up -d` brings up Kafka, Redis, MongoDB, services healthy.
- [ ] REST POST /users creates a user and returns it; logs show Thrift RPC and Kafka round-trip.
- [ ] REST GET /users returns paged list; caches populate; subsequent calls faster.
- [ ] E2E: `./gradlew :user-rest-spring-service:e2eTest` passes.

### Follow-ups

- Add DLQ and retry topics for Kafka handlers to isolate poison messages.
- Add compression (lz4/zstd) for Kafka producers to reduce bandwidth.
- Emit and propagate correlation-id across HTTP→Thrift→Kafka in MDC for tracing.
- Rate limit REST endpoints and add validation constraints on ListUsersRequest paging caps.

I walked through GET/POST flows across `user-rest-spring-service` (controller → service → port → adapter), `user-thrift-server-service` (RPC handler → Kafka request/response bridging), and `user-watcher-service` (Kafka listeners → domain service → reply). The cited lines show why each method/parameter exists and how data is mapped across boundaries.

Let me walk you through exactly how `RedisRpcService` is used by `UserServiceThriftHandler`, what each important line/annotation does, how Redis coordinates the “RPC over Kafka” flow, and why responses are stored then deleted.

### Plan

- Explain Redis-backed RPC flow end-to-end in `RedisRpcService`.
- Call out `@CircuitBreaker`, `@Retry`, `@TimeLimiter` roles and their values.
- Clarify “pending request” vs “response” keys and the reason to store-then-delete responses.
- Show where Thrift generated classes come from and why methods have current signatures.
- No code changes are needed.

### Where Thrift classes are generated

The Thrift Java stubs are generated into `shared-common/build/generated-sources/thrift`, and added to the `main` source set so they compile with the rest of Java.

```102:155:shared-common/build.gradle
tasks.register('generateThrift') {
    // ...
    exec {
        executable 'thrift'
        args '-r', '--gen', 'java', '-out', generatedSourcesDir.absolutePath, f.absolutePath
    }
}
// Compile depends on codegen; include generated sources in the jar
compileJava {
    dependsOn tasks.named('generateThrift')
    options.encoding = 'UTF-8'
}
jar {
    from generatedSourcesDir
    archiveBaseName = 'shared-common'
    archiveVersion = version
}
```

That’s why your handlers can import `com.example.kafka.thrift.*` and call `read/write`-backed getters/setters generated by Thrift.

### RedisRpcService — the core “RPC over Kafka” bridge

This bean lives in `user-thrift-server-service`. It:

- Publishes a request message to a Kafka “request topic” with correlation headers.
- Records a “pending request” in Redis so the caller can survive restarts and the correlation can be resolved across nodes.
- Waits until the corresponding “response” value appears in Redis (populated by a reply listener), then returns it.
- Uses resilience annotations to harden network operations.

Key imports and purpose:

- `KafkaTemplate<String,Object>`: send Kafka messages with headers.
- `RedisTemplate<String,Object>`: simple Redis key/value store.
- `ObjectMapper`: serialize pending/response payloads.
- `ScheduledExecutorService`: offload blocking waits to a thread pool.

```32:44:user-thrift-server-service/src/main/java/com/example/thriftserver/service/RedisRpcService.java
@Service
@RequiredArgsConstructor
public class RedisRpcService {
  private static final String PENDING_REPLIES_KEY_PREFIX = "rpc:pending:";
  // Resilience “names” bind to external config (timeouts, thresholds, etc.)
  private static final String CIRCUIT_BREAKER_NAME = "rpc-service";
  private static final String RETRY_NAME = "rpc-service";
  private static final String TIME_LIMITER_NAME = "rpc-service";

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final AppProperties appProperties;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;
  private final ScheduledExecutorService scheduledExecutorService;
}
```

- Why string constants: they allow centralized resilience config via application.yml (same name across annotations).
- `AppProperties` supplies the RPC timeout; used consistently for TTLs and waiting.

#### Sending a request (async entry point)

```48:91:user-thrift-server-service/src/main/java/com/example/thriftserver/service/RedisRpcService.java
@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackRpcRequest")
@Retry(name = RETRY_NAME)
@TimeLimiter(name = TIME_LIMITER_NAME)
public <T> CompletableFuture<T> sendRpcRequestAsync(String requestTopic, String responseTopic,
    Object request, Class<T> responseType) {
  return CompletableFuture.supplyAsync(() -> {
    try {
      String correlationId = UUID.randomUUID().toString();
      String pendingKey = PENDING_REPLIES_KEY_PREFIX + correlationId;

      // Persist “pending” metadata (who is waiting and for what type)
      PendingRpcRequest pendingRequest = new PendingRpcRequest(correlationId, responseType.getName(),
          System.currentTimeMillis());
      storePendingRequest(pendingKey, pendingRequest);

      // Build Kafka record with headers for reply routing and correlation
      ProducerRecord<String, Object> record = new ProducerRecord<>(requestTopic, correlationId, request);
      record.headers().add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, responseTopic.getBytes()));
      record.headers().add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes()));

      kafkaTemplate.send(record).whenComplete((result, ex) -> {
        if (ex != null) {
          // if send fails, cleanup pending to avoid leaks
          cleanupPendingRequest(pendingKey);
        }
      });

      // Block (poll Redis) until the response appears or timeout
      return waitForResponse(correlationId, responseType);
    } catch (Exception e) {
      throw new RpcException("RPC request failed: " + e.getMessage(), e);
    }
  }, scheduledExecutorService);
}
```

- Method params:

  - `requestTopic`: Kafka topic to publish the request.
  - `responseTopic`: topic the callee will publish the response to (set in header so responders know where to reply).
  - `request`: the Thrift DTO payload (already constructed by the handler).
  - `responseType`: class token to deserialize the response JSON from Redis back into the expected type.

- Why correlation headers:

  - `KafkaHeaders.CORRELATION_ID`: the key used end-to-end to match replies.
  - `KafkaHeaders.REPLY_TOPIC`: tells the downstream where to send the reply.

- Why Redis “pending” key first:

  - A response could arrive very fast; having a pending marker ensures the resolver knows someone is waiting.
  - Survives restarts of the Thrift server instance.

- Why `CompletableFuture.supplyAsync(..., scheduledExecutorService)`:

  - Non-block the calling thread and isolate wait loops into a managed pool (we still do a short polling wait in Redis).

- `@CircuitBreaker/@Retry/@TimeLimiter`:
  - `name = "rpc-service"`: links to external configuration; you can tune thresholds/limits in `application.yml`.
  - CircuitBreaker: opens on failure rate threshold; prevents hammering Kafka/Redis when the system is degraded.
  - Retry: retries transient failures (e.g., sporadic send errors).
  - TimeLimiter: bounds the async future time; if the future exceeds configured time, it is cancelled and fallback is invoked.

#### Synchronous convenience wrapper

```96:104:user-thrift-server-service/src/main/java/com/example/thriftserver/service/RedisRpcService.java
public <T> T sendRpcRequest(String requestTopic, String responseTopic, Object request, Class<T> responseType) {
  return sendRpcRequestAsync(requestTopic, responseTopic, request, responseType)
      .get(appProperties.getRpcTimeoutSeconds(), TimeUnit.SECONDS);
}
```

- Why: make the handler code simple and consistent with Thrift’s blocking request/response semantics.

#### Handling a response (called by Kafka listeners in the same service)

In `UserServiceThriftHandler`, each reply listener extracts the `correlationId` and hands the Thrift payload to `handleResponse`:

```331:336:user-thrift-server-service/src/main/java/com/example/thriftserver/service/UserServiceThriftHandler.java
public void onCreateUserResponse(ConsumerRecord<String, byte[]> record) {
  String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
  TUserCreateResponse response = ThriftKafkaMessageHandler.deserializeMessage(record, TUserCreateResponse.class);
  redisRpcService.handleResponse(correlationId, response);
}
```

`handleResponse` stores the response body in Redis and clears the pending key:

```109:131:user-thrift-server-service/src/main/java/com/example/thriftserver/service/RedisRpcService.java
public void handleResponse(String correlationId, Object response) {
  String pendingKey = PENDING_REPLIES_KEY_PREFIX + correlationId;
  String responseKey = getResponseKey(correlationId);

  // Validate there is a waiter
  PendingRpcRequest pendingRequest = getPendingRequest(pendingKey);
  if (pendingRequest == null) {
    // no pending => dropped/late/duplicate; ignore
    return;
  }

  // Put the response with small TTL and remove pending marker
  storeResponse(responseKey, response);
  cleanupPendingRequest(pendingKey);
}
```

- Why store in Redis then delete the pending entry:
  - The waiting thread polls for `rpc:response:{correlationId}`. Writing the response to that key is the signal that the reply is ready.
  - Deleting the pending entry avoids stale “waiting” state once the response is available.
  - If we only push to an in-memory map, a restart would lose the reply; Redis persists it across nodes until the waiter reads it.
  - The dedicated “response key” enables the waiter to read-and-delete atomically without racing with other callers.

#### Waiting for the response

```200:227:user-thrift-server-service/src/main/java/com/example/thriftserver/service/RedisRpcService.java
private <T> T waitForResponse(String correlationId, Class<T> responseType) {
  String responseKey = getResponseKey(correlationId);
  long startTime = System.currentTimeMillis();
  long timeoutMs = appProperties.getRpcTimeoutSeconds() * 1000L;

  while (System.currentTimeMillis() - startTime < timeoutMs) {
    Object value = redisTemplate.opsForValue().get(responseKey);
    if (value != null) {
      redisTemplate.delete(responseKey);
      T response = objectMapper.readValue(value.toString(), responseType);
      return response;
    }
    Thread.sleep(50); // low-cost poll interval
  }

  // timeout: cleanup pending
  cleanupPendingRequest(PENDING_REPLIES_KEY_PREFIX + correlationId);
  throw new RpcException("RPC request timeout after " + appProperties.getRpcTimeoutSeconds() + " seconds");
}
```

- Why poll Redis (read-then-delete):
  - Avoid long blocking operations on Redis; simple polling is robust and easy to reason about.
  - Delete on read ensures the response is single-consumed and no residue is left (idempotence hygiene).
  - TTL on the response key adds extra safety if the waiter crashes before deletion.

#### Persistence helpers and TTLs

```169:176:user-thrift-server-service/src/main/java/com/example/thriftserver/service/RedisRpcService.java
private void storePendingRequest(String key, PendingRpcRequest request) {
  String json = objectMapper.writeValueAsString(request);
  redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(appProperties.getRpcTimeoutSeconds() + 10));
}
```

- Pending TTL = timeout + small cushion; avoids memory leaks from lost waiters.

```191:197:user-thrift-server-service/src/main/java/com/example/thriftserver/service/RedisRpcService.java
private void storeResponse(String key, Object response) {
  String json = objectMapper.writeValueAsString(response);
  redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(30)); // Short TTL
}
```

- Response TTL is short (30s) because:
  - The waiter should read/delete almost immediately.
  - If the waiter died, TTL prevents buildup; the response isn’t a durable record, only a synchronization signal.

#### Scheduled cleanup

```146:167:user-thrift-server-service/src/main/java/com/example/thriftserver/service/RedisRpcService.java
@Scheduled(fixedRate = 60000)
public void cleanupExpiredRequests() {
  var keys = redisTemplate.keys(PENDING_REPLIES_KEY_PREFIX + "*");
  // remove pending requests older than timeout to prevent leaks
}
```

- Why: protect against edge cases where pending keys persist due to unexpected failures.

#### Fallback for resilience

```136:141:user-thrift-server-service/src/main/java/com/example/thriftserver/service/RedisRpcService.java
public <T> CompletableFuture<T> fallbackRpcRequest(String requestTopic, String responseTopic,
    Object request, Class<T> responseType, Exception ex) {
  return CompletableFuture.failedFuture(
      new RpcException("RPC service temporarily unavailable: " + ex.getMessage(), ex));
}
```

- This is invoked when the circuit is open or the time limiter triggers; it propagates a clear, typed exception.

### Why store the response then delete it immediately?

- Synchronization: The waiter needs a durable-ish signal that outlives the process if it restarts. Writing to a Redis key the waiter knows to check provides this.
- Single consumption: After the waiter reads the response, it deletes the key so that:
  - Duplicates are not consumed twice.
  - Redis doesn’t accumulate unbounded responses.
- Crash safety: If the waiter crashes before reading, TTL garbage-collects the response. If the responder publishes twice, `pending` is already gone and `handleResponse` won’t requeue infinite responses (idempotent-ish behavior).

This pattern is effectively an at-least-once RPC bridge with deduplication guarded by correlation id, leveraging Redis as a small transient store that is shared across nodes.

### What do @CircuitBreaker, @Retry, @TimeLimiter do here?

- `@CircuitBreaker(name = "rpc-service")`: Monitors calls to `sendRpcRequestAsync`. If the error rate surpasses your configured threshold, it opens the circuit and short-circuits subsequent calls to `fallbackRpcRequest`. This prevents cascading failures when Kafka/Redis are unstable.
- `@Retry(name = "rpc-service")`: Retries transient failures (e.g., send exceptions) according to your retry backoff policy bound to the name “rpc-service”.
- `@TimeLimiter(name = "rpc-service")`: Enforces a maximum duration for the async operation; if exceeded, the future is cancelled and fallback is applied. It complements the application-level timeout used within `waitForResponse`.

Each `name` ties to external configuration where you set:

- CircuitBreaker: failure rate threshold, sliding window, wait duration in open state.
- Retry: max attempts, backoff/jitter.
- TimeLimiter: timeout duration.

Keeping all three under a common logical name simplifies tuning across environments.

### How `UserServiceThriftHandler` uses it

For example, list users:

```271:303:user-thrift-server-service/src/main/java/com/example/thriftserver/service/UserServiceThriftHandler.java
TUserListResponse response = redisRpcService.sendRpcRequest(
    topicsProperties.getUserListRequest(),
    topicsProperties.getUserListResponse(),
    thriftRequest,
    TUserListResponse.class);
```

- `topicsProperties.*`: centralizes topic names (so you can change naming without code scatter).
- The call blocks until the paired `onListUsersResponse(...)` persists the response under the correlation id; then it returns the Thrift response object to the Thrift server which maps it to its local `TListUsersResponse`.

### Commands

- Build all and Docker images (skip tests):
  ```bash
  ./gradlew build buildDocker -x test
  ```
- Restart stack:
  ```bash
  docker compose down
  docker compose up -d
  ```
- Run E2E:
  ```bash
  ./gradlew :user-rest-spring-service:e2eTest
  ```

### Verification Checklist

- [ ] Build and images succeed: `./gradlew build buildDocker -x test`.
- [ ] `docker compose up -d` starts Kafka/Redis/Mongo and all services become healthy.
- [ ] POST /users creates a user; logs show correlation ids and Redis pending/response operations.
- [ ] GET /users returns data; round-trip visible in logs.
- [ ] E2E tests pass: `./gradlew :user-rest-spring-service:e2eTest`.

### Follow-ups

- Consider replacing polling with Redis pub/sub or Streams for push-style wakeups if you need lower latency under very high concurrency.
- Add metrics on pending size, time-to-response percentiles, and CB open duration for better SLOs.
- Add DLQ and retry topics to isolate poison messages on the watcher side.
- Externalize full Resilience4j config for “rpc-service” and include jittered backoff.

In short: `RedisRpcService` provides a robust, cross-node RPC correlation layer on top of Kafka by using Redis as a transient, shared rendezvous. Pending keys mark in-flight requests; response keys signal completion. Resilience annotations bound under “rpc-service” ensure bounded latency, safe retries, and fast failure during incidents.
