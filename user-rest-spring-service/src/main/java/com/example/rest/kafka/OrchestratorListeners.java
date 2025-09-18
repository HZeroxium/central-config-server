package com.example.rest.kafka;

import com.example.kafka.model.SagaEvent;
import com.example.kafka.service.SagaStateRepository;
import com.example.kafka.util.KafkaHeadersUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
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
    @KafkaListener(topics = "user.update.phase_1.event", 
                   containerFactory = "txFactory", 
                   groupId = "user-update-orchestrator")
    public void onPhase1Event(@org.springframework.messaging.handler.annotation.Payload String event,
                             @org.springframework.messaging.handler.annotation.Header("sagaId") String sagaId) {
        try {
            log.info("Received Phase 1 event for saga {}: {}", sagaId, event);
            
            if (event.contains("Failed")) {
                sagaStateRepository.update(sagaId, "FAILED", "Phase 1 failed", Map.of("phase", 1));
                log.warn("Saga {} failed at phase 1", sagaId);
                return;
            }
            
            sagaStateRepository.updatePhase(sagaId, 1);
            sendPhaseCommand("user.update.phase_2.command", sagaId, "UserUpdatePhase2Command");
            log.info("Phase 2 command sent for saga {}", sagaId);
            
        } catch (Exception e) {
            log.error("Error processing Phase 1 event for saga {}", sagaId, e);
        }
    }

    /**
     * Handle Phase 2 completion event
     */
    @KafkaListener(topics = "user.update.phase_2.event", 
                   containerFactory = "txFactory", 
                   groupId = "user-update-orchestrator")
    public void onPhase2Event(@org.springframework.messaging.handler.annotation.Payload String event,
                             @org.springframework.messaging.handler.annotation.Header("sagaId") String sagaId) {
        try {
            log.info("Received Phase 2 event for saga {}: {}", sagaId, event);
            
            if (event.contains("Failed")) {
                sagaStateRepository.update(sagaId, "FAILED", "Phase 2 failed", Map.of("phase", 2));
                log.warn("Saga {} failed at phase 2", sagaId);
                return;
            }
            
            sagaStateRepository.updatePhase(sagaId, 2);
            sendPhaseCommand("user.update.phase_3.command", sagaId, "UserUpdatePhase3Command");
            log.info("Phase 3 command sent for saga {}", sagaId);
            
        } catch (Exception e) {
            log.error("Error processing Phase 2 event for saga {}", sagaId, e);
        }
    }

    /**
     * Handle Phase 3 completion event
     */
    @KafkaListener(topics = "user.update.phase_3.event", 
                   containerFactory = "txFactory", 
                   groupId = "user-update-orchestrator")
    public void onPhase3Event(@org.springframework.messaging.handler.annotation.Payload String event,
                             @org.springframework.messaging.handler.annotation.Header("sagaId") String sagaId) {
        try {
            log.info("Received Phase 3 event for saga {}: {}", sagaId, event);
            
            if (event.contains("Failed")) {
                sagaStateRepository.update(sagaId, "FAILED", "Phase 3 failed", Map.of("phase", 3));
                log.warn("Saga {} failed at phase 3", sagaId);
                return;
            }
            
            sagaStateRepository.updatePhase(sagaId, 3);
            sendPhaseCommand("user.update.phase_4.command", sagaId, "UserUpdatePhase4Command");
            log.info("Phase 4 command sent for saga {}", sagaId);
            
        } catch (Exception e) {
            log.error("Error processing Phase 3 event for saga {}", sagaId, e);
        }
    }

    /**
     * Handle Phase 4 completion event
     */
    @KafkaListener(topics = "user.update.phase_4.event", 
                   containerFactory = "txFactory", 
                   groupId = "user-update-orchestrator")
    public void onPhase4Event(@org.springframework.messaging.handler.annotation.Payload String event,
                             @org.springframework.messaging.handler.annotation.Header("sagaId") String sagaId) {
        try {
            log.info("Received Phase 4 event for saga {}: {}", sagaId, event);
            
            if (event.contains("Failed")) {
                sagaStateRepository.update(sagaId, "FAILED", "Phase 4 failed", Map.of("phase", 4));
                log.warn("Saga {} failed at phase 4", sagaId);
                return;
            }
            
            sagaStateRepository.updatePhase(sagaId, 4);
            sagaStateRepository.update(sagaId, "DONE", null, Map.of("phase", 4, "completed", true));
            log.info("Saga {} completed successfully", sagaId);
            
        } catch (Exception e) {
            log.error("Error processing Phase 4 event for saga {}", sagaId, e);
        }
    }

    /**
     * Send command for next phase
     */
    private void sendPhaseCommand(String topic, String sagaId, String commandType) {
        try {
            SagaEvent command = SagaEvent.command(commandType, sagaId, sagaId, Map.of());
            
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, sagaId, objectMapper.writeValueAsString(command));
            KafkaHeadersUtil.addStandardHeaders(record, sagaId, sagaId, command.getCausationId(), 
                                              command.getEventId(), "thrift-client-service", command.getType());
            
            kafkaTemplate.send(record);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize command {} for saga {}", commandType, sagaId, e);
            throw new RuntimeException("Failed to serialize command", e);
        } catch (Exception e) {
            log.error("Failed to send command {} for saga {}", commandType, sagaId, e);
            throw new RuntimeException("Failed to send command", e);
        }
    }
}
