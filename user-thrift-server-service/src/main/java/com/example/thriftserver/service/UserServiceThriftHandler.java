package com.example.thriftserver.service;

import com.example.kafka.constants.KafkaConstants;
import com.example.kafka.avro.*;
import com.example.user.thrift.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.thrift.TException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class UserServiceThriftHandler implements UserService.Iface {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ConcurrentHashMap<String, CompletableFuture<Object>> pendingReplies = new ConcurrentHashMap<>();

    public UserServiceThriftHandler(
            @Qualifier("avroKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private TUser convertToTUser(UserResponse userResponse) {
        TUser tUser = new TUser();
        tUser.setId(userResponse.getId());
        tUser.setName(userResponse.getName());
        tUser.setPhone(userResponse.getPhone());
        tUser.setAddress(userResponse.getAddress());
        tUser.setStatus(TUserStatus.valueOf(userResponse.getStatus().name()));
        tUser.setRole(TUserRole.valueOf(userResponse.getRole().name()));
        tUser.setCreatedAt(userResponse.getCreatedAt() != null ? userResponse.getCreatedAt() : 0L);
        tUser.setCreatedBy(userResponse.getCreatedBy());
        tUser.setUpdatedAt(userResponse.getUpdatedAt() != null ? userResponse.getUpdatedAt() : 0L);
        tUser.setUpdatedBy(userResponse.getUpdatedBy());
        tUser.setVersion(userResponse.getVersion() != null ? userResponse.getVersion() : 1);
        return tUser;
    }

    @Override
    public TPingResponse ping() throws TException {
        try {
            String correlationId = UUID.randomUUID().toString();
            log.info("Sending ping request with correlationId: {}", correlationId);

            ProducerRecord<String, Object> record = new ProducerRecord<>(KafkaConstants.TOPIC_PING_REQUEST,
                    correlationId, new PingRequest(System.currentTimeMillis()));
            record.headers()
                    .add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, KafkaConstants.TOPIC_PING_RESPONSE.getBytes()));
            record.headers()
                    .add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes()));

            // Create future for response
            CompletableFuture<Object> future = new CompletableFuture<>();
            pendingReplies.put(correlationId, future);

            // Send request
            kafkaTemplate.send(record);

            // Wait for response
            Object response = future.get(30, TimeUnit.SECONDS);
            pendingReplies.remove(correlationId);

            if (response instanceof PingResponse) {
                PingResponse pingResponse = (PingResponse) response;
                log.info("Received ping response: {}", pingResponse.getMessage());
                return new TPingResponse(0, "Service is healthy", pingResponse.getMessage());
            } else {
                throw new RuntimeException("Unexpected response type: " + response.getClass());
            }
        } catch (Exception e) {
            log.error("Error during ping: {}", e.getMessage(), e);
            throw new TException("Ping failed: " + e.getMessage(), e);
        }
    }

    @Override
    public TCreateUserResponse createUser(TCreateUserRequest request) throws TException {
        try {
            String correlationId = UUID.randomUUID().toString();
            UserCreateRequest avroRequest = new UserCreateRequest(
                    request.getName(),
                    request.getPhone(),
                    request.getAddress() != null ? request.getAddress() : "",
                    request.getStatus() != null ? UserStatus.valueOf(request.getStatus().name()) : UserStatus.ACTIVE,
                    request.getRole() != null ? UserRole.valueOf(request.getRole().name()) : UserRole.USER);

            ProducerRecord<String, Object> record = new ProducerRecord<>(KafkaConstants.TOPIC_USER_CREATE_REQUEST,
                    correlationId, avroRequest);
            record.headers().add(
                    new RecordHeader(KafkaHeaders.REPLY_TOPIC, KafkaConstants.TOPIC_USER_CREATE_RESPONSE.getBytes()));
            record.headers()
                    .add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes()));

            // Create future for response
            CompletableFuture<Object> future = new CompletableFuture<>();
            pendingReplies.put(correlationId, future);

            // Send request
            kafkaTemplate.send(record);

            // Wait for response
            Object response = future.get(30, TimeUnit.SECONDS);
            pendingReplies.remove(correlationId);

            if (response instanceof UserCreateResponse) {
                UserCreateResponse createResponse = (UserCreateResponse) response;
                TUser tUser = convertToTUser(createResponse.getUser());
                return new TCreateUserResponse(0, "User created successfully", tUser);
            } else {
                throw new RuntimeException("Unexpected response type: " + response.getClass());
            }
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            throw new TException("User creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public TGetUserResponse getUser(TGetUserRequest request) throws TException {
        try {
            String correlationId = UUID.randomUUID().toString();
            UserGetRequest avroRequest = new UserGetRequest(request.getId());

            ProducerRecord<String, Object> record = new ProducerRecord<>(KafkaConstants.TOPIC_USER_GET_REQUEST,
                    correlationId, avroRequest);
            record.headers()
                    .add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, KafkaConstants.TOPIC_USER_GET_RESPONSE.getBytes()));
            record.headers()
                    .add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes()));

            // Create future for response
            CompletableFuture<Object> future = new CompletableFuture<>();
            pendingReplies.put(correlationId, future);

            // Send request
            kafkaTemplate.send(record);

            // Wait for response
            Object response = future.get(30, TimeUnit.SECONDS);
            pendingReplies.remove(correlationId);

            if (response instanceof UserGetResponse) {
                UserGetResponse getResponse = (UserGetResponse) response;
                if (!getResponse.getFound()) {
                    return new TGetUserResponse(2, "User not found", null);
                }
                TUser tUser = convertToTUser(getResponse.getUser());
                return new TGetUserResponse(0, "User retrieved successfully", tUser);
            } else {
                throw new RuntimeException("Unexpected response type: " + response.getClass());
            }
        } catch (Exception e) {
            log.error("Error getting user: {}", e.getMessage(), e);
            return new TGetUserResponse(2, "Error retrieving user: " + e.getMessage(), null);
        }
    }

    @Override
    public TUpdateUserResponse updateUser(TUpdateUserRequest request) throws TException {
        try {
            String correlationId = UUID.randomUUID().toString();
            UserUpdateRequest avroRequest = new UserUpdateRequest(
                    request.getId(),
                    request.getName(),
                    request.getPhone(),
                    request.getAddress() != null ? request.getAddress() : "",
                    request.getStatus() != null ? UserStatus.valueOf(request.getStatus().name()) : UserStatus.ACTIVE,
                    request.getRole() != null ? UserRole.valueOf(request.getRole().name()) : UserRole.USER,
                    request.getVersion());

            ProducerRecord<String, Object> record = new ProducerRecord<>(KafkaConstants.TOPIC_USER_UPDATE_REQUEST,
                    correlationId, avroRequest);
            record.headers().add(
                    new RecordHeader(KafkaHeaders.REPLY_TOPIC, KafkaConstants.TOPIC_USER_UPDATE_RESPONSE.getBytes()));
            record.headers()
                    .add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes()));

            // Create future for response
            CompletableFuture<Object> future = new CompletableFuture<>();
            pendingReplies.put(correlationId, future);

            // Send request
            kafkaTemplate.send(record);

            // Wait for response
            Object response = future.get(30, TimeUnit.SECONDS);
            pendingReplies.remove(correlationId);

            if (response instanceof UserUpdateResponse) {
                UserUpdateResponse updateResponse = (UserUpdateResponse) response;
                if (!updateResponse.getUpdated()) {
                    return new TUpdateUserResponse(1, "User not found", null);
                }
                TUser tUser = convertToTUser(updateResponse.getUser());
                return new TUpdateUserResponse(0, "User updated successfully", tUser);
            } else {
                throw new RuntimeException("Unexpected response type: " + response.getClass());
            }
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage(), e);
            return new TUpdateUserResponse(1, "Error updating user: " + e.getMessage(), null);
        }
    }

    @Override
    public TDeleteUserResponse deleteUser(TDeleteUserRequest request) throws TException {
        try {
            String correlationId = UUID.randomUUID().toString();
            UserDeleteRequest avroRequest = new UserDeleteRequest(request.getId());

            ProducerRecord<String, Object> record = new ProducerRecord<>(KafkaConstants.TOPIC_USER_DELETE_REQUEST,
                    correlationId, avroRequest);
            record.headers().add(
                    new RecordHeader(KafkaHeaders.REPLY_TOPIC, KafkaConstants.TOPIC_USER_DELETE_RESPONSE.getBytes()));
            record.headers()
                    .add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes()));

            // Create future for response
            CompletableFuture<Object> future = new CompletableFuture<>();
            pendingReplies.put(correlationId, future);

            // Send request
            kafkaTemplate.send(record);

            // Wait for response
            Object response = future.get(30, TimeUnit.SECONDS);
            pendingReplies.remove(correlationId);

            if (response instanceof UserDeleteResponse) {
                UserDeleteResponse deleteResponse = (UserDeleteResponse) response;
                if (!deleteResponse.getDeleted()) {
                    return new TDeleteUserResponse(1, "User not found");
                }
                return new TDeleteUserResponse(0, "User deleted successfully");
            } else {
                throw new RuntimeException("Unexpected response type: " + response.getClass());
            }
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage(), e);
            return new TDeleteUserResponse(1, "Error deleting user: " + e.getMessage());
        }
    }

    @Override
    public TListUsersResponse listUsers(TListUsersRequest request) throws TException {
        try {
            String correlationId = UUID.randomUUID().toString();
            UserListRequest avroRequest = new UserListRequest(
                    request.getPage(),
                    request.getSize(),
                    request.getSearch(),
                    request.getStatus() != null ? UserStatus.valueOf(request.getStatus().name()) : null,
                    request.getRole() != null ? UserRole.valueOf(request.getRole().name()) : null,
                    request.isIncludeDeleted());

            ProducerRecord<String, Object> record = new ProducerRecord<>(KafkaConstants.TOPIC_USER_LIST_REQUEST,
                    correlationId, avroRequest);
            record.headers().add(
                    new RecordHeader(KafkaHeaders.REPLY_TOPIC, KafkaConstants.TOPIC_USER_LIST_RESPONSE.getBytes()));
            record.headers()
                    .add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes()));

            // Create future for response
            CompletableFuture<Object> future = new CompletableFuture<>();
            pendingReplies.put(correlationId, future);

            // Send request
            kafkaTemplate.send(record);

            // Wait for response
            Object response = future.get(30, TimeUnit.SECONDS);
            pendingReplies.remove(correlationId);

            if (response instanceof UserListResponse) {
                UserListResponse listResponse = (UserListResponse) response;
                List<TUser> tUsers = listResponse.getItems().stream()
                        .map(this::convertToTUser)
                        .toList();

                return new TListUsersResponse(
                        0,
                        "Users retrieved successfully",
                        tUsers,
                        listResponse.getPage(),
                        listResponse.getSize(),
                        listResponse.getTotal(),
                        listResponse.getTotalPages());
            } else {
                throw new RuntimeException("Unexpected response type: " + response.getClass());
            }
        } catch (Exception e) {
            log.error("Error listing users: {}", e.getMessage(), e);
            throw new TException("Error listing users: " + e.getMessage(), e);
        }
    }

    // Listen for responses
    @KafkaListener(topics = "${kafka.topics.ping.response}")
    public void onPingResponse(ConsumerRecord<String, Object> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        log.info("Received ping response with correlationId: {}", correlationId);

        CompletableFuture<Object> future = pendingReplies.remove(correlationId);
        if (future != null) {
            future.complete(record.value());
        } else {
            log.warn("No pending reply found for correlationId: {}", correlationId);
        }
    }

    @KafkaListener(topics = "${kafka.topics.user.create.response}")
    public void onCreateUserResponse(ConsumerRecord<String, Object> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        log.info("Received createUser response with correlationId: {}", correlationId);

        CompletableFuture<Object> future = pendingReplies.remove(correlationId);
        if (future != null) {
            future.complete(record.value());
        } else {
            log.warn("No pending reply found for correlationId: {}", correlationId);
        }
    }

    @KafkaListener(topics = "${kafka.topics.user.get.response}")
    public void onGetUserResponse(ConsumerRecord<String, Object> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        log.info("Received getUser response with correlationId: {}", correlationId);

        CompletableFuture<Object> future = pendingReplies.remove(correlationId);
        if (future != null) {
            future.complete(record.value());
        } else {
            log.warn("No pending reply found for correlationId: {}", correlationId);
        }
    }

    @KafkaListener(topics = "${kafka.topics.user.update.response}")
    public void onUpdateUserResponse(ConsumerRecord<String, Object> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        log.info("Received updateUser response with correlationId: {}", correlationId);

        CompletableFuture<Object> future = pendingReplies.remove(correlationId);
        if (future != null) {
            future.complete(record.value());
        } else {
            log.warn("No pending reply found for correlationId: {}", correlationId);
        }
    }

    @KafkaListener(topics = "${kafka.topics.user.delete.response}")
    public void onDeleteUserResponse(ConsumerRecord<String, Object> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        log.info("Received deleteUser response with correlationId: {}", correlationId);

        CompletableFuture<Object> future = pendingReplies.remove(correlationId);
        if (future != null) {
            future.complete(record.value());
        } else {
            log.warn("No pending reply found for correlationId: {}", correlationId);
        }
    }

    @KafkaListener(topics = "${kafka.topics.user.list.response}")
    public void onListUsersResponse(ConsumerRecord<String, Object> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        log.info("Received listUsers response with correlationId: {}", correlationId);

        CompletableFuture<Object> future = pendingReplies.remove(correlationId);
        if (future != null) {
            future.complete(record.value());
        } else {
            log.warn("No pending reply found for correlationId: {}", correlationId);
        }
    }
}