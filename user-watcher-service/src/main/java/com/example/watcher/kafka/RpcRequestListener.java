package com.example.watcher.kafka;

import com.example.kafka.constants.KafkaConstants;
import com.example.kafka.dto.RpcRequest;
import com.example.kafka.dto.RpcResponse;
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
        @SuppressWarnings("unchecked")
        Map<String, Object> userData = (Map<String, Object>) payload;
        User domain = createUserFromMap(userData);
        User created = userService.create(domain);
        yield new RpcResponse(req.correlationId(), "ok", null, created);
      }
      case "getUser" -> {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestData = (Map<String, Object>) payload;
        String id = (String) requestData.get("id");
        Optional<User> user = userService.getById(id);
        if (user.isPresent()) {
          yield new RpcResponse(req.correlationId(), "ok", null, user.get());
        } else {
          yield new RpcResponse(req.correlationId(), "not_found", "User not found", null);
        }
      }
      case "updateUser" -> {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestData = (Map<String, Object>) payload;
        String id = (String) requestData.get("id");
        Optional<User> existing = userService.getById(id);
        if (existing.isEmpty()) {
          yield new RpcResponse(req.correlationId(), "not_found", "User not found", null);
        }
        User domain = createUserFromMap(requestData);
        User updated = userService.update(domain);
        yield new RpcResponse(req.correlationId(), "ok", null, updated);
      }
      case "deleteUser" -> {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestData = (Map<String, Object>) payload;
        String id = (String) requestData.get("id");
        Optional<User> existing = userService.getById(id);
        if (existing.isEmpty()) {
          yield new RpcResponse(req.correlationId(), "not_found", "User not found", null);
        }
        userService.delete(id);
        yield new RpcResponse(req.correlationId(), "ok", null, null);
      }
      case "listUsers" -> {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestData = (Map<String, Object>) payload;
        UserQueryCriteria criteria = createCriteriaFromMap(requestData);
        List<User> users = userService.listByCriteria(criteria);
        long total = userService.countByCriteria(criteria);
        Map<String, Object> result = Map.of(
            "items", users,
            "page", criteria.getPage(),
            "size", criteria.getSize(),
            "total", total,
            "totalPages", (int) Math.ceil((double) total / criteria.getSize()));
        yield new RpcResponse(req.correlationId(), "ok", null, result);
      }
      default -> new RpcResponse(req.correlationId(), "error", "Unknown action: " + action, null);
    };
  }

  private User createUserFromMap(Map<String, Object> data) {
    return User.builder()
        .id((String) data.get("id"))
        .name((String) data.get("name"))
        .phone((String) data.get("phone"))
        .address((String) data.get("address"))
        .status(
            data.get("status") != null ? User.UserStatus.valueOf((String) data.get("status")) : User.UserStatus.ACTIVE)
        .role(data.get("role") != null ? User.UserRole.valueOf((String) data.get("role")) : User.UserRole.USER)
        .version((Integer) data.get("version"))
        .createdAt((LocalDateTime) data.get("createdAt"))
        .createdBy((String) data.get("createdBy"))
        .updatedAt((LocalDateTime) data.get("updatedAt"))
        .updatedBy((String) data.get("updatedBy"))
        .deleted((Boolean) data.get("deleted"))
        .deletedAt((LocalDateTime) data.get("deletedAt"))
        .deletedBy((String) data.get("deletedBy"))
        .build();
  }

  private UserQueryCriteria createCriteriaFromMap(Map<String, Object> data) {
    return UserQueryCriteria.builder()
        .page((Integer) data.getOrDefault("page", 0))
        .size((Integer) data.getOrDefault("size", 20))
        .search((String) data.get("search"))
        .status(data.get("status") != null ? User.UserStatus.valueOf((String) data.get("status")) : null)
        .role(data.get("role") != null ? User.UserRole.valueOf((String) data.get("role")) : null)
        .includeDeleted((Boolean) data.getOrDefault("includeDeleted", false))
        .createdAfter((LocalDateTime) data.get("createdAfter"))
        .createdBefore((LocalDateTime) data.get("createdBefore"))
        .sortCriteria((List<com.example.common.domain.SortCriterion>) data.get("sortCriteria"))
        .build();
  }
}