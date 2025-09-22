package com.example.watcher.kafka;

import com.example.kafka.avro.*;
import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import com.example.user.service.port.UserServicePort;
import com.example.watcher.constants.WatcherConstants;
import com.example.watcher.service.ResponseService;
import com.example.watcher.service.UserMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceKafkaListener {

  private final UserServicePort userService;
  private final ResponseService responseService;
  private final UserMappingService userMappingService;

  @javax.annotation.PostConstruct
  public void init() {
    log.info("=== UserServiceKafkaListener initialized ===");
  }

  @KafkaListener(topics = "ping.request", groupId = WatcherConstants.CONSUMER_GROUP_ID, containerFactory = WatcherConstants.CONTAINER_FACTORY_AVRO)
  public void onPingRequest(ConsumerRecord<String, PingRequest> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());

    log.info("Received ping request with correlationId: {}", correlationId);
    try {
      String result = userService.ping();
      PingResponse response = new PingResponse(result);
      responseService.sendResponse(replyTopic, correlationId, response);
      log.info("Successfully sent ping response: {} with correlationId: {}", result, correlationId);
    } catch (Exception e) {
      log.error("Error handling ping request", e);
    }
  }

  @KafkaListener(topics = "user.create.request", groupId = WatcherConstants.CONSUMER_GROUP_ID, containerFactory = WatcherConstants.CONTAINER_FACTORY_AVRO)
  public void onCreateUserRequest(ConsumerRecord<String, UserCreateRequest> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    UserCreateRequest request = record.value();

    log.info("Received create user request: name={} with correlationId: {}", request.getName(), correlationId);
    try {
      User domain = userMappingService.createUserFromRequest(request);
      User created = userService.create(domain);
      UserResponse userResponse = userMappingService.createUserResponse(created);
      UserCreateResponse response = new UserCreateResponse(userResponse);
      responseService.sendResponse(replyTopic, correlationId, response);
      log.info("Successfully sent create user response for user: {} with correlationId: {}", created.getId(),
          correlationId);
    } catch (Exception e) {
      log.error("Error handling create user request", e);
    }
  }

  @KafkaListener(topics = "user.get.request", groupId = WatcherConstants.CONSUMER_GROUP_ID, containerFactory = WatcherConstants.CONTAINER_FACTORY_AVRO)
  public void onGetUserRequest(ConsumerRecord<String, UserGetRequest> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    UserGetRequest request = record.value();

    log.info("Received get user request: id={} with correlationId: {}", request.getId(), correlationId);
    try {
      Optional<User> user = userService.getById(request.getId());
      UserGetResponse response;
      if (user.isPresent()) {
        UserResponse userResponse = userMappingService.createUserResponse(user.get());
        response = new UserGetResponse(userResponse, true);
      } else {
        response = new UserGetResponse(null, false);
      }
      responseService.sendResponse(replyTopic, correlationId, response);
      log.info("Successfully sent get user response: found={} with correlationId: {}", user.isPresent(), correlationId);
    } catch (Exception e) {
      log.error("Error handling get user request", e);
    }
  }

  @KafkaListener(topics = "user.update.request", groupId = WatcherConstants.CONSUMER_GROUP_ID, containerFactory = WatcherConstants.CONTAINER_FACTORY_AVRO)
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
        User domain = userMappingService.createUserFromUpdateRequest(request);
        User updated = userService.update(domain);
        UserResponse userResponse = userMappingService.createUserResponse(updated);
        response = new UserUpdateResponse(userResponse, true);
      }
      responseService.sendResponse(replyTopic, correlationId, response);
      log.info("Successfully sent update user response: updated={} with correlationId: {}", existing.isPresent(),
          correlationId);
    } catch (Exception e) {
      log.error("Error handling update user request", e);
    }
  }

  @KafkaListener(topics = "user.delete.request", groupId = WatcherConstants.CONSUMER_GROUP_ID, containerFactory = WatcherConstants.CONTAINER_FACTORY_AVRO)
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
      responseService.sendResponse(replyTopic, correlationId, response);
      log.info("Successfully sent delete user response: deleted={} with correlationId: {}", existing.isPresent(),
          correlationId);
    } catch (Exception e) {
      log.error("Error handling delete user request", e);
    }
  }

  @KafkaListener(topics = "user.list.request", groupId = WatcherConstants.CONSUMER_GROUP_ID, containerFactory = WatcherConstants.CONTAINER_FACTORY_AVRO)
  public void onListUsersRequest(ConsumerRecord<String, UserListRequest> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    UserListRequest request = record.value();

    log.info("Received list users request: page={}, size={} with correlationId: {}", request.getPage(),
        request.getSize(), correlationId);
    try {
      UserQueryCriteria criteria = userMappingService.createCriteriaFromRequest(request);
      List<User> users = userService.listByCriteria(criteria);
      long total = userService.countByCriteria(criteria);
      List<UserResponse> userResponses = users.stream()
          .map(userMappingService::createUserResponse)
          .toList();
      UserListResponse response = new UserListResponse(
          userResponses,
          criteria.getPage(),
          criteria.getSize(),
          total,
          (int) Math.ceil((double) total / criteria.getSize()));
      responseService.sendResponse(replyTopic, correlationId, response);
      log.info("Successfully sent list users response: {} users with correlationId: {}", users.size(), correlationId);
    } catch (Exception e) {
      log.error("Error handling list users request", e);
    }
  }
}