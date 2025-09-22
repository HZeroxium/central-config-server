package com.example.watcher.service;

import com.example.kafka.thrift.*;
import com.example.kafka.util.ThriftKafkaMessageHandler;
import com.example.watcher.config.KafkaTopicsProperties;
import com.example.user.service.port.UserServicePort;
import com.example.common.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

/**
 * Service for consuming async commands and publishing events
 * Handles V2 async operation processing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncCommandConsumerService {

    private final UserServicePort userService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties topicsProperties;

    @KafkaListener(topics = "user.commands", groupId = "user-watcher-async", containerFactory = "kafkaListenerContainerFactory")
    public void handleUserCommand(ConsumerRecord<String, byte[]> record) {
        String correlationId = new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value());
        TUserCommand command = ThriftKafkaMessageHandler.deserializeMessage(record, TUserCommand.class);
        log.info("Processing command - Type: {}, Operation ID: {} with correlationId: {}",
                command.getCommandType(), command.getOperationId(), correlationId);

        try {
            switch (command.getCommandType()) {
                case CREATE_USER -> handleCreateUserCommand(command);
                case UPDATE_USER -> handleUpdateUserCommand(command);
                case DELETE_USER -> handleDeleteUserCommand(command);
                default -> {
                    log.warn("Unknown command type: {}", command.getCommandType());
                    publishFailedEvent(command, "Unknown command type", "UNKNOWN_COMMAND_TYPE");
                }
            }
        } catch (Exception e) {
            log.error("Failed to process command - Operation ID: {}", command.getOperationId(), e);
            publishFailedEvent(command, "Command processing failed: " + e.getMessage(), "COMMAND_PROCESSING_ERROR");
        }
    }

    private void handleCreateUserCommand(TUserCommand command) {
        log.info("Handling CREATE command - Operation ID: {}", command.getOperationId());

        try {
            if (!command.isSetCreateRequest()) {
                throw new IllegalArgumentException("Create request is missing");
            }

            TUserCreateRequest createRequest = command.getCreateRequest();
            // Create User domain object
            User newUser = User.builder()
                    .name(createRequest.getName())
                    .phone(createRequest.getPhone())
                    .address(createRequest.getAddress())
                    .status(convertToUserStatus(createRequest.getStatus()))
                    .role(convertToUserRole(createRequest.getRole()))
                    .build();

            var user = userService.create(newUser);

            // Convert to TUserResponse
            TUserResponse userResponse = convertToTUserResponse(user);

            // Publish success event
            publishUserCreatedEvent(command, userResponse);
            log.info("Successfully processed CREATE command - Operation ID: {}, User ID: {}",
                    command.getOperationId(), user.getId());

        } catch (Exception e) {
            log.error("Failed to handle CREATE command - Operation ID: {}", command.getOperationId(), e);
            publishFailedEvent(command, "User creation failed: " + e.getMessage(), "USER_CREATION_ERROR");
        }
    }

    private void handleUpdateUserCommand(TUserCommand command) {
        log.info("Handling UPDATE command - Operation ID: {}", command.getOperationId());

        try {
            if (!command.isSetUpdateRequest()) {
                throw new IllegalArgumentException("Update request is missing");
            }

            TUserUpdateRequest updateRequest = command.getUpdateRequest();
            // First get existing user
            var existingUserOpt = userService.getById(updateRequest.getId());
            if (existingUserOpt.isEmpty()) {
                throw new IllegalArgumentException("User not found: " + updateRequest.getId());
            }

            // Create updated User domain object
            User updateUser = existingUserOpt.get().toBuilder()
                    .name(updateRequest.getName())
                    .phone(updateRequest.getPhone())
                    .address(updateRequest.getAddress())
                    .status(convertToUserStatus(updateRequest.getStatus()))
                    .role(convertToUserRole(updateRequest.getRole()))
                    .version(updateRequest.getVersion())
                    .build();

            var user = userService.update(updateUser);
            var userOpt = Optional.of(user);

            if (userOpt.isPresent()) {
                // Convert to TUserResponse
                TUserResponse userResponse = convertToTUserResponse(userOpt.get());

                // Publish success event
                publishUserUpdatedEvent(command, userResponse);
                log.info("Successfully processed UPDATE command - Operation ID: {}, User ID: {}",
                        command.getOperationId(), userOpt.get().getId());
            } else {
                publishFailedEvent(command, "User not found for update", "USER_NOT_FOUND");
            }

        } catch (Exception e) {
            log.error("Failed to handle UPDATE command - Operation ID: {}", command.getOperationId(), e);
            publishFailedEvent(command, "User update failed: " + e.getMessage(), "USER_UPDATE_ERROR");
        }
    }

    private void handleDeleteUserCommand(TUserCommand command) {
        log.info("Handling DELETE command - Operation ID: {}", command.getOperationId());

        try {
            if (!command.isSetDeleteRequest()) {
                throw new IllegalArgumentException("Delete request is missing");
            }

            TUserDeleteRequest deleteRequest = command.getDeleteRequest();
            userService.delete(deleteRequest.getId());
            boolean deleted = true; // If no exception, deletion succeeded

            if (deleted) {
                // Publish success event
                publishUserDeletedEvent(command);
                log.info("Successfully processed DELETE command - Operation ID: {}, User ID: {}",
                        command.getOperationId(), deleteRequest.getId());
            } else {
                publishFailedEvent(command, "User not found for deletion", "USER_NOT_FOUND");
            }

        } catch (Exception e) {
            log.error("Failed to handle DELETE command - Operation ID: {}", command.getOperationId(), e);
            publishFailedEvent(command, "User deletion failed: " + e.getMessage(), "USER_DELETION_ERROR");
        }
    }

    private void publishUserCreatedEvent(TUserCommand command, TUserResponse user) {
        TUserEvent event = new TUserEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(TEventType.USER_CREATED);
        event.setOperationId(command.getOperationId());
        event.setCorrelationId(command.getCorrelationId());
        event.setTimestamp(System.currentTimeMillis());
        event.setUser(user);

        publishEvent(event);
    }

    private void publishUserUpdatedEvent(TUserCommand command, TUserResponse user) {
        TUserEvent event = new TUserEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(TEventType.USER_UPDATED);
        event.setOperationId(command.getOperationId());
        event.setCorrelationId(command.getCorrelationId());
        event.setTimestamp(System.currentTimeMillis());
        event.setUser(user);

        publishEvent(event);
    }

    private void publishUserDeletedEvent(TUserCommand command) {
        TUserEvent event = new TUserEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(TEventType.USER_DELETED);
        event.setOperationId(command.getOperationId());
        event.setCorrelationId(command.getCorrelationId());
        event.setTimestamp(System.currentTimeMillis());

        publishEvent(event);
    }

    private void publishFailedEvent(TUserCommand command, String errorMessage, String errorCode) {
        TUserEvent event = new TUserEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(TEventType.USER_OPERATION_FAILED);
        event.setOperationId(command.getOperationId());
        event.setCorrelationId(command.getCorrelationId());
        event.setTimestamp(System.currentTimeMillis());
        event.setErrorMessage(errorMessage);
        event.setErrorCode(errorCode);

        publishEvent(event);
    }

    private void publishEvent(TUserEvent event) {
        sendMessage(topicsProperties.getUserEvents(), event.getEventId(), event, event.getCorrelationId());
        log.debug("Published event {} for operationId: {}", event.getEventType(), event.getOperationId());
    }

    private void sendMessage(String topic, String key, Object message, String correlationId) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, message);
        record.headers().add(KafkaHeaders.CORRELATION_ID, correlationId.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record);
    }

    private TUserResponse convertToTUserResponse(com.example.common.domain.User user) {
        TUserResponse response = new TUserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setPhone(user.getPhone());
        response.setAddress(user.getAddress());
        response.setStatus(convertToKafkaUserStatus(user.getStatus()));
        response.setRole(convertToKafkaUserRole(user.getRole()));

        if (user.getCreatedAt() != null) {
            response.setCreatedAt(user.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        if (user.getCreatedBy() != null) {
            response.setCreatedBy(user.getCreatedBy());
        }
        if (user.getUpdatedAt() != null) {
            response.setUpdatedAt(user.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        if (user.getUpdatedBy() != null) {
            response.setUpdatedBy(user.getUpdatedBy());
        }
        if (user.getVersion() != null) {
            response.setVersion(user.getVersion());
        }
        response.setDeleted(user.getDeleted() != null ? user.getDeleted() : false);
        if (user.getDeletedAt() != null) {
            response.setDeletedAt(user.getDeletedAt().toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        if (user.getDeletedBy() != null) {
            response.setDeletedBy(user.getDeletedBy());
        }

        return response;
    }

    private com.example.kafka.thrift.TUserStatus convertToKafkaUserStatus(
            com.example.common.domain.User.UserStatus status) {
        return switch (status) {
            case ACTIVE -> com.example.kafka.thrift.TUserStatus.ACTIVE;
            case INACTIVE -> com.example.kafka.thrift.TUserStatus.INACTIVE;
            case SUSPENDED -> com.example.kafka.thrift.TUserStatus.SUSPENDED;
        };
    }

    private com.example.kafka.thrift.TUserRole convertToKafkaUserRole(com.example.common.domain.User.UserRole role) {
        return switch (role) {
            case ADMIN -> com.example.kafka.thrift.TUserRole.ADMIN;
            case USER -> com.example.kafka.thrift.TUserRole.USER;
            case MODERATOR -> com.example.kafka.thrift.TUserRole.MODERATOR;
            case GUEST -> com.example.kafka.thrift.TUserRole.GUEST;
        };
    }

    // Convert from Thrift to domain enums
    private com.example.common.domain.User.UserStatus convertToUserStatus(com.example.kafka.thrift.TUserStatus status) {
        return switch (status) {
            case ACTIVE -> com.example.common.domain.User.UserStatus.ACTIVE;
            case INACTIVE -> com.example.common.domain.User.UserStatus.INACTIVE;
            case SUSPENDED -> com.example.common.domain.User.UserStatus.SUSPENDED;
        };
    }

    private com.example.common.domain.User.UserRole convertToUserRole(com.example.kafka.thrift.TUserRole role) {
        return switch (role) {
            case ADMIN -> com.example.common.domain.User.UserRole.ADMIN;
            case USER -> com.example.common.domain.User.UserRole.USER;
            case MODERATOR -> com.example.common.domain.User.UserRole.MODERATOR;
            case GUEST -> com.example.common.domain.User.UserRole.GUEST;
        };
    }
}
