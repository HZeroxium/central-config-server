package com.example.rest.kafka;

import com.example.kafka.constants.KafkaConstants;
import com.example.kafka.dto.PhaseCommand;
import com.example.kafka.service.SagaStateRepository;
import com.example.kafka.util.KafkaHeadersUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka Listeners for Orchestrator
 * Handles events from workers and triggers next phase commands
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrchestratorListeners {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SagaStateRepository sagaStateRepository;
    private final ObjectMapper objectMapper;

    /**
     * Handle Phase 1 completion event
     */
    @KafkaListener(topics = KafkaConstants.TOPIC_USER_UPDATE_PHASE_1_EVENT, containerFactory = KafkaConstants.CONTAINER_FACTORY_TX, groupId = KafkaConstants.GROUP_USER_UPDATE_ORCHESTRATOR)
    public void onPhase1Event(@Payload String event,
            @Header(KafkaConstants.HEADER_SAGA_ID) String sagaId) {
        try {
            log.info("Received Phase 1 event for saga {}: {}", sagaId, event);

            if (event.contains("Failed")) {
                sagaStateRepository.update(sagaId, KafkaConstants.SAGA_STATE_FAILED,
                        KafkaConstants.ERROR_MSG_PHASE_1_FAILED, Map.of("phase", 1));
                log.warn("Saga {} failed at phase 1", sagaId);
                return;
            }

            sagaStateRepository.updatePhase(sagaId, 1);
            PhaseCommand command = new PhaseCommand(
                    sagaId,
                    "phase2",
                    KafkaConstants.COMMAND_TYPE_PHASE_2_COMMAND,
                    Map.of("phase", 2, "action", "enrichment"),
                    sagaId, // correlationId
                    sagaId, // causationId
                    null, // eventId
                    KafkaConstants.SOURCE_ORCHESTRATOR,
                    System.currentTimeMillis());
            kafkaTemplate.executeInTransaction(kt -> {
                sendPhaseCommand(KafkaConstants.TOPIC_USER_UPDATE_PHASE_2_COMMAND, command);
                return null;
            });
            log.info("Phase 2 command sent for saga {}", sagaId);

        } catch (Exception e) {
            log.error("Error processing Phase 1 event for saga {}", sagaId, e);
        }
    }

    /**
     * Handle Phase 2 completion event
     */
    @KafkaListener(topics = KafkaConstants.TOPIC_USER_UPDATE_PHASE_2_EVENT, containerFactory = KafkaConstants.CONTAINER_FACTORY_TX, groupId = KafkaConstants.GROUP_USER_UPDATE_ORCHESTRATOR)
    public void onPhase2Event(@Payload String event,
            @Header(KafkaConstants.HEADER_SAGA_ID) String sagaId) {
        try {
            log.info("Received Phase 2 event for saga {}: {}", sagaId, event);

            if (event.contains("Failed")) {
                sagaStateRepository.update(sagaId, KafkaConstants.SAGA_STATE_FAILED,
                        KafkaConstants.ERROR_MSG_PHASE_2_FAILED, Map.of("phase", 2));
                log.warn("Saga {} failed at phase 2", sagaId);
                return;
            }

            sagaStateRepository.updatePhase(sagaId, 2);
            PhaseCommand command = new PhaseCommand(
                    sagaId,
                    "phase3",
                    KafkaConstants.COMMAND_TYPE_PHASE_3_COMMAND,
                    Map.of("phase", 3, "action", "persistence"),
                    sagaId, // correlationId
                    sagaId, // causationId
                    null, // eventId
                    KafkaConstants.SOURCE_ORCHESTRATOR,
                    System.currentTimeMillis());
            kafkaTemplate.executeInTransaction(kt -> {
                sendPhaseCommand(KafkaConstants.TOPIC_USER_UPDATE_PHASE_3_COMMAND, command);
                return null;
            });
            log.info("Phase 3 command sent for saga {}", sagaId);

        } catch (Exception e) {
            log.error("Error processing Phase 2 event for saga {}", sagaId, e);
        }
    }

    /**
     * Handle Phase 3 completion event
     */
    @KafkaListener(topics = KafkaConstants.TOPIC_USER_UPDATE_PHASE_3_EVENT, containerFactory = KafkaConstants.CONTAINER_FACTORY_TX, groupId = KafkaConstants.GROUP_USER_UPDATE_ORCHESTRATOR)
    public void onPhase3Event(@Payload String event,
            @Header(KafkaConstants.HEADER_SAGA_ID) String sagaId) {
        try {
            log.info("Received Phase 3 event for saga {}: {}", sagaId, event);

            if (event.contains("Failed")) {
                sagaStateRepository.update(sagaId, KafkaConstants.SAGA_STATE_FAILED,
                        KafkaConstants.ERROR_MSG_PHASE_3_FAILED, Map.of("phase", 3));
                log.warn("Saga {} failed at phase 3", sagaId);
                return;
            }

            sagaStateRepository.updatePhase(sagaId, 3);
            PhaseCommand command = new PhaseCommand(
                    sagaId,
                    "phase4",
                    KafkaConstants.COMMAND_TYPE_PHASE_4_COMMAND,
                    Map.of("phase", 4, "action", "notification"),
                    sagaId, // correlationId
                    sagaId, // causationId
                    null, // eventId
                    KafkaConstants.SOURCE_ORCHESTRATOR,
                    System.currentTimeMillis());
            kafkaTemplate.executeInTransaction(kt -> {
                sendPhaseCommand(KafkaConstants.TOPIC_USER_UPDATE_PHASE_4_COMMAND, command);
                return null;
            });
            log.info("Phase 4 command sent for saga {}", sagaId);

        } catch (Exception e) {
            log.error("Error processing Phase 3 event for saga {}", sagaId, e);
        }
    }

    /**
     * Handle Phase 4 completion event
     */
    @KafkaListener(topics = KafkaConstants.TOPIC_USER_UPDATE_PHASE_4_EVENT, containerFactory = KafkaConstants.CONTAINER_FACTORY_TX, groupId = KafkaConstants.GROUP_USER_UPDATE_ORCHESTRATOR)
    public void onPhase4Event(@Payload String event,
            @Header(KafkaConstants.HEADER_SAGA_ID) String sagaId) {
        try {
            log.info("Received Phase 4 event for saga {}: {}", sagaId, event);

            if (event.contains("Failed")) {
                sagaStateRepository.update(sagaId, KafkaConstants.SAGA_STATE_FAILED,
                        KafkaConstants.ERROR_MSG_PHASE_4_FAILED, Map.of("phase", 4));
                log.warn("Saga {} failed at phase 4", sagaId);
                return;
            }

            sagaStateRepository.updatePhase(sagaId, 4);
            sagaStateRepository.update(sagaId, KafkaConstants.SAGA_STATE_COMPLETED, null,
                    Map.of("phase", 4, "completed", true));
            log.info("Saga {} completed successfully", sagaId);

        } catch (Exception e) {
            log.error("Error processing Phase 4 event for saga {}", sagaId, e);
        }
    }

    /**
     * Send command for next phase using DTO
     */
    private void sendPhaseCommand(String topic, PhaseCommand command) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, command.sagaId(),
                    objectMapper.writeValueAsString(command));
            KafkaHeadersUtil.addStandardHeaders(record, command.sagaId(), command.correlationId(),
                    command.causationId(),
                    command.eventId(), command.source(), command.commandType());

            kafkaTemplate.send(record);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize command {} for saga {}", command.commandType(), command.sagaId(), e);
            throw new RuntimeException("Failed to serialize command", e);
        } catch (Exception e) {
            log.error("Failed to send command {} for saga {}", command.commandType(), command.sagaId(), e);
            throw new RuntimeException("Failed to send command", e);
        }
    }
}
