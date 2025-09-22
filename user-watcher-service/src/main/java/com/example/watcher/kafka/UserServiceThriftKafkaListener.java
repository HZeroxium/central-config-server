package com.example.watcher.kafka;

import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import com.example.kafka.thrift.*;
import com.example.kafka.util.ThriftKafkaMessageHandler;
import com.example.user.service.port.UserServicePort;
import com.example.watcher.constants.WatcherConstants;
import com.example.watcher.service.ResponseService;
import com.example.watcher.service.UserMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceThriftKafkaListener {

  private final UserServicePort userService;
  private final ResponseService responseService;
  private final UserMappingService userMappingService;

  @javax.annotation.PostConstruct
  public void init() {
    log.info("=== UserServiceThriftKafkaListener initialized ===");
  }

  @KafkaListener(topics = "ping.request", groupId = WatcherConstants.CONSUMER_GROUP_ID, containerFactory = "kafkaListenerContainerFactory")
  public void onPingRequest(ConsumerRecord<String, byte[]> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    TPingRequest request = ThriftKafkaMessageHandler.deserializeMessage(record, TPingRequest.class);

    log.info("Received ping request with correlationId: {}", correlationId);
    try {
      String result = userService.ping();
      TPingResponse response = new TPingResponse(result);
      responseService.sendResponse(replyTopic, correlationId, response);
      log.info("Successfully sent ping response: {} with correlationId: {}", result, correlationId);
    } catch (Exception e) {
      log.error("Error handling ping request", e);
    }
  }

  @KafkaListener(topics = "user.create.request", groupId = WatcherConstants.CONSUMER_GROUP_ID, containerFactory = "kafkaListenerContainerFactory")
  public void onCreateUserRequest(ConsumerRecord<String, byte[]> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    TUserCreateRequest request = ThriftKafkaMessageHandler.deserializeMessage(record, TUserCreateRequest.class);

    log.info("Received create user request: name={} with correlationId: {}", request.getName(), correlationId);
    try {
      User domain = userMappingService.createUserFromThriftRequest(request);
      User created = userService.create(domain);
      TUserResponse userResponse = userMappingService.createThriftUserResponse(created);
      TUserCreateResponse response = new TUserCreateResponse(userResponse);
      responseService.sendResponse(replyTopic, correlationId, response);
      log.info("Successfully sent create user response for user: {} with correlationId: {}", created.getId(),
          correlationId);
    } catch (Exception e) {
      log.error("Error handling create user request", e);
    }
  }

  @KafkaListener(topics = "user.get.request", groupId = WatcherConstants.CONSUMER_GROUP_ID, containerFactory = "kafkaListenerContainerFactory")
  public void onGetUserRequest(ConsumerRecord<String, byte[]> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    TUserGetRequest request = ThriftKafkaMessageHandler.deserializeMessage(record, TUserGetRequest.class);

    log.info("Received get user request: id={} with correlationId: {}", request.getId(), correlationId);
    try {
      Optional<User> user = userService.getById(request.getId());
      TUserGetResponse response = new TUserGetResponse();
      if (user.isPresent()) {
        TUserResponse userResponse = userMappingService.createThriftUserResponse(user.get());
        response.setUser(userResponse);
        response.setFound(true);
      } else {
        response.setFound(false);
      }
      responseService.sendResponse(replyTopic, correlationId, response);
      log.info("Successfully sent get user response: found={} with correlationId: {}", user.isPresent(), correlationId);
    } catch (Exception e) {
      log.error("Error handling get user request", e);
    }
  }

  @KafkaListener(topics = "user.update.request", groupId = WatcherConstants.CONSUMER_GROUP_ID, containerFactory = "kafkaListenerContainerFactory")
  public void onUpdateUserRequest(ConsumerRecord<String, byte[]> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    TUserUpdateRequest request = ThriftKafkaMessageHandler.deserializeMessage(record, TUserUpdateRequest.class);

    log.info("Received update user request: id={} with correlationId: {}", request.getId(), correlationId);
    try {
      Optional<User> existing = userService.getById(request.getId());
      TUserUpdateResponse response = new TUserUpdateResponse();
      if (existing.isEmpty()) {
        response.setSuccess(false); // User not found
      } else {
        User domain = userMappingService.createUserFromThriftUpdateRequest(request);
        User updated = userService.update(domain);
        TUserResponse userResponse = userMappingService.createThriftUserResponse(updated);
        response.setUser(userResponse);
        response.setSuccess(true);
      }
      responseService.sendResponse(replyTopic, correlationId, response);
      log.info("Successfully sent update user response: updated={} with correlationId: {}", existing.isPresent(),
          correlationId);
    } catch (Exception e) {
      log.error("Error handling update user request", e);
    }
  }

  @KafkaListener(topics = "user.delete.request", groupId = WatcherConstants.CONSUMER_GROUP_ID, containerFactory = "kafkaListenerContainerFactory")
  public void onDeleteUserRequest(ConsumerRecord<String, byte[]> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    TUserDeleteRequest request = ThriftKafkaMessageHandler.deserializeMessage(record, TUserDeleteRequest.class);

    log.info("Received delete user request: id={} with correlationId: {}", request.getId(), correlationId);
    try {
      Optional<User> existing = userService.getById(request.getId());
      TUserDeleteResponse response = new TUserDeleteResponse();
      if (existing.isEmpty()) {
        response.setDeleted(false);
      } else {
        userService.delete(request.getId());
        response.setDeleted(true);
      }
      responseService.sendResponse(replyTopic, correlationId, response);
      log.info("Successfully sent delete user response: deleted={} with correlationId: {}", existing.isPresent(),
          correlationId);
    } catch (Exception e) {
      log.error("Error handling delete user request", e);
    }
  }

  @KafkaListener(topics = "user.list.request", groupId = WatcherConstants.CONSUMER_GROUP_ID, containerFactory = "kafkaListenerContainerFactory")
  public void onListUsersRequest(ConsumerRecord<String, byte[]> record) {
    String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
    String replyTopic = new String(record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value());
    TUserListRequest request = ThriftKafkaMessageHandler.deserializeMessage(record, TUserListRequest.class);

    log.info("Received list users request: page={}, size={} with correlationId: {}",
        request.isSetPage() ? request.getPage() : "default",
        request.isSetSize() ? request.getSize() : "default", correlationId);
    try {
      UserQueryCriteria criteria = userMappingService.createCriteriaFromThriftRequest(request);
      List<User> users = userService.listByCriteria(criteria);
      long total = userService.countByCriteria(criteria);
      List<TUserResponse> userResponses = users.stream()
          .map(userMappingService::createThriftUserResponse)
          .collect(Collectors.toList());
      int totalPages = (int) Math.ceil((double) total / criteria.getSize());
      TUserListResponse response = new TUserListResponse(userResponses, criteria.getPage(), criteria.getSize(), total,
          totalPages);
      responseService.sendResponse(replyTopic, correlationId, response);
      log.info("Successfully sent list users response: {} users with correlationId: {}", users.size(), correlationId);
    } catch (Exception e) {
      log.error("Error handling list users request", e);
    }
  }
}