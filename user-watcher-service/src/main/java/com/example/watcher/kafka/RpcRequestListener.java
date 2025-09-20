package com.example.watcher.kafka;

import com.example.kafka.constants.KafkaConstants;
import com.example.kafka.dto.RpcRequest;
import com.example.kafka.dto.RpcResponse;
import com.example.kafka.avro.*;
import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import com.example.user.service.port.UserServicePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RpcRequestListener {

  private final UserServicePort userService;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
      .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  // Add a PostConstruct method to verify the listener is being created
  @javax.annotation.PostConstruct
  public void init() {
    log.info("=== RpcRequestListener initialized ===");
  }

  @KafkaListener(topics = KafkaConstants.TOPIC_USER_RPC_REQUEST, groupId = "user-watcher-group", containerFactory = "kafkaListenerContainerFactory")
  public void onRequest(@Payload String requestJson, @Header("kafka_correlationId") byte[] kafkaCorrelationIdBytes) {
    log.info("=== RpcRequestListener.onRequest called with: {}", requestJson);
    log.info("Received kafka_correlationId bytes: {}", java.util.Arrays.toString(kafkaCorrelationIdBytes));
    try {
      RpcRequest request = objectMapper.readValue(requestJson, RpcRequest.class);
      log.info("Received RPC request: action={}, correlationId={}", request.action(), request.correlationId());
      RpcResponse response = handle(request);
      String responseJson = objectMapper.writeValueAsString(response);

      // Create ProducerRecord with the kafka_correlationId as the key (this is what
      // ReplyingKafkaTemplate expects)
      ProducerRecord<String, String> record = new ProducerRecord<>(KafkaConstants.TOPIC_USER_RPC_REPLY,
          new String(kafkaCorrelationIdBytes, StandardCharsets.UTF_8), responseJson);
      // Use the kafka correlation ID from the request header (this is what
      // ReplyingKafkaTemplate expects)
      record.headers().add(new RecordHeader("kafka_correlationId", kafkaCorrelationIdBytes));
      record.headers().add(new RecordHeader("correlationId", request.correlationId().getBytes(StandardCharsets.UTF_8)));

      kafkaTemplate.send(record);
      log.debug("Sent RPC response: kafkaCorrelationId={}, correlationId={}",
          new String(kafkaCorrelationIdBytes, StandardCharsets.UTF_8), request.correlationId());
    } catch (Exception e) {
      log.error("RPC request handling error for requestJson={}", requestJson, e);
      try {
        // Try to extract correlationId from JSON if possible
        String correlationId = extractCorrelationId(requestJson);
        RpcResponse errorResponse = new RpcResponse(correlationId, "error", e.getMessage(), null);
        String errorResponseJson = objectMapper.writeValueAsString(errorResponse);

        // Create ProducerRecord with the kafka_correlationId as the key for error
        // response
        ProducerRecord<String, String> errorRecord = new ProducerRecord<>(KafkaConstants.TOPIC_USER_RPC_REPLY,
            new String(kafkaCorrelationIdBytes, StandardCharsets.UTF_8), errorResponseJson);
        errorRecord.headers().add(new RecordHeader("kafka_correlationId", kafkaCorrelationIdBytes));
        errorRecord.headers().add(new RecordHeader("correlationId", correlationId.getBytes(StandardCharsets.UTF_8)));

        kafkaTemplate.send(errorRecord);
      } catch (Exception sendException) {
        log.error("Failed to send error response for requestJson={}", requestJson, sendException);
      }
    }
  }

  private String extractCorrelationId(String requestJson) {
    try {
      return objectMapper.readTree(requestJson).get("correlationId").asText();
    } catch (Exception e) {
      return "unknown";
    }
  }

  private RpcResponse handle(RpcRequest req) {
    String action = req.action();
    Object payload = req.payload();

    return switch (action) {
      case "ping" -> {
        String result = userService.ping();
        yield new RpcResponse(req.correlationId(), "ok", null, result);
      }
      case "createUser" -> {
        UserCreateRequest userCreateRequest = convertToUserCreateRequest(payload);
        User domain = createUserFromRequest(userCreateRequest);
        User created = userService.create(domain);
        UserResponse userResponse = createUserResponse(created);
        yield RpcResponse.success(req.correlationId(), convertUserResponseToJsonCompatible(userResponse));
      }
      case "getUser" -> {
        UserGetRequest userGetRequest = convertToUserGetRequest(payload);
        Optional<User> user = userService.getById(userGetRequest.getId());
        if (user.isPresent()) {
          UserResponse userResponse = createUserResponse(user.get());
          yield RpcResponse.success(req.correlationId(), convertUserResponseToJsonCompatible(userResponse));
        } else {
          yield RpcResponse.notFound(req.correlationId(), "User not found");
        }
      }
      case "updateUser" -> {
        UserUpdateRequest userUpdateRequest = convertToUserUpdateRequest(payload);
        Optional<User> existing = userService.getById(userUpdateRequest.getId());
        if (existing.isEmpty()) {
          yield RpcResponse.notFound(req.correlationId(), "User not found");
        }
        User domain = createUserFromUpdateRequest(userUpdateRequest);
        User updated = userService.update(domain);
        UserResponse userResponse = createUserResponse(updated);
        yield RpcResponse.success(req.correlationId(), convertUserResponseToJsonCompatible(userResponse));
      }
      case "deleteUser" -> {
        UserDeleteRequest userDeleteRequest = convertToUserDeleteRequest(payload);
        Optional<User> existing = userService.getById(userDeleteRequest.getId());
        if (existing.isEmpty()) {
          yield RpcResponse.notFound(req.correlationId(), "User not found");
        }
        userService.delete(userDeleteRequest.getId());
        yield RpcResponse.success(req.correlationId(), null);
      }
      case "listUsers" -> {
        UserListRequest userListRequest = convertToUserListRequest(payload);
        UserQueryCriteria criteria = createCriteriaFromRequest(userListRequest);
        List<User> users = userService.listByCriteria(criteria);
        long total = userService.countByCriteria(criteria);
        List<UserResponse> userResponses = users.stream()
            .map(this::createUserResponse)
            .toList();
        UserListResponse result = new UserListResponse(
            userResponses,
            criteria.getPage(),
            criteria.getSize(),
            total,
            (int) Math.ceil((double) total / criteria.getSize()));
        yield RpcResponse.success(req.correlationId(), convertUserListResponseToJsonCompatible(result));
      }
      default -> new RpcResponse(req.correlationId(), "error", "Unknown action: " + action, null);
    };
  }

  private User createUserFromRequest(UserCreateRequest request) {
    return User.builder()
        .name(request.getName())
        .phone(request.getPhone())
        .address(request.getAddress())
        .status(convertUserStatus(request.getStatus()))
        .role(convertUserRole(request.getRole()))
        .build();
  }

  private User createUserFromUpdateRequest(UserUpdateRequest request) {
    return User.builder()
        .id(request.getId())
        .name(request.getName())
        .phone(request.getPhone())
        .address(request.getAddress())
        .status(convertUserStatus(request.getStatus()))
        .role(convertUserRole(request.getRole()))
        .version(request.getVersion())
        .build();
  }

  private UserResponse createUserResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getPhone(),
        user.getAddress(),
        convertToAvroUserStatus(user.getStatus()),
        convertToAvroUserRole(user.getRole()),
        user.getCreatedAt() != null
            ? user.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            : null,
        user.getCreatedBy(),
        user.getUpdatedAt() != null
            ? user.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            : null,
        user.getUpdatedBy(),
        user.getVersion(),
        user.getDeleted(),
        user.getDeletedAt() != null
            ? user.getDeletedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            : null,
        user.getDeletedBy());
  }

  private UserQueryCriteria createCriteriaFromRequest(UserListRequest request) {
    return UserQueryCriteria.builder()
        .page(request.getPage() != null ? request.getPage() : 0)
        .size(request.getSize() != null ? request.getSize() : 20)
        .search(request.getSearch())
        .status(request.getStatus() != null ? convertUserStatus(request.getStatus()) : null)
        .role(request.getRole() != null ? convertUserRole(request.getRole()) : null)
        .includeDeleted(request.getIncludeDeleted() != null ? request.getIncludeDeleted() : false)
        .build();
  }

  // Helper methods for enum conversion
  private User.UserStatus convertUserStatus(UserStatus status) {
    return User.UserStatus.valueOf(status.name());
  }

  private User.UserRole convertUserRole(UserRole role) {
    return User.UserRole.valueOf(role.name());
  }

  private UserStatus convertToAvroUserStatus(User.UserStatus status) {
    return UserStatus.valueOf(status.name());
  }

  private UserRole convertToAvroUserRole(User.UserRole role) {
    return UserRole.valueOf(role.name());
  }

  // Conversion methods from JSON-compatible objects to Avro objects
  private UserCreateRequest convertToUserCreateRequest(Object payload) {
    if (payload instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) payload;
      return new UserCreateRequest(
          (String) map.get("name"),
          (String) map.get("phone"),
          (String) map.get("address"),
          map.get("status") != null ? UserStatus.valueOf((String) map.get("status")) : null,
          map.get("role") != null ? UserRole.valueOf((String) map.get("role")) : null);
    }
    return (UserCreateRequest) payload;
  }

  private UserGetRequest convertToUserGetRequest(Object payload) {
    if (payload instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) payload;
      return new UserGetRequest((String) map.get("id"));
    }
    return (UserGetRequest) payload;
  }

  private UserUpdateRequest convertToUserUpdateRequest(Object payload) {
    if (payload instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) payload;
      return new UserUpdateRequest(
          (String) map.get("id"),
          (String) map.get("name"),
          (String) map.get("phone"),
          (String) map.get("address"),
          map.get("status") != null ? UserStatus.valueOf((String) map.get("status")) : null,
          map.get("role") != null ? UserRole.valueOf((String) map.get("role")) : null,
          (Integer) map.get("version"));
    }
    return (UserUpdateRequest) payload;
  }

  private UserDeleteRequest convertToUserDeleteRequest(Object payload) {
    if (payload instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) payload;
      return new UserDeleteRequest((String) map.get("id"));
    }
    return (UserDeleteRequest) payload;
  }

  private UserListRequest convertToUserListRequest(Object payload) {
    if (payload instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) payload;
      return new UserListRequest(
          (Integer) map.get("page"),
          (Integer) map.get("size"),
          (String) map.get("search"),
          map.get("status") != null ? UserStatus.valueOf((String) map.get("status")) : null,
          map.get("role") != null ? UserRole.valueOf((String) map.get("role")) : null,
          (Boolean) map.get("includeDeleted"));
    }
    return (UserListRequest) payload;
  }

  // Conversion methods from Avro objects to JSON-compatible objects
  private Map<String, Object> convertUserResponseToJsonCompatible(UserResponse userResponse) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", userResponse.getId());
    map.put("name", userResponse.getName());
    map.put("phone", userResponse.getPhone());
    map.put("address", userResponse.getAddress());
    map.put("status", userResponse.getStatus() != null ? userResponse.getStatus().name() : null);
    map.put("role", userResponse.getRole() != null ? userResponse.getRole().name() : null);
    map.put("createdAt", userResponse.getCreatedAt());
    map.put("createdBy", userResponse.getCreatedBy());
    map.put("updatedAt", userResponse.getUpdatedAt());
    map.put("updatedBy", userResponse.getUpdatedBy());
    map.put("version", userResponse.getVersion());
    map.put("deleted", userResponse.getDeleted());
    map.put("deletedAt", userResponse.getDeletedAt());
    map.put("deletedBy", userResponse.getDeletedBy());
    return map;
  }

  private Map<String, Object> convertUserListResponseToJsonCompatible(UserListResponse listResponse) {
    Map<String, Object> map = new HashMap<>();
    List<Map<String, Object>> items = listResponse.getItems().stream()
        .map(this::convertUserResponseToJsonCompatible)
        .toList();
    map.put("items", items);
    map.put("page", listResponse.getPage());
    map.put("size", listResponse.getSize());
    map.put("total", listResponse.getTotal());
    map.put("totalPages", listResponse.getTotalPages());
    return map;
  }

}