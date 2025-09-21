package com.example.watcher.kafka;

import com.example.kafka.constants.KafkaConstants;
import com.example.kafka.avro.*;
import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import com.example.user.service.port.UserServicePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class UserServiceKafkaListener {

  private final UserServicePort userService;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  public UserServiceKafkaListener(
      UserServicePort userService,
      @Qualifier("avroKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
    this.userService = userService;
    this.kafkaTemplate = kafkaTemplate;
  }

  @javax.annotation.PostConstruct
  public void init() {
    log.info("=== UserServiceKafkaListener initialized ===");
  }

  @KafkaListener(topics = KafkaConstants.TOPIC_PING_REQUEST, groupId = "user-watcher-group", containerFactory = "avroKafkaListenerContainerFactory")
  public void onPingRequest(ConsumerRecord<String, PingRequest> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    PingRequest request = record.value();

    log.info("Received ping request with correlationId: {}", correlationId);
    try {
      String result = userService.ping();
      PingResponse response = new PingResponse(result);

      // Create ProducerRecord with correlationId header for ReplyingKafkaTemplate
      ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(replyTopic, response);
      producerRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId.getBytes());

      kafkaTemplate.send(producerRecord);
      log.info("Successfully sent ping response: {} with correlationId: {}", result, correlationId);
    } catch (Exception e) {
      log.error("Error handling ping request", e);
    }
  }

  @KafkaListener(topics = KafkaConstants.TOPIC_USER_CREATE_REQUEST, groupId = "user-watcher-group", containerFactory = "avroKafkaListenerContainerFactory")
  public void onCreateUserRequest(ConsumerRecord<String, UserCreateRequest> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    UserCreateRequest request = record.value();

    log.info("Received create user request: name={} with correlationId: {}", request.getName(), correlationId);
    try {
      User domain = createUserFromRequest(request);
      User created = userService.create(domain);
      UserResponse userResponse = createUserResponse(created);
      UserCreateResponse response = new UserCreateResponse(userResponse);

      // Create ProducerRecord with correlationId header
      ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(replyTopic, response);
      producerRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId.getBytes());

      kafkaTemplate.send(producerRecord);
      log.info("Successfully sent create user response for user: {} with correlationId: {}", created.getId(),
          correlationId);
    } catch (Exception e) {
      log.error("Error handling create user request", e);
    }
  }

  @KafkaListener(topics = KafkaConstants.TOPIC_USER_GET_REQUEST, groupId = "user-watcher-group", containerFactory = "avroKafkaListenerContainerFactory")
  public void onGetUserRequest(ConsumerRecord<String, UserGetRequest> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    UserGetRequest request = record.value();

    log.info("Received get user request: id={} with correlationId: {}", request.getId(), correlationId);
    try {
      Optional<User> user = userService.getById(request.getId());
      UserGetResponse response;
      if (user.isPresent()) {
        UserResponse userResponse = createUserResponse(user.get());
        response = new UserGetResponse(userResponse, true);
      } else {
        response = new UserGetResponse(null, false);
      }

      // Create ProducerRecord with correlationId header
      ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(replyTopic, response);
      producerRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId.getBytes());

      kafkaTemplate.send(producerRecord);
      log.info("Successfully sent get user response: found={} with correlationId: {}", user.isPresent(), correlationId);
    } catch (Exception e) {
      log.error("Error handling get user request", e);
    }
  }

  @KafkaListener(topics = KafkaConstants.TOPIC_USER_UPDATE_REQUEST, groupId = "user-watcher-group", containerFactory = "avroKafkaListenerContainerFactory")
  public void onUpdateUserRequest(ConsumerRecord<String, UserUpdateRequest> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    UserUpdateRequest request = record.value();

    log.info("Received update user request: id={} with correlationId: {}", request.getId(), correlationId);
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

      // Create ProducerRecord with correlationId header
      ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(replyTopic, response);
      producerRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId.getBytes());

      kafkaTemplate.send(producerRecord);
      log.info("Successfully sent update user response: updated={} with correlationId: {}", existing.isPresent(),
          correlationId);
    } catch (Exception e) {
      log.error("Error handling update user request", e);
    }
  }

  @KafkaListener(topics = KafkaConstants.TOPIC_USER_DELETE_REQUEST, groupId = "user-watcher-group", containerFactory = "avroKafkaListenerContainerFactory")
  public void onDeleteUserRequest(ConsumerRecord<String, UserDeleteRequest> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    UserDeleteRequest request = record.value();

    log.info("Received delete user request: id={} with correlationId: {}", request.getId(), correlationId);
    try {
      Optional<User> existing = userService.getById(request.getId());
      UserDeleteResponse response;
      if (existing.isEmpty()) {
        response = new UserDeleteResponse(false);
      } else {
        userService.delete(request.getId());
        response = new UserDeleteResponse(true);
      }

      // Create ProducerRecord with correlationId header
      ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(replyTopic, response);
      producerRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId.getBytes());

      kafkaTemplate.send(producerRecord);
      log.info("Successfully sent delete user response: deleted={} with correlationId: {}", existing.isPresent(),
          correlationId);
    } catch (Exception e) {
      log.error("Error handling delete user request", e);
    }
  }

  @KafkaListener(topics = KafkaConstants.TOPIC_USER_LIST_REQUEST, groupId = "user-watcher-group", containerFactory = "avroKafkaListenerContainerFactory")
  public void onListUsersRequest(ConsumerRecord<String, UserListRequest> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    UserListRequest request = record.value();

    log.info("Received list users request: page={}, size={} with correlationId: {}", request.getPage(),
        request.getSize(), correlationId);
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

      // Create ProducerRecord with correlationId header
      ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(replyTopic, response);
      producerRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId.getBytes());

      kafkaTemplate.send(producerRecord);
      log.info("Successfully sent list users response: {} users with correlationId: {}", users.size(), correlationId);
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