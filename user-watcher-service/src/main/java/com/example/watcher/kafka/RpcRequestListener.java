package com.example.watcher.kafka;

import com.example.kafka.constants.KafkaConstants;
import com.example.kafka.dto.RpcRequest;
import com.example.kafka.dto.RpcResponse;
import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import com.example.user.service.port.UserServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RpcRequestListener {

  private final UserServicePort userService;
  private final KafkaTemplate<String, Object> watcherKafkaTemplate;

  @KafkaListener(topics = KafkaConstants.TOPIC_USER_RPC_REQUEST, groupId = "user-watcher-group")
  public void onRequest(@Payload RpcRequest request) {
    try {
      log.debug("Received RPC request: action={}, correlationId={}", request.action(), request.correlationId());
      RpcResponse response = handle(request);
      watcherKafkaTemplate.send(KafkaConstants.TOPIC_USER_RPC_REPLY, request.correlationId(), response);
      log.debug("Sent RPC response: correlationId={}", request.correlationId());
    } catch (Exception e) {
      log.error("RPC request handling error for correlationId={}", request.correlationId(), e);
      watcherKafkaTemplate.send(KafkaConstants.TOPIC_USER_RPC_REPLY, request.correlationId(),
          new RpcResponse(request.correlationId(), "error", e.getMessage(), null));
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
            "totalPages", (int) Math.ceil((double) total / criteria.getSize())
        );
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
        .status(data.get("status") != null ? 
            User.UserStatus.valueOf((String) data.get("status")) : User.UserStatus.ACTIVE)
        .role(data.get("role") != null ? 
            User.UserRole.valueOf((String) data.get("role")) : User.UserRole.USER)
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
        .status(data.get("status") != null ? 
            User.UserStatus.valueOf((String) data.get("status")) : null)
        .role(data.get("role") != null ? 
            User.UserRole.valueOf((String) data.get("role")) : null)
        .includeDeleted((Boolean) data.getOrDefault("includeDeleted", false))
        .createdAfter((LocalDateTime) data.get("createdAfter"))
        .createdBefore((LocalDateTime) data.get("createdBefore"))
        .sortCriteria((List<com.example.common.domain.SortCriterion>) data.get("sortCriteria"))
        .build();
  }
}