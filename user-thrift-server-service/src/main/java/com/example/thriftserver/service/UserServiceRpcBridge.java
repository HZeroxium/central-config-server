package com.example.thriftserver.service;

import com.example.kafka.constants.KafkaConstants;
import com.example.kafka.dto.RpcRequest;
import com.example.kafka.dto.RpcResponse;
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
        RpcRequest request = new RpcRequest(correlationId, action, payload);

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
        Map<String, Object> userData = Map.of(
                "name", request.getName(),
                "phone", request.getPhone(),
                "address", request.getAddress() != null ? request.getAddress() : "",
                "status", request.getStatus() != null ? request.getStatus().name() : "ACTIVE",
                "role", request.getRole() != null ? request.getRole().name() : "USER");

        RpcResponse response = sendRpcRequest("createUser", userData);
        if (!"ok".equals(response.status())) {
            throw new TException(response.error());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> createdUser = (Map<String, Object>) response.payload();

        TUser tUser = new TUser();
        tUser.setId((String) createdUser.get("id"));
        tUser.setName((String) createdUser.get("name"));
        tUser.setPhone((String) createdUser.get("phone"));
        tUser.setAddress((String) createdUser.get("address"));
        tUser.setStatus(TUserStatus.valueOf((String) createdUser.get("status")));
        tUser.setRole(TUserRole.valueOf((String) createdUser.get("role")));
        tUser.setCreatedAt(0L); // Set default timestamp
        tUser.setCreatedBy((String) createdUser.get("createdBy"));
        tUser.setUpdatedAt(0L); // Set default timestamp
        tUser.setUpdatedBy((String) createdUser.get("updatedBy"));
        tUser.setVersion((Integer) createdUser.get("version"));

        return new TCreateUserResponse(0, "User created successfully", tUser);
    }

    @Override
    public TGetUserResponse getUser(TGetUserRequest request) throws TException {
        Map<String, Object> requestData = Map.of("id", request.getId());

        RpcResponse response = sendRpcRequest("getUser", requestData);
        if (!"ok".equals(response.status())) {
            return new TGetUserResponse(2, response.error(), null);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) response.payload();

        TUser tUser = new TUser();
        tUser.setId((String) user.get("id"));
        tUser.setName((String) user.get("name"));
        tUser.setPhone((String) user.get("phone"));
        tUser.setAddress((String) user.get("address"));
        tUser.setStatus(TUserStatus.valueOf((String) user.get("status")));
        tUser.setRole(TUserRole.valueOf((String) user.get("role")));
        tUser.setCreatedAt(0L); // Set default timestamp
        tUser.setCreatedBy((String) user.get("createdBy"));
        tUser.setUpdatedAt(0L); // Set default timestamp
        tUser.setUpdatedBy((String) user.get("updatedBy"));
        tUser.setVersion((Integer) user.get("version"));

        return new TGetUserResponse(0, "User retrieved successfully", tUser);
    }

    @Override
    public TUpdateUserResponse updateUser(TUpdateUserRequest request) throws TException {
        Map<String, Object> requestData = Map.of(
                "id", request.getId(),
                "name", request.getName(),
                "phone", request.getPhone(),
                "address", request.getAddress() != null ? request.getAddress() : "",
                "status", request.getStatus() != null ? request.getStatus().name() : "ACTIVE",
                "role", request.getRole() != null ? request.getRole().name() : "USER",
                "version", request.getVersion());

        RpcResponse response = sendRpcRequest("updateUser", requestData);
        if (!"ok".equals(response.status())) {
            return new TUpdateUserResponse(1, response.error(), null);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) response.payload();

        TUser tUser = new TUser();
        tUser.setId((String) user.get("id"));
        tUser.setName((String) user.get("name"));
        tUser.setPhone((String) user.get("phone"));
        tUser.setAddress((String) user.get("address"));
        tUser.setStatus(TUserStatus.valueOf((String) user.get("status")));
        tUser.setRole(TUserRole.valueOf((String) user.get("role")));
        tUser.setCreatedAt(0L); // Set default timestamp
        tUser.setCreatedBy((String) user.get("createdBy"));
        tUser.setUpdatedAt(0L); // Set default timestamp
        tUser.setUpdatedBy((String) user.get("updatedBy"));
        tUser.setVersion((Integer) user.get("version"));

        return new TUpdateUserResponse(0, "User updated successfully", tUser);
    }

    @Override
    public TDeleteUserResponse deleteUser(TDeleteUserRequest request) throws TException {
        Map<String, Object> requestData = Map.of("id", request.getId());

        RpcResponse response = sendRpcRequest("deleteUser", requestData);
        if (!"ok".equals(response.status())) {
            return new TDeleteUserResponse(1, response.error());
        }

        return new TDeleteUserResponse(0, "User deleted successfully");
    }

    @Override
    public TListUsersResponse listUsers(TListUsersRequest request) throws TException {
        Map<String, Object> requestData = new java.util.HashMap<>();
        requestData.put("page", request.getPage());
        requestData.put("size", request.getSize());
        requestData.put("search", request.getSearch() != null ? request.getSearch() : "");
        if (request.getStatus() != null) {
            requestData.put("status", request.getStatus().name());
        }
        if (request.getRole() != null) {
            requestData.put("role", request.getRole().name());
        }
        requestData.put("includeDeleted", request.isIncludeDeleted());

        RpcResponse response = sendRpcRequest("listUsers", requestData);
        if (!"ok".equals(response.status())) {
            throw new TException(response.error());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.payload();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");

        List<TUser> tUsers = items.stream().map(user -> {
            TUser tUser = new TUser();
            tUser.setId((String) user.get("id"));
            tUser.setName((String) user.get("name"));
            tUser.setPhone((String) user.get("phone"));
            tUser.setAddress((String) user.get("address"));
            tUser.setStatus(TUserStatus.valueOf((String) user.get("status")));
            tUser.setRole(TUserRole.valueOf((String) user.get("role")));
            tUser.setCreatedAt(0L); // Set default timestamp
            tUser.setCreatedBy((String) user.get("createdBy"));
            tUser.setUpdatedAt(0L); // Set default timestamp
            tUser.setUpdatedBy((String) user.get("updatedBy"));
            tUser.setVersion((Integer) user.get("version"));
            return tUser;
        }).toList();

        return new TListUsersResponse(
                0,
                "Users retrieved successfully",
                tUsers,
                (Integer) result.get("page"),
                (Integer) result.get("size"),
                ((Number) result.get("total")).longValue(),
                (Integer) result.get("totalPages"));
    }
}