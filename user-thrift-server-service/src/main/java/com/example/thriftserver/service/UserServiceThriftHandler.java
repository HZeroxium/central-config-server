package com.example.thriftserver.service;

import com.example.kafka.thrift.TPingRequest;
import com.example.kafka.thrift.TPingResponse;
import com.example.kafka.thrift.TUserCreateRequest;
import com.example.kafka.thrift.TUserCreateResponse;
import com.example.kafka.thrift.TUserGetRequest;
import com.example.kafka.thrift.TUserGetResponse;
import com.example.kafka.thrift.TUserUpdateRequest;
import com.example.kafka.thrift.TUserUpdateResponse;
import com.example.kafka.thrift.TUserDeleteRequest;
import com.example.kafka.thrift.TUserDeleteResponse;
import com.example.kafka.thrift.TUserListRequest;
import com.example.kafka.thrift.TUserListResponse;
import com.example.kafka.thrift.TUserResponse;
import com.example.kafka.util.ThriftKafkaMessageHandler;
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

    private TUser convertToTUser(TUserResponse userResponse) {
        TUser tUser = new TUser();
        tUser.setId(userResponse.getId());
        tUser.setName(userResponse.getName());
        tUser.setPhone(userResponse.getPhone());
        tUser.setAddress(userResponse.getAddress());

        // Convert enums
        tUser.setStatus(convertToThriftUserStatus(userResponse.getStatus()));
        tUser.setRole(convertToThriftUserRole(userResponse.getRole()));

        if (userResponse.isSetCreatedAt()) {
            tUser.setCreatedAt(userResponse.getCreatedAt());
        }
        if (userResponse.isSetCreatedBy()) {
            tUser.setCreatedBy(userResponse.getCreatedBy());
        }
        if (userResponse.isSetUpdatedAt()) {
            tUser.setUpdatedAt(userResponse.getUpdatedAt());
        }
        if (userResponse.isSetUpdatedBy()) {
            tUser.setUpdatedBy(userResponse.getUpdatedBy());
        }
        if (userResponse.isSetVersion()) {
            tUser.setVersion(userResponse.getVersion());
        }
        if (userResponse.isSetDeleted()) {
            tUser.setDeleted(userResponse.isDeleted());
        }
        if (userResponse.isSetDeletedAt()) {
            tUser.setDeletedAt(userResponse.getDeletedAt());
        }
        if (userResponse.isSetDeletedBy()) {
            tUser.setDeletedBy(userResponse.getDeletedBy());
        }

        return tUser;
    }

    private com.example.user.thrift.TUserStatus convertToThriftUserStatus(
            com.example.kafka.thrift.TUserStatus kafkaStatus) {
        if (kafkaStatus == null)
            return null;
        return switch (kafkaStatus) {
            case ACTIVE -> com.example.user.thrift.TUserStatus.ACTIVE;
            case INACTIVE -> com.example.user.thrift.TUserStatus.INACTIVE;
            case SUSPENDED -> com.example.user.thrift.TUserStatus.SUSPENDED;
        };
    }

    private com.example.user.thrift.TUserRole convertToThriftUserRole(com.example.kafka.thrift.TUserRole kafkaRole) {
        if (kafkaRole == null)
            return null;
        return switch (kafkaRole) {
            case ADMIN -> com.example.user.thrift.TUserRole.ADMIN;
            case USER -> com.example.user.thrift.TUserRole.USER;
            case MODERATOR -> com.example.user.thrift.TUserRole.MODERATOR;
            case GUEST -> com.example.user.thrift.TUserRole.GUEST;
        };
    }

    private com.example.kafka.thrift.TUserStatus convertToKafkaUserStatus(
            com.example.user.thrift.TUserStatus thriftStatus) {
        if (thriftStatus == null)
            return null;
        return switch (thriftStatus) {
            case ACTIVE -> com.example.kafka.thrift.TUserStatus.ACTIVE;
            case INACTIVE -> com.example.kafka.thrift.TUserStatus.INACTIVE;
            case SUSPENDED -> com.example.kafka.thrift.TUserStatus.SUSPENDED;
        };
    }

    private com.example.kafka.thrift.TUserRole convertToKafkaUserRole(com.example.user.thrift.TUserRole thriftRole) {
        if (thriftRole == null)
            return null;
        return switch (thriftRole) {
            case ADMIN -> com.example.kafka.thrift.TUserRole.ADMIN;
            case USER -> com.example.kafka.thrift.TUserRole.USER;
            case MODERATOR -> com.example.kafka.thrift.TUserRole.MODERATOR;
            case GUEST -> com.example.kafka.thrift.TUserRole.GUEST;
        };
    }

    @Override
    public com.example.user.thrift.TPingResponse ping() throws TException {
        try {
            log.info("Processing ping request");
            TPingRequest thriftRequest = new TPingRequest();
            thriftRequest.setMessage("ping");

            TPingResponse response = rpcService.sendRpcRequest(
                    topicsProperties.getPingRequest(),
                    topicsProperties.getPingResponse(),
                    thriftRequest,
                    TPingResponse.class);

            log.info("Received ping response: {}", response.getMessage());
            return new com.example.user.thrift.TPingResponse(ThriftConstants.STATUS_SUCCESS,
                    ThriftConstants.SUCCESS_PING,
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
            TUserCreateRequest thriftRequest = new TUserCreateRequest();
            thriftRequest.setName(request.getName());
            thriftRequest.setPhone(request.getPhone());
            thriftRequest.setAddress(request.getAddress() != null ? request.getAddress() : "");
            thriftRequest.setStatus(convertToKafkaUserStatus(request.getStatus()));
            thriftRequest.setRole(convertToKafkaUserRole(request.getRole()));

            TUserCreateResponse response = rpcService.sendRpcRequest(
                    topicsProperties.getUserCreateRequest(),
                    topicsProperties.getUserCreateResponse(),
                    thriftRequest,
                    TUserCreateResponse.class);

            if (response.getUser() != null) {
                TUser tUser = convertToTUser(response.getUser());
                log.info("Successfully created user: {}", tUser.getId());
                return new TCreateUserResponse(ThriftConstants.STATUS_SUCCESS, ThriftConstants.SUCCESS_USER_CREATED,
                        tUser);
            } else {
                log.warn("User creation failed - no user returned");
                return new TCreateUserResponse(ThriftConstants.STATUS_ERROR, "User creation failed", null);
            }
        } catch (Exception e) {
            log.error("Error during createUser: {}", e.getMessage(), e);
            throw new TException("User creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public TGetUserResponse getUser(TGetUserRequest request) throws TException {
        try {
            log.info("Processing getUser request for ID: {}", request.getId());
            TUserGetRequest thriftRequest = new TUserGetRequest();
            thriftRequest.setId(request.getId());

            TUserGetResponse response = rpcService.sendRpcRequest(
                    topicsProperties.getUserGetRequest(),
                    topicsProperties.getUserGetResponse(),
                    thriftRequest,
                    TUserGetResponse.class);

            if (response.isFound() && response.getUser() != null) {
                TUser tUser = convertToTUser(response.getUser());
                log.info("Successfully retrieved user: {}", tUser.getId());
                return new TGetUserResponse(ThriftConstants.STATUS_SUCCESS, ThriftConstants.SUCCESS_USERS_RETRIEVED,
                        tUser);
            } else {
                log.info("User not found: {}", request.getId());
                return new TGetUserResponse(ThriftConstants.STATUS_USER_NOT_FOUND, ThriftConstants.ERROR_USER_NOT_FOUND,
                        null);
            }
        } catch (Exception e) {
            log.error("Error during getUser: {}", e.getMessage(), e);
            throw new TException("Get user failed: " + e.getMessage(), e);
        }
    }

    @Override
    public TUpdateUserResponse updateUser(TUpdateUserRequest request) throws TException {
        try {
            log.info("Processing updateUser request for ID: {}", request.getId());
            TUserUpdateRequest thriftRequest = new TUserUpdateRequest();
            thriftRequest.setId(request.getId());
            thriftRequest.setName(request.getName());
            thriftRequest.setPhone(request.getPhone());
            thriftRequest.setAddress(request.getAddress());
            thriftRequest.setStatus(convertToKafkaUserStatus(request.getStatus()));
            thriftRequest.setRole(convertToKafkaUserRole(request.getRole()));
            thriftRequest.setVersion(request.getVersion());

            TUserUpdateResponse response = rpcService.sendRpcRequest(
                    topicsProperties.getUserUpdateRequest(),
                    topicsProperties.getUserUpdateResponse(),
                    thriftRequest,
                    TUserUpdateResponse.class);

            if (response.isSuccess() && response.getUser() != null) {
                TUser tUser = convertToTUser(response.getUser());
                log.info("Successfully updated user: {}", tUser.getId());
                return new TUpdateUserResponse(ThriftConstants.STATUS_SUCCESS, ThriftConstants.SUCCESS_USER_UPDATED,
                        tUser);
            } else {
                log.warn("User update failed for ID: {}", request.getId());
                return new TUpdateUserResponse(ThriftConstants.STATUS_USER_NOT_FOUND,
                        ThriftConstants.ERROR_USER_NOT_FOUND,
                        null);
            }
        } catch (Exception e) {
            log.error("Error during updateUser: {}", e.getMessage(), e);
            throw new TException("User update failed: " + e.getMessage(), e);
        }
    }

    @Override
    public TDeleteUserResponse deleteUser(TDeleteUserRequest request) throws TException {
        try {
            log.info("Processing deleteUser request for ID: {}", request.getId());
            TUserDeleteRequest thriftRequest = new TUserDeleteRequest();
            thriftRequest.setId(request.getId());

            TUserDeleteResponse response = rpcService.sendRpcRequest(
                    topicsProperties.getUserDeleteRequest(),
                    topicsProperties.getUserDeleteResponse(),
                    thriftRequest,
                    TUserDeleteResponse.class);

            if (response.isDeleted()) {
                log.info("Successfully deleted user: {}", request.getId());
                return new TDeleteUserResponse(ThriftConstants.STATUS_SUCCESS, ThriftConstants.SUCCESS_USER_DELETED);
            } else {
                log.warn("User deletion failed for ID: {}", request.getId());
                return new TDeleteUserResponse(ThriftConstants.STATUS_USER_NOT_FOUND,
                        ThriftConstants.ERROR_USER_NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("Error during deleteUser: {}", e.getMessage(), e);
            throw new TException("User deletion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public TListUsersResponse listUsers(TListUsersRequest request) throws TException {
        try {
            log.info("Processing listUsers request: page={}, size={}",
                    request.isSetPage() ? request.getPage() : "default",
                    request.isSetSize() ? request.getSize() : "default");

            TUserListRequest thriftRequest = new TUserListRequest();
            if (request.isSetPage()) {
                thriftRequest.setPage(request.getPage());
            }
            if (request.isSetSize()) {
                thriftRequest.setSize(request.getSize());
            }
            if (request.isSetSearch()) {
                thriftRequest.setSearch(request.getSearch());
            }
            if (request.isSetStatus()) {
                thriftRequest.setStatus(convertToKafkaUserStatus(request.getStatus()));
            }
            if (request.isSetRole()) {
                thriftRequest.setRole(convertToKafkaUserRole(request.getRole()));
            }
            if (request.isSetIncludeDeleted()) {
                thriftRequest.setIncludeDeleted(request.isIncludeDeleted());
            }

            TUserListResponse response = rpcService.sendRpcRequest(
                    topicsProperties.getUserListRequest(),
                    topicsProperties.getUserListResponse(),
                    thriftRequest,
                    TUserListResponse.class);

            List<TUser> users = response.getItems().stream()
                    .map(this::convertToTUser)
                    .toList();

            log.info("Successfully retrieved {} users", users.size());
            return new TListUsersResponse(
                    ThriftConstants.STATUS_SUCCESS,
                    ThriftConstants.SUCCESS_USERS_RETRIEVED,
                    users,
                    response.getPage(),
                    response.getSize(),
                    response.getTotal(),
                    response.getTotalPages());
        } catch (Exception e) {
            log.error("Error during listUsers: {}", e.getMessage(), e);
            throw new TException("List users failed: " + e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "ping.response", groupId = ThriftConstants.CONSUMER_GROUP_ID, containerFactory = "rpcListenerFactory")
    public void onPingResponse(ConsumerRecord<String, byte[]> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        TPingResponse response = ThriftKafkaMessageHandler.deserializeMessage(record, TPingResponse.class);
        log.debug("Received ping response with correlationId: {}", correlationId);
        rpcService.handleResponse(correlationId, response);
    }

    @KafkaListener(topics = "user.create.response", groupId = ThriftConstants.CONSUMER_GROUP_ID, containerFactory = "rpcListenerFactory")
    public void onCreateUserResponse(ConsumerRecord<String, byte[]> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        TUserCreateResponse response = ThriftKafkaMessageHandler.deserializeMessage(record, TUserCreateResponse.class);
        log.debug("Received create user response with correlationId: {}", correlationId);
        rpcService.handleResponse(correlationId, response);
    }

    @KafkaListener(topics = "user.get.response", groupId = ThriftConstants.CONSUMER_GROUP_ID, containerFactory = "rpcListenerFactory")
    public void onGetUserResponse(ConsumerRecord<String, byte[]> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        TUserGetResponse response = ThriftKafkaMessageHandler.deserializeMessage(record, TUserGetResponse.class);
        log.debug("Received get user response with correlationId: {}", correlationId);
        rpcService.handleResponse(correlationId, response);
    }

    @KafkaListener(topics = "user.update.response", groupId = ThriftConstants.CONSUMER_GROUP_ID, containerFactory = "rpcListenerFactory")
    public void onUpdateUserResponse(ConsumerRecord<String, byte[]> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        TUserUpdateResponse response = ThriftKafkaMessageHandler.deserializeMessage(record, TUserUpdateResponse.class);
        log.debug("Received update user response with correlationId: {}", correlationId);
        rpcService.handleResponse(correlationId, response);
    }

    @KafkaListener(topics = "user.delete.response", groupId = ThriftConstants.CONSUMER_GROUP_ID, containerFactory = "rpcListenerFactory")
    public void onDeleteUserResponse(ConsumerRecord<String, byte[]> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        TUserDeleteResponse response = ThriftKafkaMessageHandler.deserializeMessage(record, TUserDeleteResponse.class);
        log.debug("Received delete user response with correlationId: {}", correlationId);
        rpcService.handleResponse(correlationId, response);
    }

    @KafkaListener(topics = "user.list.response", groupId = ThriftConstants.CONSUMER_GROUP_ID, containerFactory = "rpcListenerFactory")
    public void onListUsersResponse(ConsumerRecord<String, byte[]> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        TUserListResponse response = ThriftKafkaMessageHandler.deserializeMessage(record, TUserListResponse.class);
        log.debug("Received list users response with correlationId: {}", correlationId);
        rpcService.handleResponse(correlationId, response);
    }
}