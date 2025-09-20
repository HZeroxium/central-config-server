package com.example.thriftserver.service;

import com.example.kafka.constants.KafkaConstants;
import com.example.kafka.dto.RpcRequest;
import com.example.kafka.dto.RpcResponse;
import com.example.kafka.avro.*;
import com.example.user.thrift.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.thrift.TException;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceRpcBridge implements UserService.Iface {

    private final ReplyingKafkaTemplate<String, String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private RpcResponse sendRpcRequest(String action, Object payload) throws TException {
        String correlationId = UUID.randomUUID().toString();

        // Convert Avro objects to JSON-compatible objects
        Object jsonCompatiblePayload = convertAvroToJsonCompatible(payload);
        RpcRequest request = RpcRequest.of(correlationId, action, jsonCompatiblePayload);

        try {
            log.debug("Sending RPC request: action={}, correlationId={}", action, correlationId);

            // Serialize the request to JSON string
            String requestJson = objectMapper.writeValueAsString(request);
            ProducerRecord<String, String> record = new ProducerRecord<>(KafkaConstants.TOPIC_USER_RPC_REQUEST,
                    correlationId, requestJson);
            record.headers().add(new RecordHeader(KafkaConstants.HEADER_CORRELATION_ID,
                    correlationId.getBytes(StandardCharsets.UTF_8)));

            RequestReplyFuture<String, String, String> replyFuture = kafkaTemplate.sendAndReceive(record);
            ConsumerRecord<String, String> consumerRecord = replyFuture.get(10, TimeUnit.SECONDS);

            // Deserialize the JSON string response
            String responseJson = consumerRecord.value();
            RpcResponse response = objectMapper.readValue(responseJson, RpcResponse.class);
            log.debug("Received RPC response: correlationId={}, status={}", correlationId, response.status());

            return response;

        } catch (Exception e) {
            log.error("Error during Kafka RPC for action {}: {}", action, e.getMessage(), e);
            throw new TException("Kafka RPC failed for " + action + ": " + e.getMessage(), e);
        }
    }

    @Override
    public TPingResponse ping() throws TException {
        RpcResponse response = sendRpcRequest("ping", null);
        if (!"ok".equals(response.status())) {
            throw new TException(response.error());
        }

        return new TPingResponse(0, "Service is healthy", (String) response.payload());
    }

    @Override
    public TCreateUserResponse createUser(TCreateUserRequest request) throws TException {
        UserCreateRequest userCreateRequest = new UserCreateRequest(
                request.getName(),
                request.getPhone(),
                request.getAddress() != null ? request.getAddress() : "",
                request.getStatus() != null ? UserStatus.valueOf(request.getStatus().name()) : UserStatus.ACTIVE,
                request.getRole() != null ? UserRole.valueOf(request.getRole().name()) : UserRole.USER);

        RpcResponse response = sendRpcRequest("createUser", userCreateRequest);
        if (!"ok".equals(response.status())) {
            throw new TException(response.error());
        }

        Map<String, Object> userMap = (Map<String, Object>) response.payload();

        TUser tUser = new TUser();
        tUser.setId((String) userMap.get("id"));
        tUser.setName((String) userMap.get("name"));
        tUser.setPhone((String) userMap.get("phone"));
        tUser.setAddress((String) userMap.get("address"));
        tUser.setStatus(TUserStatus.valueOf((String) userMap.get("status")));
        tUser.setRole(TUserRole.valueOf((String) userMap.get("role")));
        tUser.setCreatedAt(userMap.get("createdAt") != null ? ((Number) userMap.get("createdAt")).longValue() : 0L);
        tUser.setCreatedBy((String) userMap.get("createdBy"));
        tUser.setUpdatedAt(userMap.get("updatedAt") != null ? ((Number) userMap.get("updatedAt")).longValue() : 0L);
        tUser.setUpdatedBy((String) userMap.get("updatedBy"));
        tUser.setVersion(userMap.get("version") != null ? ((Number) userMap.get("version")).intValue() : 1);

        return new TCreateUserResponse(0, "User created successfully", tUser);
    }

    @Override
    public TGetUserResponse getUser(TGetUserRequest request) throws TException {
        UserGetRequest userGetRequest = new UserGetRequest(request.getId());

        RpcResponse response = sendRpcRequest("getUser", userGetRequest);
        if (!"ok".equals(response.status())) {
            return new TGetUserResponse(2, response.error(), null);
        }

        Map<String, Object> userMap = (Map<String, Object>) response.payload();

        TUser tUser = new TUser();
        tUser.setId((String) userMap.get("id"));
        tUser.setName((String) userMap.get("name"));
        tUser.setPhone((String) userMap.get("phone"));
        tUser.setAddress((String) userMap.get("address"));
        tUser.setStatus(TUserStatus.valueOf((String) userMap.get("status")));
        tUser.setRole(TUserRole.valueOf((String) userMap.get("role")));
        tUser.setCreatedAt(userMap.get("createdAt") != null ? ((Number) userMap.get("createdAt")).longValue() : 0L);
        tUser.setCreatedBy((String) userMap.get("createdBy"));
        tUser.setUpdatedAt(userMap.get("updatedAt") != null ? ((Number) userMap.get("updatedAt")).longValue() : 0L);
        tUser.setUpdatedBy((String) userMap.get("updatedBy"));
        tUser.setVersion(userMap.get("version") != null ? ((Number) userMap.get("version")).intValue() : 1);

        return new TGetUserResponse(0, "User retrieved successfully", tUser);
    }

    @Override
    public TUpdateUserResponse updateUser(TUpdateUserRequest request) throws TException {
        UserUpdateRequest userUpdateRequest = new UserUpdateRequest(
                request.getId(),
                request.getName(),
                request.getPhone(),
                request.getAddress() != null ? request.getAddress() : "",
                request.getStatus() != null ? UserStatus.valueOf(request.getStatus().name()) : UserStatus.ACTIVE,
                request.getRole() != null ? UserRole.valueOf(request.getRole().name()) : UserRole.USER,
                request.getVersion());

        RpcResponse response = sendRpcRequest("updateUser", userUpdateRequest);
        if (!"ok".equals(response.status())) {
            return new TUpdateUserResponse(1, response.error(), null);
        }

        Map<String, Object> userMap = (Map<String, Object>) response.payload();

        TUser tUser = new TUser();
        tUser.setId((String) userMap.get("id"));
        tUser.setName((String) userMap.get("name"));
        tUser.setPhone((String) userMap.get("phone"));
        tUser.setAddress((String) userMap.get("address"));
        tUser.setStatus(TUserStatus.valueOf((String) userMap.get("status")));
        tUser.setRole(TUserRole.valueOf((String) userMap.get("role")));
        tUser.setCreatedAt(userMap.get("createdAt") != null ? ((Number) userMap.get("createdAt")).longValue() : 0L);
        tUser.setCreatedBy((String) userMap.get("createdBy"));
        tUser.setUpdatedAt(userMap.get("updatedAt") != null ? ((Number) userMap.get("updatedAt")).longValue() : 0L);
        tUser.setUpdatedBy((String) userMap.get("updatedBy"));
        tUser.setVersion(userMap.get("version") != null ? ((Number) userMap.get("version")).intValue() : 1);

        return new TUpdateUserResponse(0, "User updated successfully", tUser);
    }

    @Override
    public TDeleteUserResponse deleteUser(TDeleteUserRequest request) throws TException {
        UserDeleteRequest userDeleteRequest = new UserDeleteRequest(request.getId());

        RpcResponse response = sendRpcRequest("deleteUser", userDeleteRequest);
        if (!"ok".equals(response.status())) {
            return new TDeleteUserResponse(1, response.error());
        }

        return new TDeleteUserResponse(0, "User deleted successfully");
    }

    @Override
    public TListUsersResponse listUsers(TListUsersRequest request) throws TException {
        UserListRequest userListRequest = new UserListRequest(
                request.getPage(),
                request.getSize(),
                request.getSearch(),
                request.getStatus() != null ? UserStatus.valueOf(request.getStatus().name()) : null,
                request.getRole() != null ? UserRole.valueOf(request.getRole().name()) : null,
                request.isIncludeDeleted());

        RpcResponse response = sendRpcRequest("listUsers", userListRequest);
        if (!"ok".equals(response.status())) {
            throw new TException(response.error());
        }

        Map<String, Object> resultMap = (Map<String, Object>) response.payload();
        List<Map<String, Object>> items = (List<Map<String, Object>>) resultMap.get("items");

        List<TUser> tUsers = items.stream().map(userMap -> {
            TUser tUser = new TUser();
            tUser.setId((String) userMap.get("id"));
            tUser.setName((String) userMap.get("name"));
            tUser.setPhone((String) userMap.get("phone"));
            tUser.setAddress((String) userMap.get("address"));
            tUser.setStatus(TUserStatus.valueOf((String) userMap.get("status")));
            tUser.setRole(TUserRole.valueOf((String) userMap.get("role")));
            tUser.setCreatedAt(userMap.get("createdAt") != null ? ((Number) userMap.get("createdAt")).longValue() : 0L);
            tUser.setCreatedBy((String) userMap.get("createdBy"));
            tUser.setUpdatedAt(userMap.get("updatedAt") != null ? ((Number) userMap.get("updatedAt")).longValue() : 0L);
            tUser.setUpdatedBy((String) userMap.get("updatedBy"));
            tUser.setVersion(userMap.get("version") != null ? ((Number) userMap.get("version")).intValue() : 1);
            return tUser;
        }).toList();

        return new TListUsersResponse(
                0,
                "Users retrieved successfully",
                tUsers,
                ((Number) resultMap.get("page")).intValue(),
                ((Number) resultMap.get("size")).intValue(),
                ((Number) resultMap.get("total")).longValue(),
                ((Number) resultMap.get("totalPages")).intValue());
    }

    private Object convertAvroToJsonCompatible(Object payload) {
        if (payload == null) {
            return null;
        }

        if (payload instanceof UserCreateRequest) {
            UserCreateRequest request = (UserCreateRequest) payload;
            Map<String, Object> map = new HashMap<>();
            map.put("name", request.getName());
            map.put("phone", request.getPhone());
            map.put("address", request.getAddress());
            map.put("status", request.getStatus() != null ? request.getStatus().name() : null);
            map.put("role", request.getRole() != null ? request.getRole().name() : null);
            return map;
        } else if (payload instanceof UserGetRequest) {
            UserGetRequest request = (UserGetRequest) payload;
            Map<String, Object> map = new HashMap<>();
            map.put("id", request.getId());
            return map;
        } else if (payload instanceof UserUpdateRequest) {
            UserUpdateRequest request = (UserUpdateRequest) payload;
            Map<String, Object> map = new HashMap<>();
            map.put("id", request.getId());
            map.put("name", request.getName());
            map.put("phone", request.getPhone());
            map.put("address", request.getAddress());
            map.put("status", request.getStatus() != null ? request.getStatus().name() : null);
            map.put("role", request.getRole() != null ? request.getRole().name() : null);
            map.put("version", request.getVersion());
            return map;
        } else if (payload instanceof UserDeleteRequest) {
            UserDeleteRequest request = (UserDeleteRequest) payload;
            Map<String, Object> map = new HashMap<>();
            map.put("id", request.getId());
            return map;
        } else if (payload instanceof UserListRequest) {
            UserListRequest request = (UserListRequest) payload;
            Map<String, Object> map = new HashMap<>();
            map.put("page", request.getPage());
            map.put("size", request.getSize());
            map.put("search", request.getSearch());
            map.put("status", request.getStatus() != null ? request.getStatus().name() : null);
            map.put("role", request.getRole() != null ? request.getRole().name() : null);
            map.put("includeDeleted", request.getIncludeDeleted());
            return map;
        }

        return payload;
    }
}