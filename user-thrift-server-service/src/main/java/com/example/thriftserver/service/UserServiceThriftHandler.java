package com.example.thriftserver.service;

import com.example.kafka.constants.KafkaConstants;
import com.example.kafka.avro.*;
import com.example.user.thrift.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.thrift.TException;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceThriftHandler implements UserService.Iface {

    private final ReplyingKafkaTemplate<String, Object, Object> kafkaTemplate;

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
            ProducerRecord<String, Object> record = new ProducerRecord<>(KafkaConstants.TOPIC_PING_REQUEST,
                    correlationId, new PingRequest(System.currentTimeMillis()));
            record.headers()
                    .add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, KafkaConstants.TOPIC_PING_RESPONSE.getBytes()));
            RequestReplyFuture<String, Object, Object> replyFuture = kafkaTemplate.sendAndReceive(record);

            ConsumerRecord<String, Object> consumerRecord = replyFuture.get(10, TimeUnit.SECONDS);
            PingResponse response = (PingResponse) consumerRecord.value();
            return new TPingResponse(0, "Service is healthy", response.getMessage());
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
            RequestReplyFuture<String, Object, Object> replyFuture = kafkaTemplate.sendAndReceive(record);

            ConsumerRecord<String, Object> consumerRecord = replyFuture.get(10, TimeUnit.SECONDS);
            UserCreateResponse response = (UserCreateResponse) consumerRecord.value();
            TUser tUser = convertToTUser(response.getUser());
            return new TCreateUserResponse(0, "User created successfully", tUser);
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
            RequestReplyFuture<String, Object, Object> replyFuture = kafkaTemplate.sendAndReceive(record);

            ConsumerRecord<String, Object> consumerRecord = replyFuture.get(10, TimeUnit.SECONDS);
            UserGetResponse response = (UserGetResponse) consumerRecord.value();
            if (!response.getFound()) {
                return new TGetUserResponse(2, "User not found", null);
            }
            TUser tUser = convertToTUser(response.getUser());
            return new TGetUserResponse(0, "User retrieved successfully", tUser);
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
            RequestReplyFuture<String, Object, Object> replyFuture = kafkaTemplate.sendAndReceive(record);

            ConsumerRecord<String, Object> consumerRecord = replyFuture.get(10, TimeUnit.SECONDS);
            UserUpdateResponse response = (UserUpdateResponse) consumerRecord.value();
            if (!response.getUpdated()) {
                return new TUpdateUserResponse(1, "User not found", null);
            }
            TUser tUser = convertToTUser(response.getUser());
            return new TUpdateUserResponse(0, "User updated successfully", tUser);
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
            RequestReplyFuture<String, Object, Object> replyFuture = kafkaTemplate.sendAndReceive(record);

            ConsumerRecord<String, Object> consumerRecord = replyFuture.get(10, TimeUnit.SECONDS);
            UserDeleteResponse response = (UserDeleteResponse) consumerRecord.value();
            if (!response.getDeleted()) {
                return new TDeleteUserResponse(1, "User not found");
            }
            return new TDeleteUserResponse(0, "User deleted successfully");
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
            RequestReplyFuture<String, Object, Object> replyFuture = kafkaTemplate.sendAndReceive(record);

            ConsumerRecord<String, Object> consumerRecord = replyFuture.get(10, TimeUnit.SECONDS);
            UserListResponse response = (UserListResponse) consumerRecord.value();
            List<TUser> tUsers = response.getItems().stream()
                    .map(this::convertToTUser)
                    .toList();

            return new TListUsersResponse(
                    0,
                    "Users retrieved successfully",
                    tUsers,
                    response.getPage(),
                    response.getSize(),
                    response.getTotal(),
                    response.getTotalPages());
        } catch (Exception e) {
            log.error("Error listing users: {}", e.getMessage(), e);
            throw new TException("Error listing users: " + e.getMessage(), e);
        }
    }

}