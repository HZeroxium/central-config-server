package com.example.watcher.kafka;

import com.example.kafka.constants.KafkaConstants;
import com.example.kafka.avro.*;
import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import com.example.user.service.port.UserServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceKafkaListener {

  private final UserServicePort userService;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @javax.annotation.PostConstruct
  public void init() {
    log.info("=== UserServiceKafkaListener initialized ===");
  }

  @KafkaListener(topics = KafkaConstants.TOPIC_PING_REQUEST, groupId = "user-watcher-group", containerFactory = "kafkaListenerContainerFactory")
  public void onPingRequest(PingRequest request) {
    log.info("Received ping request");
    try {
      String result = userService.ping();
      PingResponse response = new PingResponse(result);
      kafkaTemplate.send(KafkaConstants.TOPIC_PING_RESPONSE, response);
      log.debug("Sent ping response: {}", result);
    } catch (Exception e) {
      log.error("Error handling ping request", e);
    }
  }

  @KafkaListener(topics = KafkaConstants.TOPIC_USER_CREATE_REQUEST, groupId = "user-watcher-group", containerFactory = "kafkaListenerContainerFactory")
  public void onCreateUserRequest(UserCreateRequest request) {
    log.info("Received create user request: name={}", request.getName());
    try {
      User domain = createUserFromRequest(request);
      User created = userService.create(domain);
      UserResponse userResponse = createUserResponse(created);
      UserCreateResponse response = new UserCreateResponse(userResponse);
      kafkaTemplate.send(KafkaConstants.TOPIC_USER_CREATE_RESPONSE, response);
      log.debug("Sent create user response for user: {}", created.getId());
    } catch (Exception e) {
      log.error("Error handling create user request", e);
    }
  }

  @KafkaListener(topics = KafkaConstants.TOPIC_USER_GET_REQUEST, groupId = "user-watcher-group", containerFactory = "kafkaListenerContainerFactory")
  public void onGetUserRequest(UserGetRequest request) {
    log.info("Received get user request: id={}", request.getId());
    try {
      Optional<User> user = userService.getById(request.getId());
      UserGetResponse response;
      if (user.isPresent()) {
        UserResponse userResponse = createUserResponse(user.get());
        response = new UserGetResponse(userResponse, true);
      } else {
        response = new UserGetResponse(null, false);
      }
      kafkaTemplate.send(KafkaConstants.TOPIC_USER_GET_RESPONSE, response);
      log.debug("Sent get user response: found={}", user.isPresent());
    } catch (Exception e) {
      log.error("Error handling get user request", e);
    }
  }

  @KafkaListener(topics = KafkaConstants.TOPIC_USER_UPDATE_REQUEST, groupId = "user-watcher-group", containerFactory = "kafkaListenerContainerFactory")
  public void onUpdateUserRequest(UserUpdateRequest request) {
    log.info("Received update user request: id={}", request.getId());
    try {
      Optional<User> existing = userService.getById(request.getId());
      UserUpdateResponse response;
      if (existing.isEmpty()) {
        response = new UserUpdateResponse(null, false);
      } else {
        User domain = createUserFromUpdateRequest(request);
        User updated = userService.update(domain);
        UserResponse userResponse = createUserResponse(updated);
        response = new UserUpdateResponse(userResponse, true);
      }
      kafkaTemplate.send(KafkaConstants.TOPIC_USER_UPDATE_RESPONSE, response);
      log.debug("Sent update user response: updated={}", existing.isPresent());
    } catch (Exception e) {
      log.error("Error handling update user request", e);
    }
  }

  @KafkaListener(topics = KafkaConstants.TOPIC_USER_DELETE_REQUEST, groupId = "user-watcher-group", containerFactory = "kafkaListenerContainerFactory")
  public void onDeleteUserRequest(UserDeleteRequest request) {
    log.info("Received delete user request: id={}", request.getId());
    try {
      Optional<User> existing = userService.getById(request.getId());
      UserDeleteResponse response;
      if (existing.isEmpty()) {
        response = new UserDeleteResponse(false);
      } else {
        userService.delete(request.getId());
        response = new UserDeleteResponse(true);
      }
      kafkaTemplate.send(KafkaConstants.TOPIC_USER_DELETE_RESPONSE, response);
      log.debug("Sent delete user response: deleted={}", existing.isPresent());
    } catch (Exception e) {
      log.error("Error handling delete user request", e);
    }
  }

  @KafkaListener(topics = KafkaConstants.TOPIC_USER_LIST_REQUEST, groupId = "user-watcher-group", containerFactory = "kafkaListenerContainerFactory")
  public void onListUsersRequest(UserListRequest request) {
    log.info("Received list users request: page={}, size={}", request.getPage(), request.getSize());
    try {
      UserQueryCriteria criteria = createCriteriaFromRequest(request);
      List<User> users = userService.listByCriteria(criteria);
      long total = userService.countByCriteria(criteria);
      List<UserResponse> userResponses = users.stream()
          .map(this::createUserResponse)
          .toList();
      UserListResponse response = new UserListResponse(
          userResponses,
          criteria.getPage(),
          criteria.getSize(),
          total,
          (int) Math.ceil((double) total / criteria.getSize()));
      kafkaTemplate.send(KafkaConstants.TOPIC_USER_LIST_RESPONSE, response);
      log.debug("Sent list users response: {} users", users.size());
    } catch (Exception e) {
      log.error("Error handling list users request", e);
    }
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
}