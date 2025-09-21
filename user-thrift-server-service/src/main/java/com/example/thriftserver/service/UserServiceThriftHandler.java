package com.example.thriftserver.service;

import com.example.kafka.avro.*;
import com.example.user.thrift.*;
import com.example.thriftserver.config.KafkaTopicsProperties;
import com.example.thriftserver.constants.ThriftConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.thrift.TException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceThriftHandler implements UserService.Iface {

    private final RpcService rpcService;
    private final KafkaTopicsProperties topicsProperties;

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
            log.info("Processing ping request");
            PingRequest avroRequest = new PingRequest(System.currentTimeMillis());

            PingResponse response = rpcService.sendRpcRequest(
                    topicsProperties.getPingRequest(),
                    topicsProperties.getPingResponse(),
                    avroRequest,
                    PingResponse.class);

            log.info("Received ping response: {}", response.getMessage());
            return new TPingResponse(ThriftConstants.STATUS_SUCCESS, ThriftConstants.SUCCESS_PING,
                    response.getMessage());
        } catch (Exception e) {
            log.error("Error during ping: {}", e.getMessage(), e);
            throw new TException("Ping failed: " + e.getMessage(), e);
        }
    }

    @Override
    public TCreateUserResponse createUser(TCreateUserRequest request) throws TException {
        try {
            log.info("Processing createUser request for user: {}", request.getName());
            UserCreateRequest avroRequest = new UserCreateRequest(
                    request.getName(),
                    request.getPhone(),
                    request.getAddress() != null ? request.getAddress() : "",
                    request.getStatus() != null ? UserStatus.valueOf(request.getStatus().name()) : UserStatus.ACTIVE,
                    request.getRole() != null ? UserRole.valueOf(request.getRole().name()) : UserRole.USER);

            UserCreateResponse response = rpcService.sendRpcRequest(
                    topicsProperties.getUserCreateRequest(),
                    topicsProperties.getUserCreateResponse(),
                    avroRequest,
                    UserCreateResponse.class);

            TUser tUser = convertToTUser(response.getUser());
            return new TCreateUserResponse(ThriftConstants.STATUS_SUCCESS, ThriftConstants.SUCCESS_USER_CREATED, tUser);
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            throw new TException(ThriftConstants.ERROR_USER_CREATION_FAILED + ": " + e.getMessage(), e);
        }
    }

    @Override
    public TGetUserResponse getUser(TGetUserRequest request) throws TException {
        try {
            log.info("Processing getUser request for id: {}", request.getId());
            UserGetRequest avroRequest = new UserGetRequest(request.getId());

            UserGetResponse response = rpcService.sendRpcRequest(
                    topicsProperties.getUserGetRequest(),
                    topicsProperties.getUserGetResponse(),
                    avroRequest,
                    UserGetResponse.class);

            if (!response.getFound()) {
                return new TGetUserResponse(ThriftConstants.STATUS_USER_NOT_FOUND, ThriftConstants.ERROR_USER_NOT_FOUND,
                        null);
            }

            TUser tUser = convertToTUser(response.getUser());
            return new TGetUserResponse(ThriftConstants.STATUS_SUCCESS, ThriftConstants.SUCCESS_USER_RETRIEVED, tUser);
        } catch (Exception e) {
            log.error("Error getting user: {}", e.getMessage(), e);
            return new TGetUserResponse(ThriftConstants.STATUS_ERROR,
                    ThriftConstants.ERROR_USER_RETRIEVAL_FAILED + ": " + e.getMessage(), null);
        }
    }

    @Override
    public TUpdateUserResponse updateUser(TUpdateUserRequest request) throws TException {
        try {
            log.info("Processing updateUser request for id: {}", request.getId());
            UserUpdateRequest avroRequest = new UserUpdateRequest(
                    request.getId(),
                    request.getName(),
                    request.getPhone(),
                    request.getAddress() != null ? request.getAddress() : "",
                    request.getStatus() != null ? UserStatus.valueOf(request.getStatus().name()) : UserStatus.ACTIVE,
                    request.getRole() != null ? UserRole.valueOf(request.getRole().name()) : UserRole.USER,
                    request.getVersion());

            UserUpdateResponse response = rpcService.sendRpcRequest(
                    topicsProperties.getUserUpdateRequest(),
                    topicsProperties.getUserUpdateResponse(),
                    avroRequest,
                    UserUpdateResponse.class);

            if (!response.getUpdated()) {
                return new TUpdateUserResponse(ThriftConstants.STATUS_USER_NOT_FOUND,
                        ThriftConstants.ERROR_USER_NOT_FOUND, null);
            }

            TUser tUser = convertToTUser(response.getUser());
            return new TUpdateUserResponse(ThriftConstants.STATUS_SUCCESS, ThriftConstants.SUCCESS_USER_UPDATED, tUser);
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage(), e);
            return new TUpdateUserResponse(ThriftConstants.STATUS_ERROR,
                    ThriftConstants.ERROR_USER_UPDATE_FAILED + ": " + e.getMessage(), null);
        }
    }

    @Override
    public TDeleteUserResponse deleteUser(TDeleteUserRequest request) throws TException {
        try {
            log.info("Processing deleteUser request for id: {}", request.getId());
            UserDeleteRequest avroRequest = new UserDeleteRequest(request.getId());

            UserDeleteResponse response = rpcService.sendRpcRequest(
                    topicsProperties.getUserDeleteRequest(),
                    topicsProperties.getUserDeleteResponse(),
                    avroRequest,
                    UserDeleteResponse.class);

            if (!response.getDeleted()) {
                return new TDeleteUserResponse(ThriftConstants.STATUS_USER_NOT_FOUND,
                        ThriftConstants.ERROR_USER_NOT_FOUND);
            }

            return new TDeleteUserResponse(ThriftConstants.STATUS_SUCCESS, ThriftConstants.SUCCESS_USER_DELETED);
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage(), e);
            return new TDeleteUserResponse(ThriftConstants.STATUS_ERROR,
                    ThriftConstants.ERROR_USER_DELETION_FAILED + ": " + e.getMessage());
        }
    }

    @Override
    public TListUsersResponse listUsers(TListUsersRequest request) throws TException {
        try {
            log.info("Processing listUsers request with page: {}, size: {}", request.getPage(), request.getSize());
            UserListRequest avroRequest = new UserListRequest(
                    request.getPage(),
                    request.getSize(),
                    request.getSearch(),
                    request.getStatus() != null ? UserStatus.valueOf(request.getStatus().name()) : null,
                    request.getRole() != null ? UserRole.valueOf(request.getRole().name()) : null,
                    request.isIncludeDeleted());

            UserListResponse response = rpcService.sendRpcRequest(
                    topicsProperties.getUserListRequest(),
                    topicsProperties.getUserListResponse(),
                    avroRequest,
                    UserListResponse.class);

            List<TUser> tUsers = response.getItems().stream()
                    .map(this::convertToTUser)
                    .toList();

            return new TListUsersResponse(
                    ThriftConstants.STATUS_SUCCESS,
                    ThriftConstants.SUCCESS_USERS_RETRIEVED,
                    tUsers,
                    response.getPage(),
                    response.getSize(),
                    response.getTotal(),
                    response.getTotalPages());
        } catch (Exception e) {
            log.error("Error listing users: {}", e.getMessage(), e);
            throw new TException(ThriftConstants.ERROR_USER_LISTING_FAILED + ": " + e.getMessage(), e);
        }
    }

    // Generic response handlers
    @KafkaListener(topics = "${kafka.topics.ping-response}")
    public void onPingResponse(ConsumerRecord<String, Object> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        log.debug("Received ping response with correlationId: {}", correlationId);
        rpcService.handleResponse(correlationId, record.value());
    }

    @KafkaListener(topics = "${kafka.topics.user-create-response}")
    public void onCreateUserResponse(ConsumerRecord<String, Object> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        log.debug("Received createUser response with correlationId: {}", correlationId);
        rpcService.handleResponse(correlationId, record.value());
    }

    @KafkaListener(topics = "${kafka.topics.user-get-response}")
    public void onGetUserResponse(ConsumerRecord<String, Object> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        log.debug("Received getUser response with correlationId: {}", correlationId);
        rpcService.handleResponse(correlationId, record.value());
    }

    @KafkaListener(topics = "${kafka.topics.user-update-response}")
    public void onUpdateUserResponse(ConsumerRecord<String, Object> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        log.debug("Received updateUser response with correlationId: {}", correlationId);
        rpcService.handleResponse(correlationId, record.value());
    }

    @KafkaListener(topics = "${kafka.topics.user-delete-response}")
    public void onDeleteUserResponse(ConsumerRecord<String, Object> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        log.debug("Received deleteUser response with correlationId: {}", correlationId);
        rpcService.handleResponse(correlationId, record.value());
    }

    @KafkaListener(topics = "${kafka.topics.user-list-response}")
    public void onListUsersResponse(ConsumerRecord<String, Object> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        log.debug("Received listUsers response with correlationId: {}", correlationId);
        rpcService.handleResponse(correlationId, record.value());
    }
}